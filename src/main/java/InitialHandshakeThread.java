import application.Application;
import application.SinkApplication;
import application.SourceApplication;
import general.ConsoleLogger;
import general.InstructionMessage;
import general.NTPClient;
import general.NegotiationMessage;

import java.io.*;
import java.net.Socket;
import java.util.Date;

public class InitialHandshakeThread extends Thread {

    private final Socket client;

    boolean uplink;
    boolean mode;
    int resetTime;
    int extraDelay;
    int id;
    int simDuration;

    String clientAddress;
    int clientPort;
    int resultPort;

    Application app;
    NTPClient ntp;

    ObjectInputStream in;
    ObjectOutputStream out;

    static final long SINK_WAIT_TIME = 1000;
    static final long SOURCE_WAIT_TIME = 2000;

    public InitialHandshakeThread(Socket client, NTPClient ntp, int id, int simDuration, int resultPort) {
        this.client = client;
        this.clientAddress = client.getInetAddress().getHostAddress();
        this.id = id;
        this.ntp = ntp;
        this.simDuration = simDuration;
        this.resultPort = resultPort;

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
            this.extraDelay = negotiation.getStartDelay();
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
                        String.format("./out/sink_flow_%d_%s.csv", id, getModeString()),
                        id,
                        mode
                );
            } else {
                app = new SourceApplication(this.mode, clientAddress, clientPort, ntp, resetTime, sndBuf);
            }
            ConsoleLogger.log("... PRESS ENTER TO CONTINUE ...");
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            e.printStackTrace();
        }
    }

    private String getModeString() {
        if (mode) {
            return "iot";
        } else {
            return "bulk";
        }
    }

    public Application getApplication(Date simulationBegin) {
        ConsoleLogger.log("%s | building local application", client.getInetAddress());
        // schedule application start

        long begin = simulationBegin.getTime();
        Date startTime = this.uplink ? new Date(begin + SINK_WAIT_TIME + extraDelay) : new Date(begin + SOURCE_WAIT_TIME + extraDelay);
        Date stopTime = new Date(begin + simDuration * 1000L);

        return app.simBeginOn(simulationBegin).stopOn(stopTime).startOn(startTime);
    }

    public void sendInstructions(Date simulationBegin) {
        // negotiate start time
        long begin = simulationBegin.getTime();
        Date startTime = this.uplink ? new Date(begin + SOURCE_WAIT_TIME + extraDelay) : new Date(begin + SINK_WAIT_TIME + extraDelay);
        Date stopTime = new Date(begin + simDuration * 1000L);

        ConsoleLogger.log("%s | sending instruction message. Begin: %s Start: %s Stop: %s", client.getInetAddress(), simulationBegin, startTime, stopTime);
        try {
            InstructionMessage msg = new InstructionMessage(id, simulationBegin, startTime, stopTime, clientPort, resultPort);
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
