package application.sink;

import general.TimeProvider;
import general.Utility;
import general.logger.ConsoleLogger;
import general.logger.FileLogger;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import static application.Application.LOG_INTERVAL_IN_MS;

public class LogSink extends Sink {

    String connectedAddress;
    int id;
    int mode;

    File outFile;
    BufferedWriter writer;

    int index = 0;
    AtomicInteger rcvBytesForCsv;
    List<Long> delayForCsv;
    AtomicInteger rcvBytesForLog;
    List<Long> delayForLog;

    boolean closed = false;
    long totalRcvPackets = 0;

    long lastTraceTime = -1;
    long lastLogTime = -1;

    public LogSink(TimeProvider timeProvider, int port, int receiveBufferSize, String filePath, int id, boolean mode, int traceIntervalMs) throws IOException {
        super(timeProvider, port, receiveBufferSize, traceIntervalMs);
        this.id = id;
        this.mode = booleanToInt(mode);
        this.rcvBytesForCsv = new AtomicInteger(0);
        this.delayForCsv = Collections.synchronizedList(new ArrayList<>());
        this.rcvBytesForLog = new AtomicInteger(0);
        this.delayForLog = Collections.synchronizedList(new ArrayList<>());
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
        writer.flush();
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
                    ConsoleLogger.log("Transmission ended");
                    break;
                }
                // calc delay
                long sendTime = Utility.decodeTime(payload);
                long currentTime = this.timeProvider.getAdjustedTime();
                long delayTime = currentTime - sendTime;

                measureDelay(delayTime);
                measureBytes(payload.length);
            }
        } catch (IllegalStateException e) {
            System.exit(1);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void measureDelay(long delayTime) {
        if (delayTime < -30) {
            // if delay is less than epsilon (-30) abort. Something went seriously wrong during time sync
            ConsoleLogger.log("ERROR: Packet has negative delay: " + delayTime, ConsoleLogger.LogLevel.ERROR);
            FileLogger.log("ERROR: Packet has negative delay: " + delayTime, ConsoleLogger.LogLevel.ERROR);
            throw new IllegalStateException("Negative delay");
        }
        if (delayTime < 0) {
            // TODO this is a dirty "hack". Use a better time sync algorithm OR
            //  capture the greatest negative delay over the whole simulation and later add its absolute value to every measured delay
            // if delay is within epsilon [-30:0] set to 0. Precision error during time sync.
            ConsoleLogger.log("WARN: Packet has negative delay: " + delayTime, ConsoleLogger.LogLevel.WARN);
            delayTime = 0;
        }
        delayForCsv.add(delayTime);
        delayForLog.add(delayTime);
    }

    private void measureBytes(int payloadLength) {
        // log rcv bytes
        this.rcvBytesForCsv.addAndGet(payloadLength);
        this.rcvBytesForLog.addAndGet(payloadLength);
        this.totalRcvPackets++;
    }

    @Override
    public void scheduledWriteOutput() {
        try {
            long now = timeProvider.getAdjustedTime();
            FileLogger.log("LogSink: TimeProvider is ok");

            // trace values
            List<Long> currentDelay = delayForCsv;
            double currentRcvMBits = (rcvBytesForCsv.get() * 8) / 1e6;

            // reset values
            rcvBytesForCsv.set(0);
            delayForCsv = Collections.synchronizedList(new ArrayList<>());

            double traceIntervalInS;
            if (lastTraceTime == -1) {
                traceIntervalInS = TRACE_INTERVAL_IN_MS / 1000.0;
            } else {
                traceIntervalInS = (now - lastTraceTime) / 1000.0;
                lastTraceTime = now;
            }

            // calculate goodput
            double goodput = avgGoodput(currentRcvMBits, traceIntervalInS);
            FileLogger.log("LogSink: goodput is ok");

            // calculate delay
            double avgDelay = avgDelay(currentDelay);
            FileLogger.log("LogSink: delay is ok");

            double simTime = (now - beginTime.getTime()) / 1000.0;
            FileLogger.log("LogSink: beginTime is ok");

            // write to file
            try {
                String address;
                if (isConnected || goodput > 0 || avgDelay > 0) {
                    address = connectedAddress;
                } else {
                    address = null;
                }
                writer.write(String.format(Locale.ROOT, "%d,%.06f,%d,%d,%s,%.02f,%.02f", index, simTime, id, mode, address, goodput, avgDelay));
                FileLogger.log("LogSink: Writer.write is ok");
                writer.newLine();
                index++;
            } catch (IOException e) {
                FileLogger.log("ERROR in LogSink Writer: " + e, ConsoleLogger.LogLevel.ERROR);
                e.printStackTrace();
            }
        } catch (Exception e) {
            FileLogger.log("ERROR in LogSink: " + e, ConsoleLogger.LogLevel.ERROR);
            e.printStackTrace();
            System.exit(1);
        }
    }

    @Override
    public void scheduledLoggingOutput() {
        try {
            long now = timeProvider.getAdjustedTime();

            // trace values
            List<Long> currentDelay = delayForLog;
            double currentRcvMBits = (rcvBytesForLog.get() * 8) / 1e6;

            // reset values
            rcvBytesForLog.set(0);
            delayForLog = Collections.synchronizedList(new ArrayList<>());

            double logIntervalInS;
            if (lastLogTime == -1) {
                logIntervalInS = LOG_INTERVAL_IN_MS / 1000.0;
            } else {
                logIntervalInS = (now - lastLogTime) / 1000.0;
                lastLogTime = now;
            }

            // calculate goodput
            double goodput = avgGoodput(currentRcvMBits, logIntervalInS);

            // calculate delay
            double avgDelay = avgDelay(currentDelay);

            String address;
            if (isConnected || goodput > 0 || avgDelay > 0) {
                address = connectedAddress;
            } else {
                address = null;
            }

            ConsoleLogger.log("%d\t| %s\t| ↓ | %d packets\t[%.02f Mbps]\t[%.02f ms]", id, address, totalRcvPackets, goodput, avgDelay);
            FileLogger.log("%d | %s | ↓ | Last %d packets [%.02f Mbps] [%.02f ms]", id, address, totalRcvPackets, goodput, avgDelay);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private double avgDelay(List<Long> delayList) {
        // calculate avg delay
        double delaySum = 0;
        for (long t : delayList) {
            delaySum += t;
        }

        double avgDelay;

        if (delayList.size() == 0) {
            avgDelay = 0;
        } else {
            avgDelay = delaySum / delayList.size();
        }
        return avgDelay;
    }

    private double avgGoodput(double rcvMBits, double traceIntervalInS) {
        return rcvMBits / traceIntervalInS;
    }

    @Override
    public synchronized void close() throws IOException {
        if (closed) {
            // already closed. Nothing to do
            return;
        }
        closed = true;
        writer.flush();
        FileLogger.log("LogSink: Writer is closed");
        writer.close();
        super.close();
    }

    private int booleanToInt(boolean mode) {
        if (mode) {
            return 1;
        } else {
            return 0;
        }
    }
}
