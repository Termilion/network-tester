package application;

import application.sink.LogSink;
import general.NTPClient;

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
    public void doOnStart() throws Exception {
        try {
            new LogSink(
                    this.ntp,
                    this.port,
                    this.rcvBufferSize,
                    filePath,
                    stopTime
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getFilePath() {
        return filePath;
    }
}
