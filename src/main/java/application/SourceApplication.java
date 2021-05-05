package application;

import application.source.BulkSource;
import general.ConsoleLogger;
import general.NTPClient;

import java.net.InetAddress;

public class SourceApplication extends Application {
    final private String type;
    final private InetAddress address;
    final private int port;
    final private NTPClient ntp;
    final private int waitTime;

    public SourceApplication(String type, InetAddress address, int port, NTPClient ntp, int waitTime) {
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
        if (type.equals("b")) {
            new BulkSource(ntp, address.toString(), port, -1, 12e9, 1000);
        } else {
            new BulkSource(ntp, address.toString(), port, 1500, 1e6, 1000);
        }
    }
}

