package server;

import general.BulkMessage;
import general.Message;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.sql.Timestamp;

public class LogServer extends Server {
    public LogServer(int port) {
        super(port);
    }

    @Override
    public void executeLogic(Socket client) {
        try {
            ObjectInputStream in = new ObjectInputStream(client.getInputStream());
            long numberOfMessages = 0;
            Message message = (Message) in.readObject();

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

                switch(type) {
                    case "bulk":
                        BulkMessage bulkMessage = (BulkMessage) message;
                        int maxSize = bulkMessage.getMaxSize();
                        System.out.printf("[%s] %s: received message [%d/%d]: %s Mbps\n", client.getInetAddress().getHostName(), name, numberOfMessages, maxSize, goodput);
                        break;
                    default:
                        System.out.printf("[%s] %s: received message [%d]: %s Mbps\n", client.getInetAddress().getHostName(), name, numberOfMessages, goodput);
                }
                try {
                    message = (Message) in.readObject();
                } catch (EOFException e) {
                    break;
                }
            }
            in.close();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
