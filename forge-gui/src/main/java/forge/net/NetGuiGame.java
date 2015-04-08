package forge.net;

import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import com.google.common.base.Function;
import com.google.common.collect.Maps;

import forge.LobbyPlayer;
import forge.UiCommand;
import forge.assets.FSkinProp;
import forge.deck.CardPool;
import forge.game.GameEntityView;
import forge.game.GameView;
import forge.game.card.CardView;
import forge.game.phase.PhaseType;
import forge.game.player.DelayedReveal;
import forge.game.player.IHasIcon;
import forge.game.player.PlayerView;
import forge.game.spellability.SpellAbilityView;
import forge.game.zone.ZoneType;
import forge.interfaces.IButton;
import forge.item.PaperCard;
import forge.match.AbstractGuiGame;
import forge.match.MatchButtonType;
import forge.net.game.GuiGameEvent;
import forge.net.game.server.IToClient;
import forge.player.PlayerZoneUpdate;
import forge.trackable.TrackableCollection;
import forge.util.ITriggerEvent;

public class NetGuiGame extends AbstractGuiGame {

    private final IToClient client;
    private final Map<MatchButtonType, Map<PlayerView, NetButton>> btns = new EnumMap<MatchButtonType, Map<PlayerView,NetButton>>(MatchButtonType.class);
    public NetGuiGame(final IToClient client) {
        this.client = client;
        for (final MatchButtonType type : MatchButtonType.values()) {
            btns.put(type, Maps.<PlayerView, NetButton>newHashMap());
        }
    }

    private String methodName() {
        boolean passedFirst = false;
        for (final StackTraceElement ste : Thread.currentThread().getStackTrace()) {
            if (ste.getClassName() == getClass().getName()) {
                if (passedFirst) {
                    return ste.getMethodName();
                }
                passedFirst = true;
            }
        }
        return null;
    }

    private void send(final String method, final Object... args) {
        client.send(new GuiGameEvent(method, args));
    }
    @SuppressWarnings("unchecked")
    private <T> T sendAndWait(final String method, final Object... args) {
        try {
            return (T) client.sendAndWait(new GuiGameEvent(method, args));
        } catch (final TimeoutException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void updateGameView() {
        send("setGameView", getGameView());
    }

    @Override
    public void setGameView(final GameView gameView) {
        super.setGameView(gameView);
        updateGameView();
    }

    @Override
    public void openView(final TrackableCollection<PlayerView> myPlayers) {
        for (final MatchButtonType type : MatchButtonType.values()) {
            btns.get(type).clear();
            for (final PlayerView player : myPlayers) {
                btns.get(type).put(player, new NetButton(player, type));
            }
        }
        send(methodName(), myPlayers);
        updateGameView();
    }

    @Override
    public void afterGameEnd() {
        send(methodName());
    }

    @Override
    public void showCombat() {
        send(methodName());
    }

    @Override
    public void showPromptMessage(final PlayerView playerView, final String message) {
        updateGameView();
        send(methodName(), playerView, message);
    }

    @Override
    public boolean stopAtPhase(final PlayerView playerTurn, final PhaseType phase) {
        return sendAndWait(methodName(), playerTurn, phase);
    }

    @Override
    public IButton getBtnOK(final PlayerView playerView) {
        return btns.get(MatchButtonType.OK).get(playerView);
    }

    @Override
    public IButton getBtnCancel(final PlayerView playerView) {
        return btns.get(MatchButtonType.CANCEL).get(playerView);
    }

    @Override
    public void focusButton(final MatchButtonType button) {
        send(methodName(), button);
    }

    @Override
    public void flashIncorrectAction() {
        send(methodName());
    }

    @Override
    public void updatePhase() {
        updateGameView();
        send(methodName());
    }

    @Override
    public void updateTurn(final PlayerView player) {
        updateGameView();
        send(methodName(), player);
    }

    @Override
    public void updatePlayerControl() {
        updateGameView();
        send(methodName());
    }

    @Override
    public void enableOverlay() {
        send(methodName());
    }

    @Override
    public void disableOverlay() {
        send(methodName());
    }

    @Override
    public void finishGame() {
        send(methodName());
    }

    @Override
    public Object showManaPool(final PlayerView player) {
        send(methodName(), player);
        return null;
    }

    @Override
    public void hideManaPool(final PlayerView player, final Object zoneToRestore) {
        send(methodName(), player, zoneToRestore);
    }

    @Override
    public void updateStack() {
        updateGameView();
        send(methodName());
    }

    @Override
    public void updateZones(final Iterable<PlayerZoneUpdate> zonesToUpdate) {
        updateGameView();
        send(methodName(), zonesToUpdate);
    }

    @Override
    public void updateSingleCard(final CardView card) {
        updateGameView();
        send(methodName(), card);
    }

    @Override
    public void updateManaPool(final Iterable<PlayerView> manaPoolUpdate) {
        updateGameView();
        send(methodName(), manaPoolUpdate);
    }

    @Override
    public void updateLives(final Iterable<PlayerView> livesUpdate) {
        updateGameView();
        send(methodName(), livesUpdate);
    }

    @Override
    public void setPanelSelection(final CardView hostCard) {
        updateGameView();
        send(methodName(), hostCard);
    }

    @Override
    public SpellAbilityView getAbilityToPlay(final List<SpellAbilityView> abilities, final ITriggerEvent triggerEvent) {
        return sendAndWait(methodName(), abilities, triggerEvent);
    }

    @Override
    public Map<CardView, Integer> assignDamage(final CardView attacker, final List<CardView> blockers, final int damage, final GameEntityView defender, final boolean overrideOrder) {
        return sendAndWait(methodName(), attacker, blockers, damage, defender, overrideOrder);
    }

    @Override
    public void message(final String message, final String title) {
        send(methodName(), message, title);
    }

    @Override
    public void showErrorDialog(final String message, final String title) {
        send(methodName(), message, title);
    }

    @Override
    public boolean showConfirmDialog(final String message, final String title, final String yesButtonText, final String noButtonText, final boolean defaultYes) {
        return sendAndWait(methodName(), message, title, yesButtonText, noButtonText, defaultYes);
    }

    @Override
    public int showOptionDialog(final String message, final String title, final FSkinProp icon, final String[] options, final int defaultOption) {
        return sendAndWait(methodName(), message, title, icon, options, defaultOption);
    }

    @Override
    public int showCardOptionDialog(final CardView card, final String message, final  String title, final FSkinProp icon, final String[] options, final int defaultOption) {
        return sendAndWait(methodName(), card, message, title, icon, options, defaultOption);
    }

    @Override
    public String showInputDialog(final String message, final String title, final FSkinProp icon, final String initialInput, final String[] inputOptions) {
        return sendAndWait(methodName(), message, title, icon, initialInput, inputOptions);
    }

    @Override
    public boolean confirm(final CardView c, final String question, final boolean defaultIsYes, final String[] options) {
        return sendAndWait(methodName(), c, question, defaultIsYes, options);
    }

    @Override
    public <T> List<T> getChoices(final String message, final int min, final int max, final Collection<T> choices, final T selected, final Function<T, String> display) {
        return sendAndWait(methodName(), message, min, max, choices, selected, display);
    }

    @Override
    public <T> List<T> order(final String title, final String top, final int remainingObjectsMin, final int remainingObjectsMax, final List<T> sourceChoices, final List<T> destChoices, final CardView referenceCard, final boolean sideboardingMode) {
        return sendAndWait(methodName(), title, top, remainingObjectsMin, remainingObjectsMax, sourceChoices, destChoices, referenceCard, sideboardingMode);
    }

    @Override
    public List<PaperCard> sideboard(final CardPool sideboard, final CardPool main) {
        return sendAndWait(methodName(), sideboard, main);
    }

    @Override
    public GameEntityView chooseSingleEntityForEffect(final String title, final Collection<? extends GameEntityView> optionList, final DelayedReveal delayedReveal, final boolean isOptional) {
        return sendAndWait(methodName(), title, optionList, delayedReveal, isOptional);
    }

    @Override
    public void setCard(final CardView card) {
        updateGameView();
        send(methodName(), card);
    }

    @Override
    public void setPlayerAvatar(LobbyPlayer player, IHasIcon ihi) {
        // TODO Auto-generated method stub
    }

    @Override
    public boolean openZones(final Collection<ZoneType> zones, final Map<PlayerView, Object> players) {
        updateGameView();
        return sendAndWait(methodName(), zones, players);
    }

    @Override
    public void restoreOldZones(final Map<PlayerView, Object> playersToRestoreZonesFor) {
        send(methodName(), playersToRestoreZonesFor);
    }

    @Override
    public boolean isUiSetToSkipPhase(final PlayerView playerTurn, final PhaseType phase) {
        return sendAndWait(methodName(), playerTurn, phase);
    }

    @Override
    protected void updateCurrentPlayer(final PlayerView player) {
        // TODO Auto-generated method stub
    }

    private final class NetButton implements IButton {

        private String methodName() {
            boolean passedFirst = false;
            for (final StackTraceElement ste : Thread.currentThread().getStackTrace()) {
                if (ste.getClassName() == getClass().getName()) {
                    if (passedFirst) {
                        return ste.getMethodName();
                    }
                    passedFirst = true;
                }
            }
            return null;
        }

        private final PlayerView playerView;
        private final MatchButtonType type;
        private NetButton(final PlayerView playerView, final MatchButtonType type) {
            this.playerView = playerView;
            this.type = type;
        }

        @Override
        public void setEnabled(final boolean b0) {
            send("btn_" + methodName(), playerView, type, b0);
        }

        @Override
        public void setVisible(final boolean b0) {
            send("btn_" + methodName(), playerView, type, b0);
        }

        @Override
        public void setText(final String text0) {
            send("btn_" + methodName(), playerView, type, text0);
        }

        @Override
        public boolean isSelected() {
            return sendAndWait("btn_" + methodName(), playerView, type);
        }

        @Override
        public void setSelected(final boolean b0) {
            send("btn_" + methodName(), playerView, type, b0);
        }

        @Override
        public boolean requestFocusInWindow() {
            return sendAndWait("btn_" + methodName(), playerView, type);
        }

        @Override
        public void setCommand(final UiCommand command0) {
            send("btn_" + methodName(), playerView, type, command0);
        }

        @Override
        public void setTextColor(final FSkinProp color) {
            send("btn_" + methodName(), playerView, type, color);
        }

        @Override
        public void setTextColor(final int r, final int g, final int b) {
            send("btn_" + methodName(), playerView, type, r, g, b);
        }
    }
}
