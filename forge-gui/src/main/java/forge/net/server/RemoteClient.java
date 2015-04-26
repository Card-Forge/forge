package forge.net.server;

import forge.net.ReplyPool;
import forge.net.event.IdentifiableNetEvent;
import forge.net.event.NetEvent;
import io.netty.channel.Channel;

import java.util.concurrent.TimeoutException;

public final class RemoteClient implements IToClient {

    private final Channel channel;
    private String username;
    private int index;
    private ReplyPool replies = new ReplyPool();
    public RemoteClient(final Channel channel) {
        this.channel = channel;
    }

    @Override
    public void send(final NetEvent event) {
        System.out.println("Sending event " + event + " to " + channel);
        channel.writeAndFlush(event);
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
}
