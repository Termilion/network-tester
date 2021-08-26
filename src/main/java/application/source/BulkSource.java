package application.source;

import general.TimeProvider;
import general.Utility;
import general.logger.ConsoleLogger;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicInteger;

import static application.Application.LOG_INTERVAL_IN_MS;

public class BulkSource extends Source {

    String connectedAddress;
    int id;

    AtomicInteger sndBytes;
    long totalSndPackets = 0;

    long lastLogTime = -1;

    public BulkSource(TimeProvider timeProvider, String address, int port, int resetTime, int bufferSize, int id) {
        super(timeProvider, address, port, bufferSize, resetTime);
        this.id = id;
        this.sndBytes = new AtomicInteger(0);
    }

    @Override
    protected void executeLogic() throws IOException {
        OutputStream out = socket.getOutputStream();

        connectedAddress = socket.getInetAddress().getHostAddress();

        while (isRunning) {
            byte[] payload = new byte[1000];
            long time = this.timeProvider.getAdjustedTime();

            try {
                out.write(Utility.encodeTime(payload, time));
                out.flush();

                measureBytes(payload.length);
            } catch (Exception e) {
                // if source isn't running anymore and the socket fails, there is no need to print the stacktrace
                if (isRunning) {
                    double simTime = (time - beginTime.getTime()) / 1000.0;
                    ConsoleLogger.log("Source: Exception at simTime " + simTime);
                    e.printStackTrace();
                }
                break;
            }
        }
        ConsoleLogger.log("%s | stopped transmission. %s packets send", socket.getInetAddress().getHostAddress(), totalSndPackets);
        out.close();
    }

    private void measureBytes(int payloadLength) {
        this.sndBytes.addAndGet(payloadLength);
        this.totalSndPackets++;
    }

    @Override
    public void scheduledLoggingOutput() {
        try {
            long now = timeProvider.getAdjustedTime();

            // trace values
            double currentSndMBits = (sndBytes.get() * 8) / 1e6;

            // reset values
            sndBytes.set(0);

            double logIntervalInS;
            if (lastLogTime == -1) {
                logIntervalInS = LOG_INTERVAL_IN_MS / 1000.0;
            } else {
                logIntervalInS = (now - lastLogTime) / 1000.0;
                lastLogTime = now;
            }

            // calculate goodput
            double goodput = avgGoodput(currentSndMBits, logIntervalInS);

            String address;
            if (isConnected || goodput > 0) {
                address = connectedAddress;
            } else {
                address = null;
            }

            ConsoleLogger.log("%d\t| %s\t| ↑ | %d packets\t[%.02f Mbps]", id, address, totalSndPackets, goodput);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private double avgGoodput(double rcvMBits, double traceIntervalInS) {
        return rcvMBits / traceIntervalInS;
    }
}
