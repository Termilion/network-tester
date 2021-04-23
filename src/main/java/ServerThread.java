import application.Application;
import application.SinkApplication;
import application.SourceApplication;
import general.NTPClient;
import general.NegotiationMessage;
import general.TimeMessage;

import java.io.*;
import java.net.Socket;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class ServerThread extends Thread {
    private Socket client;
    ObjectInputStream in;
    ObjectOutputStream out;

    NegotiationMessage.FlowDirection direction;
    NegotiationMessage.FlowMode mode;

    Application app;
    NTPClient ntp;

    public ServerThread(Socket client, String ntpAddress) {
        this.client = client;
        try {
            this.ntp = new NTPClient(ntpAddress);
            this.in = new ObjectInputStream(new BufferedInputStream(client.getInputStream()));
            this.out = new ObjectOutputStream(new BufferedOutputStream(client.getOutputStream()));
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            NegotiationMessage negotiation = (NegotiationMessage) this.in.readUnshared();
            this.direction = negotiation.getFlowDirection();
            this.mode = negotiation.getFlowMode();

            // Build Corresponding Applications
            switch(negotiation.getFlowDirection()) {
                case down:
                    switch (negotiation.getFlowMode()) {
                        case iot:
                            // Build IoT Source
                            app = new SourceApplication("i", client.getInetAddress(), 5000, ntp);
                            break;
                        default:
                            // Build Bulk Source
                            app = new SourceApplication("b", client.getInetAddress(), 5000, ntp);
                    }
                    break;
                case up:
                    this.app = new SinkApplication(
                            5000,
                            1000,
                            ntp,
                            String.format("./out/%s/server.log", client.getInetAddress())
                    );
                    break;
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void sendOrder() {
        try {
            // negotiate start time
            Date startTime = new Date();
            TimeMessage msg = new TimeMessage(startTime);
            out.writeObject(msg);

            // close connection to client
            client.close();

            // schedule application start
            if (this.direction == NegotiationMessage.FlowDirection.up) {
                app.start();
            } else {
                Timer timer = new Timer();
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
                timer.schedule(task, startTime);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
