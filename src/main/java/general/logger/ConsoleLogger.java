package general.logger;

import general.TimeProvider;

import java.util.Date;

public class ConsoleLogger extends Logger {
    private static ConsoleLogger instance;

    public synchronized static ConsoleLogger create(TimeProvider timeProvider) {
        if (instance == null) {
            instance = new ConsoleLogger(timeProvider);
        }
        return instance;
    }

    private ConsoleLogger(TimeProvider timeProvider) {
        super(timeProvider);
    }

    public static void setSimulationBegin(Date simulationBegin) {
        if (ConsoleLogger.instance != null) {
            ConsoleLogger.instance.m_setSimulationBegin(simulationBegin);
        } else {
            throw new IllegalStateException("Not yet initialized");
        }
    }

    public static void log(String message) {
        if (ConsoleLogger.instance != null) {
            ConsoleLogger.instance.m_log(message);
        } else {
            throw new IllegalStateException("Not yet initialized");
        }
    }

    public static void log(String message, Object... args) {
        if (ConsoleLogger.instance != null) {
            ConsoleLogger.instance.m_log(message, args);
        } else {
            throw new IllegalStateException("Not yet initialized");
        }
    }

    public static void log(String message, ConsoleLogger.LogLevel level) {
        switch (level) {
            case WARN:
                log("\u001b[33m%s\u001b[0m", message);
                break;
            case ERROR:
                log("\u001b[31m%s\u001b[0m", message);
                break;
            default:
                log(message);
        }
    }

    @Override
    protected void m_log(String message) {
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

        System.out.printf("[%s] [%s]\t%s\n", time, simTime, message);
    }
}
