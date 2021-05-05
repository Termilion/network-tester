package general;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Date;
import java.sql.Timestamp;

public class NTPClient {
    String timeServer;

    long offset;

    public NTPClient(String timeServer) throws IOException {
        ConsoleLogger.log("started ntp process");
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
        return new Timestamp(normalized).getTime();
    }

    public long normalize(long time) {
        return new Timestamp(time + offset).getTime();
    }

    public long normalize(Date time) {
        return normalize(time.getTime());
    }
}
