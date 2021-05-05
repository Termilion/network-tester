package application.sink;

import general.ConsoleLogger;
import general.NTPClient;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public abstract class Sink {
    ArrayList<Socket> connected = new ArrayList<>();
    ArrayList<Thread> clientThreads = new ArrayList<>();

    NTPClient ntp;

    int rcvBytes = 0;
    List<Long> delay = new ArrayList<>();

    int traceIntervalInMs = 50;

    public Sink(NTPClient ntp, int port, int receiveBufferSize) throws IOException {
        this.ntp = ntp;
        listen(port, receiveBufferSize);
    }

    private void listen(int port, int receiveBufferSize) throws IOException {
        ServerSocket socket = new ServerSocket(port);
        socket.setReceiveBufferSize(receiveBufferSize);
        ConsoleLogger.log(
                String.format(
                        "opened server socket on address %s",
                        socket.getInetAddress()
                )
        );

        new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(traceIntervalInMs);
                    scheduledOperation();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                super.run();
            }
        }.start();

        while (true) {
            Socket client = socket.accept();
            Thread clientThread = new Thread() {
                @Override
                public void run() {
                    executeLogic(client);
                    try {
                        if(!client.isClosed()) {
                            client.close();
                        }
                    } catch(IOException e) {
                        e.printStackTrace();
                    }
                    super.run();
                }
            };
            connected.add(client);
            clientThreads.add(clientThread);
            clientThread.start();
        }
    }

    public abstract void scheduledOperation();

    public abstract void executeLogic(Socket client);
}
