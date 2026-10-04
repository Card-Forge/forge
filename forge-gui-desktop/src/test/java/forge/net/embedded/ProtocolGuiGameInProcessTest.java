package forge.net.embedded;

import forge.deck.Deck;
import forge.game.GameType;
import forge.game.GameView;
import forge.game.card.CardView;
import forge.game.event.GameEvent;
import forge.game.phase.PhaseType;
import forge.game.player.PlayerView;
import forge.game.player.RegisteredPlayer;
import forge.game.spellability.SpellAbilityView;
import forge.gamemodes.match.Answer;
import forge.gamemodes.match.HostedMatch;
import forge.gamemodes.match.Question;
import forge.gamemodes.net.DeltaPacket;
import forge.gamemodes.net.IRemote;
import forge.gamemodes.net.ProtocolGuiGame;
import forge.gamemodes.net.ProtocolMethod;
import forge.gamemodes.net.event.GuiGameEvent;
import forge.gamemodes.net.event.IdentifiableNetEvent;
import forge.gamemodes.net.event.NetEvent;
import forge.gui.FThreads;
import forge.interfaces.IGameController;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.net.TestUtils;
import forge.player.GamePlayerUtil;
import forge.player.LobbyPlayerHuman;
import forge.trackable.TrackableCollection;
import forge.util.MyRandom;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * A stock {@code PlayerControllerHuman} plays against the AI through {@link ProtocolGuiGame}
 * and an in-process {@link IRemote}. No FServerManager, FGameClient, socket or Netty channel.
 */
public class ProtocolGuiGameInProcessTest {

    /** In-process endpoint: records every protocol event and answers blocking ones from a script. */
    static final class RecordingRemote implements IRemote {
        final List<GuiGameEvent> log = Collections.synchronizedList(new ArrayList<>());
        final BlockingQueue<GuiGameEvent> buttonPrompts = new LinkedBlockingQueue<>();
        final Map<ProtocolMethod, AtomicInteger> counts = new ConcurrentHashMap<>();
        final List<String> blockingCalls = Collections.synchronizedList(new ArrayList<>());
        final List<String> threadsSeen = Collections.synchronizedList(new ArrayList<>());
        volatile TrackableCollection<PlayerView> myPlayers;
        volatile int fullStates, deltasWithState, deltasWithEvents, rawEvents, newObjects, changedObjects;
        volatile int wrappedEvents;
        volatile String lastPrompt = "";
        volatile Question lastQuestion;
        /** Every question asked, with null for each withdrawal, in order. */
        final List<Question> questions = Collections.synchronizedList(new ArrayList<>());

        @Override
        public void send(final NetEvent event) {
            final GuiGameEvent ev = (GuiGameEvent) event;
            record(ev);
            final Object[] args = ev.getObjects();
            switch (ev.getMethod()) {
                case openView -> myPlayers = (TrackableCollection<PlayerView>) args[0];
                case setGameView -> fullStates++;
                case applyDelta -> {
                    final DeltaPacket d = (DeltaPacket) args[0];
                    if (!d.getNewObjects().isEmpty() || !d.getObjectDeltas().isEmpty()) {
                        deltasWithState++;
                        newObjects += d.getNewObjects().size();
                        changedObjects += d.getObjectDeltas().size();
                    }
                    if (d.hasEvents()) {
                        deltasWithEvents++;
                        for (final Object o : d.getEvents()) {
                            if (o instanceof GameEvent) rawEvents++; else wrappedEvents++;
                        }
                    }
                }
                case showPromptMessage -> lastPrompt = String.valueOf(args[1]);
                case setQuestion -> {
                    lastQuestion = (Question) args[1];
                    questions.add(lastQuestion);
                    // The priority question replaces the host's prompt; the remote GUI draws its own text and buttons
                    if (lastQuestion != null) {
                        buttonPrompts.offer(ev);
                    }
                }
                case updateButtons -> buttonPrompts.offer(ev);
                default -> { }
            }
        }

        @Override
        public Object sendAndWait(final IdentifiableNetEvent event) {
            final GuiGameEvent ev = (GuiGameEvent) event;
            record(ev);
            threadsSeen.add(Thread.currentThread().getName());
            final Object[] a = ev.getObjects();
            final Object answer = switch (ev.getMethod()) {
                case getAbilityToPlay -> {
                    // Prefer playing the land over cycling it
                    final List<SpellAbilityView> abilities = (List<SpellAbilityView>) a[1];
                    SpellAbilityView pick = abilities.get(0);
                    for (final SpellAbilityView sa : abilities) {
                        if (!sa.toString().toLowerCase().contains("cycling")) { pick = sa; break; }
                    }
                    yield pick;
                }
                case getChoices -> {
                    final List<?> choices = (List<?>) a[3];
                    final int min = (Integer) a[1];
                    final int max = (Integer) a[2];
                    yield new ArrayList<>(choices.subList(0, Math.min(choices.size(), Math.max(min, Math.min(1, max)))));
                }
                case confirm -> a[2];
                case showConfirmDialog -> a[4];
                case showOptionDialog -> a[4];
                case showInputDialog -> a[3];
                default -> null;
            };
            blockingCalls.add(ev.getMethod() + " -> " + answer);
            return answer;
        }

        private void record(final GuiGameEvent ev) {
            log.add(ev);
            counts.computeIfAbsent(ev.getMethod(), k -> new AtomicInteger()).incrementAndGet();
        }
    }

    private static Deck deck(final String name, final String card) {
        final Deck d = new Deck(name);
        d.getMain().add(card, 40);
        return d;
    }

    private static final List<String> TRANSPORT_CLASSES = List.of(
            "io.netty.channel.Channel", "io.netty.channel.nio.NioEventLoopGroup", "io.netty.buffer.ByteBuf",
            "net.jpountz.lz4.LZ4BlockOutputStream",
            "forge.gamemodes.net.CompatibleObjectEncoder", "forge.gamemodes.net.CObjectOutputStream",
            "forge.gamemodes.net.TrackableSerializer", "forge.gamemodes.net.server.RemoteClient",
            "forge.gamemodes.net.server.RemoteClientGuiGame", "forge.gamemodes.net.client.FGameClient",
            "forge.gamemodes.net.server.FServerManager");

    private List<String> loadedTransportClasses() throws Exception {
        final java.lang.reflect.Method findLoaded = ClassLoader.class.getDeclaredMethod("findLoadedClass", String.class);
        findLoaded.setAccessible(true);
        final ClassLoader cl = getClass().getClassLoader();
        final List<String> loaded = new ArrayList<>();
        for (final String name : TRANSPORT_CLASSES) {
            if (findLoaded.invoke(cl, name) != null) loaded.add(name);
        }
        return loaded;
    }

    /** The card's first playable ability that is not cycling, which for these decks is playing the land. */
    static SpellAbilityView playableAbility(final Question.Priority question, final CardView card) {
        for (final SpellAbilityView sa : question.abilities().getOrDefault(card, List.of())) {
            if (sa.canPlay() && !sa.toString().toLowerCase().contains("cycling")) {
                return sa;
            }
        }
        return null;
    }

    private static boolean askedThenWithdrawn(final List<Question> questions) {
        boolean asked = false;
        synchronized (questions) {
            for (final Question question : questions) {
                if (question != null) {
                    asked = true;
                } else if (asked) {
                    return true;
                }
            }
        }
        return false;
    }

    @Test(timeOut = 180_000)
    public void humanControllerRunsThroughInProcessRemote() throws Exception {
        TestUtils.ensureFModelInitialized();
        FModel.getPreferences().setPref(FPref.UI_SHOW_ACTIONABLE_HIGHLIGHTS, false);
        MyRandom.setRandom(new Random(7));

        final List<String> loadedBefore = loadedTransportClasses();
        final RecordingRemote remote = new RecordingRemote();
        final ProtocolGuiGame gui = new ProtocolGuiGame(remote);

        final RegisteredPlayer human = new RegisteredPlayer(deck("Embedded", "Drifting Meadow"))
                .setPlayer(new LobbyPlayerHuman("Embedded"));
        final RegisteredPlayer ai = new RegisteredPlayer(deck("AI", "Plains"))
                .setPlayer(GamePlayerUtil.createAiPlayer("AI"));

        final HostedMatch match = new HostedMatch();
        match.startMatch(GameType.Constructed, null, List.of(human, ai), human, gui);

        // Client role: answer Input prompts through IGameController, on the EDT like GameProtocolHandler does
        boolean playedLand = false;
        int answered = 0;
        int deltasBeforeLand = -1;
        final long deadline = System.currentTimeMillis() + 120_000;
        while (System.currentTimeMillis() < deadline) {
            final GuiGameEvent ub = remote.buttonPrompts.poll(5, TimeUnit.SECONDS);
            if (ub == null) {
                continue;
            }
            final PlayerView owner = (PlayerView) ub.getObjects()[0];
            if (remote.myPlayers == null || owner == null || !remote.myPlayers.contains(owner)) {
                continue;
            }
            final IGameController controller = gui.getGameController(owner);
            final GameView gv = gui.getGameView();
            final boolean myMain = gv.getPlayerTurn() != null && gv.getPlayerTurn().equals(owner)
                    && gv.getPhase() == PhaseType.MAIN1;
            CardView land = null;
            if (myMain && !playedLand && owner.getHand() != null) {
                for (final CardView c : owner.getHand()) { land = c; break; }
            }
            // A front end answers the priority question; other prompts still take buttons
            final Question.Priority question = remote.lastQuestion instanceof Question.Priority q && owner.equals(q.player()) ? q : null;
            answered++;
            System.out.printf("[in-process] prompt #%d: %s turn=%d phase=%s%n", answered,
                    question != null ? "question " + question.id() + " (" + question.abilities().size() + " cards)"
                            : "'" + remote.lastPrompt.replace('\n', ' ') + "' buttons=[" + ub.getObjects()[1] + "|" + ub.getObjects()[2] + "]",
                    gv.getTurn(), gv.getPhase());
            if (land != null && question != null) {
                final SpellAbilityView landPlay = playableAbility(question, land);
                deltasBeforeLand = remote.deltasWithState;
                playedLand = true;
                FThreads.invokeInEdtLater(() -> controller.answer(new Answer.Play(question.id(), landPlay, null)));
            } else if (playedLand && gv.getTurn() >= 3) {
                FThreads.invokeInEdtLater(controller::concede);
                break;
            } else if (question != null) {
                FThreads.invokeInEdtLater(() -> controller.answer(new Answer.Pass(question.id())));
            } else {
                FThreads.invokeInEdtLater(controller::selectButtonOk);
            }
        }

        // Let the concede settle
        final long end = System.currentTimeMillis() + 30_000;
        while (match.getGameView() != null && !match.getGameView().isGameOver() && System.currentTimeMillis() < end) {
            Thread.sleep(100);
        }

        System.out.println("[in-process] method counts: " + remote.counts);
        System.out.println("[in-process] blocking calls: " + remote.blockingCalls);
        System.out.println("[in-process] blocking threads: " + remote.threadsSeen);
        System.out.printf("[in-process] fullStates=%d deltasWithState=%d (new=%d changed=%d) deltasWithEvents=%d rawEvents=%d wrapped=%d%n",
                remote.fullStates, remote.deltasWithState, remote.newObjects, remote.changedObjects,
                remote.deltasWithEvents, remote.rawEvents, remote.wrappedEvents);

        final PlayerView me = remote.myPlayers.iterator().next();
        final int myLands = me.getBattlefield() == null ? 0 : me.getBattlefield().size();
        System.out.println("[in-process] my battlefield: " + myLands + " card(s)");

        // Which transport classes did this game load that were not loaded before it?
        final List<String> loaded = loadedTransportClasses();
        loaded.removeAll(loadedBefore);
        System.out.println("[in-process] transport classes loaded by the game: " + loaded + " (already loaded: " + loadedBefore + ")");

        assertNotNull(remote.myPlayers, "openView reached the remote");
        assertEquals(myLands, 1, "the land reached the battlefield");
        assertTrue(loaded.stream().noneMatch(n -> n.startsWith("io.netty") || n.startsWith("net.jpountz")
                || n.endsWith("ObjectEncoder") || n.endsWith("CObjectOutputStream") || n.endsWith("TrackableSerializer")
                || n.endsWith("RemoteClient") || n.endsWith("FGameClient") || n.endsWith("FServerManager")),
                "no network or serialization class loaded");
        assertTrue(remote.fullStates >= 1, "full state (setGameView) reached the remote");
        assertTrue(remote.deltasWithState >= 2, "state deltas reached the remote after init");
        assertTrue(askedThenWithdrawn(remote.questions), "the priority question reached the remote and was withdrawn once answered");
        assertTrue(playedLand, "the human played a land");
        assertTrue(remote.deltasWithState > deltasBeforeLand, "state kept flowing after the answer");
        assertEquals(remote.wrappedEvents, 0, "no Java-serialized event wrappers on the embedded path");
        assertTrue(remote.rawEvents > 0, "raw GameEvents reach the remote");
        assertFalse(match.getGameView() != null && !match.getGameView().isGameOver(), "game ended after concede");
    }
}
