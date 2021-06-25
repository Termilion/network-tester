package general;

import java.io.Serializable;
import java.util.Date;

public class ResultMessage implements Serializable {
    private final int id;
    private final boolean flowMode;
    private final boolean flowDirection;
    private final byte[] fileContent;

    public ResultMessage(int id, boolean flowMode, boolean flowDirection, byte[] fileContent) {
        this.id = id;
        this.flowMode = flowMode;
        this.flowDirection = flowDirection;
        this.fileContent = fileContent;
    }

    public int getId() {
        return id;
    }

    public boolean isIoT() {
        return flowMode;
    }

    public boolean isUplink() {
        return flowDirection;
    }

    public byte[] getFileContent() {
        return fileContent;
    }
}
