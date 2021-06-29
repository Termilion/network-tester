package application;

import general.ConsoleLogger;
import general.NTPClient;
import general.TimeProvider;

import java.io.Closeable;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public abstract class Application implements Closeable {
    static SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");

    Date simulationBegin;
    Date startTime;
    Date stopTime;

    public final Application simBeginOn(Date simulationBegin) {
        this.simulationBegin = simulationBegin;
        ConsoleLogger.log(String.format("Simulation beginning %s on %s", this, formatter.format(simulationBegin)));
        return this;
    }

    public final Application stopOn(Date stopTime) {
        this.stopTime = stopTime;
        ConsoleLogger.log(String.format("Stopping %s on %s", this, formatter.format(stopTime)));
        return this;
    }

    public final Application startOn(Date startTime) {
        this.startTime = startTime;
        ConsoleLogger.log(String.format("Starting %s on %s", this, formatter.format(startTime)));
        return this;
    }

    public final void start(TimeProvider timeProvider) throws Exception {
        if (simulationBegin == null || startTime == null || stopTime == null) {
            throw new InstantiationException("simulationBegin, startTime or stopTime was not set");
        }

        long current = timeProvider.getAdjustedTime();
        long waitTime = startTime.getTime() - current;
        ConsoleLogger.log("scheduled transmission in %s ms", waitTime);
        Thread.sleep(waitTime);

        doOnStart();
    }

    protected abstract void doOnStart() throws Exception;

    @Override
    public abstract void close() throws IOException;
}
