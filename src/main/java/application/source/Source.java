package application.source;

import application.Chartable;
import general.TimeProvider;
import general.logger.ConsoleLogger;

import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static application.Application.LOG_INTERVAL_IN_MS;

public abstract class Source extends Chartable implements Closeable {
    Socket socket;
    int sendBufferSize;
    String address;
    int port;
    int resetTime;
    int id;

    TimeProvider timeProvider;
    Date beginTime;
    Date stopTime;
    int duration;

    volatile boolean isRunning = true;
    volatile boolean isConnected = false;

    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    Set<ScheduledFuture<?>> scheduledTasks = new HashSet<>();

    public Source(
            TimeProvider timeProvider,
            String address,
            int port,
            int sendBufferSize,
            int resetTime,
            int id
    ) {
        this.sendBufferSize = sendBufferSize;
        this.timeProvider = timeProvider;
        this.address = address;
        this.port = port;
        this.resetTime = resetTime;
        this.id = id;
    }

    public void init(Date beginTime, Date stopTime, int duration) {
        this.beginTime = beginTime;
        this.stopTime = stopTime;
        this.duration = duration;
    }

    public void startLogging() {
        initChart(
                (int) Math.ceil(duration * 1000.0 / LOG_INTERVAL_IN_MS),
                "Time",
                id + " | Source ↑"
        );
        scheduledTasks.add(scheduler.scheduleAtFixedRate(this::scheduledLoggingOutput, 0, LOG_INTERVAL_IN_MS, TimeUnit.MILLISECONDS));
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void startLogic() throws InterruptedException {
        if (resetTime > 0) {
            reset(resetTime);
        }
        stopOn(stopTime);

        try {
            ConsoleLogger.log("Opening source to %s:%s", address, port);
            connect(address, port);
            executeLogic();
        } catch (IOException e) {
            e.printStackTrace();
        }

        long now = timeProvider.getAdjustedTime();
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
        isConnected = true;
        if (this.sendBufferSize > 0) {
            this.socket.setSendBufferSize(this.sendBufferSize);
        }
    }

    @Override
    public synchronized void close() throws IOException {
        super.close();
        isRunning = false;
        isConnected = false;
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
                isConnected = false;
                connect(address, port);
                executeLogic();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, resetTime, resetTime, TimeUnit.MILLISECONDS));
    }

    protected void stopOn(Date stopTime) {
        long now = timeProvider.getAdjustedTime();
        long duration = stopTime.getTime() - now;
        if (duration < 0) {
            throw new IllegalArgumentException("stopTime lies in the past: " + stopTime.getTime() + " Now is " + now);
        }
        ConsoleLogger.log("Stopping source in %d ms", duration);
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

    protected double getSimTime() {
        long now = timeProvider.getAdjustedTime();
        return (now - beginTime.getTime()) / 1000.0;
    }

    protected abstract void scheduledLoggingOutput();

    protected abstract void executeLogic() throws IOException;
}
