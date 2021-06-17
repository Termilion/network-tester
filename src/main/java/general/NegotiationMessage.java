package general;

import java.io.Serializable;

public class NegotiationMessage implements Serializable {

    private boolean flowMode;
    private boolean flowDirection;
    int startDelay;
    int port;
    int resetTime;
    int sndBuf;
    int rcvBuf;

    public NegotiationMessage(
            boolean flowMode,
            boolean flowDirection,
            int startDelay,
            int port,
            int resetTime,
            int sndBuf,
            int rcvBuf
    ) {
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

    public boolean isIoT() {
        return flowMode;
    }

    public void setFlowMode(boolean flowMode) {
        this.flowMode = flowMode;
    }

    public boolean isUplink() {
        return flowDirection;
    }

    public void setFlowDirection(boolean flowDirection) {
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
}
