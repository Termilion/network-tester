package general;

import java.io.Serializable;

public class NegotiationMessage implements Serializable {
    public enum FlowMode {
        bulk,
        iot
    }

    public enum FlowDirection {
        down,
        up
    }

    private FlowMode flowMode;
    private FlowDirection flowDirection;

    public NegotiationMessage(
            FlowMode flowMode,
            FlowDirection flowDirection
    ) {
        this.flowMode = flowMode;
        this.flowDirection = flowDirection;
    }

    public FlowMode getFlowMode() {
        return flowMode;
    }

    public void setFlowMode(FlowMode flowMode) {
        this.flowMode = flowMode;
    }

    public FlowDirection getFlowDirection() {
        return flowDirection;
    }

    public void setFlowDirection(FlowDirection flowDirection) {
        this.flowDirection = flowDirection;
    }
}
