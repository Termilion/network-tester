package general;

import java.io.Serializable;
import java.util.Date;

public class InstructionMessage implements Serializable {
    private final int id;
    private final Date startTime;
    private final Date stopTime;
    private final int serverPort;
    private final int resultPort;

    public InstructionMessage(int id, Date startTime, Date stopTime, int serverPort, int resultPort) {
        this.id = id;
        this.startTime = startTime;
        this.stopTime = stopTime;
        this.serverPort = serverPort;
        this.resultPort = resultPort;
    }

    public int getId() {
        return id;
    }

    public Date getStartTime() {
        return startTime;
    }

    public Date getStopTime() {
        return stopTime;
    }

    public int getServerPort() {
        return serverPort;
    }

    public int getResultPort() {
        return resultPort;
    }
}
