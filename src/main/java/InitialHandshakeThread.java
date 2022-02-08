import application.Application;
import application.sink.LogSink;
import application.source.BulkSource;
import application.source.IoTSource;
import general.InstructionMessage;
import general.NegotiationMessage;
import general.TimeProvider;
import general.Utility.NotImplementedException;
import general.logger.ConsoleLogger;

import java.io.*;
import java.net.Socket;
import java.util.Date;

public class InitialHandshakeThread extends Thread {

    private final Socket client;

    Application.Direction direction;
    Application.Mode mode;
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
        this.id = defaultId;
        this.timeProvider = timeProvider;
        this.simDuration = simDuration;
        this.resultPort = resultPort;
        this.traceIntervalMs = traceIntervalMs;

        try {
            this.out = new ObjectOutputStream(new BufferedOutputStream(client.getOutputStream()));
            this.out.flush();
            this.in = new ObjectInputStream(new BufferedInputStream(client.getInputStream()));
            ConsoleLogger.log("connection from client: %s successful", client.getInetAddress());
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
            this.direction = negotiation.getDirection();
            this.mode = negotiation.getMode();
            this.extraDelay = negotiation.getStartDelay();
            this.clientAddress = negotiation.getClientDataIp();
            this.clientPort = negotiation.getPort() + 10 + (5 * id); // id is 0-indexed
            this.resetTime = negotiation.getResetTime();
            int sndBuf = negotiation.getSndBuf();
            int rcvBuf = negotiation.getRcvBuf();
            boolean closeSocketOnReset = negotiation.getCloseSocketOnReset();

            // Build Corresponding Applications
            if (this.direction == Application.Direction.UP) {
                // if client is uplink, build a sink
                app = new LogSink(
                        timeProvider,
                        clientPort,
                        rcvBuf,
                        String.format("./out/server_sink_flow_%d_%s.csv", id, mode.getName()),
                        id,
                        mode,
                        traceIntervalMs
                );
            } else {
                // else build a source with the clients dataAddress
                if (this.mode == Application.Mode.IOT) {
                    ConsoleLogger.log("Creating IoT source application: %s:%d", clientAddress, clientPort);
                    app = new IoTSource(timeProvider, clientAddress, clientPort, resetTime, closeSocketOnReset, sndBuf, id);
                } else if (this.mode == Application.Mode.BULK) {
                    ConsoleLogger.log("Creating Bulk source application: %s:%d", clientAddress, clientPort);
                    app = new BulkSource(timeProvider, clientAddress, clientPort, resetTime, closeSocketOnReset, sndBuf, id);
                } else {
                    throw new NotImplementedException();
                }
            }
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            e.printStackTrace();
        }
    }

    public Application buildApplication(Date simulationBegin) {
        ConsoleLogger.log("%s | building local application", client.getInetAddress());
        // schedule application start

        long begin = simulationBegin.getTime();
        // if client is uplink, then we need to wait the SINK_WAIT_TIME
        Date startTime = (this.direction == Application.Direction.UP) ? new Date(begin + SINK_WAIT_TIME + extraDelay) : new Date(begin + SOURCE_WAIT_TIME + extraDelay);
        Date stopTime = new Date(begin + simDuration * 1000L);

        return app.simBeginOn(simulationBegin).stopOn(stopTime).startOn(startTime).duration(simDuration);
    }

    public Application getApplication() {
        return app;
    }

    public void sendInstructions(Date simulationBegin) {
        // negotiate start time
        long begin = simulationBegin.getTime();
        // if client is uplink, then it needs to wait the SOURCE_WAIT_TIME
        Date startTime = (this.direction == Application.Direction.UP) ? new Date(begin + SOURCE_WAIT_TIME + extraDelay) : new Date(begin + SINK_WAIT_TIME + extraDelay);
        Date stopTime = new Date(begin + simDuration * 1000L);

        ConsoleLogger.log("%s | sending instruction message. Begin: %s Start: %s Stop: %s", client.getInetAddress(), simulationBegin, startTime, stopTime);
        try {
            ConsoleLogger.log("Sending ID to client: %d", id);
            InstructionMessage msg = new InstructionMessage(id, simulationBegin, startTime, stopTime, simDuration, clientPort, resultPort);
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
