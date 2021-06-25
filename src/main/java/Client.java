import application.Application;
import application.SinkApplication;
import application.SourceApplication;
import general.*;
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
    @CommandLine.Option(names = {"-ntp"}, defaultValue = "ptbtime1.ptb.de", description = "address of the ntp server")
    private String ntpAddress;
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

    private NTPClient ntp;

    @Override
    public Integer call() throws Exception {

        InstructionMessage msg = initialHandshake();

        int id = msg.getId();
        int appPort = msg.getServerPort();
        int resultPort = msg.getResultPort();
        Date startTime = msg.getStartTime();
        Date stopTime = msg.getStopTime();
        ConsoleLogger.log("Client received stopTime is " + stopTime);
        ConsoleLogger.log("scheduling transmission at %s", startTime);


        ConsoleLogger.log("connection closed");

        ConsoleLogger.log("building application");
        Application app = buildApplication(id, address, appPort);

        scheduleApplicationStart(startTime, app, stopTime);

        if (!uplink) {
            // only sinks need to do the post handshake
            String path = ((SinkApplication) app).getFilePath();
            postHandshake(id, resultPort, path);
        }
        return 0;
    }

    public InstructionMessage initialHandshake() throws IOException, ClassNotFoundException {
        ntp = new NTPClient(ntpAddress);

        ConsoleLogger.log("connecting to: %s", address);
        Socket socket = new Socket(address, port);
        ConsoleLogger.log("connection established");

        ConsoleLogger.log("opening streams");
        ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        out.flush();
        ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));

        ConsoleLogger.log("starting negotiation");
        sendNegotiationMessage(out);

        InstructionMessage msg = receiveInstructionMessage(in);
        out.flush();
        socket.close();
        return msg;
    }

    public Application buildApplication(int id, String ipaddress, int appPort) {
        Application app;

        if (this.uplink) {
            app = new SourceApplication(this.mode, ipaddress, appPort, ntp, resetTime, this.sndBuf);
        } else {
            app = new SinkApplication(appPort, this.rcvBuf, ntp, String.format("./out/sink_flow_%d_%s.csv", id, getModeString()), id, mode);
        }
        return app;
    }

    public void sendNegotiationMessage(ObjectOutputStream out) throws IOException {
        ConsoleLogger.log("sending negotiation message");
        out.writeObject(new NegotiationMessage(this.mode, this.uplink, this.startDelay, this.port, this.resetTime, this.sndBuf, this.rcvBuf));
        out.flush();
    }

    public InstructionMessage receiveInstructionMessage(ObjectInputStream in) throws IOException, ClassNotFoundException {
        ConsoleLogger.log("waiting for instruction message");
        InstructionMessage msg = (InstructionMessage) in.readObject();
        ConsoleLogger.log("received instruction message");
        return msg;
    }

    public void scheduleApplicationStart(Date startTime, Application app, Date stopTime) throws Exception {
        ConsoleLogger.log("Client received stopTime is " + stopTime);
        app.stopOn(stopTime).startOn(startTime).start(ntp);
    }

    public void postHandshake(int id, int resultPort, String logFilePath) throws Exception {
        ConsoleLogger.log("Finished! Submitting results...");
        //wait a short time, to ensure the receiving socket is open
        Thread.sleep(3000);

        Socket socket = new Socket(address, resultPort);
        ConsoleLogger.log("connection established");

        ConsoleLogger.log("opening streams");
        ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        out.flush();
        //ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));

        byte[] fileContent = Files.readAllBytes(new File(logFilePath).toPath());

        sendResultMessage(out, id, fileContent);

        out.flush();
        socket.close();
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
