package general.logger;

import general.TimeProvider;

import java.text.SimpleDateFormat;
import java.util.Date;

public abstract class Logger {
    public enum LogLevel {
        ERROR,
        WARN,
        INFO
    }

    protected static SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
    protected Date simulationBegin;
    protected Date simulationEnd;
    protected final TimeProvider timeProvider;

    protected Logger(TimeProvider timeProvider) {
        this.timeProvider = timeProvider;
    }

    protected void m_setSimulationBegin(Date simulationBegin) {
        this.simulationBegin = simulationBegin;
    }
    protected void m_setSimulationEnd(Date simulationEnd) {
        this.simulationEnd = simulationEnd;
    }

    protected void m_log(String message, Object... args) {
        m_log(String.format(message, args));
    }

    protected abstract void m_log(String message);
}
