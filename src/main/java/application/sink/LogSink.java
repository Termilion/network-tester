package application.sink;

import general.ConsoleLogger;
import general.TimeProvider;
import general.Utility;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class LogSink extends Sink {

    String connectedAddress;
    int id;
    int mode;

    File outFile;
    BufferedWriter writer;

    int index = 0;
    int rcvBytes = 0;
    List<Long> delay;

    boolean closed = false;
    long totalRcvBytes = 0;
    long totalRcvPackets = 0;
    double lastGoodputMpbs = 0;
    double lastDelay = 0;

    long lastTraceTime = -1;

    public LogSink(TimeProvider timeProvider, int port, int receiveBufferSize, String filePath, int id, boolean mode) throws IOException {
        super(timeProvider, port, receiveBufferSize);
        this.id = id;
        this.mode = booleanToInt(mode);
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
        writer.write("index,time,flow,type,address,sink_gp,delay_data_ms");
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
                if (delayTime < -30) {
                    // if delay is less than epsilon (-30) abort. Something went wrong during time sync
                    ConsoleLogger.log("Packet has negative delay: " + delayTime, ConsoleLogger.LogLevel.WARN);
                    throw new IllegalStateException("Negative delay");
                }
                if (delayTime < 0) {
                    // if delay is within epsilon [-50:0] set to 0. Precision error during time sync.
                    delayTime = 0;
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
        long now = timeProvider.getAdjustedTime();

        // trace values
        List<Long> currentDelay = delay;
        double currentRcvMBits = (rcvBytes * 8) / 1e6;

        // reset values
        rcvBytes = 0;
        delay = Collections.synchronizedList(new ArrayList<>());

        double traceIntervalInS;
        if (lastTraceTime == -1) {
            traceIntervalInS = TRACE_INTERVAL_IN_MS / 1000.0;
        } else {
            traceIntervalInS = (now - lastTraceTime) / 1000.0;
            lastTraceTime = now;
        }

        // calculate metrics
        double goodput = currentRcvMBits / traceIntervalInS;
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

        double simTime = (now - beginTime.getTime()) / 1000.0;

        // write to file
        try {
            writer.write(String.format(Locale.ROOT, "%d,%.06f,%d,%d,%s,%.02f,%.02f", index, simTime, id, mode, connectedAddress, goodput, avgDelay));
            writer.newLine();
            index++;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void scheduledLoggingOutput() {
        ConsoleLogger.log("%s | %d packets [%.02f Mbps] [%.02f ms]", connectedAddress, totalRcvPackets, lastGoodputMpbs, lastDelay);
    }

    @Override
    public synchronized void close() throws IOException {
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
