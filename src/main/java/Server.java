import application.Application;
import general.ConsoleLogger;
import general.DecentralizedClockSync;
import general.NTPClient;
import general.TimeProvider;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import picocli.CommandLine;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "Server", description = "Starts an instruction server, which clients can connect to.")
public class Server implements Callable<Integer> {
    @CommandLine.Option(names = {"-p", "--port"}, defaultValue = "5000", description = "port to start server on")
    private int port = 5000;

    private int resultPort() { return port + 1; }

    @CommandLine.ArgGroup(exclusive = true, multiplicity = "0..1")
    Exclusive exclusive;

    static class Exclusive {
        @CommandLine.Option(names = "--ntp", defaultValue = "ptbtime1.ptb.de", description = "address of the ntp server")
        private String ntpAddress;
        @CommandLine.Option(names = "--distributedTime", description = "address of the ntp server")
        private boolean distributedTime;
    }

    @CommandLine.Option(names = {"-t", "--time"}, defaultValue = "30", description = "Simulation duration in seconds.")
    private int simDuration = 30;

    boolean startedTransmission = false;

    int connected = 0;
    int connectedSinksPreTransmission = 0;
    int connectedSinksPostTransmission = 0;

    File outDir = new File("./out/");

    TimeProvider timeClient;

    @Override
    public Integer call() throws Exception {
        if (exclusive.distributedTime) {
            DecentralizedClockSync dcs = DecentralizedClockSync.getInstance();
            dcs.start();
        } else {
            timeClient = NTPClient.create(exclusive.ntpAddress);
        }

        clearOutFolder();
        initialHandshake();
        postHandshake();
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

    public void initialHandshake() throws IOException, InterruptedException {
        ServerSocket socket = new ServerSocket(this.port);

        ArrayList<InitialHandshakeThread> handshakeThreads = new ArrayList<>();

        new Thread(() -> {
            ConsoleLogger.log("Waiting for connections...");

            while (!startedTransmission) {
                try {
                    Socket client = socket.accept();
                    connected++;
                    ConsoleLogger.log("connection accepted from: %s", client.getInetAddress().getHostAddress());
                    InitialHandshakeThread clientThread = new InitialHandshakeThread(client, timeClient, connected, simDuration, resultPort());
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
        timeClient.close();
        ConsoleLogger.log("starting simulation ...");
        startedTransmission = true;

        ArrayList<Thread> transmissionThreads = new ArrayList<>();

        // simulation begin is now
        long current = timeClient.getAdjustedTime();
        Date simulationBegin = new Date(current);

        for (InitialHandshakeThread thread: handshakeThreads) {
            if (!thread.uplink) {
                connectedSinksPreTransmission++;
            }
            ConsoleLogger.log("send instructions to node %s", thread.id);
                Thread transmissionThread = new Thread(() -> {
                    try {
                        // send the initial instructions
                        thread.sendInstructions(simulationBegin);
                        Application app = thread.getApplication(simulationBegin);
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

        mergeOutFiles();
    }

    private void mergeOutFiles() throws IOException {
        File[] csvFiles = outDir.listFiles((dir, name) -> name.startsWith("sink_flow_") && name.endsWith(".csv"));

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

        File outFile = new File(outDir, "goodput.csv");
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
