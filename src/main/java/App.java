import java.util.concurrent.Callable;
import picocli.CommandLine;

public class App implements Callable<Integer> {

    @CommandLine.Parameters(index = "0", description = "The file whose checksum to calculate.")
    private String address;

    @CommandLine.Option(names = {"-i", "--iot"}, description = "b for bulk, i for iot")
    private boolean iot = false;

    @CommandLine.Option(names = {"-u", "--up"}, description = "set for uplink nodes")
    private boolean up = false;

    @Override
    public Integer call() throws Exception {
        return 0;
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new App()).execute(args);
        System.exit(exitCode);
    }
}
