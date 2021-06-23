import application.Application;
import application.SinkApplication;
import application.SourceApplication;
import general.ConsoleLogger;
import general.NTPClient;
import general.NegotiationMessage;
import general.InstructionMessage;
import picocli.CommandLine;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;
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
    private boolean direction;
    @CommandLine.Option(names = {"-r", "--resetTime"}, description = "time after the app gets forcefully reset")
    private int resetTime = -1;
    @CommandLine.Option(names = {"-d", "--delay"}, description = "additional time to wait before transmission")
    private int startDelay = 0;
    @CommandLine.Option(names = {"-sb", "--sndBuf"}, description = "size of the tcp send buffer in bytes")
    private int sndBuf = -1;
    @CommandLine.Option(names = {"-rb", "--rcvBuf"}, description = "size of the tcp receive buffer in bytes")
    private int rcvBuf = -1;

    private NTPClient ntp;
    ObjectOutputStream out;
    ObjectInputStream in;

    @Override
    public Integer call() throws Exception {
        ntp = new NTPClient(ntpAddress);

        ConsoleLogger.log("connecting to: %s", address);
        Socket socket = new Socket(address, port);
        ConsoleLogger.log("connection established");

        ConsoleLogger.log("opening streams");
        out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        out.flush();
        in = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));

        ConsoleLogger.log("starting negotiation");
        sendNegotiationMessage();

        InstructionMessage msg = receiveInstructionMessage();
        int appPort = msg.getPort();
        long scheduledTime = msg.getTime().getTime();
        ConsoleLogger.log("scheduling transmission at %s", scheduledTime);

        out.flush();
        socket.close();
        ConsoleLogger.log("connection closed");

        ConsoleLogger.log("building application");
        Application app = buildApplication(socket.getInetAddress(), appPort);

        scheduleApplicationStart(scheduledTime, app);
        Scanner cli = new Scanner(System.in);
        cli.nextLine();
        return 0;
    }

    public Application buildApplication(InetAddress address, int appPort) {
        Application app;

        if (this.direction) {
            app = new SourceApplication(this.mode, address, appPort, ntp, resetTime, this.sndBuf);
        } else {
            app = new SinkApplication(appPort, this.rcvBuf, ntp, String.format("./out/%s_sink.log", address.getHostAddress()));
        }
        return app;
    }

    public void sendNegotiationMessage() throws Exception {
        ConsoleLogger.log("sending negotiation message");
        out.writeObject(new NegotiationMessage(this.mode, this.direction, this.startDelay, this.port, this.resetTime, this.sndBuf, this.rcvBuf));
        out.flush();
    }

    public InstructionMessage receiveInstructionMessage() throws Exception {
        ConsoleLogger.log("waiting for instruction message");
        InstructionMessage msg = (InstructionMessage) in.readObject();
        ConsoleLogger.log("received instruction message");
        return msg;
    }

    public void scheduleApplicationStart(long startTime, Application app) throws Exception {
        long current = ntp.getCurrentTimeNormalized();
        long waitTime = startTime - current;
        ConsoleLogger.log("scheduled transmission in %s ms", waitTime);
        Thread.sleep(waitTime);
        app.start();
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Client()).execute(args);
        System.exit(exitCode);
    }
}
