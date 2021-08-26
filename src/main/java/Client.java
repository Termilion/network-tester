import application.Application;
import application.SinkApplication;
import application.SourceApplication;
import general.*;
import general.logger.ConsoleLogger;
import general.logger.FileLogger;
import picocli.CommandLine;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.util.Date;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "Client", description = "Starts a client which connects to the instruction server.")
public class Client implements Callable<Integer> {
    @CommandLine.Parameters(index = "0", description = "ipv4 address to connect to")
    private String address;
    @CommandLine.Parameters(index = "1", description = "port to connect to")
    private int port;
    @CommandLine.ArgGroup(exclusive = true, multiplicity = "1")
    Exclusive exclusive;

    static class Exclusive {
        @CommandLine.Option(names = "--ntp", defaultValue = "ptbtime1.ptb.de", description = "Address of a ntp server to sync time")
        private String ntpAddress;
        @CommandLine.Option(names = "--distributedTime", defaultValue = "false", description = "Sync time in a local distributed manner")
        private boolean distributedTime;
    }

    @CommandLine.Option(names = {"-iot"}, description = "start an iot application")
    private boolean mode;
    @CommandLine.Option(names = {"-u"}, description = "start an up-link data flow")
    private boolean uplink;
    @CommandLine.Option(names = {"-r", "--resetTime"}, description = "time after the app gets forcefully reset in milliseconds")
    private int resetTime = -1;
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

    TimeProvider timeClient;

    @Override
    public Integer call() throws Exception {
        if (exclusive.distributedTime) {
            timeClient = DecentralizedClockSync.getInstance();
        } else {
            if (exclusive.ntpAddress.contains(":")) {
                String[] ntp = exclusive.ntpAddress.split(":");
                String addr = ntp[0];
                int port = Integer.parseInt(ntp[1]);
                timeClient = NTPClient.create(addr, port);
            } else {
                timeClient = NTPClient.create(exclusive.ntpAddress);
            }
        }

        ConsoleLogger.create(timeClient);
        FileLogger.create(timeClient, "./log/client.log");

        timeClient.startSyncTime();
        boolean reconnectAfterPostHandshake;
        int run = 0;
        do {
            FileLogger.log("----------------- RUN %d -----------------", run);
            InstructionMessage msg = initialHandshake();

            int appPort = msg.getServerPort();
            int resultPort = msg.getResultPort();
            Date simulationBegin = msg.getSimulationBegin();
            Date startTime = msg.getStartTime();
            Date stopTime = msg.getStopTime();
            ConsoleLogger.setSimulationBegin(simulationBegin);
            FileLogger.setSimulationBegin(simulationBegin);
            ConsoleLogger.log("Client simulationBegin %s", simulationBegin);
            ConsoleLogger.log("Client startTime %s", startTime);
            ConsoleLogger.log("Client stopTime %s", stopTime);

            ConsoleLogger.log("connection closed");
            timeClient.stopSyncTime();

            ConsoleLogger.log("building application");
            Application app = buildApplication(id, address, appPort);

            scheduleApplicationStart(simulationBegin, startTime, app, stopTime);

            String path = null;
            if (!uplink) {
                path = ((SinkApplication) app).getFilePath();
            }
            reconnectAfterPostHandshake = postHandshake(id, resultPort, path);

            if (reconnectAfterPostHandshake) {
                // sleep a short amount of time before reconnecting, to ensure that the socket is open
                Thread.sleep(5000);
            }
            run++;
        } while (reconnectAfterPostHandshake);

        timeClient.close();
        return 0;
    }

    public InstructionMessage initialHandshake() throws IOException, ClassNotFoundException {
        ConsoleLogger.log("InitialHandshake: connecting to: %s:%s", address, port);
        Socket socket = new Socket(address, port);
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

    public Application buildApplication(int id, String ipaddress, int appPort) throws IOException {
        Application app;

        if (this.uplink) {
            app = new SourceApplication(this.mode, ipaddress, appPort, timeClient, resetTime, this.sndBuf, id);
        } else {
            app = new SinkApplication(appPort, this.rcvBuf, timeClient, String.format("./out/client_sink_flow_%d_%s.csv", id, getModeString()), id, mode, traceIntervalMs);
        }
        return app;
    }

    public void sendNegotiationMessage(ObjectOutputStream out) throws IOException {
        ConsoleLogger.log("sending negotiation message");
        out.writeObject(new NegotiationMessage(this.id, this.mode, this.uplink, this.startDelay, this.port, this.resetTime, this.sndBuf, this.rcvBuf));
        out.flush();
    }

    public InstructionMessage receiveInstructionMessage(ObjectInputStream in) throws IOException, ClassNotFoundException {
        ConsoleLogger.log("waiting for instruction message");
        InstructionMessage msg = (InstructionMessage) in.readObject();
        ConsoleLogger.log("received instruction message");
        return msg;
    }

    public void scheduleApplicationStart(Date simulationBegin, Date startTime, Application app, Date stopTime) throws Exception {
        app.simBeginOn(simulationBegin).stopOn(stopTime).startOn(startTime).start(timeClient);
    }

    public boolean postHandshake(int id, int resultPort, String logFilePath) throws Exception {
        ConsoleLogger.log("Finished! Submitting results to %s:%s", address, resultPort);
        //wait a short time, to ensure the receiving socket is open
        Thread.sleep(3000);

        Socket socket = new Socket(address, resultPort);
        ConsoleLogger.log("connection established");

        ConsoleLogger.log("opening streams");
        ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        out.flush();
        ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));

        // only sinks need to submit their results
        byte[] fileContent = new byte[0];
        if (!uplink) {
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
        out.writeObject(new ResultMessage(id, this.mode, this.uplink, fileContent));
        out.flush();
    }

    private String getModeString() {
        if (mode) {
            return "iot";
        } else {
            return "bulk";
        }
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Client()).execute(args);
        System.exit(exitCode);
    }
}
