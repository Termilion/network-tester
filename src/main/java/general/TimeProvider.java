package general;

import java.io.Closeable;
import java.io.IOException;

public abstract class TimeProvider implements Closeable {
    public abstract long getAdjustedTime();

    @Override
    public void close() throws IOException {
    }

    public abstract void startSyncTime() throws IOException;

    public void stopSyncTime() {
    }
}
