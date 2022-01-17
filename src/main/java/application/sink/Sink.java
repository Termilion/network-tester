package application.sink;

import application.Application;
import application.Chartable;
import general.TimeProvider;
import general.logger.ConsoleLogger;
import general.logger.FileLogger;

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

public abstract class Sink extends Application implements Closeable {
    Chartable chart;

    ServerSocket socket;
    Socket client;

    TimeProvider timeProvider;
    int port;
    int receiveBufferSize;

    Date beginTime;
    Date stopTime;
    int duration;

    static volatile int TRACE_INTERVAL_IN_MS = 50;

    volatile boolean isRunning = true;
    volatile boolean isConnected = false;

    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    Set<ScheduledFuture<?>> scheduledTasks = new HashSet<>();

    public Sink(TimeProvider timeProvider, int port, int receiveBufferSize, int traceIntervalMs, int id, Mode mode) {
        super(id, mode);
        this.timeProvider = timeProvider;
        this.port = port;
        this.receiveBufferSize = receiveBufferSize;
        TRACE_INTERVAL_IN_MS = traceIntervalMs;
    }

    public final void init(Date beginTime, Date stopTime, int duration) {
        this.beginTime = beginTime;
        this.stopTime = stopTime;
        this.duration = duration;
        FileLogger.log("%s beginTime: %s, stopTime: %s, duration: %d", this.getClass().getSimpleName(), beginTime, stopTime, duration);
    }

    public final void startLogging() {
        scheduledTasks.add(scheduler.scheduleAtFixedRate(this::scheduledWriteOutput, 0, TRACE_INTERVAL_IN_MS, TimeUnit.MILLISECONDS));
        this.chart = new Chartable(
                (int) Math.ceil(duration * 1000.0 / LOG_INTERVAL_IN_MS),
                "Time",
                this.id + " | Sink ↓"
        );
        scheduledTasks.add(scheduler.scheduleAtFixedRate(this::scheduledLoggingOutput, 0, LOG_INTERVAL_IN_MS, TimeUnit.MILLISECONDS));
    }

    public final void startLogic() throws IOException {
        createStopOnTask(stopTime);

        ConsoleLogger.log("Opening sink on port %s", port);
        socket = getServerSocket(port);
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
        if (this.chart != null) {
            this.chart.close();
        }
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

    protected void createStopOnTask(Date stopTime) {
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
