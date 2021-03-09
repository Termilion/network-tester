package server;

import general.BulkMessage;
import general.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;

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

            while (message != null) {
                numberOfMessages++;
                String type = message.getType();
                String name = message.getName();
                switch(type) {
                    case "bulk":
                        BulkMessage bulkMessage = (BulkMessage) message;
                        int maxSize = bulkMessage.getMaxSize();
                        System.out.printf("%s [%s]: received message [%d/%d]\n", name, client.getInetAddress(), numberOfMessages, maxSize);
                        break;
                    default:
                        System.out.printf("%s [%s]: received message [%d]\n", name, client.getInetAddress(), numberOfMessages);
                }
                message = (Message) in.readObject();
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
