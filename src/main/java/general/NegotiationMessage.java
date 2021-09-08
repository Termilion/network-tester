package general;

import application.Application.Direction;
import application.Application.Mode;

import java.io.Serializable;

public class NegotiationMessage implements Serializable {

    int previousId;
    private Mode flowMode;
    private Direction flowDirection;
    int startDelay;
    int port;
    int resetTime;
    int sndBuf;
    int rcvBuf;

    public NegotiationMessage(
            int previousId,
            Mode flowMode,
            Direction flowDirection,
            int startDelay,
            int port,
            int resetTime,
            int sndBuf,
            int rcvBuf
    ) {
        this.previousId = previousId;
        this.flowMode = flowMode;
        this.flowDirection = flowDirection;
        this.startDelay = startDelay;
        this.port = port;
        this.resetTime = resetTime;
        this.sndBuf = sndBuf;
        this.rcvBuf = rcvBuf;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public Mode getMode() {
        return flowMode;
    }

    public void setFlowMode(Mode flowMode) {
        this.flowMode = flowMode;
    }

    public Direction getDirection() {
        return flowDirection;
    }

    public void setDirection(Direction flowDirection) {
        this.flowDirection = flowDirection;
    }

    public int getStartDelay() {
        return startDelay;
    }

    public void setStartDelay(int startDelay) {
        this.startDelay = startDelay;
    }

    public int getSndBuf() {
        return sndBuf;
    }

    public int getRcvBuf() {
        return rcvBuf;
    }

    public int getResetTime() {
        return resetTime;
    }

    public boolean hasPreviousId() {
        return previousId != -1;
    }

    public int getPreviousId() {
        return previousId;
    }
}
