package application;

import application.source.BulkSource;
import application.source.IoTSource;
import application.source.Source;
import general.ConsoleLogger;
import general.NTPClient;
import general.TimeProvider;

import java.io.IOException;

public class SourceApplication extends Application {
    final private boolean type;
    final private String ipaddress;
    final private int port;
    final private TimeProvider timeProvider;
    final private int resetTime;
    final private int sndBuf;

    Source source;

    public SourceApplication(boolean mode, String ipaddress, int port, TimeProvider timeProvider, int resetTime, int sndBuf) {
        this.type = mode;
        this.ipaddress = ipaddress;
        this.port = port;
        this.timeProvider = timeProvider;
        this.resetTime = resetTime;
        this.sndBuf = sndBuf;
    }

    @Override
    public void doOnStart() throws Exception {
        ConsoleLogger.log(
                String.format(
                        "Started Source: %s:%d",
                        ipaddress,
                        port
                )
        );
        if (!type) {
            ConsoleLogger.log("starting bulk source application");
            source = new BulkSource(timeProvider, ipaddress, port, resetTime, simulationBegin, stopTime, sndBuf);
        } else {
            ConsoleLogger.log("starting IoT source application");
            source = new IoTSource(timeProvider, ipaddress, port, resetTime, simulationBegin, stopTime, sndBuf);
        }
        source.start();
    }

    @Override
    public void close() throws IOException {
        if (source != null) {
            source.close();
        }
    }
}

