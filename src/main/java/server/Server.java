package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public abstract class Server {
    ArrayList<Socket> connected = new ArrayList<>();
    ArrayList<Thread> clientThreads = new ArrayList<>();

    public Server(int port) {
        try {
            listen(port);
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    private void listen(int port) throws IOException {
        ServerSocket socket = new ServerSocket(port);
        System.out.printf("opened server socket on address %s\n", socket.getInetAddress());
        while (true) {
            Socket client = socket.accept();
            Thread clientThread = new Thread() {
                @Override
                public void run() {
                    executeLogic(client);
                    try {
                        if(!client.isClosed()) {
                            client.close();
                        }
                    } catch(IOException e) {
                        e.printStackTrace();
                    }
                    super.run();
                }
            };
            connected.add(client);
            clientThreads.add(clientThread);
            clientThread.start();
        }
    }

    public abstract void executeLogic(Socket client);
}
