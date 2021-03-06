package application;

import general.TimeProvider;
import general.Utility;
import general.logger.ConsoleLogger;
import general.logger.FileLogger;

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
        IOT("iot", 0),
        BULK("bulk", 1);

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
        UP("up"),
        DOWN("down");

        String name;

        Direction(String name){
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    protected int id;
    protected Mode mode;
    protected Direction directionAsSeenByClient;

    private String dataNetworkInterface = null;

    Date beginTime;
    Date startTime;
    Date stopTime;
    int duration;

    protected Application(int id, Mode mode, Direction directionAsSeenByClient) {
        this.id = id;
        this.mode = mode;
        this.directionAsSeenByClient = directionAsSeenByClient;
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
        ConsoleLogger.log("Sleeping %s ms until simulation begin", waitTime);
        FileLogger.log("Sleeping %s ms until simulation begin", waitTime);
        Thread.sleep(waitTime);

        long preLogTime = System.currentTimeMillis();
        startLogging();
        long postLogTime = System.currentTimeMillis();
        FileLogger.log("startLogging took %d ms", (postLogTime-preLogTime));

        current = timeProvider.getAdjustedTime();
        waitTime = startTime.getTime() - current;
        ConsoleLogger.log("Sleeping %s ms until app start", waitTime);
        FileLogger.log("Sleeping %s ms until app start", waitTime);
        Thread.sleep(waitTime);

        startLogic();
    }

    public final int getId() {
        return id;
    }

    public Mode getMode() {
        return mode;
    }

    public Direction getDirectionAsSeenByClient() {
        return directionAsSeenByClient;
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
    protected final Socket createSocket(String address, int port) throws IOException {
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
    protected final ServerSocket createServerSocket(int port) throws IOException {
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
