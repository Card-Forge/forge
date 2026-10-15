package forge.gamemodes.net.server;

import forge.game.GameView;
import forge.gamemodes.net.GameProtocolHandler;
import forge.util.IHasForgeLog;
import forge.gamemodes.net.IRemote;
import forge.gamemodes.net.ProtocolMethod;
import forge.gamemodes.net.ReplyPool;
import forge.gamemodes.net.event.GuiGameEvent;
import forge.gui.interfaces.IGuiGame;
import forge.interfaces.IGameController;
import forge.player.PlayerControllerHuman;
import io.netty.channel.ChannelHandlerContext;

import java.util.EnumSet;
import java.util.Set;

final class GameServerHandler extends GameProtocolHandler<IGameController> implements IHasForgeLog {

    private final FServerManager server = FServerManager.getInstance();
    private static final Set<ProtocolMethod> GAME_VIEW_BOUND_METHODS = EnumSet.of(
            ProtocolMethod.useMana,
            ProtocolMethod.undoLastAction,
            ProtocolMethod.selectPlayer,
            ProtocolMethod.selectCard,
            ProtocolMethod.selectButtonOk,
            ProtocolMethod.selectButtonCancel,
            ProtocolMethod.selectAbility,
            ProtocolMethod.selectStackItem,
            ProtocolMethod.getActivateDescription,
            ProtocolMethod.concede,
            ProtocolMethod.drawOfferAction,
            ProtocolMethod.alphaStrike,
            ProtocolMethod.reorderHand,
            ProtocolMethod.requestResync,
            ProtocolMethod.sendYieldUpdate,
            ProtocolMethod.nextGameDecision);

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
        final RemoteClient client = getClient(ctx);
        return client != null ? server.getController(client.getIndex()) : null;
    }

    @Override
    protected boolean beforeDispatch(final ChannelHandlerContext ctx, final GuiGameEvent event) {
        if (!requiresGameView(event) || event.getGameViewId() == null) return true;
        final RemoteClient client = getClient(ctx);
        final String reason = rejectionReason(ctx, client, event, null);
        if (reason == null) return true;
        logRejected(client, event, reason);
        return false;
    }

    @Override
    protected boolean beforeInvoke(final ChannelHandlerContext ctx, final GuiGameEvent event,
                                   final IGameController capturedTarget) {
        if (!super.beforeInvoke(ctx, event, capturedTarget)) return false;
        if (!requiresGameView(event) || event.getGameViewId() == null) return true;
        final RemoteClient client = getClient(ctx);
        final String reason = rejectionReason(ctx, client, event, capturedTarget);
        if (reason == null) return true;
        logRejected(client, event, reason);
        return false;
    }

    private static boolean requiresGameView(final GuiGameEvent event) {
        return GAME_VIEW_BOUND_METHODS.contains(event.getMethod());
    }

    private String rejectionReason(final ChannelHandlerContext ctx, final RemoteClient client,
                                   final GuiGameEvent event, final IGameController capturedTarget) {
        if (client == null || server.getClient(ctx.channel()) != client
                || server.getClientBySlotIndex(client.getIndex()) != client) {
            return "clientBindingChanged";
        }
        final RemoteClientGuiGame gui = client.getGui();
        final GameView currentView = gui == null ? null : gui.getGameView();
        final IGameController currentController = server.getController(client.getIndex());
        if (capturedTarget != null && currentController != capturedTarget) {
            return "controllerChanged";
        }
        final GameView controllerView = controllerGameView(currentController);
        if (controllerView == null || currentView == null
                || !event.getGameViewId().equals(controllerView.getId())
                || !event.getGameViewId().equals(currentView.getId())) {
            return "gameViewMismatch";
        }
        return null;
    }

    private static GameView controllerGameView(final IGameController controller) {
        if (!(controller instanceof PlayerControllerHuman humanController)) return null;
        return humanController.getGame() == null ? null : humanController.getGame().getView();
    }

    private void logRejected(final RemoteClient client, final GuiGameEvent event, final String reason) {
        final int slot = client == null ? -1 : client.getIndex();
        final RemoteClientGuiGame gui = client == null ? null : client.getGui();
        final GameView currentView = gui == null ? null : gui.getGameView();
        final GameView controllerView = client == null ? null
                : controllerGameView(server.getController(client.getIndex()));
        final int currentGameViewId = controllerView != null ? controllerView.getId()
                : currentView == null ? -1 : currentView.getId();
        final String phase = controllerView != null ? String.valueOf(controllerView.getPhase())
                : currentView == null ? "none" : String.valueOf(currentView.getPhase());
        netLog.warn("REJECTED_STALE_GAME_ACTION eventId={} method={} slot={} suppliedGameViewId={} currentGameViewId={} reason={} phase={}",
                event.getId(), event.getMethod(), slot, event.getGameViewId(), currentGameViewId, reason, phase);
    }

    @Override
    protected void beforeCall(final ChannelHandlerContext ctx, final ProtocolMethod protocolMethod, final Object[] args) {
        if (protocolMethod == ProtocolMethod.requestResync) {
            RemoteClient client = getClient(ctx);
            if (client != null) {
                IGuiGame gui = server.getGui(client.getIndex());
                if (gui instanceof RemoteClientGuiGame netGui) {
                    netLog.debug("[DeltaSync] Resync requested by client {}, deferring to game thread", client.getIndex());
                    netGui.setResyncPending();
                } else {
                    netLog.warn("[DeltaSync] GUI is not RemoteClientGuiGame, cannot resync client {}", client.getIndex());
                }
            }
        }
    }

}
