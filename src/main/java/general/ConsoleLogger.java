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
    private Date simulationBegin;
    private final TimeProvider timeProvider;

    private static ConsoleLogger instance;

    public synchronized static ConsoleLogger init(TimeProvider timeProvider) {
        if (instance == null) {
            instance = new ConsoleLogger(timeProvider);
        }
        return instance;
    }

    private ConsoleLogger(TimeProvider timeProvider) {
        this.timeProvider = timeProvider;
    }

    public static void setSimulationBegin(Date simulationBegin) {
        if (ConsoleLogger.instance != null) {
            ConsoleLogger.instance.simulationBegin = simulationBegin;
        } else {
            throw new IllegalStateException("Not yet initialized");
        }
    }

    public static void log(String message) {
        if (ConsoleLogger.instance != null) {
            ConsoleLogger.instance.log(message, LOG_LEVEL.INFO);
        } else {
            throw new IllegalStateException("Not yet initialized");
        }
    }

    public static void log(String message, Object... args) {
        log(String.format(message, args));
    }

    public void log(String message, LOG_LEVEL level) {
        long currentTime;
        try {
            currentTime = timeProvider.getAdjustedTime();
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
}
