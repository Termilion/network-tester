import application.Application;
import general.ResultMessage;
import general.logger.ConsoleLogger;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;

public class PostHandshakeThread extends Thread {
    private final Socket client;
    int run;
    boolean reconnectAfterPostHandshake;

    Application.Direction direction;
    Application.Mode mode;
    int id;
    byte[] fileContent;

    InetAddress clientAddress;

    ObjectInputStream in;
    ObjectOutputStream out;

    public PostHandshakeThread(Socket client, int run, boolean reconnectAfterPostHandshake) {
        this.client = client;
        this.clientAddress = client.getInetAddress();
        this.run = run;
        this.reconnectAfterPostHandshake = reconnectAfterPostHandshake;

        try {
            this.out = new ObjectOutputStream(new BufferedOutputStream(client.getOutputStream()));
            this.out.flush();
            this.in = new ObjectInputStream(new BufferedInputStream(client.getInputStream()));
            ConsoleLogger.log("connection to client: %s successful", client.getInetAddress());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            ConsoleLogger.log("%s | waiting for result message", client.getInetAddress());
            ResultMessage result = (ResultMessage) in.readUnshared();
            ConsoleLogger.log("%s | received result message", client.getInetAddress());
            this.direction = result.getDirection();
            this.mode = result.getMode();
            this.id = result.getId();
            this.fileContent = result.getFileContent();

            ConsoleLogger.log("%s | sending whether reconnect after handshake", client.getInetAddress());
            out.writeObject(reconnectAfterPostHandshake);
            out.flush();

            // if the client is down-link, he needs to upload his results and we need to save them
            if (direction == Application.Direction.DOWN) {
                Path path = new File(String.format("./out/received_run_%d_flow_%d_%s.csv", run, id, mode.getName())).toPath();
                Files.write(path, fileContent);
            }
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            e.printStackTrace();
        }
    }
}
