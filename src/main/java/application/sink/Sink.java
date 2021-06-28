package application.sink;

import general.ConsoleLogger;
import general.NTPClient;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public abstract class Sink implements Closeable {
    ServerSocket socket;
    Socket client;

    NTPClient ntp;

    int rcvBytes = 0;
    List<Long> delay;

    static final int TRACE_INTERVAL_IN_MS = 50;
    static final int LOG_INTERVAL_IN_MS = 1000;

    boolean isRunning = true;

    public Sink(NTPClient ntp, int port, int receiveBufferSize, Date stopTime) throws IOException {
        this.ntp = ntp;
        this.delay = Collections.synchronizedList(new ArrayList<>());

        stopOn(stopTime);

        socket = new ServerSocket(port);
        ConsoleLogger.log("opened sink on address %s:%s", socket.getInetAddress().getHostAddress(), port);
        if (receiveBufferSize > 0) {
            socket.setReceiveBufferSize(receiveBufferSize);
        }
    }

    public void start() throws IOException {
        new Thread(() -> {
            try {
                while (isRunning) {
                    Thread.sleep(TRACE_INTERVAL_IN_MS);
                    if (!isRunning)
                        break;
                    scheduledWriteOutput();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        new Thread(() -> {
            try {
                while (isRunning) {
                    Thread.sleep(LOG_INTERVAL_IN_MS);
                    if (!isRunning)
                        break;
                    scheduledLoggingOutput();
                }
            } catch (InterruptedException e) {
                // ignore
            }
        }).start();

        while (isRunning) {
            try {
                client = socket.accept();
                executeLogic();
            } catch (Exception e) {
                // fail silently if program is gracefully stopping
                if (isRunning) {
                    e.printStackTrace();
                }
            } finally {
                // The same client could reconnect as long as our application is running
                // Only close the client socket, keep ServerSocket open
                closeClient();
            }
        }

        close();
    }

    private void closeClient() throws IOException {
        if (client != null && !client.isClosed()) {
            client.close();
        }
    }

    @Override
    public void close() throws IOException {
        isRunning = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        closeClient();
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
                ConsoleLogger.log("%s | stopping sink", socket.getInetAddress().getHostAddress());
                close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public abstract void scheduledWriteOutput();

    public abstract void scheduledLoggingOutput();

    public abstract void executeLogic();
}
