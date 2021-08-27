package application;

import application.source.BulkSource;
import application.source.IoTSource;
import application.source.Source;
import general.TimeProvider;
import general.logger.ConsoleLogger;

import java.io.IOException;
import java.util.Date;

public class SourceApplication extends Application {
    Source source;

    public SourceApplication(boolean mode, String ipaddress, int port, TimeProvider timeProvider, int resetTime, int sndBuf, int id) throws IOException {
        if (!mode) {
            ConsoleLogger.log("Starting Bulk source application: %s:%d", ipaddress, port);
            source = new BulkSource(timeProvider, ipaddress, port, resetTime, sndBuf, id);
        } else {
            ConsoleLogger.log("Starting IoT source application: %s:%d", ipaddress, port);
            source = new IoTSource(timeProvider, ipaddress, port, resetTime, sndBuf, id);
        }
    }

    @Override
    protected void init(Date beginTime, Date stopTime, int duration) {
        source.init(beginTime, stopTime, duration);
    }

    @Override
    public void startLogging() {
        if (beginTime == null || stopTime == null) {
            throw new IllegalStateException("Not yet initialized");
        }
        source.startLogging();
    }

    @Override
    public void startLogic() throws Exception {
        if (beginTime == null || stopTime == null) {
            throw new IllegalStateException("Not yet initialized");
        }
        source.startLogic();
    }

    @Override
    public void close() throws IOException {
        if (source != null) {
            source.close();
        }
    }
}

