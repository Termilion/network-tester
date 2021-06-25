package application.sink;

import general.ConsoleLogger;
import general.NTPClient;
import general.Utility;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class LogSink extends Sink {

    String connectedAddress;
    int id;
    int mode;

    public LogSink(NTPClient ntp, int port, int receiveBufferSize, String filePath, Date simulationBegin, Date stopTime, int id, boolean mode) throws IOException {
        super(ntp, port, receiveBufferSize, stopTime, new Object[]{filePath, simulationBegin});
        this.id = id;
        this.mode = booleanToInt(mode);
        createLogFile(filePath);
    }

    public void createLogFile(String filePath) throws IOException {
        File outFile = new File(filePath);
        outFile.getParentFile().mkdirs();
        BufferedWriter writer = new BufferedWriter(new FileWriter(outFile, false));
        writer.write("time,flow,type,address,sink_gp,delay_data_ms");
        writer.newLine();
        writer.flush();
        writer.close();
    }

    @Override
    public void executeLogic() {
        try {
            InputStream in = client.getInputStream();
            DataInputStream dataInputStream = new DataInputStream(in);

            connectedAddress = client.getInetAddress().getHostAddress();

            byte[] payload = new byte[1000];

            int number = 0;
            int total = 0;

            while (isRunning) {
                try {
                    dataInputStream.readFully(payload);
                } catch (EOFException e) {
                    ConsoleLogger.log("reached end of file");
                    break;
                }
                // calc delay
                long sendTime = Utility.decodeTime(payload);
                long currentTime = this.ntp.getCurrentTimeNormalized();
                long delayTime = currentTime - sendTime;
                delay.add(delayTime);

                // log rcv bytes
                this.rcvBytes += payload.length;
                number++;
                if (number >= 1000) {
                    total++;
                    ConsoleLogger.log("%s | received an MByte! [%s total]", connectedAddress, total);
                    number = 0;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void scheduledOperation(Object[] args) {
        String filePath = (String) args[0];
        Date simulationBegin = (Date) args[1];

        // trace values
        List<Long> currentDelay = delay;

        // reset values
        double traceIntervalInS = (double) TRACE_INTERVAL_IN_MS / 1000;
        double currentRcvMBits = (rcvBytes * 8) / 1e6;
        rcvBytes = 0;
        delay = Collections.synchronizedList(new ArrayList<>());

        // calculate metrics
        double goodput =  currentRcvMBits / traceIntervalInS;

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

        long currentTime = this.ntp.getCurrentTimeNormalized();
        double simTime = (currentTime-simulationBegin.getTime())/1000.0;

        // write to file
        try {
            File outFile = new File(filePath);
            BufferedWriter writer = new BufferedWriter(new FileWriter(outFile, true));
            writer.write(String.format("%.06f,%d,%d,%s,%.02f,%.02f", simTime, id, mode, connectedAddress, goodput, avgDelay));
            writer.newLine();
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int booleanToInt(boolean mode) {
        if (mode) {
            return 1;
        } else {
            return 0;
        }
    }
}
