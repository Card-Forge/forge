package forge.gamemodes.net.server;

import forge.LobbyPlayer;
import forge.ai.GameState;
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
import forge.gamemodes.match.AbstractGuiGame;
import forge.gamemodes.net.GameProtocolSender;
import forge.gamemodes.net.ProtocolMethod;
import forge.item.PaperCard;
import forge.localinstance.skin.FSkinProp;
import forge.player.PlayerZoneUpdate;
import forge.player.PlayerZoneUpdates;
import forge.trackable.TrackableCollection;
import forge.util.FSerializableFunction;
import forge.util.ITriggerEvent;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class NetGuiGame extends AbstractGuiGame {

    private final GameProtocolSender sender;
    private final int slotIndex;
    private volatile boolean paused;

    public NetGuiGame(final IToClient client, final int slotIndex) {
        this.sender = new GameProtocolSender(client);
        this.slotIndex = slotIndex;
    }

    public int getSlotIndex() {
        return slotIndex;
    }

    public void pause() {
        paused = true;
    }

    public void resume() {
        paused = false;
    }

    public boolean isPaused() {
        return paused;
    }

    private void send(final ProtocolMethod method, final Object... args) {
        if (paused) { return; }
        sender.send(method, args);
    }

    private <T> T sendAndWait(final ProtocolMethod method, final Object... args) {
        if (paused) { return null; }
        return sender.sendAndWait(method, args);
    }

    public void updateGameView() {
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
    public void showCardPromptMessage(final PlayerView playerView, final String message, final CardView card) {
        updateGameView();
        send(ProtocolMethod.showCardPromptMessage, playerView, message, card);
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
    public void alertUser() { send(ProtocolMethod.alertUser); }

    @Override
    public void updatePhase(boolean saveState) {
        updateGameView();
        send(ProtocolMethod.updatePhase, saveState);
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
    public Iterable<PlayerZoneUpdate> tempShowZones(final PlayerView controller, final Iterable<PlayerZoneUpdate> zonesToUpdate) {
        updateGameView();
        return sendAndWait(ProtocolMethod.tempShowZones, controller, zonesToUpdate);
    }

    @Override
    public void hideZones(final PlayerView controller, final Iterable<PlayerZoneUpdate> zonesToUpdate) {
        updateGameView();
        send(ProtocolMethod.hideZones, controller, zonesToUpdate);
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
    public void updateShards(Iterable<PlayerView> shardsUpdate) {
        //mobile adventure local game only..
    }

    @Override
    public void setPanelSelection(final CardView hostCard) {
        updateGameView();
        send(ProtocolMethod.setPanelSelection, hostCard);
    }

    @Override
    public void refreshField() {
        updateGameView();
        send(ProtocolMethod.refreshField);
    }

    @Override
    public GameState getGamestate() {
        return null;
    }

    @Override
    public SpellAbilityView getAbilityToPlay(final CardView hostCard, final List<SpellAbilityView> abilities, final ITriggerEvent triggerEvent) {
        return sendAndWait(ProtocolMethod.getAbilityToPlay, hostCard, abilities, null/*triggerEvent*/); //someplatform don't have mousetriggerevent class or it will not allow them to click/tap
    }

    @Override
    public Map<CardView, Integer> assignCombatDamage(final CardView attacker, final List<CardView> blockers, final int damage, final GameEntityView defender, final boolean overrideOrder, final boolean maySkip) {
        return sendAndWait(ProtocolMethod.assignCombatDamage, attacker, blockers, damage, defender, overrideOrder, maySkip);
    }

    @Override
    public Map<Object, Integer> assignGenericAmount(final CardView effectSource, final Map<Object, Integer> targets, final int amount, final boolean atLeastOne, final String amountLabel) {
        return sendAndWait(ProtocolMethod.assignGenericAmount, effectSource, targets, amount, atLeastOne, amountLabel);
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
        final Boolean result = sendAndWait(ProtocolMethod.showConfirmDialog, message, title, yesButtonText, noButtonText, defaultYes);
        return result != null ? result : defaultYes;
    }

    @Override
    public int showOptionDialog(final String message, final String title, final FSkinProp icon, final List<String> options, final int defaultOption) {
        final Integer result = sendAndWait(ProtocolMethod.showOptionDialog, message, title, icon, options, defaultOption);
        return result != null ? result : defaultOption;
    }

    @Override
    public String showInputDialog(final String message, final String title, final FSkinProp icon, final String initialInput, final List<String> inputOptions, final boolean isNumeric) {
        return sendAndWait(ProtocolMethod.showInputDialog, message, title, icon, initialInput, inputOptions, isNumeric);
    }

    @Override
    public boolean confirm(final CardView c, final String question, final boolean defaultIsYes, final List<String> options) {
        final Boolean result = sendAndWait(ProtocolMethod.confirm, c, question, defaultIsYes, options);
        return result != null ? result : defaultIsYes;
    }

    @Override
    public <T> List<T> getChoices(final String message, final int min, final int max, final List<T> choices, final List<T> selected, final FSerializableFunction<T, String> display) {
        return sendAndWait(ProtocolMethod.getChoices, message, min, max, choices, selected, display);
    }

    @Override
    public <T> List<T> order(final String title, final String top, final int remainingObjectsMin, final int remainingObjectsMax, final List<T> sourceChoices, final List<T> destChoices, final CardView referenceCard, final boolean sideboardingMode) {
        return sendAndWait(ProtocolMethod.order, title, top, remainingObjectsMin, remainingObjectsMax, sourceChoices, destChoices, referenceCard, sideboardingMode);
    }

    @Override
    public List<PaperCard> sideboard(final CardPool sideboard, final CardPool main, final String message) {
        return sendAndWait(ProtocolMethod.sideboard, sideboard, main, message);
    }

    @Override
    public GameEntityView chooseSingleEntityForEffect(final String title, final List<? extends GameEntityView> optionList, final DelayedReveal delayedReveal, final boolean isOptional) {
        return sendAndWait(ProtocolMethod.chooseSingleEntityForEffect, title, optionList, delayedReveal, isOptional);
    }

    @Override
    public List<GameEntityView> chooseEntitiesForEffect(final String title, final List<? extends GameEntityView> optionList, final int min, final int max, final DelayedReveal delayedReveal) {
        return sendAndWait(ProtocolMethod.chooseEntitiesForEffect, title, optionList, min, max, delayedReveal);
    }

    @Override
    public List<CardView> manipulateCardList(final String title, final Iterable<CardView> cards, final Iterable<CardView> manipulable, final boolean toTop, final boolean toBottom, final boolean toAnywhere) {
        return sendAndWait(ProtocolMethod.manipulateCardList, title, cards, manipulable, toTop, toBottom, toAnywhere);
    }

    @Override
    public void setCard(final CardView card) {
        updateGameView();
        send(ProtocolMethod.setCard, card);
    }

    @Override
    public void setSelectables(final Iterable<CardView> cards) {
        updateGameView();
        send(ProtocolMethod.setSelectables, cards);
    }

    @Override
    public void clearSelectables() {
        updateGameView();
        send(ProtocolMethod.clearSelectables);
    }

    @Override
    public void setPlayerAvatar(final LobbyPlayer player, final IHasIcon ihi) {
        // TODO Auto-generated method stub
    }

    @Override
    public PlayerZoneUpdates openZones(PlayerView controller, final Collection<ZoneType> zones, final Map<PlayerView, Object> players, boolean backupLastZones) {
        updateGameView();
        return sendAndWait(ProtocolMethod.openZones, controller, zones, players, backupLastZones);
    }

    @Override
    public void restoreOldZones(PlayerView playerView, PlayerZoneUpdates playerZoneUpdates) {
        send(ProtocolMethod.restoreOldZones, playerView, playerZoneUpdates);
    }

    @Override
    public boolean isUiSetToSkipPhase(final PlayerView playerTurn, final PhaseType phase) {
        final Boolean result = sendAndWait(ProtocolMethod.isUiSetToSkipPhase, playerTurn, phase);
        return Boolean.TRUE.equals(result);
    }

    @Override
    public void syncYieldMode(final PlayerView player, final forge.gamemodes.match.YieldMode mode) {
        // Send yield state to client (when server clears yield due to end condition)
        send(ProtocolMethod.syncYieldMode, player, mode);
    }

    @Override
    public void showWaitingTimer(final PlayerView forPlayer, final String waitingForPlayerName) {
        send(ProtocolMethod.showWaitingTimer, forPlayer, waitingForPlayerName);
    }

    @Override
    public boolean isNetGame() { return true; }

    @Override
    protected void updateCurrentPlayer(final PlayerView player) {
        // TODO Auto-generated method stub
    }

}
