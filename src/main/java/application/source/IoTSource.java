package application.source;

import general.ConsoleLogger;
import general.NTPClient;
import general.Utility;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Date;

public class IoTSource extends Source {

    public static final double IOT_DATA_SIZE = 1e6;
    Date simulationBegin;

    public IoTSource(NTPClient ntp, String address, int port, int resetTime, Date simulationBegin, Date stopTime, int bufferSize) throws IOException {
        super(ntp, address, port, bufferSize, resetTime, stopTime);
        this.simulationBegin = simulationBegin;
    }

    @Override
    public void execute() throws IOException {
        OutputStream out = socket.getOutputStream();
        int numberSend = 0;

        double maxNumberOfPackets = Math.ceil(IOT_DATA_SIZE / 1000);
        for (int j = 0; j < maxNumberOfPackets; j++) {
            byte[] kByte = new byte[1000];
            long time = this.ntp.getCurrentTimeNormalized();

            if (isRunning) {
                try {
                    byte[] bytes = Utility.encodeTime(kByte, time);
                    out.write(bytes);
                    ConsoleLogger.log("Sending paket: SendTime %s", time);
                    out.flush();
                    numberSend++;
                    if (numberSend % 1000 == 0) {
                        ConsoleLogger.log("%s | send one MByte! [%s]", socket.getInetAddress().getHostAddress(), numberSend);
                    }
                } catch (Exception e) {
                    double simTime = (time-simulationBegin.getTime())/1000.0;
                    ConsoleLogger.log("Source: Exception at simTime " + simTime);
                    e.printStackTrace();
                    break;
                }
            } else {
                break;
            }
        }
        ConsoleLogger.log("%s | stopped transmission. %s packets send", socket.getInetAddress().getHostAddress(), numberSend);
        out.close();
    }
}
