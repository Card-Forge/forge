package forge.net.embedded;

import forge.deck.Deck;
import forge.game.GameType;
import forge.game.GameView;
import forge.game.card.CardView;
import forge.game.phase.PhaseType;
import forge.game.player.PlayerView;
import forge.game.player.RegisteredPlayer;
import forge.game.spellability.SpellAbilityView;
import forge.gamemodes.match.Answer;
import forge.gamemodes.match.HostedMatch;
import forge.gamemodes.match.Question;
import forge.gamemodes.net.ProtocolGuiGame;
import forge.gamemodes.net.ProtocolMethod;
import forge.gamemodes.net.event.GuiGameEvent;
import forge.gui.GuiBase;
import forge.gui.interfaces.IGuiBase;
import forge.interfaces.IGameController;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.net.HeadlessGuiDesktop;
import forge.net.TestUtils;
import forge.player.GamePlayerUtil;
import forge.player.LobbyPlayerHuman;
import forge.util.MyRandom;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * A single-threaded host, where the peer can only answer on the thread that is waiting.
 * The GUI overrides {@code awaitInput} to take the client's answers in place, the EDT is inline,
 * and every protocol event and every answer must happen on one thread.
 */
public class ProtocolGuiGameSingleThreadTest {

    /** EDT work runs inline on the calling thread, as in a single-threaded host. */
    static final class InlineEdtGui extends HeadlessGuiDesktop {
        @Override public void invokeInEdtNow(final Runnable r) { r.run(); }
        @Override public void invokeInEdtLater(final Runnable r) { r.run(); }
        @Override public void invokeInEdtAndWait(final Runnable r) { r.run(); }
        @Override public boolean isGuiThread() { return false; }
    }

    /** Embedded GUI: while an input waits, pull the client's next answer and run it on this thread. */
    static final class PumpingGui extends ProtocolGuiGame {
        final ProtocolGuiGameInProcessTest.RecordingRemote remote;
        final Set<String> pumpThreads = Collections.synchronizedSet(new HashSet<>());
        int pumped;
        int landTurn = -1;
        boolean cycled;

        PumpingGui(final ProtocolGuiGameInProcessTest.RecordingRemote remote) {
            super(remote);
            this.remote = remote;
        }

        @Override
        public void awaitInput(final CountDownLatch done) {
            while (done.getCount() > 0) {
                pumpThreads.add(Thread.currentThread().getName());
                nextClientAnswer().run();
                pumped++;
                if (pumped > 500) {
                    throw new IllegalStateException("client script never finished the input");
                }
            }
        }

        /** Stands in for the host reading the peer's next answer: derived from what the remote was sent. */
        private Runnable nextClientAnswer() {
            final GuiGameEvent ub = lastButtons();
            final PlayerView owner = (PlayerView) ub.getObjects()[0];
            final IGameController controller = getGameController(owner);
            final GameView gv = getGameView();
            final Question.Priority question = remote.lastQuestion instanceof Question.Priority q && owner.equals(q.player()) ? q : null;
            if ((cycled && question != null) || gv.getTurn() >= 12) {
                return controller::concede;
            }
            if (question == null) {
                // Paying for the cycling: a Shivan Reef has two mana abilities, so the host asks which, blocking
                final CardView reef = cycled ? find(owner.getBattlefield(), "Shivan Reef", false) : null;
                return reef != null ? () -> controller.selectCard(reef, null, null) : controller::selectButtonOk;
            }
            final boolean myMain = owner.equals(gv.getPlayerTurn())
                    && (gv.getPhase() == PhaseType.MAIN1 || gv.getPhase() == PhaseType.MAIN2);
            if (myMain && landTurn != gv.getTurn()) {
                CardView land = find(owner.getHand(), "Shivan Reef", false);
                if (land == null) {
                    land = find(owner.getHand(), "Drifting Meadow", false);
                }
                if (land != null) {
                    landTurn = gv.getTurn();
                    final SpellAbilityView landPlay = ProtocolGuiGameInProcessTest.playableAbility(question, land);
                    return () -> controller.answer(new Answer.Play(question.id(), landPlay, null));
                }
            }
            final CardView meadow = find(owner.getHand(), "Drifting Meadow", false);
            if (myMain && meadow != null && untappedReefs(owner) >= 2) {
                for (final SpellAbilityView sa : question.abilities().getOrDefault(meadow, List.of())) {
                    if (sa.canPlay() && sa.toString().toLowerCase().contains("cycling")) {
                        cycled = true;
                        return () -> controller.answer(new Answer.Play(question.id(), sa, null));
                    }
                }
            }
            return () -> controller.answer(new Answer.Pass(question.id()));
        }

        private static CardView find(final Iterable<CardView> cards, final String name, final boolean tapped) {
            if (cards != null) {
                for (final CardView card : cards) {
                    if (name.equals(card.getName()) && card.isTapped() == tapped) {
                        return card;
                    }
                }
            }
            return null;
        }

        private static int untappedReefs(final PlayerView owner) {
            int count = 0;
            if (owner.getBattlefield() != null) {
                for (final CardView card : owner.getBattlefield()) {
                    if ("Shivan Reef".equals(card.getName()) && !card.isTapped()) {
                        count++;
                    }
                }
            }
            return count;
        }

        private GuiGameEvent lastButtons() {
            synchronized (remote.log) {
                for (int i = remote.log.size() - 1; i >= 0; i--) {
                    final GuiGameEvent ev = remote.log.get(i);
                    if (ev.getMethod() == ProtocolMethod.updateButtons
                            || (ev.getMethod() == ProtocolMethod.setQuestion && ev.getObjects()[1] != null)) {
                        return ev;
                    }
                }
            }
            throw new IllegalStateException("input is waiting but no prompt reached the remote");
        }
    }

    @Test(timeOut = 180_000)
    public void inputsCanBeAnsweredOnTheWaitingThread() throws Exception {
        TestUtils.ensureFModelInitialized();
        final IGuiBase previous = GuiBase.getInterface();
        GuiBase.setInterface(new InlineEdtGui());
        try {
            FModel.getPreferences().setPref(FPref.UI_SHOW_ACTIONABLE_HIGHLIGHTS, false);
            MyRandom.setRandom(new Random(7));

            final ProtocolGuiGameInProcessTest.RecordingRemote remote = new ProtocolGuiGameInProcessTest.RecordingRemote();
            final PumpingGui gui = new PumpingGui(remote);
            final Deck humanDeck = new Deck("Embedded");
            humanDeck.getMain().add("Shivan Reef", 26);
            humanDeck.getMain().add("Drifting Meadow", 14);
            final Deck aiDeck = new Deck("AI");
            aiDeck.getMain().add("Plains", 40);
            final RegisteredPlayer human = new RegisteredPlayer(humanDeck).setPlayer(new LobbyPlayerHuman("Embedded"));
            final RegisteredPlayer ai = new RegisteredPlayer(aiDeck).setPlayer(GamePlayerUtil.createAiPlayer("AI"));

            final HostedMatch match = new HostedMatch();
            match.startMatch(GameType.Constructed, null, List.of(human, ai), human, gui);

            final long end = System.currentTimeMillis() + 120_000;
            while ((match.getGameView() == null || !match.getGameView().isGameOver()) && System.currentTimeMillis() < end) {
                Thread.sleep(100);
            }

            final PlayerView me = remote.myPlayers.iterator().next();
            System.out.println("[single-thread] answers pumped: " + gui.pumped + " on " + gui.pumpThreads);
            System.out.println("[single-thread] blocking calls: " + remote.blockingCalls + " on " + new HashSet<>(remote.threadsSeen));
            System.out.printf("[single-thread] fullStates=%d deltasWithState=%d rawEvents=%d battlefield=%d gameOver=%b%n",
                    remote.fullStates, remote.deltasWithState, remote.rawEvents,
                    me.getBattlefield() == null ? 0 : me.getBattlefield().size(), match.getGameView().isGameOver());

            assertTrue(match.getGameView().isGameOver(), "game finished");
            assertEquals(gui.pumpThreads.size(), 1, "every input was answered on one thread");
            final Set<String> all = new HashSet<>(gui.pumpThreads);
            all.addAll(remote.threadsSeen);
            assertEquals(all.size(), 1, "blocking dialogs ran on the same thread as the pumped inputs");
            assertTrue(gui.cycled, "a card was cycled by answering the priority question");
            assertTrue(remote.blockingCalls.stream().anyMatch(s -> s.startsWith("getAbilityToPlay")),
                    "paying for it, a blocking call nested inside a pumped answer ran on the waiting thread");
        } finally {
            GuiBase.setInterface(previous);
        }
    }
}
