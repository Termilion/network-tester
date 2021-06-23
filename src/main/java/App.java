import picocli.CommandLine;

@CommandLine.Command(subcommands = {
        Server.class,
        Client.class
})
public class App implements Runnable {

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @Override
    public void run() {
        throw new CommandLine.ParameterException(spec.commandLine(), "Missing required subcommand");
    }

    public static void main(String[] args) {
        CommandLine commandLine = new CommandLine(new App());
        System.exit(commandLine.execute(args));
    }
}
