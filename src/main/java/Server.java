import application.Application;
import general.ConsoleLogger;
import general.NTPClient;
import picocli.CommandLine;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "Server", description = "Starts an instruction server, which clients can connect to.")
public class Server implements Callable<Integer> {
    @CommandLine.Option(names = {"-p", "--port"}, defaultValue = "5000", description = "port to start server on")
    private int port = 5000;

    private int resultPort() { return port + 1; }

    @CommandLine.Option(names = {"-n", "--ntp"}, defaultValue = "ptbtime1.ptb.de", description = "address of the ntp server")
    private String ntpAddress;

    @CommandLine.Option(names = {"-t", "--time"}, defaultValue = "30", description = "Simulation duration in seconds.")
    private int simDuration = 30;

    boolean startedTransmission = false;

    int connected = 0;
    int connectedSinksPreTransmission = 0;
    int connectedSinksPostTransmission = 0;

    @Override
    public Integer call() throws Exception {
        initialHandshake();
        postHandshake();
        return 0;
    }

    public void initialHandshake() throws IOException, InterruptedException {
        ServerSocket socket = new ServerSocket(this.port);
        NTPClient ntpClient = new NTPClient(ntpAddress);

        ArrayList<InitialHandshakeThread> handshakeThreads = new ArrayList<>();

        new Thread(() -> {
            ConsoleLogger.log("Waiting for connections...");

            while (!startedTransmission) {
                try {
                    Socket client = socket.accept();
                    connected++;
                    ConsoleLogger.log("connection accepted from: %s", client.getInetAddress().getHostAddress());
                    InitialHandshakeThread clientThread = new InitialHandshakeThread(client, ntpClient, connected, simDuration, resultPort());
                    clientThread.start();
                    handshakeThreads.add(clientThread);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        Scanner input = new Scanner(System.in);

        // block until user input
        input.nextLine();
        ConsoleLogger.log("starting simulation ...");
        startedTransmission = true;

        ArrayList<Thread> transmissionThreads = new ArrayList<>();

        for (InitialHandshakeThread thread: handshakeThreads) {
            if (!thread.uplink) {
                connectedSinksPreTransmission++;
            }
            ConsoleLogger.log("send instructions to node %s", thread.id);
                Thread transmissionThread = new Thread(() -> {
                    try {
                        // send the initial instructions
                        thread.sendInstructions();
                        Application app = thread.getApplication();
                        // use the same thread to start the transmitting/receiving application
                        app.start(ntpClient);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                transmissionThread.start();
                transmissionThreads.add(transmissionThread);
        }

        // wait for all threads to complete
        for (Thread thread : transmissionThreads) {
            thread.join();
        }

        ConsoleLogger.log("simulation finished ...");
    }

    public void postHandshake() throws IOException, InterruptedException {
        ServerSocket socket = new ServerSocket(this.resultPort());
        ConsoleLogger.log("Waiting to receive results...");

        ArrayList<Thread> threads = new ArrayList<>();

        while (connectedSinksPostTransmission < connectedSinksPreTransmission) {
            Socket client = socket.accept();
            connectedSinksPostTransmission++;
            ConsoleLogger.log("connection accepted from: %s", client.getInetAddress().getHostAddress());
            PostHandshakeThread clientThread = new PostHandshakeThread(client);
            clientThread.start();
            threads.add(clientThread);
        }

        // wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        //TODO mergeOutFiles();
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Server()).execute(args);
        System.exit(exitCode);
    }
}
