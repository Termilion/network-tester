package application;

import application.source.BulkSource;
import application.source.IoTSource;
import general.ConsoleLogger;
import general.NTPClient;

import java.net.InetAddress;

public class SourceApplication extends Application {
    final private String type;
    final private InetAddress address;
    final private int port;
    final private NTPClient ntp;

    public SourceApplication(String type, InetAddress address, int port, NTPClient ntp) {
        this.type = type;
        this.address = address;
        this.port = port;
        this.ntp = ntp;
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
            new BulkSource(ntp, address.toString(), 5000, 10, 1000);
        } else {
            new IoTSource(ntp, address.toString(), port);
        }
    }
}

