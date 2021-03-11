package server;

import general.BulkMessage;
import general.Message;

import java.io.*;
import java.net.Socket;
import java.sql.Timestamp;

public class LogServer extends Server {
    public LogServer(int port) {
        super(port);
    }

    @Override
    public void executeLogic(Socket client) {
        try {
            ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(client.getInputStream()));
            long numberOfMessages = 0;
            Message message = (Message) in.readUnshared();

            while (true) {
                numberOfMessages++;
                String type = message.getType();
                String name = message.getName();
                long sendTime = message.getTimestamp().getTime();
                long currentTime = new Timestamp(System.currentTimeMillis()).getTime();
                float travelTimeInMS = currentTime - sendTime;
                float sizeInByte = message.getPayload().length;

                double goodput = -1;
                if (travelTimeInMS > 0) {
                    goodput = (sizeInByte / travelTimeInMS) / 1000;
                }

                if ("bulk".equals(type)) {
                    BulkMessage bulkMessage = (BulkMessage) message;
                    int maxSize = bulkMessage.getMaxSize();
                    System.out.printf("[%s] %s: received message [%d/%d]: %s Mbps (%s ms)\n", client.getInetAddress().getHostName(), name, numberOfMessages, maxSize, goodput, travelTimeInMS);
                } else {
                    System.out.printf("[%s] %s: received message [%d]: %s Mbps (%s ms)\n", client.getInetAddress().getHostName(), name, numberOfMessages, goodput, travelTimeInMS);
                }
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
