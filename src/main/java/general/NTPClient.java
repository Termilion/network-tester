package general;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;

import java.io.IOException;
import java.net.InetAddress;
import java.sql.Timestamp;

public class NTPClient {
    static final String TIME_SERVER = "time-a-g.nist.gov";

    long offset;

    public NTPClient() throws IOException {
        this.offset = getServerTime();
    }

    private long getServerTime() throws IOException {
        NTPUDPClient timeClient = new NTPUDPClient();
        InetAddress address = InetAddress.getByName(TIME_SERVER);
        TimeInfo timeInfo = timeClient.getTime(address);
        timeInfo.computeDetails();
        return timeInfo.getOffset();
    }

    public long getCurrentTimeNormalized() {
        long currentTime = System.currentTimeMillis();
        long normalized = currentTime + offset;
        return new Timestamp(normalized).getTime();
    }
}
