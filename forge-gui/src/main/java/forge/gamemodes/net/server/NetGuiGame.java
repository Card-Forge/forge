package forge.gamemodes.net.server;

import forge.LobbyPlayer;
import forge.ai.GameState;
import forge.deck.CardPool;
import forge.game.Game;
import forge.game.GameEntityView;
import forge.game.GameView;
import forge.game.card.CardView;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.DelayedReveal;
import forge.game.player.IHasIcon;
import forge.game.player.Player;
import forge.game.player.PlayerView;
import forge.game.spellability.SpellAbilityView;
import forge.game.zone.ZoneType;
import forge.util.Localizer;
import forge.gamemodes.net.NetworkGuiGame;
import forge.gamemodes.net.DeltaPacket;
import forge.gamemodes.net.FullStatePacket;
import forge.gamemodes.net.GameProtocolSender;
import forge.gamemodes.net.NetworkDebugLogger;
import forge.gamemodes.net.ProtocolMethod;
import forge.item.PaperCard;
import forge.localinstance.skin.FSkinProp;
import forge.model.FModel;
import forge.player.PlayerZoneUpdate;
import forge.player.PlayerZoneUpdates;
import forge.trackable.TrackableCollection;
import forge.util.FSerializableFunction;
import forge.util.ITriggerEvent;
import net.jpountz.lz4.LZ4BlockOutputStream;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class NetGuiGame extends NetworkGuiGame {

    private final GameProtocolSender sender;
    private final DeltaSyncManager deltaSyncManager;
    private final int clientIndex;
    // Delta sync is ENABLED - new objects are now properly handled.
    // New objects are sent with full property data, existing objects only send changed properties.
    private boolean useDeltaSync = true;
    private boolean initialSyncSent = false;
    private boolean fallbackLogged = false;  // Prevent duplicate fallback log messages
    private volatile boolean paused;

    public NetGuiGame(final RemoteClient client) {
        this.sender = new GameProtocolSender(client);
        this.deltaSyncManager = new DeltaSyncManager();
        this.clientIndex = client.getIndex();
    }

    @Override
    protected boolean isServerSide() {
        return true; // NetGuiGame is the server-side GUI
    }

    /** Alias for reconnection code that references slot index. */
    public int getSlotIndex() {
        return clientIndex;
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

    /**
     * Reset delta sync state for reconnection.
     * After a client reconnects, it has no prior delta baseline,
     * so we must send a full state before resuming delta sync.
     */
    public void resetForReconnect() {
        initialSyncSent = false;
        fallbackLogged = false;
        deltaSyncManager.reset();
    }

    /**
     * Process a sync acknowledgment from the client.
     * @param sequenceNumber the acknowledged sequence number
     * @param clientIndex the client's player index
     */
    public void processAcknowledgment(long sequenceNumber, int clientIndex) {
        deltaSyncManager.processAcknowledgment(clientIndex, sequenceNumber);
    }

    private void send(final ProtocolMethod method, final Object... args) {
        if (paused) { return; }
        sender.send(method, args);
    }

    private <T> T sendAndWait(final ProtocolMethod method, final Object... args) {
        if (paused) { return null; }
        return sender.sendAndWait(method, args);
    }

    // Bandwidth tracking
    private long totalDeltaBytes = 0;
    private long totalFullStateBytes = 0;
    private int deltaPacketCount = 0;
    private boolean logBandwidth = FModel.getPreferences().getPrefBoolean(forge.localinstance.properties.ForgePreferences.FPref.NET_BANDWIDTH_LOGGING);

    /**
     * Send a game view update to the client.
     * Uses delta sync if enabled and initial sync has been sent,
     * otherwise sends the full game state.
     */
    public void updateGameView() {
        GameView gameView = getGameView();
        if (gameView == null) {
            return;
        }

        if (!useDeltaSync || !initialSyncSent) {
            // Fall back to full state sync - add debug logging (only once)
            if (logBandwidth && !fallbackLogged) {
                NetworkDebugLogger.log("[DeltaSync] Client %d: Fallback to full state - useDeltaSync=%b, initialSyncSent=%b",
                    clientIndex, useDeltaSync, initialSyncSent);
                fallbackLogged = true;
            }
            send(ProtocolMethod.setGameView, gameView);
            return;
        }

        // Use delta sync
        DeltaPacket delta = deltaSyncManager.collectDeltas(gameView);
        if (delta != null && !delta.isEmpty()) {
            // Get network byte tracker stats before sending
            long networkBytesBefore = getActualNetworkBytesSent();

            send(ProtocolMethod.applyDelta, delta);

            // NOTE: We DO NOT call clearAllChanges() here anymore.
            // Per-client change tracking (via lastSentPropertyChecksums in DeltaSyncManager)
            // handles tracking what has been sent to THIS client independently.
            // This fixes a bug where multiple remote clients sharing the same GameView
            // would cause subsequent clients to miss changes after the first client cleared them.

            // Track bandwidth savings with three measurements
            if (logBandwidth) {
                int deltaSize = delta.getApproximateSize();
                int fullStateSize = estimateFullStateSize(gameView);
                long networkBytesAfter = getActualNetworkBytesSent();
                int actualNetworkBytes = (int)(networkBytesAfter - networkBytesBefore);

                totalDeltaBytes += deltaSize;
                totalFullStateBytes += fullStateSize;
                deltaPacketCount++;

                int savings = fullStateSize > 0 ? (int)((1.0 - (double)deltaSize / fullStateSize) * 100) : 0;
                int actualSavings = fullStateSize > 0 ? (int)((1.0 - (double)actualNetworkBytes / fullStateSize) * 100) : 0;

                NetworkDebugLogger.log("[DeltaSync] Packet #%d: Approximate=%d bytes, ActualNetwork=%d bytes, FullState=%d bytes",
                    deltaPacketCount, deltaSize, actualNetworkBytes, fullStateSize);
                NetworkDebugLogger.log("[DeltaSync]   Savings: Approximate=%d%%, Actual=%d%% | Cumulative: Approximate=%d, Actual=%d, FullState=%d",
                    savings, actualSavings,
                    totalDeltaBytes, networkBytesAfter, totalFullStateBytes);
            }
        }
    }

    /**
     * Get actual network bytes sent (including compression and all overhead).
     * This provides ground truth for comparison with estimated sizes.
     * @return total bytes actually sent over the network
     */
    private long getActualNetworkBytesSent() {
        try {
            return FServerManager.getInstance().getNetworkByteTracker().getTotalBytesSent();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Estimate the size of a full state sync for comparison purposes.
     * This uses LZ4 compression (same as the network layer) to provide
     * an apples-to-apples comparison with actual network bytes.
     */
    private int estimateFullStateSize(GameView gameView) {
        try {
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            LZ4BlockOutputStream lz4Out = new LZ4BlockOutputStream(baos);
            java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(lz4Out);
            oos.writeObject(gameView);
            oos.close();
            return baos.size();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Send the full game state to the client.
     * Used for initial connection and reconnection.
     */
    public void sendFullState() {
        GameView gameView = getGameView();
        if (gameView == null) {
            return;
        }

        FullStatePacket packet = deltaSyncManager.createFullStatePacket(gameView);
        send(ProtocolMethod.fullStateSync, packet);
        initialSyncSent = true;

        // Mark all objects as sent so delta sync knows they don't need full serialization
        deltaSyncManager.markObjectsAsSent(gameView);

        // Clear all change flags since we've sent everything
        deltaSyncManager.clearAllChanges(gameView);
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
        // Initialize delta sync by sending the initial full state
        sendFullState();
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
        // Enhance generic waiting messages with the actual player name
        String enhancedMessage = enhanceWaitingMessage(message, playerView);
        send(ProtocolMethod.showPromptMessage, playerView, enhancedMessage);
    }

    /**
     * Enhance generic "Waiting for opponent" messages with the actual player name.
     * This provides better feedback in multiplayer games by showing exactly who
     * we're waiting for.
     *
     * @param message the original message
     * @param forPlayer the player this message is being shown to
     * @return the enhanced message with player name, or the original if not applicable
     */
    private String enhanceWaitingMessage(final String message, final PlayerView forPlayer) {
        if (message == null || message.isEmpty()) {
            return message;
        }

        Localizer localizer = Localizer.getInstance();
        String waitingForOpponent = localizer.getMessage("lblWaitingForOpponent");
        String yieldingMessage = localizer.getMessage("lblYieldingUntilEndOfTurn");

        // Check if this is a waiting/yielding message that should show player name
        boolean isWaitingMessage = message.equals(waitingForOpponent);
        boolean isYieldingMessage = message.equals(yieldingMessage);

        if (!isWaitingMessage && !isYieldingMessage) {
            return message;
        }

        // Get the priority player from the Game object
        GameView gameView = getGameView();
        if (gameView == null) {
            return message;
        }

        Game game = gameView.getGame();
        if (game == null || game.isGameOver()) {
            return message;
        }

        PhaseHandler ph = game.getPhaseHandler();
        if (ph == null) {
            return message;
        }

        Player priorityPlayer = ph.getPriorityPlayer();
        if (priorityPlayer == null) {
            // During game setup, use the player whose turn it is
            priorityPlayer = ph.getPlayerTurn();
        }

        // During mulligan/setup, both may be null - find the other player(s)
        if (priorityPlayer == null) {
            // If forPlayer is known, find someone else
            if (forPlayer != null) {
                for (Player p : game.getPlayers()) {
                    if (p.getView().getId() != forPlayer.getId()) {
                        priorityPlayer = p;
                        break;
                    }
                }
            }
            // If forPlayer is null or we couldn't find anyone, find any player not in our local set
            if (priorityPlayer == null) {
                for (Player p : game.getPlayers()) {
                    if (!isLocalPlayer(p.getView())) {
                        priorityPlayer = p;
                        break;
                    }
                }
            }
        }

        if (priorityPlayer == null) {
            return message;
        }

        // Don't enhance if waiting for self (shouldn't happen, but be safe)
        if (forPlayer != null && priorityPlayer.getView().getId() == forPlayer.getId()) {
            return message;
        }

        // Return enhanced message with player name
        if (isYieldingMessage) {
            return localizer.getMessage("lblYieldingWaitingForPlayer", priorityPlayer.getName());
        } else {
            return localizer.getMessage("lblWaitingForPlayer", priorityPlayer.getName());
        }
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
        updateGameView(); // Ensure game state is synced before asking for input
        final Integer result = sendAndWait(ProtocolMethod.showOptionDialog, message, title, icon, options, defaultOption);
        return result != null ? result : defaultOption;
    }

    @Override
    public String showInputDialog(final String message, final String title, final FSkinProp icon, final String initialInput, final List<String> inputOptions, final boolean isNumeric) {
        updateGameView(); // Ensure game state is synced before asking for input
        return sendAndWait(ProtocolMethod.showInputDialog, message, title, icon, initialInput, inputOptions, isNumeric);
    }

    @Override
    public boolean confirm(final CardView c, final String question, final boolean defaultIsYes, final List<String> options) {
        updateGameView(); // Ensure game state is synced before asking for input
        final Boolean result = sendAndWait(ProtocolMethod.confirm, c, question, defaultIsYes, options);
        return result != null ? result : defaultIsYes;
    }

    @Override
    public <T> List<T> getChoices(final String message, final int min, final int max, final List<T> choices, final List<T> selected, final FSerializableFunction<T, String> display) {
        updateGameView(); // Ensure game state is synced before asking for input
        return sendAndWait(ProtocolMethod.getChoices, message, min, max, choices, selected, display);
    }

    @Override
    public <T> List<T> order(final String title, final String top, final int remainingObjectsMin, final int remainingObjectsMax, final List<T> sourceChoices, final List<T> destChoices, final CardView referenceCard, final boolean sideboardingMode) {
        updateGameView(); // Ensure game state is synced before asking for input
        return sendAndWait(ProtocolMethod.order, title, top, remainingObjectsMin, remainingObjectsMax, sourceChoices, destChoices, referenceCard, sideboardingMode);
    }

    @Override
    public List<PaperCard> sideboard(final CardPool sideboard, final CardPool main, final String message) {
        updateGameView(); // Ensure game state is synced before asking for input
        return sendAndWait(ProtocolMethod.sideboard, sideboard, main, message);
    }

    @Override
    public GameEntityView chooseSingleEntityForEffect(final String title, final List<? extends GameEntityView> optionList, final DelayedReveal delayedReveal, final boolean isOptional) {
        updateGameView(); // Ensure game state is synced before asking for input
        return sendAndWait(ProtocolMethod.chooseSingleEntityForEffect, title, optionList, delayedReveal, isOptional);
    }

    @Override
    public List<GameEntityView> chooseEntitiesForEffect(final String title, final List<? extends GameEntityView> optionList, final int min, final int max, final DelayedReveal delayedReveal) {
        updateGameView(); // Ensure game state is synced before asking for input
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
    public void showWaitingTimer(final PlayerView forPlayer, final String waitingForPlayerName) {
        send(ProtocolMethod.showWaitingTimer, forPlayer, waitingForPlayerName);
    }

    @Override
    public boolean isNetGame() { return true; }

    @Override
    protected void updateCurrentPlayer(final PlayerView player) {
        // TODO Auto-generated method stub
    }

    @Override
    public String toString() {
        GameView gv = getGameView();
        return String.format("NetGuiGame[client=%d, deltaSyncEnabled=%b, initialSyncSent=%b, gameView=%s]",
                clientIndex, useDeltaSync, initialSyncSent,
                gv != null ? "GameView@" + Integer.toHexString(System.identityHashCode(gv)) : "null");
    }

}
