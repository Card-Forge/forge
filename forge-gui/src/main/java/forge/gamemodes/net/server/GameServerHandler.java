package forge.gamemodes.net.server;

import forge.gamemodes.net.GameProtocolHandler;
import forge.gamemodes.net.IRemote;
import forge.gamemodes.net.NetworkDebugLogger;
import forge.gamemodes.net.ProtocolMethod;
import forge.gamemodes.net.ReplyPool;
import forge.gui.interfaces.IGuiGame;
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
    protected void beforeCall(final ChannelHandlerContext ctx, final ProtocolMethod protocolMethod, final Object[] args) {
        // Handle delta sync protocol methods
        if (protocolMethod == ProtocolMethod.ackSync && args.length > 0) {
            RemoteClient client = getClient(ctx);
            if (client != null) {
                long sequenceNumber = (Long) args[0];
                IGuiGame gui = server.getGui(client.getIndex());
                if (gui instanceof NetGuiGame) {
                    ((NetGuiGame) gui).processAcknowledgment(sequenceNumber, client.getIndex());
                }
            }
        } else if (protocolMethod == ProtocolMethod.requestResync) {
            // Handle resync request
            RemoteClient client = getClient(ctx);
            if (client != null) {
                NetworkDebugLogger.debug("[DeltaSync] Resync requested by client %d", client.getIndex());
                IGuiGame gui = server.getGui(client.getIndex());
                if (gui instanceof NetGuiGame) {
                    NetGuiGame netGui = (NetGuiGame) gui;
                    NetworkDebugLogger.debug("[DeltaSync] Sending full state to client %d", client.getIndex());
                    netGui.sendFullState();
                } else {
                    NetworkDebugLogger.warn("[DeltaSync] GUI is not NetGuiGame, cannot resync");
                }
            }
        }
    }

}