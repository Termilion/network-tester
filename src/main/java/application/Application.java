package application;

import general.ConsoleLogger;
import general.NTPClient;

import java.util.Date;

public abstract class Application {
    Date startTime;
    Date stopTime;

    public final Application stopOn(Date stopTime) {
        this.stopTime = stopTime;
        return this;
    }

    public final Application startOn(Date startTime) {
        this.startTime = startTime;
        return this;
    }

    public final void start(NTPClient ntp) throws Exception {
        long current = ntp.getCurrentTimeNormalized();
        long waitTime = startTime.getTime() - current;
        ConsoleLogger.log("scheduled transmission in %s ms", waitTime);
        Thread.sleep(waitTime);

        doOnStart();
    }

    protected abstract void doOnStart() throws Exception;
}
