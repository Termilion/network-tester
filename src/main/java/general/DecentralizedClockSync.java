package general;

import general.logger.ConsoleLogger;

import java.io.Closeable;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.nio.ByteBuffer;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Distributed UDP/Multicast class for figuring out the clock differences on a LAN
 * Replace System.currentTimeMillis() with ClockSync.getAdjustedTime() which will gradually adapt once startSync() is called
 * Should work with any number of LAN clients joining/leaving.
 *
 * No server, totally distributed.
 */
public class DecentralizedClockSync extends TimeProvider implements Closeable {

    private static DecentralizedClockSync instance;

    public static DecentralizedClockSync getInstance() throws IOException {
        if (instance == null) {
            synchronized (DecentralizedClockSync.class) {
                if (instance == null) {
                    instance = new DecentralizedClockSync();
                }
            }
        }
        return instance;
    }

    protected volatile AtomicLong timerOffset = new AtomicLong(0);

    final static short PACKET_CLASS = 2700; // random
    static final Random r = new Random();
    static final String UPNP_ADDRESS = "239.255.255.250";
    static final int UPNP_MULTI_PORT = 1900;
    static final int BROADCAST_INTERVAL = 3000;

    final ByteBuffer bin, bout;
    final int BUFF_SIZE = Short.SIZE + Integer.SIZE + Long.SIZE; // Packet class + machine ID + Time
    final InetAddress group;
    final MulticastSocket ms;
    final int myId;
    final ReceiveRunnable rc = new ReceiveRunnable();
    Thread receiveThread;
    Timer broadcastTask;

    private DecentralizedClockSync() throws IOException {
        this.ms = new MulticastSocket(null);
        this.group = InetAddress.getByName(UPNP_ADDRESS);
        this.ms.setTimeToLive(4);
        this.ms.setSoTimeout(0);
        this.ms.setLoopbackMode(true);
        this.ms.setReuseAddress(true);
        if (!this.ms.getReuseAddress()) {
            ConsoleLogger.log("MS Socket can't reuse address");
        }
        this.ms.bind(new InetSocketAddress(InetAddress.getByName("0.0.0.0"), UPNP_MULTI_PORT));
        this.ms.joinGroup(this.group);

        this.bin = ByteBuffer.allocate(this.BUFF_SIZE);
        this.bout = ByteBuffer.allocate(this.BUFF_SIZE);

        this.myId = r.nextInt();
    }

    /**
     * The reason for this to exist.  Use instead of System.currentTimeMillis()
     */
    @Override
    public long getAdjustedTime() {
        return System.currentTimeMillis() + timerOffset.get();
    }

    /**
     * Don't forget to run this!
     */
    @Override
    public void startSyncTime() {
        this.receiveThread = new Thread(rc);
        this.receiveThread.start();
        this.broadcastTask = new Timer();
        this.broadcastTask.schedule(new BroadcastTask(), 0, BROADCAST_INTERVAL);
    }

    /**
     * Don't bother stopping it if you are ok with the overhead, in case new clients join
     */
    @Override
    public void stopSyncTime() {
        this.rc.running = false;
        this.broadcastTask.cancel();
    }

    @Override
    public void close() throws IOException {
        if (!this.ms.isClosed()) {
            if (this.ms.isBound() && !this.ms.isClosed()) {
                this.ms.leaveGroup(this.group);
                this.ms.close();
            }
        }
    }

    /**
     * Sends out a timestamped multicast "ping"
     */
    final class BroadcastTask extends TimerTask {

        @Override
        public void run() {
            DecentralizedClockSync.this.bout.putShort(PACKET_CLASS);
            DecentralizedClockSync.this.bout.putInt(DecentralizedClockSync.this.myId);
            //DecentralizedClockSync.this.bout.putLong(DecentralizedClockSync.this.getAdjustedTime());
            DecentralizedClockSync.this.bout.putLong(System.currentTimeMillis());
            final DatagramPacket dp = new DatagramPacket(DecentralizedClockSync.this.bout.array(), DecentralizedClockSync.this.bout.position(), DecentralizedClockSync.this.group, UPNP_MULTI_PORT);
            try {
                DecentralizedClockSync.this.ms.send(dp);
                DecentralizedClockSync.this.bout.clear();
            } catch (final IOException ex) {
                ex.printStackTrace();
            }
            //ConsoleLogger.log("Sent ClockSync ping");
        }
    }

    /**
     * Blocking thread for getting timer packets
     */
    final class ReceiveRunnable implements Runnable {

        boolean running = false;

        @Override
        public void run() {
            this.running = true;
            while (this.running) {
                try {
                    DecentralizedClockSync.this.bin.clear();
                    final DatagramPacket dp = new DatagramPacket(DecentralizedClockSync.this.bin.array(), DecentralizedClockSync.this.bin.capacity());
                    DecentralizedClockSync.this.ms.receive(dp);  // BLOCK HERE
                    final long now = DecentralizedClockSync.this.getAdjustedTime(); // Get time as fast as possible after receiving
                    DecentralizedClockSync.this.bin.rewind();
                    final short pclass = DecentralizedClockSync.this.bin.getShort();
                    if (PACKET_CLASS != pclass) {
                        // Random packet, skipping
                        continue;
                    }

                    final int id = DecentralizedClockSync.this.bin.getInt();
                    if (id == DecentralizedClockSync.this.myId) {
                        // My own packet, skipping
                        continue;
                    }

                    final long ts = DecentralizedClockSync.this.bin.getLong();
                    if (now >= ts) {
                        ConsoleLogger.log("Other peer is behind me, skipping");
                        continue;
                    }

                    final long ahead = ts - now;
                    DecentralizedClockSync.this.timerOffset.addAndGet(ahead);
                    ConsoleLogger.log("Other peer %d ms ahead, catching up with a new offset of %d", ahead, timerOffset.get());
                } catch (final IOException ex) {
                    this.running = false;
                }
            }
        }
    }
}
