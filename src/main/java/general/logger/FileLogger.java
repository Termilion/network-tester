package general.logger;

import general.TimeProvider;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

public class FileLogger extends Logger {
    private static FileLogger instance;

    File outFile;
    FileWriter out;

    public synchronized static FileLogger create(TimeProvider timeProvider, String path) throws IOException {
        if (instance == null) {
            instance = new FileLogger(timeProvider, path);
        }
        return instance;
    }

    private FileLogger(TimeProvider timeProvider, String path) throws IOException {
        super(timeProvider);
        this.outFile = new File(path);
        outFile.getParentFile().mkdirs();
        this.out = new FileWriter(outFile);
    }

    public static void setSimulationBegin(Date simulationBegin) {
        if (FileLogger.instance != null) {
            FileLogger.instance.m_setSimulationBegin(simulationBegin);
        } else {
            throw new IllegalStateException("Not yet initialized");
        }
    }

    public static void log(String message) {
        if (FileLogger.instance != null) {
            FileLogger.instance.m_log(message);
        } else {
            throw new IllegalStateException("Not yet initialized");
        }
    }

    public static void log(String message, Object... args) {
        if (FileLogger.instance != null) {
            FileLogger.instance.m_log(message, args);
        } else {
            throw new IllegalStateException("Not yet initialized");
        }
    }

    public static void log(String message, ConsoleLogger.LogLevel level) {
        switch (level) {
            case WARN:
                log("[WARN] %s", message);
                break;
            case ERROR:
                log("[ERROR] %s", message);
                break;
            default:
                log(message);
        }
    }

    @Override
    protected void m_log(String message) {
        if (instance == null) {
            throw new IllegalStateException("Not yet initialized");
        }

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
        try {
            out.write(String.format("[%s] [%s] %s\n", time, simTime, message));
            out.flush();
        } catch (IOException e) {
            ConsoleLogger.log("FileLogger encountered a problem", LogLevel.ERROR);
            e.printStackTrace();
            System.exit(1);
        }
    }
}
