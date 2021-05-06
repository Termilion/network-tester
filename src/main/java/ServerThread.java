import application.Application;
import application.SinkApplication;
import application.SourceApplication;
import general.ConsoleLogger;
import general.NTPClient;
import general.NegotiationMessage;
import general.InstructionMessage;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Date;
import java.util.Scanner;

public class ServerThread extends Thread {
    private Socket client;

    boolean uplink;
    boolean mode;
    int waitTime;
    int delay;
    int id;

    InetAddress clientAddress;
    int clientPort;

    Application app;
    NTPClient ntp;

    ObjectInputStream in;
    ObjectOutputStream out;

    static final long SINK_WAIT_TIME=1000;
    static final long SOURCE_WAIT_TIME=2000;

    public ServerThread(Socket client, NTPClient ntp, int waitTime, int id) {
        this.client = client;
        this.clientAddress = client.getInetAddress();
        this.waitTime = waitTime;
        this.id = id;
        this.ntp = ntp;

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
            this.clientPort = negotiation.getPort() + (5*id);

            // Build Corresponding Applications
            if(negotiation.isUplink()) {
                this.app = new SinkApplication(
                        clientPort,
                        1000,
                        ntp,
                        String.format("./out/%s_sink.log", client.getInetAddress().getHostAddress())
                );
            } else {
                app = new SourceApplication(negotiation.isIoT(), clientAddress, clientPort, ntp, waitTime);
            }
            ConsoleLogger.log("... PRESS ENTER TO CONTINUE ...");
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void startLocalApplications() {
        try {
            ConsoleLogger.log("%s | starting local application", client.getInetAddress());
            // schedule application start
            long waitTime = this.uplink ?
                    ntp.normalize(SINK_WAIT_TIME + delay + this.waitTime) :
                    ntp.normalize(SOURCE_WAIT_TIME + delay + this.waitTime);
            ConsoleLogger.log("%s | scheduled transmission in %s ms", clientAddress, waitTime);
            Thread.sleep(waitTime);
            app.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendInstructions() {
        ConsoleLogger.log("%s | sending instruction message", client.getInetAddress());
        // negotiate start time
        long current = ntp.getCurrentTimeNormalized();
        Date startTime = this.uplink ? new Date(current + (SOURCE_WAIT_TIME) + delay + waitTime) : new Date(current + SINK_WAIT_TIME + delay + waitTime);
        try {
            InstructionMessage msg = new InstructionMessage(startTime, clientPort);
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
