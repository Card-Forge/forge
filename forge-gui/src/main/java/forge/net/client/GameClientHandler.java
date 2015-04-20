package forge.net.client;

import forge.FThreads;
import forge.UiCommand;
import forge.assets.FSkinProp;
import forge.deck.CardPool;
import forge.game.GameEntityView;
import forge.game.GameView;
import forge.game.card.CardView;
import forge.game.phase.PhaseType;
import forge.game.player.DelayedReveal;
import forge.game.player.PlayerView;
import forge.game.spellability.SpellAbilityView;
import forge.game.zone.ZoneType;
import forge.interfaces.IButton;
import forge.interfaces.IGuiGame;
import forge.match.MatchButtonType;
import forge.model.FModel;
import forge.net.event.GuiGameEvent;
import forge.net.event.LoginEvent;
import forge.net.event.ReplyEvent;
import forge.player.PlayerZoneUpdates;
import forge.properties.ForgePreferences.FPref;
import forge.trackable.TrackableCollection;
import forge.util.ITriggerEvent;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.common.base.Function;

class GameClientHandler extends ChannelInboundHandlerAdapter {
    private final FGameClient client;
    private final IGuiGame gui;

    /**
     * Creates a client-side game handler.
     */
    public GameClientHandler(final FGameClient client) {
        this.client = client;
        this.gui = client.getGui();
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) {
        // Don't use send() here, as this.channel is not yet set!
        ctx.channel().writeAndFlush(new LoginEvent(FModel.getPreferences().getPref(FPref.PLAYER_NAME), Integer.parseInt(FModel.getPreferences().getPref(FPref.UI_AVATARS).split(",")[0])));
    }

    @SuppressWarnings("unchecked")
    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) {
        System.out.println("Client received: " + msg);
        if (msg instanceof ReplyEvent) {
            final ReplyEvent event = (ReplyEvent) msg;
            client.getReplyPool().complete(event.getIndex(), event.getReply());
        } else if (msg instanceof GuiGameEvent) {
            final GuiGameEvent event = (GuiGameEvent) msg;
            final String method = event.getMethod();
            final Object[] args = event.getObjects();
            Serializable reply = null;
            boolean doReply = false;

            final IButton btn;
            if (method.startsWith("btn_") && args.length >= 2 && args[0] instanceof PlayerView && args[1] instanceof MatchButtonType) {
                btn = args[1] == MatchButtonType.OK ? gui.getBtnOK((PlayerView) args[0]) : gui.getBtnCancel((PlayerView) args[0]);
            } else {
                btn = null;
            }

            switch (method) {
            case "setGameView":
                gui.setGameView((GameView) args[0]);
                break;
            case "openView":
                final TrackableCollection<PlayerView> myPlayers = (TrackableCollection<PlayerView>) args[0];
                client.setGameControllers(myPlayers);
                FThreads.invokeInEdtNowOrLater(new Runnable() {
                    @Override public final void run() {
                        gui.openView(myPlayers);
                    }
                });
                break;
            case "afterGameEnd":
                gui.afterGameEnd();
                break;
            case "showCombat":
                gui.showCombat();
                break;
            case "showPromptMessage":
                gui.showPromptMessage((PlayerView) args[0], (String) args[1]);
                break;
            case "stopAtPhase":
                reply = gui.stopAtPhase((PlayerView) args[0], (PhaseType) args[1]);
                doReply = true;
                break;
            case "focusButton":
                gui.focusButton((MatchButtonType) args[0]);
                break;
            case "flashIncorrectAction":
                gui.flashIncorrectAction();
                break;
            case "updatePhase":
                gui.updatePhase();
                break;
            case "updateTurn":
                FThreads.invokeInEdtNowOrLater(new Runnable() {
                    @Override public final void run() {
                        gui.updateTurn((PlayerView) args[0]);
                    }
                });
                break;
            case "udpdatePlayerControl":
                gui.updatePlayerControl();
                break;
            case "enableOverlay":
                gui.enableOverlay();
                break;
            case "disbleOverlay":
                gui.disableOverlay();
                break;
            case "finishGame":
                gui.finishGame();
                break;
            case "showManaPool":
                gui.showManaPool((PlayerView) args[0]);
                break;
            case "hideManaPool":
                gui.hideManaPool((PlayerView) args[0], args[1]);
                break;
            case "updateStack":
                gui.updateStack();
                break;
            case "updateZones":
                FThreads.invokeInEdtNowOrLater(new Runnable() {
                    @Override public final void run() {
                        gui.updateZones((PlayerZoneUpdates) args[0]);
                    }
                });
                break;
            case "updateSingleCard":
                gui.updateSingleCard((CardView) args[0]);
                break;
            case "updateManaPool":
                gui.updateManaPool((Iterable<PlayerView>) args[0]);
                break;
            case "updateLives":
                gui.updateLives((Iterable<PlayerView>) args[0]);
                break;
            case "setPanelSelection":
                gui.setPanelSelection((CardView) args[0]);
                break;
            case "getAbilityToPlay":
                reply = gui.getAbilityToPlay((List<SpellAbilityView>) args[0], (ITriggerEvent) args[1]);
                doReply = true;
                break;
            case "assignDamage":
                reply = (Serializable) gui.assignDamage((CardView) args[0], (List<CardView>) args[1], (int) args[2], (GameEntityView) args[3], (boolean) args[4]);
                doReply = true;
                break;
            case "message":
                gui.message((String) args[0], (String) args[1]);
                break;
            case "showErrorDialog":
                gui.showErrorDialog((String) args[0], (String) args[1]);
                break;
            case "showConfirmDialog":
                reply = gui.showConfirmDialog((String) args[0], (String) args[1], (String) args[2], (String) args[3], (boolean) args[4]);
                doReply = true;
                break;
            case "showOptionDialog":
                reply = gui.showOptionDialog((String) args[0], (String) args[1], (FSkinProp) args[2], (String[]) args[3], (int) args[4]);
                doReply = true;
                break;
            case "showCardOptionDialog":
                reply = gui.showCardOptionDialog((CardView) args[0], (String) args[1], (String) args[2], (FSkinProp) args[3], (String[]) args[4], (int) args[5]);
                doReply = true;
                break;
            case "showInputDialog":
                reply = gui.showInputDialog((String) args[0], (String) args[1], (FSkinProp) args[2], (String) args[3], (String[]) args[4]);
                doReply = true;
                break;
            case "confirm":
                reply = gui.confirm((CardView) args[0], (String) args[1], (boolean) args[2], (String[]) args[3]);
                doReply = true;
                break;
            case "getChoices":
                reply = (Serializable) gui.getChoices((String) args[0], (int) args[1], (int) args[2], (Collection<Object>) args[3], args[4], (Function<Object, String>) args[5]); 
                doReply = true;
                break;
            case "order":
                reply = (Serializable) gui.order((String) args[0], (String) args[1], (int) args[2], (int) args[3], (List<Object>) args[4], (List<Object>) args[5], (CardView) args[6], (boolean) args[7]); 
                doReply = true;
                break;
            case "sideboard":
                reply = (Serializable) gui.sideboard((CardPool) args[0], (CardPool) args[1]);
                doReply = true;
                break;
            case "chooseSingleEntityForEffect":
                reply = gui.chooseSingleEntityForEffect((String) args[0], (TrackableCollection<GameEntityView>) args[1], (DelayedReveal) args[2], (boolean) args[3]);
                doReply = true;
                break;
            case "setCard":
                gui.setCard((CardView) args[0]);
                break;
            // TODO case "setPlayerAvatar":
            case "openZones":
                reply = gui.openZones((Collection<ZoneType>) args[0], (Map<PlayerView, Object>) args[1]);
                doReply = true;
                break;
            case "restoreOldZones":
                gui.restoreOldZones((Map<PlayerView, Object>) args[0]);
                break;
            case "isUiSetToSkipPhase":
                reply = gui.isUiSetToSkipPhase((PlayerView) args[0], (PhaseType) args[1]);
                doReply = true;
                break;
            // BUTTONS
            case "btn_setEnabled":
                btn.setEnabled((boolean) args[2]);
                break;
            case "btn_setVisible":
                btn.setVisible((boolean) args[2]);
                break;
            case "btn_setText":
                btn.setText((String) args[2]);
                break;
            case "btn_isSelected":
                reply = btn.isSelected();
                doReply = true;
                break;
            case "btn_setSelected":
                btn.setSelected((boolean) args[2]);
                break;
            case "btn_requestFocusInWindows":
                reply = btn.requestFocusInWindow();
                doReply = true;
                break;
            case "btn_setCommand":
                btn.setCommand((UiCommand) args[2]);
                break;
            case "btn_setTextColor":
                if (args.length == 3) {
                    btn.setTextColor((FSkinProp) args[2]);
                } else {
                    btn.setTextColor((int) args[2], (int) args[3], (int) args[4]);
                }
                break;
            default:
                System.err.println("Unsupported game event " + event.getMethod());
                break;
            }
            if (doReply) {
                client.send(new ReplyEvent(event.getId(), reply));
            }
        }
    }

    @Override
    public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}