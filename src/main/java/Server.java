import application.Application;
import application.SinkApplication;
import general.DecentralizedClockSync;
import general.NTPClient;
import general.NTPServer;
import general.TimeProvider;
import general.logger.ConsoleLogger;
import general.logger.FileLogger;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import picocli.CommandLine;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@CommandLine.Command(name = "Server", description = "Starts an instruction server, which clients can connect to.")
public class Server implements Callable<Integer> {
    @CommandLine.Option(names = {"-p", "--port"}, defaultValue = "10000", description = "port to start server on")
    private int port = 10000;

    private int resultPort() {
        return port + 1;
    }

    @CommandLine.ArgGroup(exclusive = true, multiplicity = "1")
    Exclusive exclusive;

    static class Exclusive {
        @CommandLine.Option(names = "--ntp", defaultValue = "ptbtime1.ptb.de", description = "Address of a ntp server to sync time")
        private String ntpAddress;
        @CommandLine.Option(names = "--ntpServerPort", defaultValue = "-1", description = "Start a ntp server on this machine with the given number as ntp port. The local time will be used, to sync incoming client requests. The server additionally uses a ntpClient against its own ntpServer")
        private int ntpServerPort = -1;
        @CommandLine.Option(names = "--distributedTime", defaultValue = "false", description = "Sync time in a local distributed manner")
        private boolean distributedTime;
    }

    @CommandLine.Option(names = {"-t", "--time"}, defaultValue = "30", description = "Simulation duration in seconds.")
    private int simDuration = 30;

    @CommandLine.Option(names = "--runs", defaultValue = "15", description = "Number of repetitions that are performed")
    private int runs = 15;

    @CommandLine.Option(names = "--trace", defaultValue = "50", description = "Trace interval in ms.")
    private int traceIntervalMs = 50;

    @CommandLine.Option(names = {"--no-gui"}, description = "do not plot metrics in a gui window")
    private boolean noGui;

    boolean startedTransmission = false;

    int connectedPreTransmission = -1;
    int connectedPostTransmission = -1;

    File outDir = new File("./out/");

    TimeProvider timeClient;
    NTPServer timeServer;

    @Override
    public Integer call() throws Exception {
        if (exclusive.ntpServerPort != -1) {
            timeClient = NTPClient.create("localhost", exclusive.ntpServerPort);
        } else if (exclusive.distributedTime) {
            timeClient = DecentralizedClockSync.getInstance();
        } else {
            if (exclusive.ntpAddress.contains(":")) {
                String[] ntp = exclusive.ntpAddress.split(":");
                String addr = ntp[0];
                int port = Integer.parseInt(ntp[1]);
                timeClient = NTPClient.create(addr, port);
            } else {
                timeClient = NTPClient.create(exclusive.ntpAddress);
            }
        }

        ConsoleLogger.create(timeClient);
        FileLogger.create(timeClient, "./log/server.log");

        if (exclusive.ntpServerPort != -1) {
            timeServer = new NTPServer(exclusive.ntpServerPort);
            timeServer.start();
        }

        clearOutFolder();
        timeClient.startSyncTime();

        for (int run = 0; run < runs; run++) {
            ConsoleLogger.log("Initializing run %d", run);
            FileLogger.log("----------------- RUN %d -----------------", run);
            List<InitialHandshakeThread> initialHandshakeThreads = initialHandshake();
            timeClient.stopSyncTime();
            List<SinkApplication> serverSideSinks = transmission(initialHandshakeThreads);
            moveServerSideLogs(serverSideSinks, run);
            boolean reconnectAfterPostHandshake = (run < runs - 1);
            postHandshake(run, reconnectAfterPostHandshake);
            mergeOutFiles(run);
        }
        timeClient.close();
        if (timeServer != null) {
            timeServer.stop();
        }
        return 0;
    }

    public void clearOutFolder() throws IOException {
        File[] files = outDir.listFiles((dir, name) -> name.endsWith(".csv"));
        if (files != null) {
            for (File file : files) {
                boolean deleted = file.delete();
                if (!deleted) {
                    throw new IOException("Could not delete file " + file.getPath());
                }
            }
        }
    }

    public List<InitialHandshakeThread> initialHandshake() throws IOException, InterruptedException {
        ConsoleLogger.log("Opening InitialHandshake Socket on port %s", this.port);
        ServerSocket socket = new ServerSocket(this.port);
        startedTransmission = false;

        ArrayList<InitialHandshakeThread> handshakeThreads = new ArrayList<>();

        connectedPreTransmission = 0;

        new Thread(() -> {
            ConsoleLogger.log("Waiting for connections...");

            while (!startedTransmission) {
                try {
                    Socket client = socket.accept();
                    connectedPreTransmission++;
                    ConsoleLogger.log("connection accepted from: %s", client.getInetAddress().getHostAddress());
                    int defaultId = connectedPreTransmission - 1;
                    InitialHandshakeThread clientThread = new InitialHandshakeThread(client, timeClient, defaultId, simDuration, resultPort(), traceIntervalMs, noGui);
                    clientThread.start();
                    handshakeThreads.add(clientThread);
                } catch (IOException e) {
                    if (!(e instanceof SocketException && startedTransmission)) {
                        // print any error that is not a SocketException and every SocketException that occurs while transmission hasn't started yet
                        e.printStackTrace();
                    }
                }
            }
        }).start();

        if (connectedPostTransmission == -1) {
            // in the first run: block until user input #enter
            Scanner input = new Scanner(System.in);
            input.nextLine();
        } else {
            // in subsequent runs: block until all previously connected clients are again connected
            while (connectedPreTransmission < connectedPostTransmission) {
                Thread.sleep(2000);
                ConsoleLogger.log("Waiting for clients: %d/%d", connectedPreTransmission, connectedPostTransmission);
            }
        }

        startedTransmission = true;
        socket.close();

        return handshakeThreads;
    }

    public List<SinkApplication> transmission(List<InitialHandshakeThread> handshakeThreads) throws InterruptedException {
        ConsoleLogger.log("starting simulation ...");

        ArrayList<Thread> transmissionThreads = new ArrayList<>();

        // simulation begin in 2s
        long current = timeClient.getAdjustedTime();
        long beginDelay = 2000L;
        Date simulationBegin = new Date(current + beginDelay);
        ConsoleLogger.setSimulationBegin(simulationBegin);
        FileLogger.setSimulationBegin(simulationBegin);

        for (InitialHandshakeThread thread : handshakeThreads) {
            ConsoleLogger.log("send instructions to node %s", thread.id);
            Thread transmissionThread = new Thread(() -> {
                try {
                    // send the initial instructions
                    thread.sendInstructions(simulationBegin);
                    Application app = thread.buildApplication(simulationBegin);
                    // use the same thread to start the transmitting/receiving application
                    app.start(timeClient);
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

        // return all Server-side sinks
        return handshakeThreads.stream().filter(it -> it.getApplication() instanceof SinkApplication).map(it -> (SinkApplication) it.getApplication()).collect(Collectors.toList());
    }

    public void postHandshake(int run, boolean reconnectAfterPostHandshake) throws IOException, InterruptedException {
        ConsoleLogger.log("Opening PostHandshake socket on port %s", this.resultPort());
        ServerSocket socket = new ServerSocket(this.resultPort());
        ConsoleLogger.log("Waiting to receive results...");

        ArrayList<Thread> threads = new ArrayList<>();
        connectedPostTransmission = 0;

        while (connectedPostTransmission < connectedPreTransmission) {
            Socket client = socket.accept();
            connectedPostTransmission++;
            ConsoleLogger.log("connection accepted from: %s", client.getInetAddress().getHostAddress());
            PostHandshakeThread clientThread = new PostHandshakeThread(client, run, reconnectAfterPostHandshake);
            clientThread.start();
            threads.add(clientThread);
        }

        socket.close();

        // wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }
    }

    private void moveServerSideLogs(List<SinkApplication> serverSideSinks, int run) throws IOException {
        for (SinkApplication sink : serverSideSinks) {
            File csvInFile = new File(sink.getFilePath());
            File csvOutFile = new File(String.format("./out/received_run_%d_flow_%d_%s.csv", run, sink.getId(), sink.getModeString()));
            Files.copy(csvInFile.toPath(), csvOutFile.toPath());
        }
    }

    private void mergeOutFiles(int run) throws IOException {
        File[] csvFiles = outDir.listFiles((dir, name) -> name.startsWith(String.format("received_run_%d", run)) && name.endsWith(".csv"));

        if (csvFiles == null) {
            throw new FileNotFoundException("No csv files found!");
        }

        List<CSVRecord> csvRecordsList = new ArrayList<>();
        List<String> headers = new ArrayList<>();

        for (File file : csvFiles) {
            Reader inputStreamReader = new InputStreamReader(new FileInputStream(file));
            CSVParser csvParser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(inputStreamReader);
            if (headers.isEmpty()) {
                headers = csvParser.getHeaderNames();
            }

            csvParser.forEach(csvRecordsList::add);
        }

        Comparator<CSVRecord> comparator = Comparator.comparing(r -> Float.valueOf(r.get("time")));
        csvRecordsList.sort(comparator);

        File outFile = new File(outDir, String.format("hardware-%d-goodput.csv", run));
        FileOutputStream out = new FileOutputStream(outFile);
        Writer outputStreamWriter = new OutputStreamWriter(out);
        CSVPrinter csvPrinter = CSVFormat.DEFAULT.withFirstRecordAsHeader().print(outputStreamWriter);
        csvPrinter.printRecord(headers);
        csvPrinter.printRecords(csvRecordsList);
        csvPrinter.flush();
        csvPrinter.close();

        ConsoleLogger.log("Finished merging out files...");
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Server()).execute(args);
        System.exit(exitCode);
    }
}
