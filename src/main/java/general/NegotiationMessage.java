package general;

import java.io.Serializable;

public class NegotiationMessage implements Serializable {

    private boolean flowMode;
    private boolean flowDirection;
    int startDelay;

    public NegotiationMessage(
            boolean flowMode,
            boolean flowDirection,
            int startDelay
    ) {
        this.flowMode = flowMode;
        this.flowDirection = flowDirection;
        this.startDelay = startDelay;
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
