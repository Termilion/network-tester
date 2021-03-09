package client;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

public abstract class Client {
    Socket socket;
    String name;
    int numberOfMBytes;

    int waitTime = 10000; // 10s

    public Client(String address, int port, String name, int numberOfMBytes, int numberOfTransmissions) {
        this.name = name;
        this.numberOfMBytes = numberOfMBytes;
        try {
            for (int i = 0; i < numberOfTransmissions; i++) {
                connect(address, port);
                execute();
                close();
                Thread.sleep(waitTime);
            }
        } catch(InterruptedException e) {
            System.out.println("Thread has been interrupted");
        } catch(UnknownHostException e) {
            System.out.printf("Server not found: %s:%d\n", address, port);
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    private void connect(String address, int port) throws IOException {
        socket = new Socket(address, port);
    }

    private void close() throws IOException {
        socket.close();
    }

    public abstract void execute() throws IOException;
}
