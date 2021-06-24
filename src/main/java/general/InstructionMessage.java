package general;

import java.io.Serializable;
import java.util.Date;

public class InstructionMessage implements Serializable {
    //TODO Flowid?
    private Date startTime;
    private Date stopTime;
    private int port;

    public InstructionMessage(Date startTime, Date stopTime, int port) {
        this.startTime = startTime;
        this.stopTime = stopTime;
        this.port = port;
    }

    public Date getStartTime() {
        return startTime;
    }

    public Date getStopTime() {
        return stopTime;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }
}
