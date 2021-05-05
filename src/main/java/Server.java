import general.ConsoleLogger;
import picocli.CommandLine;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;
import java.util.concurrent.Callable;

public class Server implements Callable<Integer> {
    @CommandLine.Parameters(index = "0", description = "port to start server on")
    private int port;

    @CommandLine.Option(names = {"-a", "--ntp"}, description = "address of the ntp server")
    private String ntpAddress = "localhost";

    @CommandLine.Option(names = {"-r", "--resetTime"}, description = "time after the app gets forcefully reset")
    private int waitTime = -1;

    ArrayList<ServerThread> threads = new ArrayList<>();

    boolean started = false;

    @Override
    public Integer call() throws Exception {
        initialHandshake();
        return 0;
    }

    public void initialHandshake() throws IOException {
        ServerSocket socket = new ServerSocket(this.port);

        Thread instructionThread = new Thread(() -> {
            Scanner input = new Scanner(System.in);

            // block until user input
            input.nextLine();
            started = true;

            for (ServerThread thread: threads) {
                Date startTime = thread.sendInstructions();
                thread.startLocalApplications(startTime);
            }
        });
        instructionThread.start();

        while(true) {
            Socket client = socket.accept();
            if (!started) {
                ServerThread clientThread = new ServerThread(client, ntpAddress, waitTime);
                clientThread.start();
                threads.add(clientThread);
            } else {
                ConsoleLogger.log(String.format("Connection attempt by %s, but already started", client.getInetAddress().getHostAddress()));
            }
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
