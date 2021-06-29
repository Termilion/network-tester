package application.source;

import general.ConsoleLogger;
import general.NTPClient;

import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public abstract class Source implements Closeable {
    Socket socket;
    int sendBufferSize;
    String address;
    int port;
    int resetTime;
    Date stopTime;

    NTPClient ntp;

    boolean isRunning = true;

    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    Set<ScheduledFuture<?>> scheduledTasks = new HashSet<>();

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
        this.resetTime = resetTime;
        this.stopTime = stopTime;

        if(resetTime > 0) {
            reset(resetTime);
        }
        stopOn(stopTime);

        if (this.sendBufferSize > 0) {
            this.socket.setSendBufferSize(this.sendBufferSize);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void start() throws IOException, InterruptedException {
        try {
            ConsoleLogger.log("connecting to %s:%s", address, port);
            connect(address, port);
            execute();
        } catch (IOException e) {
            e.printStackTrace();
        }

        long now = ntp.getCurrentTimeNormalized();
        long buffer = 5000L;
        long timeout = stopTime.getTime() - now + buffer;
        scheduler.awaitTermination(timeout, TimeUnit.MILLISECONDS);

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
    }

    protected void reset(int resetTime) {
        scheduledTasks.add(scheduler.scheduleAtFixedRate(() -> {
            try {
                ConsoleLogger.log("%s | calling reset", socket.getInetAddress().getHostAddress());
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
                connect(address, port);
                execute();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, resetTime, resetTime, TimeUnit.MILLISECONDS));
    }

    protected void stopOn(Date stopTime) {
        long now = ntp.getCurrentTimeNormalized();
        long duration = stopTime.getTime() - now;
        if (duration < 0) {
            throw new IllegalArgumentException("stopTime lies in the past: " + stopTime.getTime() + " Now is " + now);
        }
        ConsoleLogger.log("Stopping source in "+ duration +"ms");
        scheduledTasks.add(scheduler.schedule(() -> {
            try {
                isRunning = false;
                ConsoleLogger.log("%s | stopping source", socket.getInetAddress().getHostAddress());
                close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, duration, TimeUnit.MILLISECONDS));
    }

    public abstract void execute() throws IOException;
}
