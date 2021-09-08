package application;

import general.TimeProvider;
import general.Utility;
import general.logger.ConsoleLogger;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;

public abstract class Application implements Closeable {
    public static final int LOG_INTERVAL_IN_MS = 1000;

    public enum Mode {
        IOT("iot", 1),
        BULK("bulk", 0);

        String name;
        int logInt;

        Mode(String name, int logInt) {
            this.name = name;
            this.logInt = logInt;
        }

        public String getName() {
            return name;
        }

        public int getLogInt() {
            return logInt;
        }
    }

    public enum Direction {
        UP,
        DOWN
    }

    protected int id;
    protected Mode mode;

    private String dataNetworkInterface = null;

    Date beginTime;
    Date startTime;
    Date stopTime;
    int duration;

    protected Application(int id, Mode mode) {
        this.id = id;
        this.mode = mode;
    }

    //TODO builder pattern mixed with normal methods in the same class. Push builder pattern methods in own class
    public final Application simBeginOn(Date simulationBegin) {
        this.beginTime = simulationBegin;
        return this;
    }

    public final Application stopOn(Date stopTime) {
        this.stopTime = stopTime;
        return this;
    }

    public final Application startOn(Date startTime) {
        this.startTime = startTime;
        return this;
    }

    public final Application duration(int duration) {
        this.duration = duration;
        return this;
    }

    public final void start(TimeProvider timeProvider) throws Exception {
        if (beginTime == null || startTime == null || stopTime == null) {
            throw new InstantiationException("simulationBegin, startTime or stopTime was not set");
        }

        init(beginTime, stopTime, duration);

        long current = timeProvider.getAdjustedTime();
        long waitTime = beginTime.getTime() - current;
        ConsoleLogger.log("simulation begin in %s ms", waitTime);
        Thread.sleep(waitTime);

        startLogging();

        current = timeProvider.getAdjustedTime();
        waitTime = startTime.getTime() - current;
        ConsoleLogger.log("scheduled App start in %s ms", waitTime);
        Thread.sleep(waitTime);

        startLogic();
    }

    public final int getId() {
        return id;
    }

    public Mode getMode() {
        return mode;
    }

    @Override
    public abstract void close() throws IOException;

    public final void setDataNetworkInterface(String networkInterface) {
        this.dataNetworkInterface = networkInterface;
    }

    /**
     * Provides a Socket.
     * Child classes must use this method to obtain a socket, to ensure that the socket respects the user specified
     * network interface.
     *
     * @param address address
     * @param port    port
     * @return a Socket (bound to an interface)
     * @throws IOException e
     */
    protected final Socket getSocket(String address, int port) throws IOException {
        Socket socket = new Socket(address, port);
        if (dataNetworkInterface != null) {
            NetworkInterface ni = NetworkInterface.getByName(dataNetworkInterface);
            if (ni == null) {
                throw new Utility.InterfaceNotFoundException(dataNetworkInterface);
            }
            socket.bind(new InetSocketAddress(ni.getInetAddresses().nextElement(), 0));
        }
        return socket;
    }

    /**
     * Provides a ServerSocket.
     * Child classes must use this method to obtain a ServerSocket, to ensure that the ServerSocket respects the user
     * specified network interface.
     *
     * @param port port
     * @return a ServerSocket (bound to an interface)
     * @throws IOException e
     */
    protected final ServerSocket getServerSocket(int port) throws IOException {
        ServerSocket serverSocket;
        if (dataNetworkInterface != null) {
            NetworkInterface ni = NetworkInterface.getByName(dataNetworkInterface);
            if (ni == null) {
                throw new Utility.InterfaceNotFoundException(dataNetworkInterface);
            }
            // in order to bind a ServerSocket to an address, we has to be initialized with a parameterless constructor
            serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress(ni.getInetAddresses().nextElement(), 0));
        } else {
            serverSocket = new ServerSocket(port);
        }
        return serverSocket;
    }

    protected abstract void init(Date beginTime, Date stopTime, int duration);

    protected void startLogging() {
    }

    protected abstract void startLogic() throws Exception;
}
