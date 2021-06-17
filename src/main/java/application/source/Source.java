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

    NTPClient ntp;

    public Source(
            NTPClient ntp,
            String address,
            int port,
            double numberOfBytesToSend,
            int sendBufferSize,
            int resetTime
    ) {
        this.numberOfBytesToSend = numberOfBytesToSend;
        this.sendBufferSize = sendBufferSize;
        this.ntp = ntp;
        this.address = address;
        this.port = port;

        try {
            ConsoleLogger.log("connecting to %s:%s", address, port);
            connect(address, port);

            if(resetTime > 0) {
                reset(resetTime);
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

    protected void reset(int resetTime) {
        new Thread() {
            @Override
            public void run() {
                try {
                    while(true) {
                        Thread.sleep(resetTime);
                        ConsoleLogger.log("%s | calling reset", socket.getInetAddress().getHostAddress());
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
