import picocli.CommandLine;
import server.LogServer;

import java.util.concurrent.Callable;

public class ServerApplication implements Callable<Integer> {
    @CommandLine.Option(names = {"-p", "--port"}, defaultValue = "5000", description = "Number of the server port, default value: 5000")
    int port;

    @CommandLine.Option(names = {"-b", "--buffer"}, defaultValue = "1000", description = "ReceiveBufferSize in packets, default value: 1000")
    int receiveBufferSize;

    @Override
    public Integer call() throws Exception {
        new LogServer(port, receiveBufferSize);
        return 0;
    }

    public static void main(String[] args) {
        new CommandLine(new ServerApplication()).execute(args);
    }
}
