import application.Application;
import application.Chartable;
import application.sink.LogSink;
import application.source.BulkSource;
import application.source.IoTSource;
import general.*;
import general.Utility.NotImplementedException;
import general.logger.ConsoleLogger;
import general.logger.FileLogger;
import picocli.CommandLine;

import java.io.*;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.nio.file.Files;
import java.util.Date;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "Client", description = "Starts a client which connects to the instruction server.")
public class Client implements Callable<Integer> {
    @CommandLine.Parameters(index = "0", description = "Ip address (of the 1st NIC) of the server. If additionally '--data-ip' is supplied this address will only be used to communicate control messages. Useful if both client and server have 2 NICs and the real stress-test shall only be performed on one of the two.")
    private String controlAddress;
    @CommandLine.Parameters(index = "1", description = "port to connect to")
    private int port;
    @CommandLine.Parameters(index = "2", description = "the application mode: ${COMPLETION-CANDIDATES}")
    private Application.Mode mode;
    @CommandLine.Parameters(index = "3", description = "the direction in which the data will flow (respective from the Client point of view): ${COMPLETION-CANDIDATES}")
    private Application.Direction direction;

    @CommandLine.Option(names = {"--data-ip"}, description = "Ip address (of the 2nd NIC) of the server. When specified this ip will be used to route the data packages which will test the network link. the 'controlAddress' is used to communicate control messages over the more reliable / not-to-test link.")
    private String dataAddress;

    @CommandLine.ArgGroup(multiplicity = "1")
    TimeSyncArg timeSyncArgs;

    static class TimeSyncArg {
        @CommandLine.Option(names = "--ntp", defaultValue = "ptbtime1.ptb.de", description = "Address of a ntp server to sync time. Caution: When using different 'controlAddress' and 'dataAddress' the ntp packets will use the default routing no matter the specified 'controlAddress'")
        private String ntpAddress;
        @CommandLine.Option(names = "--distributedTime", defaultValue = "false", description = "Sync time in a local distributed manner")
        private boolean distributedTime;
    }

    @CommandLine.Option(names = {"-r", "--resetTime"}, description = "time after the app gets forcefully reset in milliseconds")
    private int resetTime = -1;
    @CommandLine.Option(names = "--close-socket-on-reset", description = "Whether the Source shall close its socket on a 'reset' event or keep it open", defaultValue = "false")
    boolean closeSocketOnReset = false;
    @CommandLine.Option(names = {"-d", "--delay"}, description = "additional time to wait before transmission")
    private int startDelay = 0;
    @CommandLine.Option(names = {"-sb", "--sndBuf"}, description = "size of the tcp send buffer in bytes")
    private int sndBuf = -1;
    @CommandLine.Option(names = {"-rb", "--rcvBuf"}, description = "size of the tcp receive buffer in bytes")
    private int rcvBuf = -1;
    @CommandLine.Option(names = "--id", description = "The id of this node. If not set, id will be sequentially chosen by the server.", defaultValue = "-1")
    int id = -1;
    @CommandLine.Option(names = "--trace", defaultValue = "50", description = "Trace interval in ms.")
    private int traceIntervalMs = 50;
    @CommandLine.Option(names = {"--no-gui"}, description = "do not plot metrics in a gui window")
    private boolean noGui;

    TimeProvider timeClient;

    @Override
    public Integer call() throws Exception {
        if (noGui) {
            Chartable.disablePlotting();
        }
        if (dataAddress == null) {
            dataAddress = controlAddress;
        }

        if (timeSyncArgs.distributedTime) {
            timeClient = DecentralizedClockSync.create(controlAddress);
        } else {
            if (timeSyncArgs.ntpAddress.contains(":")) {
                String[] ntp = timeSyncArgs.ntpAddress.split(":");
                String addr = ntp[0];
                int port = Integer.parseInt(ntp[1]);
                timeClient = NTPClient.create(addr, port);
            } else {
                timeClient = NTPClient.create(timeSyncArgs.ntpAddress);
            }
        }

        ConsoleLogger.create(timeClient);
        FileLogger.create(timeClient, "./log/client.log");

        timeClient.startSyncTime();
        boolean reconnectAfterPostHandshake;
        int run = 0;
        do {
            FileLogger.log("----------------- RUN %d -----------------", run);
            InstructionMessage msg = initialHandshake(controlAddress);

            int appPort = msg.getServerPort();
            int resultPort = msg.getResultPort();
            Date simulationBegin = msg.getSimulationBegin();
            Date startTime = msg.getStartTime();
            Date stopTime = msg.getStopTime();
            int simulationDuration = msg.getDuration();
            ConsoleLogger.setSimulationBegin(simulationBegin);
            ConsoleLogger.setSimulationEnd(stopTime);
            FileLogger.setSimulationBegin(simulationBegin);
            FileLogger.setSimulationEnd(stopTime);
            ConsoleLogger.log("Client simulationBegin %s", simulationBegin);
            ConsoleLogger.log("Client startTime %s", startTime);
            ConsoleLogger.log("Client stopTime %s", stopTime);
            FileLogger.log("Client simulationBegin %s in %d ms", simulationBegin, simulationBegin.getTime()-timeClient.getAdjustedTime());
            FileLogger.log("Client startTime %s in %d ms", startTime, startTime.getTime()-timeClient.getAdjustedTime());
            FileLogger.log("Client stopTime %s in %d ms", stopTime, stopTime.getTime()-timeClient.getAdjustedTime());

            ConsoleLogger.log("connection closed");
            timeClient.stopSyncTime();

            ConsoleLogger.log("building application");
            Application app = buildApplication(id, dataAddress, appPort);

            scheduleApplicationStart(app, simulationBegin, startTime, stopTime, simulationDuration);

            String path = null;
            if (direction == Application.Direction.DOWN) {
                path = ((LogSink) app).getFilePath();
            }
            reconnectAfterPostHandshake = postHandshake(id, controlAddress, resultPort, path);

            if (reconnectAfterPostHandshake) {
                // sleep a short amount of time before reconnecting, to ensure that the socket is open
                Thread.sleep(5000);
            }
            run++;
        } while (reconnectAfterPostHandshake);

        timeClient.close();
        return 0;
    }

    public InstructionMessage initialHandshake(String controlAddress) throws IOException, ClassNotFoundException {
        ConsoleLogger.log("InitialHandshake: connecting to: %s:%s", controlAddress, port);
        Socket socket = new Socket(controlAddress, port);
        ConsoleLogger.log("connection established");

        ConsoleLogger.log("opening streams");
        ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        out.flush();
        ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));

        ConsoleLogger.log("starting negotiation");
        sendNegotiationMessage(out);

        InstructionMessage msg = receiveInstructionMessage(in);

        if (id == -1) {
            id = msg.getId();
            ConsoleLogger.log("Got ID from server: %d", id);
        }

        out.flush();
        socket.close();
        return msg;
    }

    public Application buildApplication(int id, String dataAddress, int appPort) throws IOException {
        Application app;

        if (this.direction == Application.Direction.UP) {
            if (this.mode == Application.Mode.IOT) {
                ConsoleLogger.log("Creating IoT source application: %s:%d", dataAddress, appPort);
                app = new IoTSource(timeClient, dataAddress, appPort, resetTime, closeSocketOnReset, sndBuf, id);
            } else if (this.mode == Application.Mode.BULK) {
                ConsoleLogger.log("Creating Bulk source application: %s:%d", dataAddress, appPort);
                app = new BulkSource(timeClient, dataAddress, appPort, resetTime, closeSocketOnReset, sndBuf, id);
            } else {
                throw new NotImplementedException();
            }
        } else {
            ConsoleLogger.log("Creating Log sink application: port %d", appPort);
            app = new LogSink(timeClient, appPort, this.rcvBuf, "./out/client", id, this.mode, this.traceIntervalMs);
        }

        return app;
    }

    public void sendNegotiationMessage(ObjectOutputStream out) throws IOException {
        ConsoleLogger.log("sending negotiation message");
        String myDataIp = findMyDataIp(this.dataAddress);
        out.writeObject(new NegotiationMessage(this.id, this.mode, this.direction, myDataIp, this.port, this.startDelay, this.resetTime, this.closeSocketOnReset, this.sndBuf, this.rcvBuf));
        out.flush();
    }

    /**
     * This method searches for the local ip of the local interface which would route the packet to the {@link Client#dataAddress}
     * in order to tell the server where to send data packets, if the Client is the sink.
     * @param serverDataIp the data ip of the server
     * @return clients data ip
     */
    private String findMyDataIp(String serverDataIp) throws IOException {
        /*
         * Setting up a UDP datagram socket doesn't send anything.
         * It simply checks permissions and routing between the two end points.
         * The IP we specify doesn't need to be reachable either for this to work.
         */
        DatagramSocket s = new DatagramSocket();
        // here we have to use any port other than 0
        s.connect(InetAddress.getByName(serverDataIp), port);
        NetworkInterface n = NetworkInterface.getByInetAddress(s.getLocalAddress());
        InetAddress myInterfaceIP = n.getInetAddresses().nextElement();
        return myInterfaceIP.getHostAddress();
    }

    public InstructionMessage receiveInstructionMessage(ObjectInputStream in) throws IOException, ClassNotFoundException {
        ConsoleLogger.log("waiting for instruction message");
        InstructionMessage msg = (InstructionMessage) in.readObject();
        ConsoleLogger.log("received instruction message");
        return msg;
    }

    public void scheduleApplicationStart(Application app, Date simulationBegin, Date startTime, Date stopTime, int duration) throws Exception {
        app.simBeginOn(simulationBegin).stopOn(stopTime).startOn(startTime).duration(duration).start(timeClient);
    }

    public boolean postHandshake(int id, String controlAddress, int resultPort, String logFilePath) throws Exception {
        ConsoleLogger.log("Finished! Submitting results to %s:%s", controlAddress, resultPort);
        //wait a short time, to ensure the receiving socket is open
        Thread.sleep(3000);

        Socket socket = new Socket(controlAddress, resultPort);
        ConsoleLogger.log("connection established");

        ConsoleLogger.log("opening streams");
        ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        out.flush();
        ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));

        // only sinks need to submit their results
        byte[] fileContent = new byte[0];
        if (direction == Application.Direction.DOWN) {
            fileContent = Files.readAllBytes(new File(logFilePath).toPath());
        }

        sendResultMessage(out, id, fileContent);
        out.flush();

        boolean reconnect = receiveReconnectAfterPostHandshake(in);

        socket.close();

        return reconnect;
    }

    private boolean receiveReconnectAfterPostHandshake(ObjectInputStream in) throws IOException, ClassNotFoundException {
        Boolean reconnect = (Boolean) in.readUnshared();
        ConsoleLogger.log("received reconnect after PostHandshake " + reconnect);
        return reconnect;
    }

    public void sendResultMessage(ObjectOutputStream out, int id, byte[] fileContent) throws IOException {
        ConsoleLogger.log("sending result message");
        out.writeObject(new ResultMessage(id, this.mode, this.direction, fileContent));
        out.flush();
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Client()).execute(args);
        System.exit(exitCode);
    }
}
