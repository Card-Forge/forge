package forge.gamemodes.net.server;

import forge.gamemodes.net.CompatibleObjectDecoder;
import forge.gamemodes.net.CompatibleObjectEncoder;
import forge.gamemodes.net.IHasNetLog;
import forge.gamemodes.net.ReplyPool;
import forge.trackable.Tracker;
import forge.gamemodes.net.event.IdentifiableNetEvent;
import forge.gamemodes.net.event.NetEvent;
import io.netty.channel.Channel;

import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

public final class RemoteClient implements IToClient, IHasNetLog {

    /** Special value indicating the client hasn't been assigned a slot yet. */
    public static final int UNASSIGNED_SLOT = -1;

    private volatile Channel channel;
    private String username;
    private int index = UNASSIGNED_SLOT;
    private volatile ReplyPool replies = new ReplyPool();
    private final AtomicInteger sendErrors = new AtomicInteger(0);

    public RemoteClient(final Channel channel) {
        this.channel = channel;
    }

    /**
     * Swap the underlying channel for a reconnecting client.
     * Updates the channel and creates a fresh ReplyPool.
     */
    public void swapChannel(final Channel newChannel) {
        this.channel = newChannel;
        this.replies = new ReplyPool();
    }

    /**
     * Check if this client has been assigned a valid lobby slot.
     * @return true if the client has a valid slot (index >= 0)
     */
    public boolean hasValidSlot() {
        return index >= 0;
    }

    @Override
    public void send(final NetEvent event) {
        try {
            long startMs = System.currentTimeMillis();
            channel.writeAndFlush(event).sync();
            long elapsed = System.currentTimeMillis() - startMs;
            if (elapsed > 50) {
                netLog.info("send() blocked {} ms for {} (event: {})", elapsed, username, event);
            }
        } catch (Exception e) {
            sendErrors.incrementAndGet();
            netLog.error("Network send error for {} (event: {})", username, event, e);
        }
    }

    @Override
    public void write(final NetEvent event) {
        channel.write(event);
    }

    @Override
    public Object sendAndWait(final IdentifiableNetEvent event) throws TimeoutException {
        replies.initialize(event.getId());
        send(event);
        return replies.get(event.getId());
    }

    public String getUsername() {
        return username;
    }
    public void setUsername(final String username) {
        this.username = username;
    }

    public int getIndex() {
        return index;
    }
    public void setIndex(final int index) {
        this.index = index;
    }

    /**
     * Set the tracker on the channel's encoder and decoder for IdRef
     * replacement/resolution. Called when the game starts (before any
     * client protocol messages arrive).
     */
    public void setCodecTracker(Tracker tracker) {
        CompatibleObjectEncoder encoder = channel.pipeline().get(CompatibleObjectEncoder.class);
        if (encoder != null) {
            encoder.setTracker(tracker);
        }
        CompatibleObjectDecoder decoder = channel.pipeline().get(CompatibleObjectDecoder.class);
        if (decoder != null) {
            decoder.setTracker(tracker);
        }
    }

    public int getSendErrorCount() {
        return sendErrors.get();
    }

    ReplyPool getReplyPool() {
        return replies;
    }
}
