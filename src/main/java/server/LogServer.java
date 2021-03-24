package server;

import general.BulkMessage;
import general.Message;

import java.io.*;
import java.net.Socket;
import java.sql.Timestamp;

public class LogServer extends Server {

    public LogServer(String ntpAddress, int port, int receiveBufferSize, BufferedWriter writer) throws IOException {
        super(ntpAddress, port, receiveBufferSize, writer);
    }

    @Override
    public void executeLogic(Socket client, BufferedWriter writer) {
        try {
            ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(client.getInputStream()));
            long initialTime = new Timestamp(System.currentTimeMillis()).getTime();
            long numberOfMessages = 0;
            Message message = (Message) in.readUnshared();

            while (true) {
                numberOfMessages++;
                String type = message.getType();
                String name = message.getName();
                String address = client.getInetAddress().getHostName();
                long sendTime = message.getTimestamp().getTime();
                long currentTime = this.ntp.getCurrentTimeNormalized();
                float travelTimeInMS = currentTime - sendTime;
                float sizeInByte = message.getPayload().length;

                double goodput = -1;
                if (travelTimeInMS > 0) {
                    goodput = (sizeInByte / travelTimeInMS) / 1000;
                }

                if ("bulk".equals(type)) {
                    BulkMessage bulkMessage = (BulkMessage) message;
                    int maxSize = bulkMessage.getMaxSize();
                    System.out.printf("[%s] %s: received message [%d/%d]: %s Mbps (%s ms)\n", address, name, numberOfMessages, maxSize, goodput, travelTimeInMS);
                } else {
                    System.out.printf("[%s] %s: received message [%d]: %s Mbps (%s ms)\n", address, name, numberOfMessages, goodput, travelTimeInMS);
                }
                writer.write(String.format("%s;%s;%s;%s;%s", currentTime-initialTime, address, name, goodput, travelTimeInMS));
                writer.newLine();
                try {
                    message = (Message) in.readObject();
                } catch (EOFException e) {
                    break;
                }
            }
            in.close();
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        }
    }
}
