package application.sink;

import general.ConsoleLogger;
import general.NTPClient;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Sink {
    Socket client;

    NTPClient ntp;

    int rcvBytes = 0;
    List<Long> delay;

    int traceIntervalInMs = 50;

    public Sink(NTPClient ntp, int port, int receiveBufferSize, Object[] scheduleArgs) throws IOException {
        this.ntp = ntp;
        this.delay = Collections.synchronizedList(new ArrayList<>());

        new Thread() {
            @Override
            public void run() {
                try {
                    while (true) {
                        Thread.sleep(traceIntervalInMs);
                        scheduledOperation(scheduleArgs);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                super.run();
            }
        }.start();

        ServerSocket socket = new ServerSocket(port);
        ConsoleLogger.log("opened sink on address %s:%s", socket.getInetAddress().getHostAddress(), port);
        socket.setReceiveBufferSize(receiveBufferSize);

        while (true) {
            try {
                client = socket.accept();
                executeLogic();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                close();
            }
        }
    }

    public void close() throws IOException {
        if (client != null) {
            if (!client.isClosed()) {
                client.close();
            }
        }
    }

    public abstract void scheduledOperation(Object[] args);

    public abstract void executeLogic();
}
