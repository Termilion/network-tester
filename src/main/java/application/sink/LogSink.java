package application.sink;

import general.ConsoleLogger;
import general.NTPClient;
import general.Utility;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LogSink extends Sink {

    String connectedAddress;

    public LogSink(NTPClient ntp, int port, int receiveBufferSize, String filePath) throws IOException {
        super(ntp, port, receiveBufferSize, new Object[]{filePath, new Timestamp(System.currentTimeMillis()).getTime()});
        createLogFile(filePath);
    }

    public void createLogFile(String filePath) throws IOException {
        File outFile = new File(filePath);
        outFile.getParentFile().mkdirs();
        BufferedWriter writer = new BufferedWriter(new FileWriter(outFile, false));
        writer.write("Time;Address;Goodput;Delay");
        writer.newLine();
        writer.flush();
        writer.close();
    }

    @Override
    public void executeLogic() {
        try {
            InputStream in = client.getInputStream();
            DataInputStream dataInputStream = new DataInputStream(in);

            connectedAddress = client.getInetAddress().getHostName();

            byte[] payload = new byte[1000];

            while (true) {
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
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void scheduledOperation(Object[] args) {
        String filePath = (String) args[0];
        long initialTime = (long) args[1];

        // trace values
        double currentRcvBytes = rcvBytes;
        List<Long> currentDelay = delay;

        // reset values
        double traceIntervalInS = (double) traceIntervalInMs / 1000;
        double currentRcvMBits = (rcvBytes * 8) / 1e6;
        rcvBytes = 0;
        delay = new ArrayList<>();

        // calculate metrics
        double goodput =  currentRcvMBits / traceIntervalInS;

        double delaySum = 0;
        for (long t : currentDelay) {
            delaySum += t;
        }
        double avgDelay = delaySum/currentDelay.size();

        long currentTime = this.ntp.getCurrentTimeNormalized();
        long simTime = currentTime-initialTime;

        // write to file
        try {
            File outFile = new File(filePath);
            BufferedWriter writer = new BufferedWriter(new FileWriter(outFile, true));
            writer.write(String.format("%s;%s;%.02f;%.02f", simTime, connectedAddress, goodput, avgDelay));
            writer.newLine();
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
