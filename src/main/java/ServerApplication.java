import picocli.CommandLine;
import server.LogServer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.concurrent.Callable;

public class ServerApplication implements Callable<Integer> {
    @CommandLine.Option(names = {"-ntp"}, defaultValue = "0.de.pool.ntp.org", description = "Address of the ntp server")
    String ntpAddress;

    @CommandLine.Option(names = {"-p", "--port"}, defaultValue = "5000", description = "Number of the server port, default value: 5000")
    int port;

    @CommandLine.Option(names = {"-b", "--buffer"}, defaultValue = "1000", description = "ReceiveBufferSize in packets, default value: 1000")
    int receiveBufferSize;

    @CommandLine.Option(names = {"-o", "--out"}, defaultValue = "./out/server.log", description = "Path to output log file, default: ./out/server.log")
    String filePath;

    @Override
    public Integer call() throws Exception {
        File outFile = new File(filePath);
        outFile.getParentFile().mkdirs();
        BufferedWriter writer = new BufferedWriter(new FileWriter(outFile));
        writer.write("Time;Address;Name;Goodput;Delay");
        writer.newLine();
        try {
            new LogServer(ntpAddress, port, receiveBufferSize, writer);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            writer.flush();
            writer.close();
        }
        return 0;
    }

    public static void main(String[] args) {
        new CommandLine(new ServerApplication()).execute(args);
    }
}
