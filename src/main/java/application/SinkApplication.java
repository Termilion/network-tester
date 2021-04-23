package application;

import general.NTPClient;
import picocli.CommandLine;
import application.sink.LogSink;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.concurrent.Callable;

public class SinkApplication extends Application {
    String filePath;
    int port;
    int rcvBufferSize;

    NTPClient ntp;

    public SinkApplication(int port, int rcvBufferSize, NTPClient ntp, String filePath) {
        this.filePath = filePath;
        this.port = port;
        this.rcvBufferSize = rcvBufferSize;
        this.ntp = ntp;
    }

    @Override
    public void start() throws Exception {
        File outFile = new File(filePath);
        outFile.getParentFile().mkdirs();
        BufferedWriter writer = new BufferedWriter(new FileWriter(outFile));
        writer.write("Time;Address;Name;Goodput;Delay");
        writer.newLine();
        try {
            new LogSink(
                    this.ntp,
                    this.port,
                    this.rcvBufferSize,
                    writer
            );
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            writer.flush();
            writer.close();
        }
    }
}
