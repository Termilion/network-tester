import application.Application;
import application.SinkApplication;
import application.SourceApplication;
import general.NTPClient;
import general.NegotiationMessage;
import general.TimeMessage;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

public class Client {
    private NTPClient ntp;
    private String address;
    private int port;
    private int bufferSize;
    private NegotiationMessage.FlowDirection direction;
    private NegotiationMessage.FlowMode mode;

    public Client(String address, int port, String ntpAddress, int bufferSize, String type, String direction) {
        try {
            this.ntp = new NTPClient(ntpAddress);
            this.address = address;
            this.port = port;
            this.bufferSize = bufferSize;

            Socket socket = new Socket(address, port);
            ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));

            setupEnums(type, direction);

            Application app = buildApplication(socket.getInetAddress());

            negotiate(socket, out);

            scheduleTransmission(socket, in, app);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setupEnums(String type, String direction) {
        if (type.equals("i")) {
            this.mode = NegotiationMessage.FlowMode.iot;
        } else {
            this.mode = NegotiationMessage.FlowMode.bulk;
        }

        if (direction.equals("up")) {
            this.direction = NegotiationMessage.FlowDirection.up;
        } else {
            this.direction = NegotiationMessage.FlowDirection.down;
        }
    }

    public Application buildApplication(InetAddress address) {
        Application app;

        switch (this.direction) {
            case up:
                switch (this.mode) {
                    case iot:
                        app = new SourceApplication("iot", address, port, ntp);
                        break;
                    default:
                        app = new SourceApplication("bulk", address, port, ntp);
                }
                break;
            default:
                app = new SinkApplication(port, bufferSize, ntp, "/out/server.log");
        }
        return app;
    }

    public void negotiate(Socket socket, ObjectOutputStream out) throws Exception {
        System.out.println("sending negotiation message ...");
        out.writeObject(new NegotiationMessage(this.mode, this.direction));
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
}
