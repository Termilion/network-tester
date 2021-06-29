package general;

import java.text.SimpleDateFormat;
import java.util.Date;

enum LOG_LEVEL {
    ERROR,
    WARN,
    INFO
}

public class ConsoleLogger {
    static SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");

    public static void log(String message) {
        log(message, LOG_LEVEL.INFO);
    }

    public static void log(String message, LOG_LEVEL level) {
        long currentTime;
        try {
            currentTime = NTPClient.getInstance().getAdjustedTime();
        } catch (NullPointerException e) {
            currentTime = System.currentTimeMillis();
        }
        String time = formatter.format(new Date(currentTime));

        switch (level) {
            case WARN:
                System.out.printf("[%s] \u001b[33m%s\u001b[0m\n", time, message);
                break;
            case ERROR:
                System.out.printf("[%s] \u001b[31m%s\u001b[0m\n", time, message);
                break;
            default:
                System.out.printf("[%s] %s\n", time, message);
        }
    }

    public static void log(String message, Object... args) {
        log(String.format(message, args));
    }
}
