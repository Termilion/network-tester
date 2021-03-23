import client.BulkClient;
import client.Client;
import client.IoTClient;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.concurrent.Callable;

public class ClientApplication implements Callable<Integer> {
    @CommandLine.Option(names = {"-a", "--address"}, defaultValue = "localhost", description = "Server IP address, default value: localhost")
    String address;

    @CommandLine.Option(names = {"-p", "--port"}, defaultValue = "5000", description = "Number of the server port, default value: 5000")
    int port;

    @CommandLine.Option(names = {"-n", "-number"}, defaultValue = "1", description = "Number of Clients to start, default value: 3")
    int number;

    @CommandLine.Option(names = {"--name"}, defaultValue = "Client", description = "Prefix of the client names, default value: Client")
    String namePrefix;

    @CommandLine.Option(names = {"-l", "--loops"}, defaultValue = "3", description = "Number of sequential transmissions, default value: 3")
    int numberOfTransmissions;

    @CommandLine.Option(names = {"-t", "--type"}, defaultValue = "b", description = "type of the transmission. b = bulk, i = iot, default value: b")
    String type;

    @CommandLine.Option(names = {"-b", "--buffer"}, defaultValue = "1000", description = "SendBufferSize in packets, default value: 1000")
    int sendBufferSize;

    ArrayList<Client> clients = new ArrayList<>();

    @Override
    public Integer call() throws Exception {
        for (int i = 0; i < number; i++) {
            String name = String.format("%s_%d", namePrefix, i);
            System.out.printf("Started %s: %s:%d\n", name, address, port);
            if (type.equals("b")) {
                clients.add(new BulkClient(address, port, name, numberOfTransmissions, sendBufferSize));
            } else {
                clients.add(new IoTClient(address, port, name));
            }
        }
        return 0;
    }

    public static void main(String[] args) {
        new CommandLine(new ClientApplication()).execute(args);
    }
}

