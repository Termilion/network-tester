package application.source;

import general.ConsoleLogger;
import general.NTPClient;

import java.io.IOException;
import java.net.Socket;

public abstract class Source {
    Socket socket;
    int sendBufferSize;
    double numberOfBytesToSend;
    String address;
    int port;

    int waitTime = -1; // 1s

    NTPClient ntp;

    public Source(
            NTPClient ntp,
            String address,
            int port,
            double numberOfBytesToSend,
            int sendBufferSize,
            int waitTime
    ) {
        this.waitTime = waitTime;
        this.numberOfBytesToSend = numberOfBytesToSend;
        this.sendBufferSize = sendBufferSize;
        this.ntp = ntp;
        this.address = address;
        this.port = port;

        try {
            ConsoleLogger.log("connecting to %s:%s", address, port);
            connect(address, port);

            if(waitTime > 0) {
                reset(waitTime);
            }
            execute();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }


    protected void connect(String address, int port) throws IOException {
        socket = new Socket(address, port);
    }

    protected void close() throws IOException {
        if (socket != null) {
            if(!socket.isClosed()) {
                socket.close();
            }
        }
    }

    protected void reset(int waitTime) {
        new Thread() {
            @Override
            public void run() {
                try {
                    while(true) {
                        Thread.sleep(waitTime);
                        close();
                        connect(address, port);
                        execute();
                    }
                } catch(Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();
    }

    public abstract void execute() throws IOException;
}
