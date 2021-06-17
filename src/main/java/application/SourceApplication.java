package application;

import application.source.BulkSource;
import general.ConsoleLogger;
import general.NTPClient;

import java.net.InetAddress;

public class SourceApplication extends Application {
    final private boolean type;
    final private InetAddress address;
    final private int port;
    final private NTPClient ntp;
    final private int resetTime;
    final private int sndBuf;

    final private double BULK_DATA = 12e9;
    final private double IOT_DATA = 1e6;

    public SourceApplication(boolean mode, InetAddress address, int port, NTPClient ntp, int resetTime, int sndBuf) {
        this.type = mode;
        this.address = address;
        this.port = port;
        this.ntp = ntp;
        this.resetTime = resetTime;
        this.sndBuf = sndBuf;
    }

    public void start() throws Exception {
        ConsoleLogger.log(
                String.format(
                        "Started Source: %s:%d",
                        address,
                        port
                )
        );
        if (!type) {
            ConsoleLogger.log("starting bulk source application");
            new BulkSource(ntp, address.getHostAddress(), port, resetTime, BULK_DATA, sndBuf);
        } else {
            ConsoleLogger.log("starting IoT source application");
            new BulkSource(ntp, address.getHostAddress(), port, resetTime, IOT_DATA, sndBuf);
        }
    }
}

