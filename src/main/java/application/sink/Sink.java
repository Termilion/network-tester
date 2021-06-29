package application.sink;

import general.ConsoleLogger;
import general.NTPClient;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Time;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public abstract class Sink implements Closeable {
    ServerSocket socket;
    Socket client;

    NTPClient ntp;

    static final int TRACE_INTERVAL_IN_MS = 50;
    static final int LOG_INTERVAL_IN_MS = 1000;

    boolean isRunning = true;

    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    Set<ScheduledFuture<?>> scheduledTasks = new HashSet<>();

    public Sink(NTPClient ntp, int port, int receiveBufferSize, Date stopTime) throws IOException {
        this.ntp = ntp;

        stopOn(stopTime);

        socket = new ServerSocket(port);
        ConsoleLogger.log("opened sink on address %s:%s", socket.getInetAddress().getHostAddress(), port);
        if (receiveBufferSize > 0) {
            socket.setReceiveBufferSize(receiveBufferSize);
        }
    }

    public void start() throws IOException {
        scheduledTasks.add(scheduler.scheduleAtFixedRate(this::scheduledWriteOutput, 0, TRACE_INTERVAL_IN_MS, TimeUnit.MILLISECONDS));
        scheduledTasks.add(scheduler.scheduleAtFixedRate(this::scheduledLoggingOutput, 0, LOG_INTERVAL_IN_MS, TimeUnit.MILLISECONDS));

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
    public synchronized void close() throws IOException {
        isRunning = false;
        scheduler.shutdown();
        for (ScheduledFuture<?> sf : scheduledTasks) {
            sf.cancel(true);
        }
        scheduledTasks.clear();
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        closeClient();
    }

    protected void stopOn(Date stopTime) {
        long now = ntp.getCurrentTimeNormalized();
        long duration = stopTime.getTime() - now;
        if (duration < 0) {
            throw new IllegalArgumentException("stopTime lies in the past: " + stopTime.getTime() + " Now is " + now);
        }
        scheduledTasks.add(scheduler.schedule(() -> {
            try {
                isRunning = false;
                ConsoleLogger.log("%s | stopping sink", socket.getInetAddress().getHostAddress());
                close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, duration, TimeUnit.MILLISECONDS));
    }

    public abstract void scheduledWriteOutput();

    public abstract void scheduledLoggingOutput();

    public abstract void executeLogic();
}
