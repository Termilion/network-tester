import application.Application;
import application.SinkApplication;
import application.SourceApplication;
import general.ConsoleLogger;
import general.InstructionMessage;
import general.NegotiationMessage;
import general.TimeProvider;

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
    int traceIntervalMs;

    Application app;
    TimeProvider timeProvider;

    ObjectInputStream in;
    ObjectOutputStream out;

    static final long SINK_WAIT_TIME = 1000;
    static final long SOURCE_WAIT_TIME = 2000;

    public InitialHandshakeThread(Socket client, TimeProvider timeProvider, int defaultId, int simDuration, int resultPort, int traceIntervalMs) {
        this.client = client;
        this.clientAddress = client.getInetAddress().getHostAddress();
        this.id = defaultId;
        this.timeProvider = timeProvider;
        this.simDuration = simDuration;
        this.resultPort = resultPort;
        this.traceIntervalMs = traceIntervalMs;

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
            if (negotiation.hasPreviousId()) {
                ConsoleLogger.log("Received ID from Client. Old: %d New: %d", this.id, negotiation.getPreviousId());
                this.id = negotiation.getPreviousId();
            }
            ConsoleLogger.log("%s | received negotiation message. ID: %d", client.getInetAddress(), this.id);
            this.uplink = negotiation.isUplink();
            this.mode = negotiation.isIoT();
            this.extraDelay = negotiation.getStartDelay();
            this.clientPort = negotiation.getPort() + 10 + (5 * id); // id is 0-indexed
            this.resetTime = negotiation.getResetTime();
            int sndBuf = negotiation.getSndBuf();
            int rcvBuf = negotiation.getRcvBuf();

            // Build Corresponding Applications
            if (negotiation.isUplink()) {
                app = new SinkApplication(
                        clientPort,
                        rcvBuf,
                        timeProvider,
                        String.format("./out/server_sink_flow_%d_%s.csv", id, getModeString()),
                        id,
                        mode,
                        traceIntervalMs
                );
            } else {
                app = new SourceApplication(this.mode, clientAddress, clientPort, timeProvider, resetTime, sndBuf);
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

    public Application buildApplication(Date simulationBegin) {
        ConsoleLogger.log("%s | building local application", client.getInetAddress());
        // schedule application start

        long begin = simulationBegin.getTime();
        // if client is uplink, then we need to wait the SINK_WAIT_TIME
        Date startTime = this.uplink ? new Date(begin + SINK_WAIT_TIME + extraDelay) : new Date(begin + SOURCE_WAIT_TIME + extraDelay);
        Date stopTime = new Date(begin + simDuration * 1000L);

        return app.simBeginOn(simulationBegin).stopOn(stopTime).startOn(startTime);
    }

    public Application getApplication() {
        return app;
    }

    public void sendInstructions(Date simulationBegin) {
        // negotiate start time
        long begin = simulationBegin.getTime();
        // if client is uplink, then it needs to wait the SOURCE_WAIT_TIME
        Date startTime = this.uplink ? new Date(begin + SOURCE_WAIT_TIME + extraDelay) : new Date(begin + SINK_WAIT_TIME + extraDelay);
        Date stopTime = new Date(begin + simDuration * 1000L);

        ConsoleLogger.log("%s | sending instruction message. Begin: %s Start: %s Stop: %s", client.getInetAddress(), simulationBegin, startTime, stopTime);
        try {
            ConsoleLogger.log("Sending ID to client: %d", id);
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
