package general;

import java.io.Serializable;
import java.util.Date;

public class InstructionMessage implements Serializable {
    private final int id;
    private final Date simulationBegin;
    private final Date startTime;
    private final Date stopTime;
    private final int duration;
    private final int serverPort;
    private final int resultPort;

    public InstructionMessage(int id, Date simulationBegin, Date startTime, Date stopTime, int duration, int serverPort, int resultPort) {
        this.id = id;
        this.simulationBegin = simulationBegin;
        this.startTime = startTime;
        this.stopTime = stopTime;
        this.duration = duration;
        this.serverPort = serverPort;
        this.resultPort = resultPort;
    }

    public int getId() {
        return id;
    }

    public Date getSimulationBegin() {
        return simulationBegin;
    }

    public Date getStartTime() {
        return startTime;
    }

    public Date getStopTime() {
        return stopTime;
    }

    public int getDuration() {
        return duration;
    }

    public int getServerPort() {
        return serverPort;
    }

    public int getResultPort() {
        return resultPort;
    }
}
