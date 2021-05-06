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
    final private int waitTime;

    public SourceApplication(boolean type, InetAddress address, int port, NTPClient ntp, int waitTime) {
        this.type = type;
        this.address = address;
        this.port = port;
        this.ntp = ntp;
        this.waitTime = waitTime;
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
            new BulkSource(ntp, address.getHostAddress(), port, waitTime, 12e9, 1000);
        } else {
            new BulkSource(ntp, address.getHostAddress(), port, waitTime, 1e6, 1000);
        }
    }
}

