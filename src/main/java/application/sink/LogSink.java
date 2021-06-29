package application.sink;

import general.ConsoleLogger;
import general.TimeProvider;
import general.Utility;

import java.io.*;
import java.util.*;

public class LogSink extends Sink {

    String connectedAddress;
    int id;
    int mode;
    Date simulationBegin;

    File outFile;
    BufferedWriter writer;

    int rcvBytes = 0;
    List<Long> delay;

    boolean closed = false;
    long totalRcvBytes = 0;
    long totalRcvPackets = 0;
    double lastGoodputMpbs = 0;
    double lastDelay = 0;

    public LogSink(TimeProvider timeProvider, int port, int receiveBufferSize, String filePath, Date simulationBegin, Date stopTime, int id, boolean mode) throws IOException {
        super(timeProvider, port, receiveBufferSize, stopTime);
        this.id = id;
        this.mode = booleanToInt(mode);
        this.simulationBegin = simulationBegin;
        this.delay = Collections.synchronizedList(new ArrayList<>());
        createLogFile(filePath);
    }

    public void createLogFile(String filePath) throws IOException {
        outFile = new File(filePath);
        outFile.getParentFile().mkdirs();
        if (outFile.exists()) {
            boolean deleted = outFile.delete();
            if (!deleted) {
                throw new IOException("Can not delete " + filePath);
            }
        }
        writer = new BufferedWriter(new FileWriter(outFile, true));
        writer.write("time,flow,type,address,sink_gp,delay_data_ms");
        writer.newLine();
    }

    @Override
    public void executeLogic() {
        try {
            InputStream in = client.getInputStream();
            DataInputStream dataInputStream = new DataInputStream(in);

            connectedAddress = client.getInetAddress().getHostAddress();

            byte[] payload = new byte[1000];

            while (isRunning) {
                try {
                    dataInputStream.readFully(payload);
                } catch (EOFException e) {
                    ConsoleLogger.log("reached end of file");
                    break;
                }
                // calc delay
                long sendTime = Utility.decodeTime(payload);
                long currentTime = this.timeProvider.getAdjustedTime();
                long delayTime = currentTime - sendTime;
                ConsoleLogger.log("Received paket: SendTime %s CurrentTime %s Delay %s", sendTime, currentTime, delayTime);
                if (delayTime < 0) {
                    //TODO check why delay is negative
                    throw new IllegalStateException("Negative delay");
                }
                delay.add(delayTime);

                // log rcv bytes
                this.rcvBytes += payload.length;
                this.totalRcvBytes += payload.length;
                this.totalRcvPackets++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void scheduledWriteOutput() {
        // trace values
        List<Long> currentDelay = delay;

        // reset values
        double traceIntervalInS = (double) TRACE_INTERVAL_IN_MS / 1000;
        double currentRcvMBits = (rcvBytes * 8) / 1e6;
        rcvBytes = 0;
        delay = Collections.synchronizedList(new ArrayList<>());

        // calculate metrics
        double goodput =  currentRcvMBits / traceIntervalInS;
        lastGoodputMpbs = goodput;

        double delaySum = 0;
        for (long t : currentDelay) {
            delaySum += t;
        }

        double avgDelay;

        if (currentDelay.size() == 0) {
            avgDelay = 0;
        } else {
            avgDelay = delaySum / currentDelay.size();
        }
        lastDelay = avgDelay;

        long currentTime = this.timeProvider.getAdjustedTime();
        double simTime = (currentTime-simulationBegin.getTime())/1000.0;

        // write to file
        try {
            writer.write(String.format(Locale.ROOT, "%.06f,%d,%d,%s,%.02f,%.02f", simTime, id, mode, connectedAddress, goodput, avgDelay));
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void scheduledLoggingOutput() {
        long currentTime = this.timeProvider.getAdjustedTime();
        double simTime = (currentTime-simulationBegin.getTime())/1000.0;
        ConsoleLogger.log("%s | %.02fs | received %d packets [%.02f Mbps] [%.02f ms]", connectedAddress, simTime, totalRcvPackets, lastGoodputMpbs, lastDelay);
    }

    @Override
    public void close() throws IOException {
        if (closed) {
            // already closed. Nothing to do
            return;
        }
        super.close();
        closed = true;
        writer.flush();
        writer.close();
    }

    private int booleanToInt(boolean mode) {
        if (mode) {
            return 1;
        } else {
            return 0;
        }
    }
}
