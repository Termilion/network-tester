package application.sink;

import general.ConsoleLogger;
import general.NTPClient;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public abstract class Sink {
    ServerSocket socket;
    Socket client;

    NTPClient ntp;

    int rcvBytes = 0;
    List<Long> delay;

    static final int TRACE_INTERVAL_IN_MS = 50;

    boolean isRunning = true;

    public Sink(NTPClient ntp, int port, int receiveBufferSize, Date stopTime, Object[] scheduleArgs) throws IOException {
        this.ntp = ntp;
        this.delay = Collections.synchronizedList(new ArrayList<>());

        stopOn(stopTime);

        new Thread() {
            @Override
            public void run() {
                try {
                    while (isRunning) {
                        Thread.sleep(TRACE_INTERVAL_IN_MS);
                        scheduledOperation(scheduleArgs);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                super.run();
            }
        }.start();

        socket = new ServerSocket(port);
        ConsoleLogger.log("opened sink on address %s:%s", socket.getInetAddress().getHostAddress(), port);
        if (receiveBufferSize > 0) {
            socket.setReceiveBufferSize(receiveBufferSize);
        }

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
                close();
            }
        }
    }

    public void close() throws IOException {
        isRunning = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        if (client != null && !client.isClosed()) {
            client.close();
        }
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
                ConsoleLogger.log("%s | stopping sink", socket.getInetAddress().getHostAddress());
                close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public abstract void scheduledOperation(Object[] args);

    public abstract void executeLogic();
}
