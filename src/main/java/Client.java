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

public class Client implements Callable<Integer> {
    @CommandLine.Parameters(index = "0", description = "ipv4 address to connect to")
    private String address;
    @CommandLine.Parameters(index = "1", description = "port to connect to")
    private int port;
    @CommandLine.Parameters(index = "2", description = "address of the ntp server")
    private String ntpAddress;
    @CommandLine.Option(names = {"-b", "--bufferSize"}, description = "maximum size of the tcp buffer [pkt]")
    private int bufferSize = 1000;
    @CommandLine.Option(names = {"-iot"}, description = "start an iot application")
    private boolean direction;
    @CommandLine.Option(names = {"-u"}, description = "start a downlink application")
    private boolean mode;
    @CommandLine.Option(names = {"-r", "--resetTime"}, description = "time after the app gets forcefully reset")
    private int waitTime = -1;
    @CommandLine.Option(names = {"-d", "--delay"}, description = "additional time to wait before transmission")
    private int startDelay = 0;

    private NTPClient ntp;
    ObjectOutputStream out;
    ObjectInputStream in;

    @Override
    public Integer call() throws Exception {
        ConsoleLogger.log("connecting to: %s", address);
        Socket socket = new Socket(address, port);
        ConsoleLogger.log("connection established");

        ConsoleLogger.log("opening out streams");
        out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        out.flush();
        ConsoleLogger.log("opening in streams");
        in = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));

        ntp = new NTPClient(ntpAddress);

        ConsoleLogger.log("starting negotiation");
        sendNegotiationMessage();

        ConsoleLogger.log("scheduling transmission");
        InstructionMessage msg = receiveInstructionMessage();
        int appPort = msg.getPort();
        long scheduledTime = msg.getTime().getTime();

        out.flush();
        socket.close();

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
            app = new SourceApplication(this.mode, address, appPort, ntp, waitTime);
        } else {
            app = new SinkApplication(appPort, bufferSize, ntp, String.format("./out/%s_sink.log", address.getHostAddress()));
        }
        return app;
    }

    public void sendNegotiationMessage() throws Exception {
        ConsoleLogger.log("sending negotiation message");
        out.writeObject(new NegotiationMessage(this.mode, this.direction, this.startDelay, this.port));
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
