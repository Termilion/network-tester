package application;

import general.TimeProvider;
import general.logger.ConsoleLogger;

import java.io.Closeable;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public abstract class Application implements Closeable {
    static SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");

    Date beginTime;
    Date startTime;
    Date stopTime;

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

    public final void start(TimeProvider timeProvider) throws Exception {
        if (beginTime == null || startTime == null || stopTime == null) {
            throw new InstantiationException("simulationBegin, startTime or stopTime was not set");
        }

        init(beginTime, stopTime);

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

    protected abstract void init(Date beginTime, Date stopTime);

    protected void startLogging() {
    }

    protected abstract void startLogic() throws Exception;

    @Override
    public abstract void close() throws IOException;
}
