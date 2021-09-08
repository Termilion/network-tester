package application.source;

import general.TimeProvider;
import general.Utility;
import general.logger.ConsoleLogger;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicLong;

public class IoTSource extends Source {

    public static final double IOT_DATA_SIZE = 1e6;

    String connectedAddress;
    int id;

    final AtomicLong sndBytes;
    long totalSndPackets = 0;

    long lastLogTime = -1;

    public IoTSource(TimeProvider timeProvider, String address, int port, int resetTime, int bufferSize, int id) {
        super(timeProvider, address, port, bufferSize, resetTime, id, Mode.IOT);
        this.id = id;
        this.sndBytes = new AtomicLong(0);
    }

    @Override
    protected void executeLogic() throws IOException {
        OutputStream out = socket.getOutputStream();

        connectedAddress = socket.getInetAddress().getHostAddress();

        double maxNumberOfPackets = Math.ceil(IOT_DATA_SIZE / 1000);
        for (int j = 0; j < maxNumberOfPackets; j++) {
            byte[] payload = new byte[1000];
            long time = this.timeProvider.getAdjustedTime();

            if (isRunning) {
                try {
                    byte[] bytes = Utility.encodeTime(payload, time);
                    out.write(bytes);
                    out.flush();

                    measureBytes(payload.length);
                } catch (Exception e) {
                    double simTime = (time - beginTime.getTime()) / 1000.0;
                    ConsoleLogger.log("Source: Exception at simTime " + simTime);
                    e.printStackTrace();
                    break;
                }
            } else {
                break;
            }
        }
        ConsoleLogger.log("%s | stopped transmission. %s packets send", socket.getInetAddress().getHostAddress(), totalSndPackets);
        out.close();
        socket.close();
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
            double currentSndMBits;
            synchronized (sndBytes) {
                currentSndMBits = (sndBytes.get() * 8) / 1e6;
                sndBytes.set(0);
            }

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
            this.chart.plotData((int) getSimTime(), goodput);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private double avgGoodput(double rcvMBits, double traceIntervalInS) {
        return rcvMBits / traceIntervalInS;
    }
}
