package application.sink;

import general.ConsoleLogger;
import general.NTPClient;
import general.Utility;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class LogSink extends Sink {

    long initialTime;
    String connectedAddress;
    BufferedWriter writer;

    public LogSink(NTPClient ntp, int port, int receiveBufferSize, BufferedWriter writer) throws IOException {
        super(ntp, port, receiveBufferSize);
        initialTime = new Timestamp(System.currentTimeMillis()).getTime();
        this.writer = writer;
    }

    @Override
    public void executeLogic(Socket client) {
        try {
            InputStream in = client.getInputStream();

            connectedAddress = client.getInetAddress().getHostName();

            byte[] payload = new byte[1000];

            while (in.read(payload) != -1) {
                // calc delay
                long sendTime = Utility.decodeTime(payload);
                long currentTime = this.ntp.getCurrentTimeNormalized();
                long delayTime = currentTime - sendTime;
                delay.add(delayTime);

                // log rcv bytes
                this.rcvBytes += payload.length;
            }
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void scheduledOperation() {
        // trace values
        int timeIntervalInSeconds = traceIntervalInMs * 1000;
        int currentRcvBytes = rcvBytes;
        List<Long> currentDelay = delay;

        // reset values
        rcvBytes = 0;
        delay = new ArrayList<>();

        // calculate metrics
        double goodput = (currentRcvBytes * 8 / timeIntervalInSeconds) / 1e6;

        double delaySum = 0;
        for (long t : currentDelay) {
            delaySum += t;
        }
        double avgDelay = delaySum/currentDelay.size();

        long currentTime = this.ntp.getCurrentTimeNormalized();
        long simTime = currentTime-initialTime;

        // write to file
        try {
            writer.write(String.format("%s;%s;%s;%s", simTime, connectedAddress, goodput, avgDelay));
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
