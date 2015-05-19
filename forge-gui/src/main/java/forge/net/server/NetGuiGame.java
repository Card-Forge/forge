package forge.net.server;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.common.base.Function;

import forge.LobbyPlayer;
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
import forge.item.PaperCard;
import forge.match.AbstractGuiGame;
import forge.net.GameProtocolSender;
import forge.net.ProtocolMethod;
import forge.player.PlayerZoneUpdate;
import forge.trackable.TrackableCollection;
import forge.util.ITriggerEvent;

public class NetGuiGame extends AbstractGuiGame {

    private final GameProtocolSender sender;
    public NetGuiGame(final IToClient client) {
        this.sender = new GameProtocolSender(client);
    }

    private void send(final ProtocolMethod method, final Object... args) {
        sender.send(method, args);
    }

    private <T> T sendAndWait(final ProtocolMethod method, final Object... args) {
        return sender.sendAndWait(method, args);
    }

    private void updateGameView() {
        send(ProtocolMethod.setGameView, getGameView());
    }

    @Override
    public void setGameView(final GameView gameView) {
        super.setGameView(gameView);
        updateGameView();
    }

    @Override
    public void openView(final TrackableCollection<PlayerView> myPlayers) {
        send(ProtocolMethod.openView, myPlayers);
        updateGameView();
    }

    @Override
    public void afterGameEnd() {
        send(ProtocolMethod.afterGameEnd);
    }

    @Override
    public void showCombat() {
        send(ProtocolMethod.showCombat);
    }

    @Override
    public void showPromptMessage(final PlayerView playerView, final String message) {
        updateGameView();
        send(ProtocolMethod.showPromptMessage, playerView, message);
    }

    @Override
    public void updateButtons(final PlayerView owner, final String label1, final String label2, final boolean enable1, final boolean enable2, final boolean focus1) {
        send(ProtocolMethod.updateButtons, owner, label1, label2, enable1, enable2, focus1);
    }

    @Override
    public void flashIncorrectAction() {
        send(ProtocolMethod.flashIncorrectAction);
    }

    @Override
    public void updatePhase() {
        updateGameView();
        send(ProtocolMethod.updatePhase);
    }

    @Override
    public void updateTurn(final PlayerView player) {
        updateGameView();
        send(ProtocolMethod.updateTurn, player);
    }

    @Override
    public void updatePlayerControl() {
        updateGameView();
        send(ProtocolMethod.updatePlayerControl);
    }

    @Override
    public void enableOverlay() {
        send(ProtocolMethod.enableOverlay);
    }

    @Override
    public void disableOverlay() {
        send(ProtocolMethod.disableOverlay);
    }

    @Override
    public void finishGame() {
        send(ProtocolMethod.finishGame);
    }

    @Override
    public void showManaPool(final PlayerView player) {
        send(ProtocolMethod.showManaPool, player);
    }

    @Override
    public void hideManaPool(final PlayerView player) {
        send(ProtocolMethod.hideManaPool, player);
    }

    @Override
    public void updateStack() {
        updateGameView();
        send(ProtocolMethod.updateStack);
    }

    @Override
    public void updateZones(final Iterable<PlayerZoneUpdate> zonesToUpdate) {
        updateGameView();
        send(ProtocolMethod.updateZones, zonesToUpdate);
    }

    @Override
    public void updateCards(final Iterable<CardView> cards) {
        updateGameView();
        send(ProtocolMethod.updateCards, cards);
    }

    @Override
    public void updateManaPool(final Iterable<PlayerView> manaPoolUpdate) {
        updateGameView();
        send(ProtocolMethod.updateManaPool, manaPoolUpdate);
    }

    @Override
    public void updateLives(final Iterable<PlayerView> livesUpdate) {
        updateGameView();
        send(ProtocolMethod.updateLives, livesUpdate);
    }

    @Override
    public void setPanelSelection(final CardView hostCard) {
        updateGameView();
        send(ProtocolMethod.setPanelSelection, hostCard);
    }

    @Override
    public SpellAbilityView getAbilityToPlay(final CardView hostCard, final List<SpellAbilityView> abilities, final ITriggerEvent triggerEvent) {
        return sendAndWait(ProtocolMethod.getAbilityToPlay, hostCard, abilities, triggerEvent);
    }

    @Override
    public Map<CardView, Integer> assignDamage(final CardView attacker, final List<CardView> blockers, final int damage, final GameEntityView defender, final boolean overrideOrder) {
        return sendAndWait(ProtocolMethod.assignDamage, attacker, blockers, damage, defender, overrideOrder);
    }

    @Override
    public void message(final String message, final String title) {
        send(ProtocolMethod.message, message, title);
    }

    @Override
    public void showErrorDialog(final String message, final String title) {
        send(ProtocolMethod.showErrorDialog, message, title);
    }

    @Override
    public boolean showConfirmDialog(final String message, final String title, final String yesButtonText, final String noButtonText, final boolean defaultYes) {
        return sendAndWait(ProtocolMethod.showConfirmDialog, message, title, yesButtonText, noButtonText, defaultYes);
    }

    @Override
    public int showOptionDialog(final String message, final String title, final FSkinProp icon, final List<String> options, final int defaultOption) {
        return sendAndWait(ProtocolMethod.showOptionDialog, message, title, icon, options, defaultOption);
    }

    @Override
    public String showInputDialog(final String message, final String title, final FSkinProp icon, final String initialInput, final List<String> inputOptions) {
        return sendAndWait(ProtocolMethod.showInputDialog, message, title, icon, initialInput, inputOptions);
    }

    @Override
    public boolean confirm(final CardView c, final String question, final boolean defaultIsYes, final List<String> options) {
        return sendAndWait(ProtocolMethod.confirm, c, question, defaultIsYes, options);
    }

    @Override
    public <T> List<T> getChoices(final String message, final int min, final int max, final List<T> choices, final T selected, final Function<T, String> display) {
        return sendAndWait(ProtocolMethod.getChoices, message, min, max, choices, selected, display);
    }

    @Override
    public <T> List<T> order(final String title, final String top, final int remainingObjectsMin, final int remainingObjectsMax, final List<T> sourceChoices, final List<T> destChoices, final CardView referenceCard, final boolean sideboardingMode) {
        return sendAndWait(ProtocolMethod.order, title, top, remainingObjectsMin, remainingObjectsMax, sourceChoices, destChoices, referenceCard, sideboardingMode);
    }

    @Override
    public List<PaperCard> sideboard(final CardPool sideboard, final CardPool main) {
        return sendAndWait(ProtocolMethod.sideboard, sideboard, main);
    }

    @Override
    public GameEntityView chooseSingleEntityForEffect(final String title, final List<? extends GameEntityView> optionList, final DelayedReveal delayedReveal, final boolean isOptional) {
        return sendAndWait(ProtocolMethod.chooseSingleEntityForEffect, title, optionList, delayedReveal, isOptional);
    }

    @Override
    public void setCard(final CardView card) {
        updateGameView();
        send(ProtocolMethod.setCard, card);
    }

    @Override
    public void setPlayerAvatar(final LobbyPlayer player, final IHasIcon ihi) {
        // TODO Auto-generated method stub
    }

    @Override
    public boolean openZones(final Collection<ZoneType> zones, final Map<PlayerView, Object> players) {
        updateGameView();
        return sendAndWait(ProtocolMethod.openZones, zones, players);
    }

    @Override
    public void restoreOldZones(final Map<PlayerView, Object> playersToRestoreZonesFor) {
        send(ProtocolMethod.restoreOldZones, playersToRestoreZonesFor);
    }

    @Override
    public boolean isUiSetToSkipPhase(final PlayerView playerTurn, final PhaseType phase) {
        return sendAndWait(ProtocolMethod.isUiSetToSkipPhase, playerTurn, phase);
    }

    @Override
    protected void updateCurrentPlayer(final PlayerView player) {
        // TODO Auto-generated method stub
    }

}
