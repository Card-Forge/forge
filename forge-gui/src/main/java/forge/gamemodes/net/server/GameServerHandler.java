package forge.gamemodes.net.server;

import forge.gamemodes.match.AbstractGuiGame;
import forge.gamemodes.net.GameProtocolHandler;
import forge.gamemodes.net.IRemote;
import forge.gamemodes.net.ProtocolMethod;
import forge.gamemodes.net.ReplyPool;
import forge.gui.interfaces.IGuiGame;
import forge.interfaces.IGameController;
import io.netty.channel.ChannelHandlerContext;

final class GameServerHandler extends GameProtocolHandler<IGameController> {

    private final FServerManager server = FServerManager.getInstance();
    private ChannelHandlerContext currentContext = null;

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
        // Handle delta sync protocol methods
        if (protocolMethod == ProtocolMethod.ackSync && args.length > 0) {
            // Get client from current context
            if (currentContext != null) {
                RemoteClient client = getClient(currentContext);
                if (client != null) {
                    long sequenceNumber = (Long) args[0];
                    IGuiGame gui = server.getGui(client.getIndex());
                    if (gui instanceof NetGuiGame) {
                        ((NetGuiGame) gui).processAcknowledgment(sequenceNumber, client.getIndex());
                    }
                }
            }
        } else if (protocolMethod == ProtocolMethod.requestResync) {
            // Handle resync request
            if (currentContext != null) {
                RemoteClient client = getClient(currentContext);
                if (client != null) {
                    System.out.println("[DeltaSync] Resync requested by client " + client.getIndex());
                    IGuiGame gui = server.getGui(client.getIndex());
                    if (gui instanceof NetGuiGame) {
                        NetGuiGame netGui = (NetGuiGame) gui;
                        System.out.println("[DeltaSync] Sending full state to client " + client.getIndex());
                        netGui.sendFullState();
                    } else {
                        System.err.println("[DeltaSync] WARNING: GUI is not NetGuiGame, cannot resync");
                    }
                }
            }
        }
    }

    @Override
    public final void channelRead(final ChannelHandlerContext ctx, final Object msg) {
        // Store context so beforeCall can access it
        currentContext = ctx;
        try {
            super.channelRead(ctx, msg);
        } finally {
            currentContext = null;
        }
    }

}