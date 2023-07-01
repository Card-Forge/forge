package forge.gamemodes.net.server;

import forge.gamemodes.net.GameProtocolHandler;
import forge.gamemodes.net.IRemote;
import forge.gamemodes.net.ProtocolMethod;
import forge.gamemodes.net.ReplyPool;
import forge.interfaces.IGameController;
import io.netty.channel.ChannelHandlerContext;

final class GameServerHandler extends GameProtocolHandler<IGameController> {

    private final FServerManager server = FServerManager.getInstance();
    GameServerHandler() {
        super(false);
    }

    private RemoteClient getClient(final ChannelHandlerContext ctx) {
        return server.getClient(ctx.channel());
    }

    @Override
    protected ReplyPool getReplyPool(final ChannelHandlerContext ctx) {
        return getClient(ctx).getReplyPool();
    }

    @Override
    protected IRemote getRemote(final ChannelHandlerContext ctx) {
        return getClient(ctx);
    }

    @Override
    protected IGameController getToInvoke(final ChannelHandlerContext ctx) {
        return server.getController(getClient(ctx).getIndex());
    }

    @Override
    protected void beforeCall(final ProtocolMethod protocolMethod, final Object[] args) {
        // Nothing needs to be done here
    }

}