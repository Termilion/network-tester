package general;

import java.io.Serializable;

public class NegotiationMessage implements Serializable {

    private boolean flowMode;
    private boolean flowDirection;
    int startDelay;
    int port;

    public NegotiationMessage(
            boolean flowMode,
            boolean flowDirection,
            int startDelay,
            int port
    ) {
        this.flowMode = flowMode;
        this.flowDirection = flowDirection;
        this.startDelay = startDelay;
        this.port = port;
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
}
