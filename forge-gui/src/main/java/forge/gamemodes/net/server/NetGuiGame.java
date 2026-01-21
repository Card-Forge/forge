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
import forge.gamemodes.net.DeltaPacket;
import forge.gamemodes.net.FullStatePacket;
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

    private GameProtocolSender sender;
    private final DeltaSyncManager deltaSyncManager;
    private final int clientIndex;
    // Delta sync is ENABLED - new objects are now properly handled.
    // New objects are sent with full property data, existing objects only send changed properties.
    private boolean useDeltaSync = true;
    private boolean initialSyncSent = false;

    public NetGuiGame(final RemoteClient client) {
        this.sender = new GameProtocolSender(client);
        this.deltaSyncManager = new DeltaSyncManager();
        this.clientIndex = client.getIndex();
    }

    /**
     * Get the client index for this GUI.
     * @return the client's player index
     */
    public int getClientIndex() {
        return clientIndex;
    }

    /**
     * Update the client connection for this GUI.
     * Called when a player reconnects with a new connection.
     * @param client the new client connection
     */
    public void updateClient(final RemoteClient client) {
        this.sender = new GameProtocolSender(client);
        System.out.println("[NetGuiGame] Updated client connection for player " + clientIndex);
    }

    /**
     * Enable or disable delta sync mode.
     * When disabled, falls back to sending full state on every update.
     * @param enabled true to enable delta sync
     */
    public void setDeltaSyncEnabled(boolean enabled) {
        this.useDeltaSync = enabled;
    }

    /**
     * Check if delta sync is enabled.
     * @return true if delta sync is enabled
     */
    public boolean isDeltaSyncEnabled() {
        return useDeltaSync;
    }

    /**
     * Get the delta sync manager for this game.
     * @return the DeltaSyncManager instance
     */
    public DeltaSyncManager getDeltaSyncManager() {
        return deltaSyncManager;
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
        sender.send(method, args);
    }

    private <T> T sendAndWait(final ProtocolMethod method, final Object... args) {
        return sender.sendAndWait(method, args);
    }

    // Bandwidth tracking
    private long totalDeltaBytes = 0;
    private long totalFullStateBytes = 0;
    private int deltaPacketCount = 0;
    // Enable via system property: -Dforge.network.logBandwidth=true
    private boolean logBandwidth = Boolean.getBoolean("forge.network.logBandwidth");

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
            // Fall back to full state sync - add debug logging
            if (logBandwidth) {
                System.out.println(String.format(
                    "[DeltaSync] Client %d: Fallback to full state - useDeltaSync=%b, initialSyncSent=%b",
                    clientIndex, useDeltaSync, initialSyncSent));
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

            // CRITICAL: Clear changes after sending to prevent accumulation
            deltaSyncManager.clearAllChanges(gameView);

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

                System.out.println(String.format(
                    "[DeltaSync] Packet #%d: Approximate=%d bytes, ActualNetwork=%d bytes, FullState=%d bytes",
                    deltaPacketCount, deltaSize, actualNetworkBytes, fullStateSize));
                System.out.println(String.format(
                    "[DeltaSync]   Savings: Approximate=%d%%, Actual=%d%% | Cumulative: Approximate=%d, Actual=%d, FullState=%d",
                    savings, actualSavings,
                    totalDeltaBytes, networkBytesAfter, totalFullStateBytes
                ));
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
     * This uses ObjectOutputStream serialization (same as network layer before compression).
     */
    private int estimateFullStateSize(GameView gameView) {
        try {
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(baos);
            oos.writeObject(gameView);
            oos.close();
            return baos.size();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Enable or disable bandwidth logging.
     */
    public void setLogBandwidth(boolean enabled) {
        this.logBandwidth = enabled;
    }

    /**
     * Get bandwidth statistics.
     * @return array of [totalDeltaBytes, totalFullStateBytes, packetCount]
     */
    public long[] getBandwidthStats() {
        return new long[] { totalDeltaBytes, totalFullStateBytes, deltaPacketCount };
    }

    /**
     * Reset bandwidth statistics.
     */
    public void resetBandwidthStats() {
        totalDeltaBytes = 0;
        totalFullStateBytes = 0;
        deltaPacketCount = 0;
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

    /**
     * Send full state for reconnection with session credentials.
     * @param sessionId the session identifier
     * @param sessionToken the session token
     */
    public void sendFullStateForReconnect(String sessionId, String sessionToken) {
        GameView gameView = getGameView();
        if (gameView == null) {
            return;
        }

        FullStatePacket packet = deltaSyncManager.createFullStatePacketForReconnect(gameView, sessionId, sessionToken);
        send(ProtocolMethod.reconnectAccepted, packet);
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
        // Send session credentials after opening the view
        sendSessionCredentials();
    }

    /**
     * Send session credentials to the client for reconnection support.
     * This must be called after the initial game view is sent.
     */
    private void sendSessionCredentials() {
        System.out.println("[DeltaSync] sendSessionCredentials called for client " + clientIndex);
        GameSession session = FServerManager.getInstance().getCurrentGameSession();
        if (session == null) {
            System.out.println("[DeltaSync] WARN: session is null - cannot send credentials");
            return;
        }

        PlayerSession playerSession = session.getPlayerSession(clientIndex);
        if (playerSession == null) {
            System.out.println("[DeltaSync] WARN: playerSession is null for client " + clientIndex);
            return;
        }

        // Send credentials via fullStateSync with session info
        GameView gameView = getGameView();
        if (gameView == null) {
            System.out.println("[DeltaSync] WARN: gameView is null");
            return;
        }

        FullStatePacket packet = new FullStatePacket(
                deltaSyncManager.getCurrentSequence(),
                gameView,
                session.getSessionId(),
                playerSession.getSessionToken()
        );
        send(ProtocolMethod.fullStateSync, packet);
        initialSyncSent = true;

        // Mark all objects as sent so delta sync knows they don't need full serialization
        deltaSyncManager.markObjectsAsSent(gameView);

        System.out.println("[DeltaSync] Session credentials sent, initialSyncSent = true, objects marked as sent");
        // Clear all change flags since we've sent everything
        deltaSyncManager.clearAllChanges(gameView);
    }

    @Override
    public void afterGameEnd() {
        send(ProtocolMethod.afterGameEnd);
        // End the game session when the match ends
        FServerManager.getInstance().onGameEnded();
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
        return sendAndWait(ProtocolMethod.showConfirmDialog, message, title, yesButtonText, noButtonText, defaultYes);
    }

    @Override
    public int showOptionDialog(final String message, final String title, final FSkinProp icon, final List<String> options, final int defaultOption) {
        updateGameView(); // Ensure game state is synced before asking for input
        return sendAndWait(ProtocolMethod.showOptionDialog, message, title, icon, options, defaultOption);
    }

    @Override
    public String showInputDialog(final String message, final String title, final FSkinProp icon, final String initialInput, final List<String> inputOptions, final boolean isNumeric) {
        updateGameView(); // Ensure game state is synced before asking for input
        return sendAndWait(ProtocolMethod.showInputDialog, message, title, icon, initialInput, inputOptions, isNumeric);
    }

    @Override
    public boolean confirm(final CardView c, final String question, final boolean defaultIsYes, final List<String> options) {
        updateGameView(); // Ensure game state is synced before asking for input
        return sendAndWait(ProtocolMethod.confirm, c, question, defaultIsYes, options);
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
        return sendAndWait(ProtocolMethod.isUiSetToSkipPhase, playerTurn, phase);
    }

    @Override
    protected void updateCurrentPlayer(final PlayerView player) {
        // TODO Auto-generated method stub
    }

}
