package forge.gamemodes.net.server;

import java.util.concurrent.TimeoutException;

import forge.gamemodes.net.ReplyPool;
import forge.gamemodes.net.event.IdentifiableNetEvent;
import forge.gamemodes.net.event.NetEvent;
import io.netty.channel.Channel;

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
}
