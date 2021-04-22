package client;

import general.NTPClient;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

public abstract class Source {
    Socket socket;
    String name;
    int sendBufferSize;
    int numberOfMBytes;

    int waitTime = 10000; // 10s

    NTPClient ntp;

    public Source(
            String ntpAddress,
            String address,
            int port,
            String name,
            int numberOfMBytes,
            int numberOfTransmissions,
            int sendBufferSize,
            int waitTime
    ) throws IOException {
        this(ntpAddress, address, port, name, numberOfMBytes, numberOfTransmissions, sendBufferSize);
        this.waitTime = waitTime;
    }

    public Source(
            String ntpAddress,
            String address,
            int port,
            String name,
            int numberOfMBytes,
            int numberOfTransmissions,
            int sendBufferSize
    ) throws IOException {
        this.name = name;
        this.numberOfMBytes = numberOfMBytes;
        this.sendBufferSize = sendBufferSize;
        this.ntp = new NTPClient(ntpAddress);

        try {
            for (int i = 0; i < numberOfTransmissions; i++) {
                connect(address, port);
                execute();
                close();
                Thread.sleep(this.waitTime);
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
