package application;

import application.sink.LogSink;
import application.sink.Sink;
import general.TimeProvider;

import java.io.IOException;
import java.util.Date;

public class SinkApplication extends Application {
    String filePath;
    int id;
    boolean mode;

    Sink sink;

    public SinkApplication(int port, int rcvBufferSize, TimeProvider timeProvider, String filePath, int id, boolean mode, int traceIntervalMs, boolean noGui) throws IOException {
        this.filePath = filePath;
        this.id = id;
        this.mode = mode;

        sink = new LogSink(
                timeProvider,
                port,
                rcvBufferSize,
                filePath,
                id,
                mode,
                traceIntervalMs
        );

        if (noGui) {
            sink.disablePlotting();
        }
    }

    @Override
    public void init(Date beginTime, Date stopTime, int duration) {
        sink.init(beginTime, stopTime, duration);
    }

    @Override
    public void startLogging() {
        if (beginTime == null || stopTime == null) {
            throw new IllegalStateException("Not yet initialized");
        }
        sink.startLogging();
    }

    @Override
    public void startLogic() throws Exception {
        if (beginTime == null || stopTime == null) {
            throw new IllegalStateException("Not yet initialized");
        }
        sink.startLogic();
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

    public int getId() {
        return id;
    }

    public String getModeString() {
        if (mode) {
            return "iot";
        } else {
            return "bulk";
        }
    }
}
