package general;

import java.io.Serializable;
import java.util.Date;

public class InstructionMessage implements Serializable {
    private Date time;
    private int port;

    public InstructionMessage(Date time, int port) {
        this.time = time;
        this.port = port;
    }

    public Date getTime() {
        return time;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setTime(Date time) {
        this.time = time;
    }
}
