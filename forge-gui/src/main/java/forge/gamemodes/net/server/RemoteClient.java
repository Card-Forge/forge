package forge.gamemodes.net.server;

import forge.gamemodes.net.ReplyPool;
import forge.gamemodes.net.event.IdentifiableNetEvent;
import forge.gamemodes.net.event.NetEvent;
import io.netty.channel.Channel;

import java.util.concurrent.TimeoutException;

public final class RemoteClient implements IToClient {

    /** Special value indicating the client hasn't been assigned a slot yet. */
    public static final int UNASSIGNED_SLOT = -1;

    private volatile Channel channel;
    private String username;
    private int index = UNASSIGNED_SLOT;  // Initialize to -1 to indicate not yet assigned
    private volatile ReplyPool replies = new ReplyPool();

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
        System.out.println("Sending event " + event + " to " + channel);
        try {
            channel.writeAndFlush(event).sync();
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    ReplyPool getReplyPool() {
        return replies;
    }

    /**
     * Cancel all pending replies for this client.
     * This is used when converting a player to AI control to unblock the game thread.
     */
    public void cancelPendingReplies() {
        replies.cancelAll();
    }
}
