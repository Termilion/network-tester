package application.sink;

import application.Chartable;
import general.TimeProvider;
import general.logger.ConsoleLogger;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static application.Application.LOG_INTERVAL_IN_MS;

public abstract class Sink extends Chartable implements Closeable {
    ServerSocket socket;
    Socket client;

    TimeProvider timeProvider;
    int port;
    int receiveBufferSize;
    int id;

    Date beginTime;
    Date stopTime;
    int duration;

    static volatile int TRACE_INTERVAL_IN_MS = 50;

    volatile boolean isRunning = true;
    volatile boolean isConnected = false;

    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    Set<ScheduledFuture<?>> scheduledTasks = new HashSet<>();

    public Sink(TimeProvider timeProvider, int port, int receiveBufferSize, int traceIntervalMs, int id) {
        this.timeProvider = timeProvider;
        this.port = port;
        this.receiveBufferSize = receiveBufferSize;
        TRACE_INTERVAL_IN_MS = traceIntervalMs;
        this.id = id;
    }

    public void init(Date beginTime, Date stopTime, int duration) {
        this.beginTime = beginTime;
        this.stopTime = stopTime;
        this.duration = duration;
    }

    public void startLogging() {
        scheduledTasks.add(scheduler.scheduleAtFixedRate(this::scheduledWriteOutput, 0, TRACE_INTERVAL_IN_MS, TimeUnit.MILLISECONDS));
        initChart(
                (int) Math.ceil(duration * 1000.0 / LOG_INTERVAL_IN_MS),
                "Time",
                id + " | Sink ↓"
        );
        scheduledTasks.add(scheduler.scheduleAtFixedRate(this::scheduledLoggingOutput, 0, LOG_INTERVAL_IN_MS, TimeUnit.MILLISECONDS));
    }

    public void startLogic() throws IOException {
        stopOn(stopTime);

        ConsoleLogger.log("Opening sink on port %s", port);
        socket = new ServerSocket(port);
        if (receiveBufferSize > 0) {
            socket.setReceiveBufferSize(receiveBufferSize);
        }

        while (isRunning) {
            try {
                client = socket.accept();
                isConnected = true;
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
        isConnected = false;
    }

    @Override
    public synchronized void close() throws IOException {
        super.close();
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
        long now = timeProvider.getAdjustedTime();
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

    protected double getSimTime() {
        long now = timeProvider.getAdjustedTime();
        return (now - beginTime.getTime()) / 1000.0;
    }

    protected abstract void scheduledWriteOutput();

    protected abstract void scheduledLoggingOutput();

    protected abstract void executeLogic();
}
