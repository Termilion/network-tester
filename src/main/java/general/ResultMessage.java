package general;

import application.Application.Direction;
import application.Application.Mode;

import java.io.Serializable;

public class ResultMessage implements Serializable {
    private final int id;
    private final Mode flowMode;
    private final Direction flowDirection;
    private final byte[] fileContent;

    public ResultMessage(int id, Mode flowMode, Direction flowDirection, byte[] fileContent) {
        this.id = id;
        this.flowMode = flowMode;
        this.flowDirection = flowDirection;
        this.fileContent = fileContent;
    }

    public int getId() {
        return id;
    }

    public Mode getMode() {
        return flowMode;
    }

    public Direction getDirection() {
        return flowDirection;
    }

    public byte[] getFileContent() {
        return fileContent;
    }
}
