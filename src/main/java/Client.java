import application.Application;
import application.SinkApplication;
import application.SourceApplication;
import general.NTPClient;
import general.NegotiationMessage;
import general.TimeMessage;
import picocli.CommandLine;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;

public class Client implements Callable<Integer> {
    @CommandLine.Parameters(index = "0", description = "ipv4 address to connect to")
    private String address;
    @CommandLine.Parameters(index = "1", description = "port to connect to")
    private int port;
    @CommandLine.Option(names = {"-b", "--bufferSize"}, description = "maximum size of the tcp buffer [pkt]")
    private int bufferSize;
    @CommandLine.Option(names = {"-iot"}, description = "start an iot application")
    private boolean direction;
    @CommandLine.Option(names = {"-u"}, description = "start a downlink application")
    private boolean mode;
    @CommandLine.Option(names = {"-r", "--resetTime"}, description = "time after the app gets forcefully reset")
    private int waitTime = -1;
    @CommandLine.Option(names = {"-d", "--delay"}, description = "additional time to wait before transmission")
    private int startDelay = 0;

    private NTPClient ntp;

    @Override
    public Integer call() throws Exception {
        Socket socket = new Socket(address, port);
        ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));

        Application app = buildApplication(socket.getInetAddress());

        negotiate(socket, out);

        scheduleTransmission(socket, in, app);
        return 0;
    }

    public Application buildApplication(InetAddress address) {
        Application app;

        if (this.direction) {
            if (this.mode) {
                app = new SourceApplication("iot", address, port, ntp, waitTime);
            } else {
                app = new SourceApplication("bulk", address, port, ntp, waitTime);
            }
        } else {
            app = new SinkApplication(port, bufferSize, ntp, "/out/server.log");
        }
        return app;
    }

    public void negotiate(Socket socket, ObjectOutputStream out) throws Exception {
        System.out.println("sending negotiation message ...");
        out.writeObject(new NegotiationMessage(this.mode, this.direction, this.startDelay));
    }

    public void scheduleTransmission(Socket socket, ObjectInputStream in, Application app) throws Exception {
        TimeMessage msg = (TimeMessage) in.readUnshared();
        socket.close();
        Timer time = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                try {
                    app.start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        time.schedule(task, msg.getTime());
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Client()).execute(args);
        System.exit(exitCode);
    }
}
