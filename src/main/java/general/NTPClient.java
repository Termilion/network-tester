package general;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Date;

public class NTPClient {
    String timeServer;
    long offset;

    private static NTPClient instance;

    public static NTPClient getInstance() throws NullPointerException {
        synchronized (NTPClient.class) {
            if (instance == null) {
                throw new NullPointerException("Not yet initialized");
            } else {
                return instance;
            }
        }
    }

    public static NTPClient create(String timeServer) throws IOException {
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

    private NTPClient(String timeServer) throws IOException {
        ConsoleLogger.log(String.format("started ntp process to address: %s", timeServer));
        this.timeServer = timeServer;
        this.offset = getServerTime();
        ConsoleLogger.log("finished ntp process");
    }

    private long getServerTime() throws IOException {
        NTPUDPClient timeClient = new NTPUDPClient();
        InetAddress address = InetAddress.getByName(timeServer);
        TimeInfo timeInfo = timeClient.getTime(address);
        timeInfo.computeDetails();
        ConsoleLogger.log(String.format("ntp results: delay %s ms; offset %s ms", timeInfo.getDelay(), timeInfo.getOffset()));
        return timeInfo.getOffset();
    }

    public long getCurrentTimeNormalized() {
        long currentTime = System.currentTimeMillis();
        long normalized = currentTime + offset;
        return normalized;
    }

    public long normalize(long time) {
        return time + offset;
    }

    public long normalize(Date time) {
        return normalize(time.getTime());
    }
}
