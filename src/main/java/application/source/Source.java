package application.source;

import general.NTPClient;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

public abstract class Source {
    Socket socket;
    int sendBufferSize;
    int numberOfMBytes;

    int waitTime = 1000; // 1s

    NTPClient ntp;

    public Source(
            NTPClient ntp,
            String address,
            int port,
            int numberOfMBytes,
            int numberOfTransmissions,
            int sendBufferSize,
            int waitTime
    ) {
        this(ntp, address, port, numberOfMBytes, numberOfTransmissions, sendBufferSize);
        this.waitTime = waitTime;
    }

    public Source(
            NTPClient ntp,
            String address,
            int port,
            int numberOfMBytes,
            int numberOfTransmissions,
            int sendBufferSize
    ) {
        this.numberOfMBytes = numberOfMBytes;
        this.sendBufferSize = sendBufferSize;
        this.ntp = ntp;

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
