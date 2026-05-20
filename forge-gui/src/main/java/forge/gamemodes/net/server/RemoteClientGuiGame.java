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
import forge.gamemodes.match.YieldUpdate;
import forge.gamemodes.net.NetworkGuiGame;
import forge.gamemodes.net.DeltaPacket;
import forge.gamemodes.net.GameProtocolSender;
import forge.util.IHasForgeLog;
import forge.gamemodes.net.ProtocolMethod;
import forge.gamemodes.net.TrackableSerializer;
import forge.gui.control.GameEventForwarder;
import forge.gui.interfaces.IGuiGame;
import forge.item.PaperCard;
import forge.localinstance.skin.FSkinProp;
import forge.model.FModel;
import forge.player.PlayerZoneUpdate;
import forge.player.PlayerZoneUpdates;
import forge.trackable.TrackableCollection;
import forge.util.FSerializableFunction;
import forge.util.ITriggerEvent;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Server-side proxy for one remote client's GUI. One instance per connected remote player,
 * constructed at lobby time and reused across the match (and across reconnects, via
 * {@link #resetForReconnect}).
 *
 * <p>IGuiGame overrides forward through one of four helpers named for their behavior:
 * {@link #send} (fire), {@link #syncAndSend} (walk the trackable graph, then fire),
 * {@link #sendAndWait} (block for reply), {@link #syncAndSendAndWait} (walk, then block).
 * {@code sync*} ensures the client has the entities referenced in the payload; the bare
 * forms are for payloads that are pure IDs, strings, or flags.
 *
 * <p>{@code sync*} requires the game thread because the trackable graph is single-mutator;
 * the bare variants skip the walk and are safe from any thread.
 */
public class RemoteClientGuiGame extends NetworkGuiGame implements IHasForgeLog {

    // New objects are sent with full property data, existing objects only send changed properties
    public static boolean useDeltaSync = true;

    private final RemoteClient client;
    private final GameProtocolSender sender;
    private final DeltaSyncManager syncManager;

    private boolean initialSyncSent = false;
    private boolean objectsRegistered = false;
    private boolean codecTrackerSet = false;
    private boolean fallbackLogged = false;  // Prevent duplicate fallback log messages
    private volatile boolean paused;
    private volatile boolean resyncPending;

    private GameEventForwarder forwarder;
    private boolean flushing;

    public RemoteClientGuiGame(final RemoteClient client) {
        this.client = client;
        sender = new GameProtocolSender(client);
        syncManager = new DeltaSyncManager();
        client.setGui(this);
    }

    public RemoteClient getClient() {
        return client;
    }

    @Override
    public boolean isLibgdxPort() {
        return client.isLibgdx();
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

    /**
     * Dispatches a self-contained payload (IDs, strings, flags) — no graph walk,
     * safe from any thread. Used when the client doesn't need fresh state to
     * consume the message.
     */
    private void send(final ProtocolMethod method, final Object... args) {
        if (paused) { return; }
        sender.send(method, args);
    }

    /**
     * Walks the trackable graph via {@link #updateGameView} before dispatching, so
     * the client has the entities referenced in the payload. Game-thread-only.
     */
    private void syncAndSend(final ProtocolMethod method, final Object... args) {
        if (paused) { return; }
        updateGameView();
        sender.send(method, args);
    }

    /**
     * Dispatches and blocks for the client's reply, with no graph walk. Used for
     * dialogs whose args carry the full question (e.g. plain-text confirms).
     */
    private <T> T sendAndWait(final ProtocolMethod method, final Object... args) {
        if (paused) { return null; }
        return sender.sendAndWait(method, args);
    }

    /**
     * Walks the trackable graph, dispatches, and blocks for the client's reply.
     * Used for dialogs that reference entities, cards, or zones the client must
     * have to render the prompt. Game-thread-only.
     */
    private <T> T syncAndSendAndWait(final ProtocolMethod method, final Object... args) {
        if (paused) { return null; }
        updateGameView();
        return sender.sendAndWait(method, args);
    }

    // Bandwidth tracking — both sides measured via serialize+compress for apples-to-apples comparison
    private long totalDeltaBytes = 0;
    private long totalFullStateBytes = 0;
    private int deltaPacketCount = 0;
    private boolean logBandwidth = FModel.getNetPreferences().getPrefBoolean(forge.localinstance.properties.ForgeNetPreferences.FNetPref.NET_BANDWIDTH_LOGGING);

    /**
     * Push pending state to the client. Flushes the event queue if any events
     * are queued; otherwise walks the trackable graph for state changes that
     * didn't fire an event and sends an applyDelta. Falls back to a full
     * setGameView before delta sync is established. Game-thread-only.
     *
     * <p>Called internally by {@link #syncAndSend} and {@link #syncAndSendAndWait};
     * also invoked directly from the reconnect handshake.
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
                    client.getIndex(), useDeltaSync, initialSyncSent);
                fallbackLogged = true;
            }
            // Batch flush already emits setGameView; skip the duplicate.
            if (forwarder != null && forwarder.hasPendingEvents()) {
                flushPendingEvents();
                return;
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

        // If events are pending, the batch flush bundles dirty props with
        // them into applyDelta — skip the redundant graph walk here.
        if (forwarder != null && forwarder.hasPendingEvents()) {
            flushPendingEvents();
            return;
        }
        DeltaPacket delta = syncManager.collectDeltas(gameView);
        if (!delta.isEmpty()) {
            if (flush) {
                sender.send(ProtocolMethod.applyDelta, delta);
            } else {
                sender.write(ProtocolMethod.applyDelta, delta);
            }

            if (logBandwidth) {
                int deltaSize = TrackableSerializer.measureSize(delta, gameView.getTracker());
                int fullStateSize = TrackableSerializer.measureSize(gameView, null);

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
        // Set codec tracker before any client protocol messages arrive.
        // setGameView is called before openView, and the client can't respond
        // until after openView — so the encoder/decoder are ready in time.
        if (!codecTrackerSet && gameView != null && gameView.getTracker() != null) {
            client.setCodecTracker(gameView.getTracker(), syncManager.getConsumerId());
            codecTrackerSet = true;
        }
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
        syncAndSend(ProtocolMethod.afterGameEnd);
    }

    @Override
    public void showCombat() {
        syncAndSend(ProtocolMethod.showCombat);
    }

    @Override
    public void showPromptMessage(final PlayerView playerView, final String message) {
        send(ProtocolMethod.showPromptMessage, playerView, message);
    }

    @Override
    public void applyYieldUpdate(final YieldUpdate update) {
        send(ProtocolMethod.applyYieldUpdate, update);
    }

    @Override
    public void showCardPromptMessage(final PlayerView playerView, final String message, final CardView card) {
        syncAndSend(ProtocolMethod.showCardPromptMessage, playerView, message, card);
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
        syncAndSend(ProtocolMethod.finishGame);
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
        return syncAndSendAndWait(ProtocolMethod.tempShowZones, controller, zonesToUpdate);
    }

    @Override
    public void hideZones(final PlayerView controller, final Iterable<PlayerZoneUpdate> zonesToUpdate) {
        syncAndSend(ProtocolMethod.hideZones, controller, zonesToUpdate);
    }

    @Override
    public void updateShards(Iterable<PlayerView> shardsUpdate) {
        //mobile adventure local game only..
    }

    @Override
    public void setPanelSelection(final CardView hostCard) {
        syncAndSend(ProtocolMethod.setPanelSelection, hostCard);
    }

    @Override
    public GameState getGamestate() {
        return null;
    }

    @Override
    public SpellAbilityView getAbilityToPlay(final CardView hostCard, final List<SpellAbilityView> abilities, final ITriggerEvent triggerEvent) {
        return syncAndSendAndWait(ProtocolMethod.getAbilityToPlay, hostCard, abilities, null/*triggerEvent*/); //someplatform don't have mousetriggerevent class or it will not allow them to click/tap
    }

    @Override
    public Map<CardView, Integer> assignCombatDamage(final CardView attacker, final List<CardView> blockers, final int damage, final GameEntityView defender, final boolean overrideOrder, final boolean maySkip) {
        return syncAndSendAndWait(ProtocolMethod.assignCombatDamage, attacker, blockers, damage, defender, overrideOrder, maySkip);
    }

    @Override
    public Map<Object, Integer> assignGenericAmount(final CardView effectSource, final Map<Object, Integer> targets, final int amount, final boolean atLeastOne, final String amountLabel) {
        return syncAndSendAndWait(ProtocolMethod.assignGenericAmount, effectSource, targets, amount, atLeastOne, amountLabel);
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
        final Integer result = syncAndSendAndWait(ProtocolMethod.showOptionDialog, message, title, icon, options, defaultOption);
        return result != null ? result : defaultOption;
    }

    @Override
    public String showInputDialog(final String message, final String title, final FSkinProp icon, final String initialInput, final List<String> inputOptions, final boolean isNumeric) {
        return syncAndSendAndWait(ProtocolMethod.showInputDialog, message, title, icon, initialInput, inputOptions, isNumeric);
    }

    @Override
    public boolean confirm(final CardView c, final String question, final boolean defaultIsYes, final List<String> options) {
        final Boolean result = syncAndSendAndWait(ProtocolMethod.confirm, c, question, defaultIsYes, options);
        return result != null ? result : defaultIsYes;
    }

    @Override
    public <T> List<T> getChoices(final String message, final int min, final int max, final List<T> choices, final List<T> selected, final FSerializableFunction<T, String> display) {
        return syncAndSendAndWait(ProtocolMethod.getChoices, message, min, max, choices, selected, display);
    }

    @Override
    public <T> IGuiGame.OrderResult<T> order(final String title, final String top, final int remainingObjectsMin, final int remainingObjectsMax, final List<T> sourceChoices, final List<T> destChoices, final CardView referenceCard, final boolean sideboardingMode, final boolean showRememberCheckbox) {
        return syncAndSendAndWait(ProtocolMethod.order, title, top, remainingObjectsMin, remainingObjectsMax, sourceChoices, destChoices, referenceCard, sideboardingMode, showRememberCheckbox);
    }

    @Override
    public List<PaperCard> sideboard(final CardPool sideboard, final CardPool main, final String message) {
        return syncAndSendAndWait(ProtocolMethod.sideboard, sideboard, main, message);
    }

    @Override
    public GameEntityView chooseSingleEntityForEffect(final String title, final List<? extends GameEntityView> optionList, final DelayedReveal delayedReveal, final boolean isOptional) {
        return syncAndSendAndWait(ProtocolMethod.chooseSingleEntityForEffect, title, optionList, delayedReveal, isOptional);
    }

    @Override
    public List<GameEntityView> chooseEntitiesForEffect(final String title, final List<? extends GameEntityView> optionList, final int min, final int max, final DelayedReveal delayedReveal) {
        return syncAndSendAndWait(ProtocolMethod.chooseEntitiesForEffect, title, optionList, min, max, delayedReveal);
    }

    @Override
    public List<CardView> manipulateCardList(final String title, final Iterable<CardView> cards, final Iterable<CardView> manipulable, final boolean toTop, final boolean toBottom, final boolean toAnywhere) {
        return syncAndSendAndWait(ProtocolMethod.manipulateCardList, title, cards, manipulable, toTop, toBottom, toAnywhere);
    }

    public void setHighlighted(final Iterable<GameEntityView> entities, final boolean value) {
        super.setHighlighted(entities, value);
        syncAndSend(ProtocolMethod.setHighlighted, entities, value);
    }

    @Override
    public void setCard(final CardView card) {
        syncAndSend(ProtocolMethod.setCard, card);
    }

    @Override
    public void setSelectables(final Iterable<CardView> cards, final int min, final int max) {
        syncAndSend(ProtocolMethod.setSelectables, cards, min, max);
    }

    @Override
    public void clearSelectables() {
        syncAndSend(ProtocolMethod.clearSelectables);
    }

    @Override
    public void setPlayerAvatar(final LobbyPlayer player, final IHasIcon ihi) {
        // TODO Auto-generated method stub
    }

    @Override
    public PlayerZoneUpdates openZones(PlayerView controller, final Collection<ZoneType> zones, final Map<PlayerView, Object> players, boolean backupLastZones) {
        return syncAndSendAndWait(ProtocolMethod.openZones, controller, zones, players, backupLastZones);
    }

    @Override
    public void restoreOldZones(PlayerView playerView, PlayerZoneUpdates playerZoneUpdates) {
        syncAndSend(ProtocolMethod.restoreOldZones, playerView, playerZoneUpdates);
    }

    @Override
    public boolean isUiSetToSkipPhase(final PlayerView playerTurn, final PhaseType phase) {
        // Host reads from PlayerControllerHuman's cache; this gui-side path is unreachable
        return false;
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
        if (useDeltaSync && initialSyncSent && objectsRegistered) {
            // Bundle events with delta so they're applied atomically:
            // delta properties first, then events forwarded.
            GameView gameView = getGameView();
            if (gameView != null) {
                DeltaPacket delta = syncManager.collectDeltas(gameView);
                delta.setEvents(TrackableSerializer.wrapEvents(events, gameView.getTracker()));
                sender.send(ProtocolMethod.applyDelta, delta);

                if (logBandwidth) {
                    int deltaSize = TrackableSerializer.measureSize(delta, gameView.getTracker());
                    int eventsSize = TrackableSerializer.measureSize(events, gameView.getTracker());
                    int stateOnlyFullSize = TrackableSerializer.measureSize(gameView, null);
                    int fullStateSize = stateOnlyFullSize + eventsSize;
                    int stateOnlyDeltaSize = TrackableSerializer.measureSize(delta.withoutEvents(), gameView.getTracker());

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
            GameView gameView = getGameView();
            forge.trackable.Tracker tracker = gameView != null ? gameView.getTracker() : null;
            sender.send(ProtocolMethod.applyDelta, DeltaPacket.eventsOnly(TrackableSerializer.wrapEvents(events, tracker)));
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
                client.getIndex(), useDeltaSync, initialSyncSent,
                gv != null ? "GameView@" + Integer.toHexString(System.identityHashCode(gv)) : "null");
    }

}
