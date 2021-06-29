package application;

import application.sink.LogSink;
import application.sink.Sink;
import general.TimeProvider;

import java.io.IOException;

public class SinkApplication extends Application {
    String filePath;
    int port;
    int rcvBufferSize;
    int id;
    boolean mode;

    TimeProvider timeProvider;

    Sink sink;

    public SinkApplication(int port, int rcvBufferSize, TimeProvider timeProvider, String filePath, int id, boolean mode) {
        this.filePath = filePath;
        this.port = port;
        this.rcvBufferSize = rcvBufferSize;
        this.timeProvider = timeProvider;
        this.id = id;
        this.mode = mode;
    }

    @Override
    public void doOnStart() throws Exception {
        try {
            sink = new LogSink(
                    this.timeProvider,
                    this.port,
                    this.rcvBufferSize,
                    filePath,
                    simulationBegin,
                    stopTime,
                    id,
                    mode
            );
            sink.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() throws IOException {
        if (sink != null) {
            sink.close();
        }
    }

    public String getFilePath() {
        return filePath;
    }
}
