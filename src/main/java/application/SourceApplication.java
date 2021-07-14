package application;

import application.source.BulkSource;
import application.source.IoTSource;
import application.source.Source;
import general.ConsoleLogger;
import general.TimeProvider;

import java.io.IOException;
import java.util.Date;

public class SourceApplication extends Application {
    final private String ipaddress;
    final private int port;

    Source source;

    public SourceApplication(boolean mode, String ipaddress, int port, TimeProvider timeProvider, int resetTime, int sndBuf) throws IOException {
        this.ipaddress = ipaddress;
        this.port = port;

        if (!mode) {
            ConsoleLogger.log("starting bulk source application");
            source = new BulkSource(timeProvider, ipaddress, port, resetTime, sndBuf);
        } else {
            ConsoleLogger.log("starting IoT source application");
            source = new IoTSource(timeProvider, ipaddress, port, resetTime, sndBuf);
        }
    }

    @Override
    protected void init(Date beginTime, Date stopTime) {
        this.beginTime = beginTime;
        this.stopTime = stopTime;
    }

    @Override
    public void startLogic() throws Exception {
        if (beginTime == null || stopTime == null) {
            throw new IllegalStateException("Not yet initialized");
        }
        ConsoleLogger.log(
                String.format(
                        "Started Source: %s:%d",
                        ipaddress,
                        port
                )
        );
        source.startLogic(beginTime, stopTime);
    }

    @Override
    public void close() throws IOException {
        if (source != null) {
            source.close();
        }
    }
}

