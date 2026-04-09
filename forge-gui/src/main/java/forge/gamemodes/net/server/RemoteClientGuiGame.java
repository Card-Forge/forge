package forge.gamemodes.net.server;

import forge.LobbyPlayer;
import forge.ai.GameState;
import forge.deck.CardPool;
import forge.game.GameEntityView;
import forge.game.event.GameEvent;
import forge.game.GameView;
import forge.game.card.CardView;
import forge.game.phase.PhaseType;
import forge.game.player.DelayedReveal;
import forge.game.player.IHasIcon;
import forge.game.player.PlayerView;
import forge.game.spellability.SpellAbilityView;
import forge.game.zone.ZoneType;
import forge.gamemodes.match.YieldPrefs;
import forge.gamemodes.net.NetworkGuiGame;
import forge.gamemodes.net.DeltaPacket;
import forge.gamemodes.net.GameEventProxy;
import forge.gamemodes.net.GameProtocolSender;
import forge.gamemodes.net.IHasNetLog;
import forge.gamemodes.net.ProtocolMethod;
import forge.gui.control.GameEventForwarder;
import forge.item.PaperCard;
import forge.localinstance.skin.FSkinProp;
import forge.model.FModel;
import forge.player.PlayerZoneUpdate;
import forge.player.PlayerZoneUpdates;
import forge.trackable.Tracker;
import forge.trackable.TrackableCollection;
import forge.util.FSerializableFunction;
import forge.util.ITriggerEvent;

import net.jpountz.lz4.LZ4BlockOutputStream;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RemoteClientGuiGame extends NetworkGuiGame implements IHasNetLog {

    // New objects are sent with full property data, existing objects only send changed properties
    public static boolean useDeltaSync = false;

    private final GameProtocolSender sender;
    private final DeltaSyncManager syncManager;
    private final int clientIndex;
    private boolean initialSyncSent = false;
    private boolean objectsRegistered = false;
    private boolean fallbackLogged = false;  // Prevent duplicate fallback log messages
    private volatile boolean paused;
    private volatile boolean resyncPending;
    private volatile YieldPrefs remoteYieldPrefs;

    private GameEventForwarder forwarder;
    private boolean flushing;

    public RemoteClientGuiGame(final RemoteClient client) {
        sender = new GameProtocolSender(client);
        syncManager = new DeltaSyncManager();
        clientIndex = client.getIndex();
    }

    /** Alias for reconnection code that references slot index. */
    public int getSlotIndex() {
        return clientIndex;
    }

    @Override
    public boolean isRemoteGuiProxy() {
        return true;
    }

    @Override
    public void setRemoteYieldPrefs(YieldPrefs prefs) {
        remoteYieldPrefs = prefs;
    }

    @Override
    public YieldPrefs getRemoteYieldPrefs() {
        return remoteYieldPrefs;
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
        objectsRegistered = false;
        fallbackLogged = false;
        syncManager.reset();
    }

    public void setForwarder(GameEventForwarder forwarder) {
        this.forwarder = forwarder;
    }

    public GameEventForwarder getForwarder() {
        return forwarder;
    }

    public void shutdownForwarder() {
        if (forwarder != null) {
            forwarder.flush();
            forwarder = null;
        }
    }

    private void flushPendingEvents() {
        if (forwarder != null && !flushing) {
            flushing = true;
            try {
                forwarder.flush();
            } finally {
                flushing = false;
            }
        }
    }

    private void send(final ProtocolMethod method, final Object... args) {
        if (paused) { return; }
        flushPendingEvents();
        sender.send(method, args);
    }

    private <T> T sendAndWait(final ProtocolMethod method, final Object... args) {
        if (paused) { return null; }
        flushPendingEvents();
        return sender.sendAndWait(method, args);
    }

    // Bandwidth tracking — both sides measured via serialize+compress for apples-to-apples comparison
    private long totalDeltaBytes = 0;
    private long totalFullStateBytes = 0;
    private int deltaPacketCount = 0;
    private boolean logBandwidth = FModel.getNetPreferences().getPrefBoolean(forge.localinstance.properties.ForgeNetPreferences.FNetPref.NET_BANDWIDTH_LOGGING);

    /**
     * Send a game view update to the client.
     * Uses delta sync if enabled and initial sync has been sent,
     * otherwise sends the full game state.
     */
    public void updateGameView() {
        updateGameView(true);
    }
    private void updateGameView(boolean flush) {
        GameView gameView = getGameView();
        if (gameView == null) {
            return;
        }

        if (!useDeltaSync || !initialSyncSent) {
            if (logBandwidth && !fallbackLogged) {
                netLog.info("[DeltaSync] Client {}: Fallback to full state - useDeltaSync={}, initialSyncSent={}",
                    clientIndex, useDeltaSync, initialSyncSent);
                fallbackLogged = true;
            }
            if (flush) {
                send(ProtocolMethod.setGameView, gameView, -1L);
            } else {
                sender.write(ProtocolMethod.setGameView, gameView, -1L);
            }
            return;
        }

        // Resync requested by client (checksum mismatch) — send full state
        // from the game thread where the gameView is consistent.
        if (resyncPending) {
            resyncPending = false;
            sendFullState();
            return;
        }

        // Flush pending events BEFORE collecting deltas — ensures events
        // get the dirty props bundled with them, and this standalone delta
        // only picks up whatever changed after the flush.
        flushPendingEvents();
        DeltaPacket delta = syncManager.collectDeltas(gameView);
        if (!delta.isEmpty()) {
            if (flush) {
                sender.send(ProtocolMethod.applyDelta, delta);
            } else {
                sender.write(ProtocolMethod.applyDelta, delta);
            }

            if (logBandwidth) {
                int deltaSize = measureSerializedSize(delta);
                int fullStateSize = measureSerializedSize(gameView);

                totalDeltaBytes += deltaSize;
                totalFullStateBytes += fullStateSize;
                deltaPacketCount++;

                int savings = fullStateSize > 0 ? (int)((1.0 - (double)deltaSize / fullStateSize) * 100) : 0;

                netLog.info("[DeltaSync] Packet #{}: Delta={} bytes, FullState={} bytes, Savings={}%",
                    deltaPacketCount, deltaSize, fullStateSize, savings);
                netLog.info("[DeltaSync]   Cumulative: Delta={}, FullState={}, Savings={}%",
                    totalDeltaBytes, totalFullStateBytes,
                    totalFullStateBytes > 0 ? (int)((1.0 - (double)totalDeltaBytes / totalFullStateBytes) * 100) : 0);
            }
        }
    }

    /**
     * Measure the serialized+compressed size of an object.
     * Uses ObjectOutputStream + LZ4 — same pipeline as the network encoder —
     * so delta and full-state measurements are directly comparable.
     */
    private int measureSerializedSize(Object obj) {
        try {
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            LZ4BlockOutputStream lz4Out = new LZ4BlockOutputStream(baos);
            java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(lz4Out);
            oos.writeObject(obj);
            oos.close();
            return baos.size();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Request a full state resync on the next updateGameView call.
     * Called from the Netty thread; the actual send happens on the game thread.
     */
    public void setResyncPending() {
        resyncPending = true;
        syncManager.onResyncRequested();
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

        long seq = syncManager.getCurrentSequence();
        send(ProtocolMethod.setGameView, gameView, seq);
        initialSyncSent = true;

        // Consumer registration is deferred — it happens on the first collectDeltas()
        // call (see updateGameView). This ensures that all objects created during game
        // initialization (prepareAllZones, hand dealing, commander placement) are present
        // in the view graph when registration occurs. If we registered here, the view
        // is still empty (openView runs before match.startGame/prepareAllZones), and
        // zone updates during init would set dirty bits on the 3 registered objects
        // but the Command zone dirty bit is sporadically lost in Commander games.
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
        updateGameView();
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
    public void updateShards(Iterable<PlayerView> shardsUpdate) {
        //mobile adventure local game only..
    }

    @Override
    public void setPanelSelection(final CardView hostCard) {
        updateGameView();
        send(ProtocolMethod.setPanelSelection, hostCard);
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
    public void setHighlighted(final GameEntityView ge, final boolean value) {
        super.setHighlighted(ge, value);
        send(ProtocolMethod.setHighlighted, ge, value);
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
    public void setHostYieldEnabled(final boolean enabled) {
        send(ProtocolMethod.setHostYieldEnabled, enabled);
    }

    @Override
    public void showWaitingTimer(final PlayerView forPlayer, final String waitingForPlayerName) {
        send(ProtocolMethod.showWaitingTimer, forPlayer, waitingForPlayerName);
    }

    @Override
    public void handleGameEvent(GameEvent event) {
        handleGameEvents(List.of(event));
    }

    @Override
    public void handleGameEvents(List<GameEvent> events) {
        if (paused) { return; }
        netLog.info("Sending batch of {}: [{}]", events.size(),
                events.stream().map(e -> e.getClass().getSimpleName()).collect(Collectors.joining(", ")));
        // When GameEventGameStarted arrives, prepareAllZones has completed —
        // commanders are placed and zones are populated. Register consumers on
        // any objects not yet tracked (without clearing dirty bits).
        if (!objectsRegistered) {
            for (GameEvent ev : events) {
                if (ev instanceof forge.game.event.GameEventGameStarted) {
                    GameView gv = getGameView();
                    if (gv != null) {
                        syncManager.registerNewObjects(gv);
                        objectsRegistered = true;
                    }
                    break;
                }
            }
        }
        Tracker tracker = getGameView() != null ? getGameView().getTracker() : null;
        List<Object> proxied = GameEventProxy.wrapAll(events, tracker);
        if (useDeltaSync && initialSyncSent && objectsRegistered) {
            // Bundle events with delta so they're applied atomically:
            // delta properties first, then events forwarded.
            GameView gameView = getGameView();
            if (gameView != null) {
                DeltaPacket delta = syncManager.collectDeltas(gameView);
                delta.setProxiedEvents(proxied);
                sender.send(ProtocolMethod.applyDelta, delta);

                if (logBandwidth) {
                    int deltaSize = measureSerializedSize(delta);
                    int eventsSize = measureSerializedSize(proxied);
                    int stateOnlyFullSize = measureSerializedSize(gameView);
                    int fullStateSize = stateOnlyFullSize + eventsSize;
                    DeltaPacket stateOnly = delta.withoutEvents();
                    int stateOnlyDeltaSize = measureSerializedSize(stateOnly);

                    totalDeltaBytes += deltaSize;
                    totalFullStateBytes += fullStateSize;
                    deltaPacketCount++;

                    int savings = fullStateSize > 0 ? (int)((1.0 - (double)deltaSize / fullStateSize) * 100) : 0;

                    netLog.info("[DeltaSync] Packet #{}: Delta={} bytes, FullState={} bytes, Savings={}%, StateOnlyDelta={} bytes, StateOnlyFull={} bytes",
                        deltaPacketCount, deltaSize, fullStateSize, savings, stateOnlyDeltaSize, stateOnlyFullSize);
                    netLog.info("[DeltaSync]   Cumulative: Delta={}, FullState={}, Savings={}%",
                        totalDeltaBytes, totalFullStateBytes,
                        totalFullStateBytes > 0 ? (int)((1.0 - (double)totalDeltaBytes / totalFullStateBytes) * 100) : 0);
                }
            }
        } else {
            updateGameView(false);
            sender.send(ProtocolMethod.applyDelta, DeltaPacket.eventsOnly(proxied));
        }
    }

    @Override
    public boolean isNetGame() {
        return true;
    }

    @Override
    protected void updateCurrentPlayer(final PlayerView player) {}

    @Override
    public String toString() {
        GameView gv = getGameView();
        return String.format("RemoteClientGuiGame[client=%d, deltaSyncEnabled=%b, initialSyncSent=%b, gameView=%s]",
                clientIndex, useDeltaSync, initialSyncSent,
                gv != null ? "GameView@" + Integer.toHexString(System.identityHashCode(gv)) : "null");
    }

}
