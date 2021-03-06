package application.sink;

import general.TimeProvider;
import general.Utility.TransmissionPayload;
import general.logger.ConsoleLogger;
import general.logger.FileLogger;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

public class LogSink extends Sink {

    public static final String IDENTIFIER_GOODPUT = "goodput";
    public static final String IDENTIFIER_ABSOLUTE_DELAY = "absolute_delay";
    public static final String IDENTIFIER_DURATION = "duration";

    public static final String[] IDENTIFIERS = new String[] {
            IDENTIFIER_GOODPUT,
            IDENTIFIER_ABSOLUTE_DELAY,
            IDENTIFIER_DURATION
    };

    private static class AbsoluteDelayItem {
        long receiveTime;
        int round;
        int packetId;
        long delayTime;

        public AbsoluteDelayItem(long receiveTime, int round, int packetId, long delayTime) {
            this.receiveTime = receiveTime;
            this.round = round;
            this.packetId = packetId;
            this.delayTime = delayTime;
        }
    }

    String connectedAddress;
    int modeInt;

    String goodputCsvPath;
    File goodputCsvFile;
    BufferedWriter goodputCsv;
    String absoluteDelayCsvPath;
    File absoluteDelayCsvFile;
    BufferedWriter absoluteDelayCsv;
    String durationCsvPath;
    File durationCsvFile;
    BufferedWriter durationCsv;

    int index = 0;
    final AtomicLong rcvBytesForGoodputCsv;
    final List<Long> delayForGoodputCsv;
    final Queue<AbsoluteDelayItem> absoluteDelayItemQueue;
    final Map<Integer, Long> roundStartTimes;
    final AtomicLong rcvBytesForLog;
    final List<Long> delayForLog;

    boolean closed = false;
    long totalRcvPackets = 0;

    long lastTraceTime = -1;
    long lastLogTime = -1;

    public LogSink(TimeProvider timeProvider, int port, int receiveBufferSize, String parentPath, int traceIntervalMs, int id, Mode mode, Direction directionAsSeenByClient) throws IOException {
        super(timeProvider, port, receiveBufferSize, traceIntervalMs, id, mode, directionAsSeenByClient);
        this.modeInt = mode.getLogInt();
        this.rcvBytesForGoodputCsv = new AtomicLong(0);
        this.delayForGoodputCsv = Collections.synchronizedList(new ArrayList<>());
        this.absoluteDelayItemQueue = new ConcurrentLinkedQueue<>();
        this.roundStartTimes = new HashMap<>();
        this.rcvBytesForLog = new AtomicLong(0);
        this.delayForLog = Collections.synchronizedList(new ArrayList<>());

        this.goodputCsvPath = String.format("%s_flow_%d_%s_%s.csv", parentPath, id, mode.getName(), IDENTIFIER_GOODPUT);
        this.absoluteDelayCsvPath = String.format("%s_flow_%d_%s_%s.csv", parentPath, id, mode.getName(), IDENTIFIER_ABSOLUTE_DELAY);
        this.durationCsvPath = String.format("%s_flow_%d_%s_%s.csv", parentPath, id, mode.getName(), IDENTIFIER_DURATION);
        createLogFiles();
    }

    private void createLogFiles() throws IOException {
        goodputCsvFile = new File(goodputCsvPath);
        goodputCsvFile.getParentFile().mkdirs();
        if (goodputCsvFile.exists()) {
            boolean deleted = goodputCsvFile.delete();
            if (!deleted) {
                throw new IOException("Can not delete " + goodputCsvPath);
            }
        }
        absoluteDelayCsvFile = new File(absoluteDelayCsvPath);
        absoluteDelayCsvFile.getParentFile().mkdirs();
        if (absoluteDelayCsvFile.exists()) {
            boolean deleted = absoluteDelayCsvFile.delete();
            if (!deleted) {
                throw new IOException("Can not delete " + absoluteDelayCsvPath);
            }
        }
        durationCsvFile = new File(durationCsvPath);
        durationCsvFile.getParentFile().mkdirs();
        if (durationCsvFile.exists()) {
            boolean deleted = durationCsvFile.delete();
            if (!deleted) {
                throw new IOException("Can not delete " + durationCsvPath);
            }
        }
        goodputCsv = new BufferedWriter(new FileWriter(goodputCsvFile, true));
        goodputCsv.write("index,time,flow,type,direction,address,sink_gp,delay_data_ms");
        goodputCsv.newLine();
        goodputCsv.flush();
        absoluteDelayCsv = new BufferedWriter(new FileWriter(absoluteDelayCsvFile, true));
        absoluteDelayCsv.write("time,flow,type,direction,address,round,packet_id,delay_data_ms");
        absoluteDelayCsv.newLine();
        absoluteDelayCsv.flush();
        durationCsv = new BufferedWriter(new FileWriter(durationCsvFile, true));
        durationCsv.write("time,flow,type,direction,address,round,duration_ms");
        durationCsv.newLine();
        durationCsv.flush();
    }

    @Override
    public void executeLogic() {
        try {
            InputStream in = client.getInputStream();
            DataInputStream dataInputStream = new DataInputStream(in);

            connectedAddress = client.getInetAddress().getHostAddress();

            byte[] payload = new byte[1000];

            while (isRunning) {
                TransmissionPayload transmissionPayload;
                try {
                    dataInputStream.readFully(payload);
                    transmissionPayload = TransmissionPayload.decode(payload);
                } catch (EOFException e) {
                    ConsoleLogger.log("Transmission ended");
                    break;
                } catch (Exception e) {
                    ConsoleLogger.log("Problem decoding payload");
                    FileLogger.log("Problem decoding payload");
                    break;
                }

                // calc delay
                int packetId = transmissionPayload.getId();
                long sendTime = transmissionPayload.getTime();
                int round = transmissionPayload.getRound();
                boolean firstPacketOfRound = transmissionPayload.isFirstPacketOfRound();
                boolean lastPacketOfRound = transmissionPayload.isLastPacketOfRound();

                long currentTime = this.timeProvider.getAdjustedTime();
                long delayTime = currentTime - sendTime;

                measureDelay(delayTime, packetId, currentTime, round);
                measureBytes(payload.length);
                measureDuration(sendTime, currentTime, round, firstPacketOfRound, lastPacketOfRound);
            }
        } catch (IllegalStateException e) {
            System.exit(1);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void measureDelay(long delayTime, int packetId, long receiveTime, int round) {
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
        // Put results into lists, to not block the receiving thread (calculations are performed on another thread)
        delayForGoodputCsv.add(delayTime);
        delayForLog.add(delayTime);
        if (mode == Mode.IOT) {
            absoluteDelayItemQueue.add(new AbsoluteDelayItem(receiveTime, round, packetId, delayTime));
        }
    }

    private void measureBytes(int payloadLength) {
        // log rcv bytes
        this.rcvBytesForGoodputCsv.addAndGet(payloadLength);
        this.rcvBytesForLog.addAndGet(payloadLength);
        this.totalRcvPackets++;
    }

    private void measureDuration(long sendTime, long receiveTime, int round, boolean firstPacketOfRound, boolean lastPacketOfRound) {
        if (firstPacketOfRound) {
            roundStartTimes.put(round, sendTime);
        }
        if (lastPacketOfRound) {
            Long firstPacketSendTime = roundStartTimes.get(round);
            if (firstPacketSendTime == null) {
                throw new IllegalStateException("This is the last packet of round " + round + " but there is no start time available");
            }
            long duration = receiveTime - firstPacketSendTime;

            //time,flow,type,address,round,duration_ms
            try {
                String address = isConnected ? connectedAddress : null;
                double receiveSimTime = (receiveTime-beginTime.getTime()) / 1000.0;

                durationCsv.write(String.format(Locale.ROOT, "%.06f,%d,%d,%s,%s,%d,%d", receiveSimTime, id, modeInt, directionAsSeenByClient.getName(), address, round, duration));
                durationCsv.newLine();
            } catch (IOException e) {
                FileLogger.log("ERROR in LogSink: " + e, ConsoleLogger.LogLevel.ERROR);
                e.printStackTrace();
            }
        }
    }

    @Override
    public void scheduledWriteOutput() {
        try {
            long now = timeProvider.getAdjustedTime();
            double simTime = (now - beginTime.getTime()) / 1000.0;
            writeGoodput(now, simTime);
            writeAbsoluteDelay();
        } catch (Exception e) {
            FileLogger.log("ERROR in LogSink: " + e, ConsoleLogger.LogLevel.ERROR);
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void writeGoodput(long now, double simTime) {
        // trace values
        List<Long> currentDelay;
        synchronized (delayForGoodputCsv) {
            currentDelay = new ArrayList<>(delayForGoodputCsv);
            delayForGoodputCsv.clear();
        }

        double currentRcvMBits;
        synchronized (rcvBytesForGoodputCsv) {
            currentRcvMBits = (rcvBytesForGoodputCsv.get() * 8) / 1e6;
            rcvBytesForGoodputCsv.set(0);
        }

        double traceIntervalInS;
        if (lastTraceTime == -1) {
            traceIntervalInS = TRACE_INTERVAL_IN_MS / 1000.0;
        } else {
            traceIntervalInS = (now - lastTraceTime) / 1000.0;
            lastTraceTime = now;
        }

        // calculate goodput
        double goodput = avgGoodput(currentRcvMBits, traceIntervalInS);

        // calculate delay
        double avgDelay = avgDelay(currentDelay);

        // write to file
        try {
            String address;
            if (isConnected || goodput > 0 || avgDelay > 0) {
                address = connectedAddress;
            } else {
                address = null;
            }
            goodputCsv.write(String.format(Locale.ROOT, "%d,%.06f,%d,%d,%s,%s,%.02f,%.02f", index, simTime, id, modeInt, directionAsSeenByClient.getName(), address, goodput, avgDelay));
            goodputCsv.newLine();
            index++;
        } catch (IOException e) {
            FileLogger.log("ERROR in LogSink: " + e, ConsoleLogger.LogLevel.ERROR);
            e.printStackTrace();
        }
    }

    private void writeAbsoluteDelay() {
        // time,flow,type,direction,address,round,packet_id,delay_data_ms
        AbsoluteDelayItem item;
        while ((item = absoluteDelayItemQueue.poll()) != null) {
            // write to file
            try {
                String address = isConnected ? connectedAddress : null;

                double receiveSimTime = (item.receiveTime - beginTime.getTime()) / 1000.0;

                absoluteDelayCsv.write(String.format(Locale.ROOT, "%.06f,%d,%d,%s,%s,%d,%d,%d", receiveSimTime, id, modeInt, directionAsSeenByClient.getName(), address, item.round, item.packetId, item.delayTime));
                absoluteDelayCsv.newLine();
            } catch (IOException e) {
                FileLogger.log("ERROR in LogSink: " + e, ConsoleLogger.LogLevel.ERROR);
                e.printStackTrace();
            }
        }
    }

    @Override
    public void scheduledLoggingOutput() {
        try {
            long now = timeProvider.getAdjustedTime();

            // trace values
            List<Long> currentDelay;
            synchronized (delayForLog) {
                currentDelay = new ArrayList<>(delayForLog);
                delayForLog.clear();
            }

            double currentRcvMBits;
            synchronized (rcvBytesForLog) {
                currentRcvMBits = (rcvBytesForLog.get() * 8) / 1e6;
                rcvBytesForLog.set(0);
            }

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

            ConsoleLogger.log("%d\t| %s\t| ??? | %d packets\t[%.02f Mbps]\t[%.02f ms]", id, address, totalRcvPackets, goodput, avgDelay);
            FileLogger.log("%d | %s | ??? | Last %d packets [%.02f Mbps] [%.02f ms]", id, address, totalRcvPackets, goodput, avgDelay);
            this.chart.plotData((int) getSimTime(), goodput, avgDelay);
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
        goodputCsv.flush();
        absoluteDelayCsv.flush();
        durationCsv.flush();
        goodputCsv.close();
        absoluteDelayCsv.close();
        durationCsv.close();
        super.close();
    }

    public Map<String, String> getFilePaths() {
        return new HashMap<String, String>() {{
            put(IDENTIFIER_GOODPUT, goodputCsvPath);
            put(IDENTIFIER_ABSOLUTE_DELAY, absoluteDelayCsvPath);
            put(IDENTIFIER_DURATION, durationCsvPath);
        }};
    }
}
