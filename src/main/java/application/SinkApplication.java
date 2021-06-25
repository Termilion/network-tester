package application;

import application.sink.LogSink;
import general.NTPClient;

public class SinkApplication extends Application {
    String filePath;
    int port;
    int rcvBufferSize;
    int id;
    boolean mode;

    NTPClient ntp;

    public SinkApplication(int port, int rcvBufferSize, NTPClient ntp, String filePath, int id, boolean mode) {
        this.filePath = filePath;
        this.port = port;
        this.rcvBufferSize = rcvBufferSize;
        this.ntp = ntp;
        this.id = id;
        this.mode = mode;
    }

    @Override
    public void doOnStart() throws Exception {
        try {
            new LogSink(
                    this.ntp,
                    this.port,
                    this.rcvBufferSize,
                    filePath,
                    simulationBegin,
                    stopTime,
                    id,
                    mode
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getFilePath() {
        return filePath;
    }
}
