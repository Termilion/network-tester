package application.sink;

import general.ConsoleLogger;
import general.NTPClient;
import general.Utility;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.sql.Timestamp;

public class LogSink extends Sink {

    public LogSink(NTPClient ntp, int port, int receiveBufferSize, BufferedWriter writer) throws IOException {
        super(ntp, port, receiveBufferSize, writer);
    }

    @Override
    public void executeLogic(Socket client, BufferedWriter writer) {
        try {
            InputStream in = client.getInputStream();

            long initialTime = new Timestamp(System.currentTimeMillis()).getTime();
            long numberOfMessages = 0;

            byte[] payload = new byte[1000];
            in.read(payload);

            while (true) {
                long sendTime = Utility.decodeTime(payload);

                String address = client.getInetAddress().getHostName();

                long currentTime = this.ntp.getCurrentTimeNormalized();
                float travelTimeInMS = currentTime - sendTime;
                float sizeInByte = payload.length;

                double goodput = -1;
                if (travelTimeInMS > 0) {
                    goodput = (sizeInByte * 8 / travelTimeInMS) / 1000000;
                }

                ConsoleLogger.log(
                        String.format(
                                "[%s] [%d]: %.02f Mbps (%s ms)",
                                address,
                                numberOfMessages,
                                goodput,
                                travelTimeInMS
                        )
                );
                writer.write(String.format("%s;%s;%s;%s", currentTime-initialTime, address, goodput, travelTimeInMS));
                writer.newLine();
                try {
                    in.read(payload);
                } catch (EOFException e) {
                    break;
                }
            }
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
