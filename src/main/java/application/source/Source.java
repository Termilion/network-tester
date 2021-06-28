package application.source;

import general.ConsoleLogger;
import general.NTPClient;

import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;
import java.util.Date;

@SuppressWarnings("BusyWait")
public abstract class Source implements Closeable {
    Socket socket;
    int sendBufferSize;
    String address;
    int port;

    NTPClient ntp;

    boolean isRunning = true;

    Thread resetThread;

    public Source(
            NTPClient ntp,
            String address,
            int port,
            int sendBufferSize,
            int resetTime,
            Date stopTime
    ) throws IOException {
        this.sendBufferSize = sendBufferSize;
        this.ntp = ntp;
        this.address = address;
        this.port = port;

        if(resetTime > 0) {
            reset(resetTime);
        }
        stopOn(stopTime);

        if (this.sendBufferSize > 0) {
            this.socket.setSendBufferSize(this.sendBufferSize);
        }
    }

    public void start() throws IOException, InterruptedException {
        try {
            ConsoleLogger.log("connecting to %s:%s", address, port);
            connect(address, port);
            execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (resetThread != null) {
            // if execute finishes earlier than the simulation (resetThread) lasts, keep waiting
            resetThread.join();
        }
        try {
            close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void connect(String address, int port) throws IOException {
        socket = new Socket(address, port);
    }

    @Override
    public void close() throws IOException {
        isRunning = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    protected void reset(int resetTime) {
        resetThread = new Thread(() -> {
            try {
                while (isRunning) {
                    Thread.sleep(resetTime);
                    if (!isRunning) {
                        break;
                    }
                    ConsoleLogger.log("%s | calling reset", socket.getInetAddress().getHostAddress());
                    if (socket != null && !socket.isClosed()) {
                        socket.close();
                    }
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
        });
        resetThread.start();
    }

    protected void stopOn(Date stopTime) {
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
