package general;

import java.io.Serializable;
import java.util.Date;

public class TimeMessage implements Serializable {
    private Date time;

    public TimeMessage(Date time) {
        this.time = time;
    }

    public TimeMessage() {
        this.time = new Date();
    }

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }
}
