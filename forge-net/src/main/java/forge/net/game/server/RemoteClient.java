package forge.net.game.server;

import forge.net.game.NetEvent;
import io.netty.channel.Channel;

public final class RemoteClient implements IToClient {

    private final Channel channel;
    private String username;
    private int index;
    public RemoteClient(final Channel channel) {
        this.channel = channel;
    }

    @Override
    public void send(final NetEvent event) {
        channel.writeAndFlush(event);
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
}
