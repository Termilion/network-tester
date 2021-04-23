package application.sink;

import general.ConsoleLogger;
import general.NTPClient;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public abstract class Sink {
    ArrayList<Socket> connected = new ArrayList<>();
    ArrayList<Thread> clientThreads = new ArrayList<>();

    NTPClient ntp;

    public Sink(NTPClient ntp, int port, int receiveBufferSize, BufferedWriter writer) throws IOException {
        this.ntp = ntp;
        listen(port, receiveBufferSize, writer);
    }

    private void listen(int port, int receiveBufferSize, BufferedWriter writer) throws IOException {
        ServerSocket socket = new ServerSocket(port);
        socket.setReceiveBufferSize(receiveBufferSize);
        ConsoleLogger.log(
                String.format(
                        "opened server socket on address %s",
                        socket.getInetAddress()
                )
        );
        while (true) {
            Socket client = socket.accept();
            Thread clientThread = new Thread() {
                @Override
                public void run() {
                    executeLogic(client, writer);
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

    public abstract void executeLogic(Socket client, BufferedWriter writer);
}
