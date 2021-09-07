package general;

import general.logger.ConsoleLogger;
import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.NtpV3Packet;
import org.apache.commons.net.ntp.TimeInfo;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;

public class NTPClient extends TimeProvider {
    String timeServer;
    int ntpPort;
    String networkInterface;
    volatile long offset;

    private static NTPClient instance;

    private NTPUDPClient timeClient;

    public static NTPClient getInstance() throws NullPointerException {
        synchronized (NTPClient.class) {
            if (instance == null) {
                throw new NullPointerException("Not yet initialized");
            } else {
                return instance;
            }
        }
    }

    public static NTPClient create(String timeServer) {
        return create(timeServer, NtpV3Packet.NTP_PORT);
    }

    public static NTPClient create(String timeServer, int port) {
        return create(timeServer, port, null);
    }

    public static NTPClient create(String timeServer, String networkInterface) {
        return create(timeServer, NtpV3Packet.NTP_PORT, networkInterface);
    }

    public static NTPClient create(String timeServer, int port, String networkInterface) {
        if (instance == null) {
            synchronized (NTPClient.class) {
                if (instance == null) {
                    instance = new NTPClient(timeServer, port, networkInterface);
                }
            }
        } else {
            throw new IllegalStateException("Already initialized");
        }
        return instance;
    }

    private NTPClient(String timeServer, int port, String networkInterface) {
        this.timeServer = timeServer;
        this.ntpPort = port;
        this.networkInterface = networkInterface;
    }

    @Override
    public void startSyncTime() throws IOException {
        ConsoleLogger.log(String.format("started ntp process to address: %s", timeServer));
        this.offset = getServerTime();
        ConsoleLogger.log("finished ntp process");
    }

    private long getServerTime() throws IOException {
        timeClient = new NTPUDPClient();
        if (networkInterface != null) {
            NetworkInterface ni = NetworkInterface.getByName(networkInterface);
            if (ni == null) {
                throw new IOException("Specified time sync network interface not found!");
            }
            timeClient.open(0, ni.getInetAddresses().nextElement());
        }
        InetAddress address = InetAddress.getByName(timeServer);
        TimeInfo timeInfo = timeClient.getTime(address, ntpPort);
        timeInfo.computeDetails();
        ConsoleLogger.log(String.format("ntp results: delay %s ms; offset %s ms", timeInfo.getDelay(), timeInfo.getOffset()));
        return timeInfo.getOffset();
    }

    @Override
    public long getAdjustedTime() {
        long currentTime = System.currentTimeMillis();
        return currentTime + offset;
    }

    @Override
    public void close() throws IOException {
        timeClient.close();
    }
}
