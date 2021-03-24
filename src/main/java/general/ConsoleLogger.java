package general;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;

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
        String time = formatter.format(new Timestamp(System.currentTimeMillis()));

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
}
