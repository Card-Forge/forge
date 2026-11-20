package forge.web.match;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import forge.ImageKeys;
import forge.LobbyPlayer;
import forge.card.MagicColor;
import forge.deck.CardPool;
import forge.game.DrawOffer;
import forge.game.GameEntityView;
import forge.game.GameLog;
import forge.game.GameView;
import forge.game.card.CardFaceView;
import forge.game.card.CardView;
import forge.game.event.GameEvent;
import forge.game.event.GameEventGameOutcome;
import forge.game.event.GameEventZone;
import forge.game.phase.PhaseType;
import forge.game.player.DelayedReveal;
import forge.game.player.IHasIcon;
import forge.game.player.PlayerView;
import forge.game.spellability.SpellAbilityView;
import forge.game.spellability.StackItemView;
import forge.game.zone.ZoneType;
import forge.gamemodes.match.DrawOfferMessage;
import forge.gamemodes.match.NextGameDecision;
import forge.gamemodes.match.Selection;
import forge.gamemodes.match.YieldController;
import forge.gamemodes.match.YieldMarker;
import forge.gamemodes.match.YieldUpdate;
import forge.gamemodes.net.DeltaPacket;
import forge.gamemodes.net.NetworkGuiGame;
import forge.gamemodes.net.server.DeltaSyncManager;
import forge.interfaces.IGameController;
import forge.item.PaperCard;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.localinstance.skin.FSkinProp;
import forge.player.AutoYieldStore.TriggerDecision;
import forge.player.PlayerZoneUpdate;
import forge.trackable.TrackableCollection;
import forge.trackable.TrackableProperty;
import forge.trackable.TrackableTypes;
import forge.trackable.TrackableTypes.TrackableType;
import forge.trackable.Tracker;
import forge.util.FSerializableFunction;
import forge.util.ITriggerEvent;
import forge.util.Localizer;
import forge.web.PlayerSettings;
import forge.web.Options;
import forge.web.images.Foil;
import forge.web.protocol.BrowserChannel;
import forge.web.protocol.Commands;
import forge.web.protocol.Commands.Handler;
import forge.web.protocol.FromBrowser.*;
import forge.web.protocol.PropertyJson;
import forge.web.protocol.ToBrowser;
import forge.web.protocol.ToBrowser.*;
import forge.web.protocol.MessageJson;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;
import java.util.stream.IntStream;

/**
 * One player's view of a match for the browser. Game calls run on one dispatch thread. Browser replies arrive on the socket thread.
 * The waiting timer also changes the prompt from the host UI thread.
 * This class sends the browser the game's full state and then each change, with the events and sounds the change caused. It also shows the
 * prompt, asks the game's questions and waits for the answers, keeps the phase stops and yields, and runs the browser's commands.
 */
public class WebGuiGame extends NetworkGuiGame {
    public static final Commands<WebGuiGame> COMMANDS = new Commands<WebGuiGame>()
            .on(Reply.class, (g, ch, r) -> g.requests.complete(r.id(), r.value()))
            .on(KeyCommand.class, WebGuiGame::keyCommand)
            .on(SelectCard.class, play((g, controller, click) -> {
                final CardView card = g.lookup(click.key(), TrackableTypes.CardViewType);
                if (card != null) {
                    final BrowserClick at = new BrowserClick(click.menu(), click.x(), click.y());
                    g.lastClick.set(at);
                    controller.selectCard(card, null, at);
                }
            }))
            .on(Plain.ok, play((g, controller, c) -> controller.selectButtonOk()))
            .on(Plain.cancel, play((g, controller, c) -> controller.selectButtonCancel()))
            .on(Plain.concede, play((g, controller, c) -> controller.concede()))
            .on(DrawOfferCommand.class, play((g, controller, d) -> controller.drawOfferAction(d.action())))
            .on(AutoDecisionCommand.class, play((g, controller, a) -> g.autoDecisions(controller, a)))
            .on(Plain.endTurn, play((g, controller, c) -> {
                YieldController.endTurn(controller, g.getCurrentPlayer());
                // The yield is set here at once, so the chip beside the phase pill shows it immediately
                g.send(g.controlsMessage());
            }))
            .on(Plain.stopYield, play((g, controller, c) -> {
                // Ends whichever pass is running. It comes from a second press of End Turn, or a click on the chip that names the pass
                YieldController.stopYielding(controller, g.getCurrentPlayer(), g);
                g.send(g.controlsMessage());
            }))
            .on(Plain.undo, play((g, controller, c) -> controller.undoLastAction()))
            // The host records the player's actions and asks how many times to repeat them, as desktop's dock buttons do
            .on(Plain.macroRecord, play((g, controller, c) -> controller.macros().setRememberedActions()))
            .on(Plain.macroRepeat, play((g, controller, c) -> controller.macros().repeatRememberedActions()))
            .on(Plain.autoPass, play((g, controller, c) -> {
                Options.set(g.settings, controller, "autoPassNoActions", String.valueOf(!g.settings.getBoolean(FPref.YIELD_AUTO_PASS_NO_ACTIONS)));
                g.send(g.controlsMessage());
            }))
            .on(PhaseCommand.class, play((g, controller, p) -> {
                if (p.t() == PhaseAction.toggleStop) {
                    g.toggleStop(p.phase(), p.mine());
                    g.send(g.controlsMessage());
                } else {
                    g.toggleMarker(p.phase(), p.mine());
                }
            }))
            .on(UseMana.class, play((g, controller, m) -> controller.useMana(m.color())))
            .on(StackYield.class, play((g, controller, yield) -> {
                final StackItemView item = g.lookup(yield.key(), TrackableTypes.StackItemViewType);
                if (item != null && yield.action() != null) {
                    g.stackYield(controller, item, yield.action());
                    g.send(g.stackMenuMessage(controller, item));
                }
            }))
            .on(AutoYieldTop.class, play((g, controller, y) -> g.autoYieldTop(controller, y.yes())))
            .on(NextGame.class, play((g, controller, n) -> controller.nextGameDecision(n.decision())));
    private final ReentrantLock mirrorLock = new ReentrantLock();
    private final ExecutorService dispatch = Executors.newSingleThreadExecutor(
            new ThreadFactoryBuilder().setNameFormat("WebClient").setDaemon(true).build());
    private volatile boolean seesAll;
    /** Every card key the browser was sent in this game. The visible list is chosen from these. */
    private final Set<Integer> cardKeys = new LinkedHashSet<>();
    private List<Integer> localPlayerKeys = List.of();
    private final MatchQuestions requests = new MatchQuestions(this::send,
            () -> getCurrentPlayer() == null ? "a player" : getCurrentPlayer().getName());
    private final DeltaSyncManager snapshotter = new DeltaSyncManager();
    private final AtomicInteger skippedProperties = new AtomicInteger();
    /** Netplay drops a click's trigger event, so getAbilityToPlay reads the click from here. */
    private final AtomicReference<BrowserClick> lastClick = new AtomicReference<>();
    private final ShownZones zones = new ShownZones(this::send, this::isLocalPlayer);
    private final PromptState prompt = new PromptState(p -> {
        zones.flush();
        send(p);
    });
    /** A number the browser gave that was not in the list offered. It is kept for the typed prompt that follows. */
    private Integer typedNumber;
    /** Written on the dispatch thread. The socket thread resends it to a reloading browser. */
    private final WebGameLog gameLog;
    private final BrowserSounds sounds = new BrowserSounds(this::getCurrentPlayer);
    /** The settings of the player this GUI shows the match to. Only the host's settings are the shared preferences. */
    private final PlayerSettings settings;
    private volatile BrowserChannel browser;
    private volatile boolean gameOver;
    /** Each player's games won in the match. It is read from the engine's outcome of the game that just ended. */
    private volatile List<MatchScore> score = List.of();
    // The last of each that the browser was sent. A reloading browser gets them again, because they are sent only when they change
    private volatile Playable playable;
    private volatile ToBrowser.DrawOffer drawOffer;
    /** Run once, when this seat's game ends. */
    private volatile Runnable onGameOver;
    /** What the game did since the last state message, in order. Only the dispatch thread fills and empties it. */
    private final List<Record> events = new ArrayList<>();
    /** Run when the game opens. For a guest, the host decides when that happens. */
    private volatile Runnable onOpen = () -> { };

    public void onGameOver(final Runnable listener) {
        onGameOver = listener;
    }

    public WebGuiGame(final PlayerSettings settings) {
        this.settings = settings;
        this.gameLog = new WebGameLog(this::mayView, settings);
    }

    /** Answers any open request with its default, stops the waiting timer and shuts down the dispatch thread. Called when this GUI is no longer used. */
    public void close() {
        cancelAwaitNextInput();
        // A question the browser can no longer answer would keep the dispatch thread waiting forever
        requests.cancelAll();
        dispatch.shutdown();
    }

    /**
     * Turns dev mode's "see every card" on or off for this seat. The host's player controller holds the same switch, but this GUI
     * is a netplay client, and a netplay client's controller always answers that it may not look at every card.
     */
    public void seeAll(final boolean on) {
        seesAll = on;
    }

    @Override
    public boolean mayView(final CardView c) {
        return seesAll || super.mayView(c);
    }

    /** Sends the whole board again, so a change in which cards this seat may see reaches the browser. */
    public void resendState() {
        dispatchExecutor().execute(() -> {
            final JsonObject state = fullState();
            if (state != null) {
                send(state);
            }
        });
    }

    public Executor dispatchExecutor() {
        return task -> dispatch.execute(() -> {
            mirrorLock.lock();
            try {
                task.run();
            } catch (final RuntimeException e) {
                netLog.error(e, "Web client call failed");
            } finally {
                mirrorLock.unlock();
            }
        });
    }

    /** Holds the mirror lock, so no delta reaches the browser before the full state that holds it. A waiting request releases that lock. */
    public void attach(final BrowserChannel channel) {
        mirrorLock.lock();
        try {
            browser = channel;
            final JsonObject state = fullState();
            if (state != null) {
                channel.send(state);
            }
            final Playable p = playable;
            if (p != null) {
                channel.send(p);
            }
            final ToBrowser.DrawOffer d = drawOffer;
            if (d != null) {
                channel.send(d);
            }
            prompt.sendTo(channel::send);
            zones.sendTo(channel::send);
            channel.send(controlsMessage());
            channel.send(gameLog.all());
            requests.replay(channel::send);
            if (gameOver) {
                channel.send(new GameOver(score));
            }
        } finally {
            mirrorLock.unlock();
        }
    }

    /** Stops sending to that browser, unless another browser was attached since. */
    public void detach(final BrowserChannel channel) {
        if (browser == channel) {
            browser = null;
        }
    }

    private void send(final JsonObject message) {
        final BrowserChannel b = browser;
        if (b != null) {
            b.send(message);
        }
    }

    private void send(final Record message) {
        send(MessageJson.encode(message));
    }

    /** How many properties were dropped because they have no JSON form. Tests check that it stays at zero. */
    int skippedProperties() {
        return skippedProperties.get();
    }

    /** The whole game as one state message, built from this client's game view. Null with no game or a failed snapshot. Call while holding the mirror lock. */
    JsonObject fullState() {
        final Map<Integer, Map<TrackableProperty, Object>> snapshot = snapshotter.snapshot(getGameView());
        if (snapshot == null) {
            return null;
        }
        final Map<Integer, JsonObject> objects = encode(snapshot);
        cardKeys.clear();
        addCardKeys(objects);
        return stateMessage(true, -1, objects, Map.of(), List.of());
    }

    private void addCardKeys(final Map<Integer, JsonObject> objects) {
        for (final int key : objects.keySet()) {
            if (DeltaPacket.getTypeFromDeltaKey(key) == DeltaPacket.TYPE_CARD_VIEW) {
                cardKeys.add(key);
            }
        }
    }

    private JsonObject stateMessage(final boolean full, final long seq, final Map<Integer, JsonObject> newObjects,
            final Map<Integer, JsonObject> deltas, final List<Record> happened) {
        return MessageJson.encode(new StateMessage(full, seq, rootKey(getGameView()), byKey(newObjects), byKey(deltas),
                visibleCardKeys(), localPlayerKeys, happened));
    }

    private static Map<String, JsonObject> byKey(final Map<Integer, JsonObject> objects) {
        final Map<String, JsonObject> out = new LinkedHashMap<>();
        objects.forEach((k, v) -> out.put(String.valueOf(k), v));
        return out;
    }

    private Map<Integer, JsonObject> encode(final Map<Integer, Map<TrackableProperty, Object>> objects) {
        return PropertyJson.encodeAll(objects, name -> {
            skippedProperties.incrementAndGet();
            netLog.warn("Web client: no JSON form for property {}", name);
        });
    }

    /** Moves the game view of a later game under the key of the client's one game view. The browser reads only that key. */
    static Map<Integer, Map<TrackableProperty, Object>> asRootGame(final Map<Integer, Map<TrackableProperty, Object>> objects,
            final int root) {
        Map<Integer, Map<TrackableProperty, Object>> out = objects;
        for (final Map.Entry<Integer, Map<TrackableProperty, Object>> e : objects.entrySet()) {
            final int key = e.getKey();
            if (key != root && DeltaPacket.getTypeFromDeltaKey(key) == DeltaPacket.TYPE_GAME_VIEW) {
                if (out == objects) {
                    out = new LinkedHashMap<>(objects);
                }
                out.remove(key);
                out.merge(root, e.getValue(), (was, now) -> {
                    final Map<TrackableProperty, Object> both = new HashMap<>(was);
                    both.putAll(now);
                    return both;
                });
            }
        }
        return out;
    }

    private static int rootKey(final GameView gv) {
        return DeltaPacket.makeDeltaKey(gv);
    }

    @Override
    public void setGameView(final GameView gameView) {
        super.setGameView(gameView);
        if (getGameView() == null) {
            return;
        }
        final JsonObject state = fullState();
        if (state == null) {
            // A failed snapshot must never clear the browser. The old state is more accurate than an empty one
            netLog.warn("Web client: game view snapshot failed; keeping the previous browser state");
            return;
        }
        gameOver = false;
        score = List.of();
        playable = null;
        drawOffer = null;
        // A full new state has no earlier state to animate from, so the queued events are dropped
        events.clear();
        send(state);
    }

    @Override
    public void applyDelta(final DeltaPacket packet) {
        super.applyDelta(packet);
        final GameView gv = getGameView();
        if (packet == null || gv == null) {
            return;
        }
        final int root = rootKey(gv);
        final Map<Integer, JsonObject> newObjects = encode(asRootGame(packet.getNewObjects(), root));
        final Map<Integer, JsonObject> deltas = encode(asRootGame(packet.getObjectDeltas(), root));
        if (!newObjects.isEmpty() || !deltas.isEmpty() || !events.isEmpty()) {
            addCardKeys(newObjects);
            final List<Record> happened = List.copyOf(events);
            events.clear();
            send(stateMessage(false, packet.getSequenceNumber(), newObjects, deltas, happened));
        }
        if (gv.isGameOver() && !gameOver) {
            // Replaces finishGame from FControlGameEventHandler, which does not run here
            gameOver = true;
            send(new GameOver(score));
            final Runnable listener = onGameOver;
            if (listener != null) {
                listener.run();
            }
        }
    }

    private List<Integer> visibleCardKeys() {
        final Tracker tracker = getGameView().getTracker();
        final List<Integer> visible = new ArrayList<>();
        if (tracker == null) {
            return visible;
        }
        // A card the local player may play from another zone counts as visible. This includes another player's card or a face-down card
        final Set<Integer> playable = new HashSet<>();
        for (final PlayerView p : getLocalPlayers()) {
            p.getCards(ZoneType.Flashback).forEach(c -> playable.add(c.getId()));
        }
        for (final int key : cardKeys) {
            final CardView card = tracker.getObj(TrackableTypes.CardViewType, DeltaPacket.getIdFromDeltaKey(key));
            if (card != null && (mayView(card) || playable.contains(card.getId()))) {
                visible.add(key);
            }
        }
        return visible;
    }

    /** Sets what runs on the dispatch thread once openView has sent this seat's first state. */
    public void whenOpened(final Runnable action) {
        onOpen = action;
    }

    @Override
    public void openView(final TrackableCollection<PlayerView> myPlayers) {
        final List<Integer> keys = new ArrayList<>();
        for (final PlayerView p : myPlayers) {
            keys.add(DeltaPacket.makeDeltaKey(p));
        }
        localPlayerKeys = keys;
        // Each game has a new player controller, which starts with "see every card" off
        seesAll = false;
        final JsonObject state = fullState();
        if (state != null) {
            send(state);
        }
        // For a remote seat, the host skips phases only by the state the client seeds (PlayerControllerHuman.isUiSetToSkipPhase)
        seedYieldStateOnHost();
        // The seed reads the shared preferences, so this player's own settings are applied after it
        final IGameController controller = getGameController();
        if (controller != null) {
            Options.applyAll(settings, controller);
        }
        onOpen.run();
    }

    @Override
    public boolean isUiSetToSkipPhase(final PlayerView playerTurn, final PhaseType phase) {
        // The first phase has no stop of its own
        return phase.ordinal() > 0 && !settings.getBoolean(Options.stopKey(phase, isLocalPlayer(playerTurn)));
    }

    @Override
    public void handleGameEvent(final GameEvent event) {
        // FControlGameEventHandler would post to the host UI thread, so the log, sounds and animations are sent from here
        if (event instanceof GameEventGameOutcome outcome) {
            score = matchScore(outcome.matchSummary());
            // The outcome can arrive after the game is over, so the game-over message is sent again with the score
            if (gameOver) {
                send(new GameOver(score));
            }
        }
        final GameView gv = getGameView();
        final Record forwarded = BrowserEvents.forwarded(event, gv == null ? null : gv.getTracker());
        if (forwarded != null) {
            events.add(forwarded);
        }
        final GameLog log = gv == null ? null : gv.getGameLog();
        if (log == null) {
            return;
        }
        log.getEventVisitor().recieve(event);
        final ToBrowser.LogMessage entries = gameLog.added(log);
        if (entries != null) {
            send(entries);
        }
        if (settings.getBoolean(FPref.UI_ENABLE_SOUNDS) && settings.getInt(FPref.UI_VOL_SOUNDS) > 0) {
            final Sound sound = sounds.soundFor(withKnownCard(event, gv));
            if (sound != null) {
                send(sound);
            }
        }
    }

    /** The event for a moved card holds only its name and picture, so the sound is chosen from this client's copy of the card. */
    private static GameEvent withKnownCard(final GameEvent event, final GameView gv) {
        if (!(event instanceof GameEventZone zone) || zone.card() == null) {
            return event;
        }
        final CardView known = gv.getTracker().getObj(TrackableTypes.CardViewType, zone.card().getId());
        return known == null ? event : new GameEventZone(zone.zoneType(), zone.player(), zone.mode(), known, zone.sa());
    }

    /**
     * Finds each seated player's name in the engine's match summary ("Alice: 1 Bob: 0 "). A name that contains a colon or space is still read whole.
     * The outcome event carries only this untranslated summary. Every web seat runs in the host's process, so its names match the player views.
     */
    private List<MatchScore> matchScore(final String summary) {
        final GameView gv = getGameView();
        if (gv == null || summary == null) {
            return List.of();
        }
        final List<MatchScore> out = new ArrayList<>();
        for (final PlayerView p : gv.getPlayers()) {
            final String name = p.getName() + ": ";
            final int at = (" " + summary).indexOf(" " + name);
            if (at < 0) {
                continue;
            }
            int end = at + name.length();
            while (end < summary.length() && Character.isDigit(summary.charAt(end))) {
                end++;
            }
            if (end > at + name.length()) {
                out.add(new MatchScore(Ref.player(p.getId()), Integer.parseInt(summary.substring(at + name.length(), end))));
            }
        }
        return out;
    }

    // Phase stops use the desktop preference keys. One row is for the local player's turns and one for all other turns
    private Controls controlsMessage() {
        final IGameController controller = getGameController();
        final YieldController yields = controller == null ? null : controller.getYieldController();
        final YieldMarker marker = yields == null ? null : yields.getAutoPassUntilMarker();
        return new Controls(Options.stops(settings, FPref.PHASES_HUMAN), Options.stops(settings, FPref.PHASES_AI),
                settings.getBoolean(FPref.YIELD_AUTO_PASS_NO_ACTIONS), getDayTime(),
                marker == null ? null : new TurnMarker(marker.getPhase(), isLocalPlayer(marker.getPhaseOwner())),
                yields != null && yields.autoPassUntilEndOfTurn(), yields != null && yields.autoPassUntilStackEmpty(),
                Options.values(settings));
    }

    @Override
    public void refreshYieldUi(final PlayerView player) {
        send(controlsMessage());
    }

    @Override
    public void updateDayTime(final String daytime) {
        super.updateDayTime(daytime);
        send(controlsMessage());
    }

    private void toggleStop(final PhaseType phase, final boolean mine) {
        if (phase.ordinal() > 0) {
            setStop(phase, mine, !settings.getBoolean(Options.stopKey(phase, mine)));
        }
    }

    private void setStop(final PhaseType phase, final boolean mine, final boolean stop) {
        settings.set(Options.stopKey(phase, mine), stop);
        settings.save();
        pushStop(phase, mine);
    }

    /** Tells the game a stop changed. A stop set before the game has players is read when the players are seeded instead. */
    private void pushStop(final PhaseType phase, final boolean mine) {
        if (getGameView() == null) {
            return;
        }
        for (final PlayerView p : getGameView().getPlayers()) {
            if (isLocalPlayer(p) == mine) {
                pushSkipPhaseToControllers(p, phase);
            }
        }
    }

    // Desktop's right-click on a phase passes priority until that phase. The opponents' row sets the marker for the first opponent
    private void toggleMarker(final PhaseType phase, final boolean mine) {
        if (phase.ordinal() == 0) {
            return;
        }
        PlayerView owner = mine ? getCurrentPlayer() : null;
        for (final PlayerView p : getGameView().getPlayers()) {
            if (owner == null && !isLocalPlayer(p)) {
                owner = p;
            }
        }
        if (owner != null) {
            // A marker only fires at a phase the player stops at
            handleYieldMarkerToggle(owner, phase, () -> setStop(phase, mine, true));
        }
    }

    // The choices of desktop's right-click menu on a stack item
    private StackMenu stackMenuMessage(final IGameController controller, final StackItemView item) {
        final String yieldKey = item.getKey();
        return new StackMenu(DeltaPacket.makeDeltaKey(item),
                item.isAbility() ? controller.shouldAutoYield(yieldKey) : null,
                answersTrigger(item) ? controller.getTriggerDecision(yieldKey) : null);
    }

    private boolean answersTrigger(final StackItemView item) {
        return item.isOptionalTrigger() && isLocalPlayer(item.getActivatingPlayer()) && !item.getKey().isEmpty();
    }

    /** The Always Yes and Always No shortcuts, handled as on desktop. The player yields to the ability on top of the stack. Its trigger is also answered if it is the player's to answer. */
    private void autoYieldTop(final IGameController controller, final boolean yes) {
        final StackItemView item = getGameView().peekStack();
        if (item == null || !item.isAbility()) {
            return;
        }
        final boolean abilityScope = controller.getYieldController().isAbilityScope();
        if (answersTrigger(item)) {
            controller.setTriggerDecision(item.getKey(), yes ? TriggerDecision.ACCEPT : TriggerDecision.DECLINE, abilityScope);
        }
        controller.setShouldAutoYield(item.getKey(), true, abilityScope);
    }

    private void stackYield(final IGameController controller, final StackItemView item, final YieldAction action) {
        final String yieldKey = item.getKey();
        final boolean abilityScope = controller.getYieldController().isAbilityScope();
        // A key press arrives here for any stack item. The menu offers these two only for the player's own optional trigger
        if ((action == YieldAction.alwaysYes || action == YieldAction.alwaysNo) && !answersTrigger(item)) {
            return;
        }
        switch (action) {
            case autoYield -> controller.setShouldAutoYield(yieldKey, !controller.shouldAutoYield(yieldKey), abilityScope);
            case alwaysYes -> controller.setTriggerDecision(yieldKey,
                    controller.getTriggerDecision(yieldKey) == TriggerDecision.ACCEPT ? TriggerDecision.ASK : TriggerDecision.ACCEPT, abilityScope);
            case alwaysNo -> controller.setTriggerDecision(yieldKey,
                    controller.getTriggerDecision(yieldKey) == TriggerDecision.DECLINE ? TriggerDecision.ASK : TriggerDecision.DECLINE, abilityScope);
            case yieldToStack, yieldToEntireStack -> {
                final PlayerView local = getCurrentPlayer();
                if (local != null) {
                    controller.sendYieldUpdate(new YieldUpdate.StackYield(local, true, action == YieldAction.yieldToStack));
                }
            }
        }
    }

    @Override
    protected void updateCurrentPlayer(final PlayerView player) {
    }

    @Override
    public void showPromptMessage(final PlayerView playerView, final String message, final CardView card) {
        cancelAwaitNextInput();
        prompt.message(message, card);
    }

    @Override
    public void updateButtons(final PlayerView owner, final String label1, final String label2, final boolean enable1, final boolean enable2, final boolean focus1) {
        prompt.buttons(label1, label2, enable1, enable2, focus1);
    }

    /**
     * A choice among cards opens the zones that hold them, as on desktop. A maximum of 0 only highlights the cards. A new pick
     * in the same choice has the same cards, so their zones are left as they are. Zones change before the prompt does, because
     * the prompt sends the zones the last choice closed, and this choice may reopen them.
     */
    @Override
    public void setSelection(final Selection selection) {
        final Selection before = getSelection();
        super.setSelection(selection);
        if (!selection.cards().equals(before.cards()) || selection.max() != before.max()) {
            if (selection.max() > 0) {
                zones.showForSelection(getZonesHolding(selection.cards()));
            } else {
                zones.hideSelection();
            }
        }
        if (!selection.actionable().equals(before.actionable())) {
            sendPlayable(selection.actionable());
        }
        prompt.selection(selection);
    }

    // Cards the engine says the player can act on now. Cards at strength two or more are the ones the Auto button would tap
    private void sendPlayable(final List<CardView> cards) {
        final List<Ref> playable = new ArrayList<>();
        final List<Ref> autoTap = new ArrayList<>();
        for (final CardView c : Sets.newHashSet(cards)) {
            playable.add(PromptState.cardRef(c));
            if (getActionableStrength(c) >= 2) {
                autoTap.add(PromptState.cardRef(c));
            }
        }
        this.playable = new Playable(playable, autoTap);
        send(this.playable);
    }

    @Override
    public void showRevealedCards(final Iterable<CardView> cards) {
        // The host sends every reveal here. Only an opponent's hand is opened. Other reveals also come as a reveal dialog
        final List<PlayerZoneUpdate> hands = new ArrayList<>();
        for (final PlayerZoneUpdate update : getZonesHolding(cards)) {
            if (update.getZones().contains(ZoneType.Hand) && !isLocalPlayer(update.getPlayer())) {
                hands.add(new PlayerZoneUpdate(update.getPlayer(), ZoneType.Hand));
            }
        }
        zones.showRevealed(hands);
    }

    @Override
    public void hideRevealedCards() {
        zones.hideRevealed();
    }

    @Override
    public void setCard(final CardView card) {
        prompt.card(card);
    }

    @Override
    public void flashIncorrectAction() {
        send(new Flash());
    }

    @Override
    public void message(final String message, final String title) {
        send(new Notice(title, message, false));
    }

    @Override
    public void showErrorDialog(final String message, final String title) {
        send(new Notice(title, message, true));
    }

    @Override public void showCombat() { }
    @Override public void finishGame() { }
    @Override public void alertUser() { }
    @Override public void setPanelSelection(final CardView hostCard) { }
    @Override public void setPlayerAvatar(final LobbyPlayer player, final IHasIcon ihi) { }

    // While the player decides, the mirror lock is released so browser commands and a reconnecting browser can still run
    private JsonElement ask(final Record request, final Predicate<JsonElement> valid) {
        zones.flush();
        final int holds = mirrorLock.getHoldCount();
        for (int i = 0; i < holds; i++) {
            mirrorLock.unlock();
        }
        try {
            return requests.await(request, valid);
        } finally {
            for (int i = 0; i < holds; i++) {
                mirrorLock.lock();
            }
        }
    }

    private boolean isInMirror(final CardView card) {
        return lookup(DeltaPacket.makeDeltaKey(card), TrackableTypes.CardViewType) == card;
    }

    private <T> List<RequestOption> options(final List<T> items, final FSerializableFunction<T, String> display) {
        final List<RequestOption> out = new ArrayList<>();
        for (final T item : items) {
            final String label = display != null ? display.apply(item) : String.valueOf(item);
            if (item instanceof CardView card) {
                // Temporary views (pile splits, getCardForUi copies, LKI) have no mirror entry to refer to
                out.add(isInMirror(card)
                        ? new RequestOption(label, PromptState.cardRef(card), null, null, null, null, null)
                        : new RequestOption(label, null, card.getName(),
                                card.getCurrentState() == null ? null : Foil.key(card, card.getCurrentState()), null, null, null));
            } else if (item instanceof PlayerView player) {
                out.add(new RequestOption(label, null, null, null, Ref.player(player.getId()), null, null));
            } else if (item instanceof PaperCard paper) {
                out.add(new RequestOption(label, null, paper.getName(), Foil.key(paper), null, null, null));
            } else if (item instanceof CardFaceView face) {
                // Naming a card offers every card in the game, so the names are a list of words and only a hovered one loads its picture
                out.add(new RequestOption(label, null, null, ImageKeys.CARD_PREFIX + face.getName(), null, null, null));
            } else if (item instanceof SpellAbilityView ability && ability.getHostCard() != null && isInMirror(ability.getHostCard())) {
                out.add(new RequestOption(label, null, null, null, null, PromptState.cardRef(ability.getHostCard()), null));
            } else {
                // A colour is offered as a colour value or, by some effects, as its English name
                final MagicColor.Color colour = item instanceof MagicColor.Color c ? c
                        : item instanceof String s ? MagicColor.Color.fromName(s) : null;
                out.add(new RequestOption(label, null, null, null, null, null, colour == null ? null : colour.getShortName()));
            }
        }
        return out;
    }

    private void revealFirst(final DelayedReveal reveal) {
        if (reveal == null) {
            return;
        }
        final String prefix = reveal.getMessagePrefix() == null ? "" : reveal.getMessagePrefix();
        getChoices(prefix, -1, -1, new ArrayList<>(reveal.getCards()), null, null);
    }

    @Override
    public <T> List<T> getChoices(final String message, final int min, final int max, final List<T> choices, final List<T> selected, final FSerializableFunction<T, String> display) {
        if (min < 0 && max < 0) {
            // AbstractGuiGame.reveal only displays the cards. The return value is ignored
            ask(choicesRequest(ChoiceKind.reveal, message, min, max, choices, selected, display, null, null, List.of()), v -> true);
            return new ArrayList<>();
        }
        if (asksOptionalCosts(message)) {
            return askCosts(message, choices);
        }
        final int need = Math.min(Math.max(min, 0), choices.size());
        // Choices the host labels, such as stickers offered by their place in a list, are read as labels, not as numbers
        final List<T> number = max == 1 && display == null ? askNumber(message, need == 0, choices) : null;
        if (number != null) {
            return number;
        }
        // Spells being chosen are already drawn on the stack, so the player picks them there instead of from a list
        final ChoicesRequest request = choicesRequest(ChoiceKind.choices, message, need, max, choices, selected, display,
                stackKeysFor(choices), null, IntStream.range(0, need).boxed().toList());
        return Answers.pick(choices, ask(request, Answers.indexList(choices.size(), need, max)));
    }

    /** Asks for a number when the choices count up by one. A last entry may mean any larger number. Returns null when the choices do not count up. */
    private <T> List<T> askNumber(final String message, final boolean maySkip, final List<T> choices) {
        final int last = choices.size() - 1;
        final boolean open = last > 0 && choices.get(last) instanceof String;
        final int count = open ? last : choices.size();
        if (count < 2 || !(choices.get(0) instanceof Integer first)) {
            return null;
        }
        for (int i = 1; i < count; i++) {
            if (!Integer.valueOf(first + i).equals(choices.get(i))) {
                return null;
            }
        }
        final Integer top = open ? null : Integer.valueOf(first + count - 1);
        final JsonElement reply = ask(new NumberRequest(message, first, top, maySkip, maySkip ? null : first), v -> {
            if (v.isJsonNull()) {
                return maySkip;
            }
            return v.isJsonPrimitive() && v.getAsString().matches("-?\\d{1,9}") && v.getAsInt() >= first && (open || v.getAsInt() <= top);
        });
        if (reply.isJsonNull()) {
            return new ArrayList<>();
        }
        final int n = reply.getAsInt();
        if (n - first < count) {
            return Lists.newArrayList(choices.get(n - first));
        }
        typedNumber = n;
        return Lists.newArrayList(choices.get(last));
    }

    /** The stack item key of each choice, in the same order. Null unless every choice is a different spell on the stack. */
    private <T> List<Integer> stackKeysFor(final List<T> choices) {
        final GameView gv = getGameView();
        if (gv == null || gv.getStack() == null || choices.isEmpty()) {
            return null;
        }
        final List<Integer> keys = new ArrayList<>();
        for (final T choice : choices) {
            Integer found = null;
            if (choice instanceof StackItemView target) {
                found = DeltaPacket.makeDeltaKey(target);
            } else if (choice instanceof SpellAbilityView spell && spell.getHostCard() != null) {
                for (final StackItemView item : gv.getStack()) {
                    if (item.getSourceCard() != null && item.getSourceCard().getId() == spell.getHostCard().getId()) {
                        found = DeltaPacket.makeDeltaKey(item);
                        break;
                    }
                }
            }
            // Two abilities of one card are one item on the stack. A click could not tell them apart
            if (found == null || keys.contains(found)) {
                return null;
            }
            keys.add(found);
        }
        return keys;
    }

    private <T> ChoicesRequest choicesRequest(final ChoiceKind kind, final String message, final int min, final int max,
            final List<T> items, final List<T> selected, final FSerializableFunction<T, String> display,
            final List<Integer> stackKeys, final BrowserClick at, final List<Integer> onDefault) {
        return new ChoicesRequest(kind, message, min, max, options(items, display), Answers.indicesOf(items, selected), stackKeys,
                at == null ? null : at.x(), at == null ? null : at.y(), onDefault);
    }

    /** Whether the host is asking for a spell's optional costs, told by its title because every web seat uses the host's Localizer. */
    private static boolean asksOptionalCosts(final String title) {
        return Localizer.getInstance().getMessage("lblChooseOptionalCosts").equals(title);
    }

    /** Asks for any of a spell's optional costs, where none is an answer because the spell is already being cast. */
    private <T> List<T> askCosts(final String message, final List<T> costs) {
        final ChoicesRequest request = choicesRequest(ChoiceKind.costs, message, 0, costs.size(), costs, null, null, null, null, List.of());
        return Answers.pick(costs, ask(request, Answers.indexList(costs.size(), 0, costs.size())));
    }

    /** Asks the browser to pick from a list. {@code onDefault} is the answer used when the browser cannot answer. */
    private <T> List<T> askChoices(final String message, final int min, final int max, final List<T> items,
            final List<T> selected, final FSerializableFunction<T, String> display, final List<Integer> onDefault) {
        final ChoicesRequest request = choicesRequest(ChoiceKind.choices, message, min, max, items, selected, display,
                null, null, onDefault);
        return Answers.pick(items, ask(request, Answers.indexList(items.size(), min, max)));
    }

    @Override
    public <T> OrderResult<T> order(final String title, final String top, final int remainingObjectsMin, final int remainingObjectsMax, final List<T> sourceChoices, final List<T> destChoices, final CardView referenceCard, final boolean sideboardingMode, final boolean showRememberCheckbox) {
        // The host asks for two or more optional costs as an order with no order to choose
        if (asksOptionalCosts(title) && (destChoices == null || destChoices.isEmpty())) {
            return new OrderResult<>(askCosts(title, sourceChoices), false);
        }
        final List<T> items = new ArrayList<>(sourceChoices);
        if (destChoices != null) {
            items.addAll(destChoices);
        }
        final int remainingMax = remainingObjectsMax < 0 ? items.size() : Math.min(remainingObjectsMax, items.size());
        final int min = Math.max(0, items.size() - remainingMax);
        final int max = Math.max(min, items.size() - Math.max(remainingObjectsMin, 0));
        final List<Integer> preselected = destChoices == null || destChoices.isEmpty() ? IntStream.range(0, min).boxed().toList()
                : IntStream.range(sourceChoices.size(), items.size()).boxed().toList();
        // The engine labels a library, graveyard or planar deck order at its top, and the page labels the bottom. Web seats share the host's Localizer
        final Localizer loc = Localizer.getInstance();
        final String bottom = loc.getMessage("lblClosestToTop").equals(top) ? loc.getMessage("lblClosestToBottom")
                : loc.getMessage("lblClosestToBottom").equals(top) ? loc.getMessage("lblClosestToTop") : null;
        final OrderRequest request = new OrderRequest(title, top, bottom, min, max, options(items, null), preselected,
                showRememberCheckbox, PromptState.cardRef(referenceCard), new OrderAnswer(preselected, false));
        final Predicate<JsonElement> indicesOk = Answers.indexList(items.size(), min, max);
        final JsonElement reply = ask(request, v -> v.isJsonObject() && v.getAsJsonObject().has("indices") && indicesOk.test(v.getAsJsonObject().get("indices")));
        final JsonObject r = reply.getAsJsonObject();
        return new OrderResult<>(Answers.pick(items, r.get("indices")), r.get("remember") instanceof JsonPrimitive p && p.isBoolean() && p.getAsBoolean());
    }

    @Override
    public List<CardView> manipulateCardList(final String title, final Iterable<CardView> cards, final Iterable<CardView> manipulable, final boolean toTop, final boolean toBottom, final boolean toAnywhere) {
        final List<CardView> list = Lists.newArrayList(cards);
        // The original order is always a valid answer. arrangeForMove throws on an empty list
        final ManipulateRequest request = new ManipulateRequest(title, options(list, null),
                Answers.indicesOf(list, Lists.newArrayList(manipulable)), toTop, toBottom, IntStream.range(0, list.size()).boxed().toList());
        return Answers.pick(list, ask(request, Answers.indexList(list.size(), list.size(), list.size())));
    }

    private int askOption(final String title, final String message, final CardView card, final List<String> labels, final int defaultIndex) {
        final int def = Math.max(0, Math.min(defaultIndex, labels.size() - 1));
        final OptionRequest request = new OptionRequest(title, message, card != null && isInMirror(card) ? PromptState.cardRef(card) : null,
                new ArrayList<>(labels), def);
        return ask(request, Answers.singleIndex(labels.size())).getAsInt();
    }

    @Override
    public boolean confirm(final CardView c, final String question, final boolean defaultIsYes, final List<String> options) {
        final Localizer loc = Localizer.getInstance();
        final List<String> labels = options == null || options.size() < 2 ? List.of(loc.getMessage("lblYes"), loc.getMessage("lblNo")) : options;
        return askOption("", question, c, labels, defaultIsYes ? 0 : 1) == 0;
    }

    @Override
    public boolean showConfirmDialog(final String message, final String title, final String yesButtonText, final String noButtonText, final boolean defaultYes) {
        return askOption(title, message, null, List.of(yesButtonText, noButtonText), defaultYes ? 0 : 1) == 0;
    }

    @Override
    public int showOptionDialog(final String message, final String title, final FSkinProp icon, final List<String> options, final int defaultOption) {
        return askOption(title, message, null, options, defaultOption);
    }

    @Override
    public String showInputDialog(final String message, final String title, final FSkinProp icon, final String initialInput, final List<String> inputOptions, final boolean isNumeric) {
        if (isNumeric && typedNumber != null) {
            final String typed = typedNumber.toString();
            typedNumber = null;
            return typed;
        }
        if (inputOptions != null && !inputOptions.isEmpty()) {
            return inputOptions.get(askOption(title, message, null, inputOptions, Math.max(0, inputOptions.indexOf(initialInput))));
        }
        final JsonElement reply = ask(new TextRequest(title, message, initialInput, isNumeric, initialInput), v -> {
            if (v.isJsonNull()) {
                return true;
            }
            if (!v.isJsonPrimitive()) {
                return false;
            }
            return !isNumeric || v.getAsString().matches("-?\\d+");
        });
        return reply.isJsonNull() ? null : reply.getAsString();
    }

    @Override
    public GameEntityView chooseSingleEntityForEffect(final String title, final List<? extends GameEntityView> optionList, final DelayedReveal delayedReveal, final boolean isOptional) {
        revealFirst(delayedReveal);
        final List<GameEntityView> list = new ArrayList<>(optionList);
        final int need = isOptional || list.isEmpty() ? 0 : 1;
        final List<GameEntityView> picked = askChoices(title, need, 1, list, null, null, IntStream.range(0, need).boxed().toList());
        return picked.isEmpty() ? null : picked.get(0);
    }

    @Override
    public List<GameEntityView> chooseEntitiesForEffect(final String title, final List<? extends GameEntityView> optionList, final int min, final int max, final DelayedReveal delayedReveal) {
        revealFirst(delayedReveal);
        final List<GameEntityView> list = new ArrayList<>(optionList);
        final int need = Math.min(Math.max(min, 0), list.size());
        return askChoices(title, need, max, list, null, null, IntStream.range(0, need).boxed().toList());
    }

    /** Lists, forgets or switches off remembered decisions through the controller, as desktop's dialog does. */
    private void autoDecisions(final IGameController controller, final AutoDecisionCommand command) {
        final YieldController yields = controller.getYieldController();
        if (yields == null) {
            return;
        }
        final boolean abilityScope = yields.isAbilityScope();
        switch (command.action()) {
            case remove -> forget(controller, yields, command.key(), abilityScope);
            case clear -> {
                final List<String> keys = Lists.newArrayList(yields.getAutoYields());
                yields.getAutoTriggers().forEach(e -> keys.add(e.getKey()));
                keys.forEach(key -> forget(controller, yields, key, abilityScope));
            }
            case clearOrders -> controller.sendYieldUpdate(new YieldUpdate.ClearAbilityOrders());
            case disableYields -> controller.setDisableAutoYields(command.on());
            case disableTriggers -> controller.setDisableAutoTriggers(command.on());
            case list -> { }
        }
        final List<ToBrowser.AutoDecision> entries = new ArrayList<>();
        yields.getAutoYields().forEach(key -> entries.add(new ToBrowser.AutoDecision(key, ToBrowser.AutoDecisionKind.yield)));
        yields.getAutoTriggers().forEach(e -> entries.add(new ToBrowser.AutoDecision(e.getKey(),
                e.getValue() == TriggerDecision.ACCEPT ? ToBrowser.AutoDecisionKind.accept : ToBrowser.AutoDecisionKind.decline)));
        entries.sort(Comparator.comparing(ToBrowser.AutoDecision::key, String.CASE_INSENSITIVE_ORDER));
        send(new ToBrowser.AutoDecisions(entries, yields.getDisableAutoYields(), yields.getDisableAutoTriggers()));
    }

    private static void forget(final IGameController controller, final YieldController yields, final String key,
            final boolean abilityScope) {
        if (key == null) {
            return;
        }
        if (Iterables.contains(yields.getAutoYields(), key)) {
            controller.setShouldAutoYield(key, false, abilityScope);
        }
        if (Iterables.any(yields.getAutoTriggers(), e -> key.equals(e.getKey()))) {
            controller.setTriggerDecision(key, TriggerDecision.ASK, abilityScope);
        }
    }

    @Override
    public void updateDrawOffer(final DrawOfferMessage.Status update) {
        if (update == null) {
            return;
        }
        final PlayerView offerer = update.offerer();
        final boolean open = update.result() == null;
        final boolean mine = offerer != null && isLocalPlayer(offerer);
        final boolean waitingOnMe = open && update.entries().stream()
                .anyMatch(e -> isLocalPlayer(e.player()) && e.vote() == DrawOffer.Vote.PENDING);
        drawOffer = new ToBrowser.DrawOffer(offerer == null ? null : Ref.player(offerer.getId()), open, mine, waitingOnMe);
        send(drawOffer);
        if (open) {
            return;
        }
        final Localizer loc = Localizer.getInstance();
        if (update.result() == DrawOfferMessage.Result.ACCEPTED) {
            send(new Notice(loc.getMessage("lblWebGameDrawAgreed"), loc.getMessage("lblWebGameDrawAccepted"), false));
            return;
        }
        final List<String> declined = update.entries().stream()
                .filter(e -> e.vote() == DrawOffer.Vote.DECLINED && e.player() != null)
                .map(e -> isLocalPlayer(e.player()) ? StringUtils.capitalize(loc.getMessage("lblYou")) : e.player().getName()).toList();
        send(new Notice(loc.getMessage("lblWebGameDrawDeclined"), declined.isEmpty() ? loc.getMessage("lblWebGameGoesOn")
                : loc.getMessage("lblWebGameDeclined", String.join(", ", declined)), false));
    }

    @Override
    public SpellAbilityView getAbilityToPlay(final CardView hostCard, final List<SpellAbilityView> abilities, final ITriggerEvent event) {
        final ITriggerEvent triggerEvent = event != null ? event : lastClick.getAndSet(null);
        if (abilities.isEmpty()) {
            return null;
        }
        // A single ability needs no question unless it says to ask. canPlay is not checked, because an engine question such as a trigger can arrive with a remembered click
        if (abilities.size() == 1 && (triggerEvent == null || !abilities.get(0).promptIfOnlyPossibleAbility())) {
            return abilities.get(0);
        }
        // A click with either button shows a menu, at the click, of what the card can do now. A single option is done at once
        final List<SpellAbilityView> offered = triggerEvent == null ? abilities
                : abilities.stream().filter(SpellAbilityView::canPlay).toList();
        if (offered.isEmpty()) {
            return null;
        }
        if (triggerEvent != null && offered.size() == 1 && !offered.get(0).promptIfOnlyPossibleAbility()) {
            return offered.get(0);
        }
        // No answer means no ability is chosen. A cancelled click gives the same result
        // Each ability is listed by its first line, as desktop's menu lists it
        final ChoicesRequest request = choicesRequest(ChoiceKind.choices, hostCard == null ? "" : hostCard.getName(), 0, 1,
                offered, null, a -> StringUtils.substringBefore(String.valueOf(a), "\n"), null, triggerEvent instanceof BrowserClick click ? click : null, List.of());
        final List<SpellAbilityView> picked = Answers.pick(offered, ask(request, Answers.indexList(offered.size(), 0, 1)));
        return picked.isEmpty() ? null : picked.get(0);
    }

    @Override
    public Map<CardView, Integer> assignCombatDamage(final CardView attacker, final List<CardView> blockers, final int damage, final GameEntityView defender, final boolean overrideOrder, final boolean maySkip) {
        final List<GameEntityView> recipients = new ArrayList<>(blockers);
        if (defender != null) {
            recipients.add(defender);
        }
        final String title = Localizer.getInstance().getMessage("lbLAssignDamageDealtBy").replace("%s", attacker == null ? "" : attacker.getName());
        // As on desktop, one damage from a deathtouch attacker is lethal
        final boolean deathtouch = attacker != null && attacker.getCurrentState().hasDeathtouch();
        final List<Integer> lethal = new ArrayList<>();
        for (final CardView blocker : blockers) {
            final int toKill = Math.max(0, blocker.getLethalDamage());
            lethal.add(deathtouch ? Math.min(toKill, 1) : toKill);
        }
        final int[] split = Answers.defaultCombatSplit(lethal, damage, defender != null);
        if (defender != null) {
            lethal.add(0);
        }
        final DistributeRequest request = new DistributeRequest(title, damage, 0,
                options(recipients, null), PromptState.cardRef(attacker), maySkip, Ints.asList(split), lethal);
        final JsonElement reply = ask(request, Answers.amounts(recipients.size(), damage, 0, maySkip));
        if (reply.isJsonNull()) {
            return null;
        }
        final Map<CardView, Integer> result = new HashMap<>();
        final JsonArray a = reply.getAsJsonArray();
        for (int i = 0; i < recipients.size(); i++) {
            // PlayerControllerHuman reads a null key as the defender
            result.put(i < blockers.size() ? blockers.get(i) : null, a.get(i).getAsInt());
        }
        return result;
    }

    @Override
    public Map<Object, Integer> assignGenericAmount(final CardView effectSource, final Map<Object, Integer> target, final int amount, final boolean atLeastOne, final String amountLabel) {
        final List<Object> recipients = new ArrayList<>(target.keySet());
        final int perMin = atLeastOne ? 1 : 0;
        final int[] def = new int[recipients.size()];
        int remaining = amount;
        for (int i = 0; i < def.length; i++) {
            def[i] = perMin;
            remaining -= perMin;
        }
        if (def.length > 0) {
            def[0] += Math.max(0, remaining);
        }
        // amountLabel is the engine's lower-case name for what is assigned
        final String title = Localizer.getInstance().getMessage("lbLAssignAmountForEffect", amountLabel,
                effectSource == null ? "" : effectSource.getName());
        final DistributeRequest request = new DistributeRequest(title, amount, perMin, options(recipients, null),
                PromptState.cardRef(effectSource), false, Ints.asList(def), null);
        final JsonArray reply = ask(request, Answers.amounts(recipients.size(), amount, perMin, false)).getAsJsonArray();
        final Map<Object, Integer> result = new LinkedHashMap<>();
        for (int i = 0; i < recipients.size(); i++) {
            result.put(recipients.get(i), reply.get(i).getAsInt());
        }
        return result;
    }

    // The reply is how many copies of each distinct card go in the main deck. PlayerControllerHuman asks again if the deck is illegal
    @Override
    public List<PaperCard> sideboard(final CardPool sideboard, final CardPool main, final String message) {
        final Map<PaperCard, int[]> counts = new LinkedHashMap<>();
        for (final Map.Entry<PaperCard, Integer> e : main) {
            counts.computeIfAbsent(e.getKey(), k -> new int[2])[0] += e.getValue();
        }
        for (final Map.Entry<PaperCard, Integer> e : sideboard) {
            counts.computeIfAbsent(e.getKey(), k -> new int[2])[1] += e.getValue();
        }
        final List<PaperCard> cards = new ArrayList<>(counts.keySet());
        final List<SideboardEntry> entries = new ArrayList<>();
        final List<Integer> inMain = new ArrayList<>();
        final int[] totals = new int[cards.size()];
        for (int i = 0; i < cards.size(); i++) {
            final PaperCard card = cards.get(i);
            final int[] c = counts.get(card);
            totals[i] = c[0] + c[1];
            entries.add(new SideboardEntry(card.getName(), Foil.key(card), totals[i]));
            inMain.add(c[0]);
        }
        final JsonElement reply = ask(new SideboardRequest(message, entries, inMain, inMain), v -> {
            if (!v.isJsonArray() || v.getAsJsonArray().size() != totals.length) {
                return false;
            }
            for (int i = 0; i < totals.length; i++) {
                final JsonElement e = v.getAsJsonArray().get(i);
                if (!e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber() || e.getAsInt() < 0 || e.getAsInt() > totals[i]) {
                    return false;
                }
            }
            return true;
        });
        final List<PaperCard> newMain = new ArrayList<>();
        for (int i = 0; i < cards.size(); i++) {
            for (int n = reply.getAsJsonArray().get(i).getAsInt(); n > 0; n--) {
                newMain.add(cards.get(i));
            }
        }
        return newMain;
    }

    private static <T extends Record> Handler<WebGuiGame, T> play(final Play<T> action) {
        return (g, ch, command) -> {
            final IGameController controller = g.getGameController();
            if (controller != null) {
                action.run(g, controller, command);
            }
        };
    }

    /** Runs a browser command on the socket thread. A reply to an open question is handled at once. Other commands take the mirror lock, as game calls do. */
    public void onCommand(final BrowserChannel channel, final Record command) {
        if (command instanceof Reply) {
            COMMANDS.run(this, channel, command);
            return;
        }
        final boolean leaving = command instanceof Bare b && b.t() == Plain.concede
                || command instanceof NextGame n && n.decision() == NextGameDecision.QUIT;
        if (leaving) {
            // The host's engine thread waits on an open request with no timeout, so the request is answered before leaving
            requests.cancelAll();
        }
        mirrorLock.lock();
        try {
            COMMANDS.run(this, channel, command);
        } finally {
            mirrorLock.unlock();
        }
    }

    private void keyCommand(final BrowserChannel channel, final KeyCommand command) {
        final IGameController controller = getGameController();
        switch (command.t()) {
            case detail -> {
                final CardView card = lookup(command.key(), TrackableTypes.CardViewType);
                if (card != null) {
                    send(CardDetails.card(card, getGameView(), mayView(card), mayFlip(card)));
                }
            }
            case playerDetail -> {
                final PlayerView player = lookup(command.key(), TrackableTypes.PlayerViewType);
                if (player != null) {
                    send(CardDetails.player(player));
                }
            }
            case selectPlayer -> {
                final PlayerView player = lookup(command.key(), TrackableTypes.PlayerViewType);
                if (player != null && controller != null) {
                    controller.selectPlayer(player, null);
                }
            }
            case selectStackItem -> {
                final StackItemView item = lookup(command.key(), TrackableTypes.StackItemViewType);
                if (item != null && controller != null) {
                    controller.selectStackItem(item);
                }
            }
            case stackMenu -> {
                final StackItemView item = lookup(command.key(), TrackableTypes.StackItemViewType);
                if (item != null && controller != null) {
                    send(stackMenuMessage(controller, item));
                }
            }
        }
    }

    /** A setting belongs to the player, not the game, so a setting sent before the game has a controller still applies. */
    public void setSetting(final SetSetting setting) {
        mirrorLock.lock();
        try {
            Options.set(settings, getGameController(), setting.key(), setting.value());
            send(controlsMessage());
        } finally {
            mirrorLock.unlock();
        }
    }

    /** Stops also belong to the player. Once the game has players, it is told of each stop that changed. */
    public void setStops(final SetStops stops) {
        mirrorLock.lock();
        try {
            for (final PhaseType phase : Options.setStops(settings, stops.mine(), stops.phases())) {
                pushStop(phase, stops.mine());
            }
            send(controlsMessage());
        } finally {
            mirrorLock.unlock();
        }
    }

    private <T> T lookup(final int key, final TrackableType<T> type) {
        final GameView gv = getGameView();
        if (gv == null || gv.getTracker() == null) {
            return null;
        }
        return gv.getTracker().getObj(type, DeltaPacket.getIdFromDeltaKey(key));
    }

    /** A click from the browser. The right button reports button 3, which the attack and block prompts read as taking a card out of combat. */
    private record BrowserClick(boolean menu, int x, int y) implements ITriggerEvent {
        @Override
        public int getButton() {
            return menu ? 3 : 1;
        }

        @Override
        public int getX() {
            return x;
        }

        @Override
        public int getY() {
            return y;
        }
    }

    /** A command run through the player's controller. It is ignored while there is no controller. */
    private interface Play<T> {
        void run(WebGuiGame gui, IGameController controller, T command);
    }
}
