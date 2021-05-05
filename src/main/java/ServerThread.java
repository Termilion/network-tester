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

    boolean uplink;
    boolean mode;
    int waitTime;
    int delay;

    Application app;
    NTPClient ntp;

    static final long ONE_MINUTE_IN_MILLIS=60000;

    public ServerThread(Socket client, NTPClient ntp, int waitTime) {
        this.client = client;
        try {
            this.ntp = ntp;
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
            this.uplink = negotiation.isUplink();
            this.mode = negotiation.isIoT();
            this.delay = negotiation.getStartDelay();

            // Build Corresponding Applications
            if(negotiation.isUplink()) {
                this.app = new SinkApplication(
                        5000,
                        1000,
                        ntp,
                        String.format("/out/%s_sink.log", client.getInetAddress().getHostAddress())
                );
            } else {
                app = new SourceApplication(negotiation.isIoT(), client.getInetAddress(), client.getPort(), ntp, waitTime);
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void startLocalApplications(Date startTime) {
        try {
            // schedule application start
            if (this.uplink) {
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

    public Date sendInstructions() {
        // negotiate start time
        long current = new Date().getTime();
        Date startTime = new Date(current + (ONE_MINUTE_IN_MILLIS) + delay);

        try {
            TimeMessage msg = new TimeMessage(new Date(ntp.normalize(startTime)));
            out.writeObject(msg);

            // close connection to client
            client.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return startTime;
    }
}
