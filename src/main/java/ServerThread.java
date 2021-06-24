import application.Application;
import application.SinkApplication;
import application.SourceApplication;
import general.ConsoleLogger;
import general.InstructionMessage;
import general.NTPClient;
import general.NegotiationMessage;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Date;

public class ServerThread extends Thread {
    private Socket client;

    boolean uplink;
    boolean mode;
    int resetTime;
    int delay;
    int id;
    int simDuration;

    InetAddress clientAddress;
    int clientPort;

    Application app;
    NTPClient ntp;

    ObjectInputStream in;
    ObjectOutputStream out;

    static final long SINK_WAIT_TIME = 1000;
    static final long SOURCE_WAIT_TIME = 2000;

    public ServerThread(Socket client, NTPClient ntp, int id, int simDuration) {
        this.client = client;
        this.clientAddress = client.getInetAddress();
        this.id = id;
        this.ntp = ntp;
        this.simDuration = simDuration;

        try {
            this.out = new ObjectOutputStream(new BufferedOutputStream(client.getOutputStream()));
            this.out.flush();
            this.in = new ObjectInputStream(new BufferedInputStream(client.getInputStream()));
            ConsoleLogger.log("connection to client: %s successful", client.getInetAddress());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void run() {
        try {
            ConsoleLogger.log("%s | waiting for negotiation message", client.getInetAddress());
            NegotiationMessage negotiation = (NegotiationMessage) in.readUnshared();
            ConsoleLogger.log("%s | received negotiation message", client.getInetAddress());
            this.uplink = negotiation.isUplink();
            this.mode = negotiation.isIoT();
            this.delay = negotiation.getStartDelay();
            this.clientPort = negotiation.getPort() + (5 * id);
            this.resetTime = negotiation.getResetTime();
            int sndBuf = negotiation.getSndBuf();
            int rcvBuf = negotiation.getRcvBuf();

            // Build Corresponding Applications
            if (negotiation.isUplink()) {
                app = new SinkApplication(
                        clientPort,
                        rcvBuf,
                        ntp,
                        String.format("./out/%s_sink.log", client.getInetAddress().getHostAddress())
                );
            } else {
                app = new SourceApplication(this.mode, clientAddress, clientPort, ntp, resetTime, sndBuf);
            }
            ConsoleLogger.log("... PRESS ENTER TO CONTINUE ...");
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            e.printStackTrace();
        }
    }

    public void startLocalApplications() {
        try {
            ConsoleLogger.log("%s | starting local application", client.getInetAddress());
            // schedule application start

            long current = ntp.getCurrentTimeNormalized();
            Date startTime = this.uplink ? new Date(current + SINK_WAIT_TIME + delay) : new Date(current + SOURCE_WAIT_TIME + delay);
            Date stopTime = new Date(current + delay + simDuration * 1000L);

            ConsoleLogger.log("Server calculated stopTime is " + stopTime);
            app.stopOn(stopTime).startOn(startTime).start(ntp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendInstructions() {
        ConsoleLogger.log("%s | sending instruction message", client.getInetAddress());
        // negotiate start time
        long current = ntp.getCurrentTimeNormalized();
        Date startTime = this.uplink ? new Date(current + SOURCE_WAIT_TIME + delay) : new Date(current + SINK_WAIT_TIME + delay);
        Date stopTime = new Date(current + delay + simDuration * 1000L);
        try {
            InstructionMessage msg = new InstructionMessage(startTime, stopTime, clientPort);
            out.writeObject(msg);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                // close connection to client
                out.flush();
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
