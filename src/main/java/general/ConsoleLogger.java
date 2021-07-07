package general;

import java.text.SimpleDateFormat;
import java.util.Date;

enum LOG_LEVEL {
    ERROR,
    WARN,
    INFO
}

public class ConsoleLogger {
    static SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
    public static Date simulationBegin;

    public static void log(String message) {
        log(message, LOG_LEVEL.INFO);
    }

    public static void log(String message, LOG_LEVEL level) {
        long currentTime;
        try {
            currentTime = NTPClient.getInstance().getAdjustedTime();
        } catch (NullPointerException e) {
            currentTime = System.currentTimeMillis();
        }
        String time = formatter.format(new Date(currentTime));
        String simTime;
        if (simulationBegin == null) {
            simTime = "-";
        } else {
            if (currentTime < simulationBegin.getTime()) {
                simTime = "<";
            } else {
                simTime = String.format("%.02fs", (currentTime - simulationBegin.getTime()) / 1000.0);
            }
        }

        switch (level) {
            case WARN:
                System.out.printf("[%s] [%s] \u001b[33m%s\u001b[0m\n", time, simTime, message);
                break;
            case ERROR:
                System.out.printf("[%s] [%s] \u001b[31m%s\u001b[0m\n", time, simTime, message);
                break;
            default:
                System.out.printf("[%s] [%s] %s\n", time, simTime, message);
        }
    }

    public static void log(String message, Object... args) {
        log(String.format(message, args));
    }
}
