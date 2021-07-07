import general.ConsoleLogger;
import general.ResultMessage;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;

public class PostHandshakeThread extends Thread {
    private final Socket client;
    int run;
    boolean reconnectAfterPostHandshake;

    boolean uplink;
    boolean mode;
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
            this.uplink = result.isUplink();
            this.mode = result.isIoT();
            this.id = result.getId();
            this.fileContent = result.getFileContent();

            ConsoleLogger.log("%s | sending whether reconnect after handshake", client.getInetAddress());
            out.writeObject(reconnectAfterPostHandshake);
            out.flush();

            if (!uplink) {
                Path path = new File(String.format("./out/sink_run_%d_flow_%d_%s.csv", run, id, getModeString())).toPath();
                Files.write(path, fileContent);
            }
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            e.printStackTrace();
        }
    }

    private String getModeString() {
        if (mode) {
            return "iot";
        } else {
            return "bulk";
        }
    }
}
