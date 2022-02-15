package general;

import application.Application.Direction;
import application.Application.Mode;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class ResultMessage implements Serializable {
    private final int id;
    private final Mode flowMode;
    private final Direction flowDirection;
    private final Map<String, byte[]> files;

    public ResultMessage(int id, Mode flowMode, Direction flowDirection) {
        this.id = id;
        this.flowMode = flowMode;
        this.flowDirection = flowDirection;
        this.files = new HashMap<>();
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

    public ResultMessage addFile(String fileName, byte[] fileContent) {
        this.files.put(fileName, fileContent);
        return this;
    }

    public Map<String, byte[]> getFiles() {
        return files;
    }
}
