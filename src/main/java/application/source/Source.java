package application.source;

import general.ConsoleLogger;
import general.NTPClient;

import java.io.IOException;
import java.net.Socket;
import java.util.Date;

@SuppressWarnings("BusyWait")
public abstract class Source {
    Socket socket;
    int sendBufferSize;
    String address;
    int port;

    NTPClient ntp;

    boolean isRunning = true;

    public Source(
            NTPClient ntp,
            String address,
            int port,
            int sendBufferSize,
            int resetTime,
            Date stopTime
    ) {
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
            stopOn(stopTime);
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
        isRunning = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    protected void reset(int resetTime) {
        new Thread(() -> {
            try {
                while (true) {
                    Thread.sleep(resetTime);
                    if (!isRunning) {
                        break;
                    }
                    ConsoleLogger.log("%s | calling reset", socket.getInetAddress().getHostAddress());
                    close();
                    connect(address, port);
                    execute();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    protected void stopOn(Date stopTime) {
        ConsoleLogger.log("Stopping sink on " + stopTime);
        new Thread(() -> {
            try {
                long now = ntp.getCurrentTimeNormalized();
                long duration = stopTime.getTime() - now;
                if (duration < 0) {
                    throw new IllegalArgumentException("stopTime lies in the past: " + stopTime.getTime() + " Now is " + now);
                }
                Thread.sleep(duration);
                isRunning = false;
                ConsoleLogger.log("%s | stopping source", socket.getInetAddress().getHostAddress());
                close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public abstract void execute() throws IOException;
}
