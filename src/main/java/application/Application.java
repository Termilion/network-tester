package application;

import general.TimeProvider;
import general.logger.ConsoleLogger;

import java.io.Closeable;
import java.io.IOException;
import java.util.Date;

public abstract class Application implements Closeable {
    public static final int LOG_INTERVAL_IN_MS = 1000;

    public enum Mode {
        IOT("iot", 1),
        BULK("bulk", 0);

        String name;
        int logInt;

        Mode(String name, int logInt) {
            this.name = name;
            this.logInt = logInt;
        }

        public String getName() {
            return name;
        }

        public int getLogInt() {
            return logInt;
        }
    }

    public enum Direction {
        UP,
        DOWN
    }

    protected int id;
    protected Mode mode;

    Date beginTime;
    Date startTime;
    Date stopTime;
    int duration;

    protected Application(int id, Mode mode) {
        this.id = id;
        this.mode = mode;
    }

    public final Application simBeginOn(Date simulationBegin) {
        this.beginTime = simulationBegin;
        return this;
    }

    public final Application stopOn(Date stopTime) {
        this.stopTime = stopTime;
        return this;
    }

    public final Application startOn(Date startTime) {
        this.startTime = startTime;
        return this;
    }

    public final Application duration(int duration) {
        this.duration = duration;
        return this;
    }

    public final void start(TimeProvider timeProvider) throws Exception {
        if (beginTime == null || startTime == null || stopTime == null) {
            throw new InstantiationException("simulationBegin, startTime or stopTime was not set");
        }

        init(beginTime, stopTime, duration);

        long current = timeProvider.getAdjustedTime();
        long waitTime = beginTime.getTime() - current;
        ConsoleLogger.log("simulation begin in %s ms", waitTime);
        Thread.sleep(waitTime);

        startLogging();

        current = timeProvider.getAdjustedTime();
        waitTime = startTime.getTime() - current;
        ConsoleLogger.log("scheduled App start in %s ms", waitTime);
        Thread.sleep(waitTime);

        startLogic();
    }

    public final int getId() {
        return id;
    }

    public Mode getMode() {
        return mode;
    }

    protected abstract void init(Date beginTime, Date stopTime, int duration);

    protected void startLogging() {
    }

    protected abstract void startLogic() throws Exception;

    @Override
    public abstract void close() throws IOException;
}
