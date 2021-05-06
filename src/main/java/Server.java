import general.ConsoleLogger;
import general.NTPClient;
import general.Utility;
import picocli.CommandLine;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Scanner;
import java.util.concurrent.Callable;

public class Server implements Callable<Integer> {
    @CommandLine.Option(names = {"-p", "--port"}, description = "port to start server on")
    private int port = 5000;

    @CommandLine.Option(names = {"-n", "--ntp"}, defaultValue = "ntp1.versatel.de",description = "address of the ntp server")
    private String ntpAddress;

    @CommandLine.Option(names = {"-r", "--resetTime"}, description = "time after the app gets forcefully reset")
    private int waitTime = -1;

    ArrayList<ServerThread> threads = new ArrayList<>();

    boolean started = false;

    int connected = 0;

    @Override
    public Integer call() throws Exception {
        initialHandshake();
        return 0;
    }

    public void initialHandshake() throws IOException {
        ServerSocket socket = new ServerSocket(this.port);
        NTPClient ntpClient = new NTPClient(ntpAddress);

        Thread instructionThread = new Thread(() -> {
            Scanner input = new Scanner(System.in);

            // block until user input
            input.nextLine();
            ConsoleLogger.log("starting simulation ...");
            started = true;

            for (ServerThread thread: threads) {
                ConsoleLogger.log("send instructions to node %s", thread.id);
                new Thread() {
                    @Override
                    public void run() {
                        super.run();
                        thread.sendInstructions();
                        thread.startLocalApplications();
                    }
                }.start();
            }
        });
        instructionThread.start();

        while(true) {
            Socket client = socket.accept();
            connected++;
            if (!started) {
                ConsoleLogger.log("connection accepted from: %s", client.getInetAddress().getHostAddress());
                ServerThread clientThread = new ServerThread(client, ntpClient, waitTime, connected);
                clientThread.start();
                threads.add(clientThread);
            } else {
                ConsoleLogger.log("Connection attempt by %s, but already started", client.getInetAddress().getHostAddress());
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

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Server()).execute(args);
        System.exit(exitCode);
    }
}
