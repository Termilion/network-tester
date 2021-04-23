import application.Application;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

public class Server {
    private int port;
    private String ntpAddress;

    ArrayList<ServerThread> threads = new ArrayList<>();

    public Server(int port, String ntpAddress) {
        this.port = port;
        this.ntpAddress = ntpAddress;
    }

    public void initialHandshake() throws IOException {
        ServerSocket socket = new ServerSocket(this.port);

        Thread startOrderThread = new Thread(() -> {
            Scanner input = new Scanner(System.in);
            input.nextLine();

            for (ServerThread thread: threads) {
                thread.sendOrder();
            }
        });
        startOrderThread.start();

        while(true) {
            Socket client = socket.accept();
            ServerThread clientThread = new ServerThread(client, ntpAddress);
            clientThread.start();
            threads.add(clientThread);
        }

    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getNtpAddress() {
        return ntpAddress;
    }

    public void setNtpAddress(String ntpAddress) {
        this.ntpAddress = ntpAddress;
    }
}
