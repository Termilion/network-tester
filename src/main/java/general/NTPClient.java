package general;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;

import java.io.IOException;
import java.net.InetAddress;

public class NTPClient extends TimeProvider {
    String timeServer;
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
        if (instance == null) {
            synchronized (NTPClient.class) {
                if (instance == null) {
                    instance = new NTPClient(timeServer);
                }
            }
        } else {
            throw new IllegalStateException("Already initialized");
        }
        return instance;
    }

    private NTPClient(String timeServer) {
        this.timeServer = timeServer;
    }

    @Override
    public void startSyncTime() throws IOException {
        ConsoleLogger.log(String.format("started ntp process to address: %s", timeServer));
        this.offset = getServerTime();
        ConsoleLogger.log("finished ntp process");
    }

    private long getServerTime() throws IOException {
        timeClient = new NTPUDPClient();
        InetAddress address = InetAddress.getByName(timeServer);
        TimeInfo timeInfo = timeClient.getTime(address);
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
