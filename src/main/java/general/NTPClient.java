package general;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;

import java.io.IOException;
import java.net.InetAddress;
import java.sql.Timestamp;

public class NTPClient {
    static final String TIME_SERVER = "time-a-g.nist.gov";
    long baseTimeServer;
    long baseTimeLocal;

    long difference;

    public NTPClient() throws IOException {
        this.baseTimeServer = getServerTime();
        this.baseTimeLocal = System.currentTimeMillis();
        this.difference = this.baseTimeServer - this.baseTimeLocal;
    }

    private long getServerTime() throws IOException {
        NTPUDPClient timeClient = new NTPUDPClient();
        InetAddress address = InetAddress.getByName(TIME_SERVER);
        TimeInfo timeInfo = timeClient.getTime(address);
        return timeInfo.getReturnTime();
    }

    public long getCurrentTimeNormalized() {
        long currentTime = System.currentTimeMillis();
        long normalized = currentTime + difference;
        return new Timestamp(normalized).getTime();
    }
}
