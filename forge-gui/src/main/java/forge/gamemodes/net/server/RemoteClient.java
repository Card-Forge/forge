package forge.gamemodes.net.server;

import forge.gamemodes.net.CompatibleObjectDecoder;
import forge.gamemodes.net.CompatibleObjectEncoder;
import forge.gamemodes.net.ReplyPool;
import forge.trackable.Tracker;
import forge.gamemodes.net.event.IdentifiableNetEvent;
import forge.gamemodes.net.event.NetEvent;
import forge.util.IHasForgeLog;

import io.netty.channel.Channel;

import java.util.concurrent.atomic.AtomicInteger;

public final class RemoteClient implements IToClient, IHasForgeLog {

    /** Special value indicating the client hasn't been assigned a slot yet. */
    public static final int UNASSIGNED_SLOT = -1;

    private volatile Channel channel;
    private String username;
    private int index = UNASSIGNED_SLOT;
    private volatile ReplyPool replies = new ReplyPool();
    private volatile Tracker codecTracker;
    private final AtomicInteger sendErrors = new AtomicInteger(0);

    // Package-private: SaturationLoggingHandler reads/resets these on writability transitions
    volatile long saturationStartMs = 0L;
    final AtomicInteger sendsDuringSaturation = new AtomicInteger(0);

    private void recordSendIfSaturated(final Channel ch) {
        if (!ch.isWritable()) {
            sendsDuringSaturation.incrementAndGet();
        }
    }

    public RemoteClient(final Channel channel) {
        this.channel = channel;
    }

    /**
     * Swap the underlying channel for a reconnecting client.
     * Updates the channel, creates a fresh ReplyPool, and re-applies the codec
     * tracker to the new channel's pipeline so IdRef resolution keeps working.
     */
    public void swapChannel(final Channel newChannel) {
        this.channel = newChannel;
        this.replies = new ReplyPool();
        applyCodecTracker(newChannel);
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
        final Channel ch = channel;
        recordSendIfSaturated(ch);
        ch.writeAndFlush(event).addListener(f -> {
            if (!f.isSuccess()) {
                sendErrors.incrementAndGet();
                Throwable c = f.cause();
                if (c != null) {
                    netLog.error(c, "Network send error for {} (event: {})", username, event);
                } else {
                    netLog.error("Network send error for {} (event: {}, cause: {})",
                            username, event, f.isCancelled() ? "cancelled" : "no cause");
                }
            }
        });
    }

    @Override
    public void write(final NetEvent event) {
        final Channel ch = channel;
        recordSendIfSaturated(ch);
        ch.write(event);
    }

    @Override
    public Object sendAndWait(final IdentifiableNetEvent event) {
        replies.initialize(event.getId());
        final Channel ch = channel;
        recordSendIfSaturated(ch);
        ch.writeAndFlush(event).addListener(f -> {
            if (!f.isSuccess()) {
                sendErrors.incrementAndGet();
                Throwable c = f.cause();
                if (c != null) {
                    netLog.error(c, "sendAndWait write failed for {} (event: {})", username, event);
                } else {
                    netLog.error("sendAndWait write failed for {} (event: {}, cause: {})",
                            username, event, f.isCancelled() ? "cancelled" : "no cause");
                }
                replies.complete(event.getId(), null);
            }
        });
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
     * client protocol messages arrive). Cached so that {@link #swapChannel}
     * can re-apply it after a reconnect.
     */
    public void setCodecTracker(Tracker tracker) {
        this.codecTracker = tracker;
        applyCodecTracker(channel);
    }

    private void applyCodecTracker(Channel ch) {
        if (codecTracker == null || ch == null) {
            return;
        }
        CompatibleObjectEncoder encoder = ch.pipeline().get(CompatibleObjectEncoder.class);
        if (encoder != null) {
            encoder.setTracker(codecTracker);
        }
        CompatibleObjectDecoder decoder = ch.pipeline().get(CompatibleObjectDecoder.class);
        if (decoder != null) {
            decoder.setTracker(codecTracker);
        }
    }

    public int getSendErrorCount() {
        return sendErrors.get();
    }

    ReplyPool getReplyPool() {
        return replies;
    }
}
