package forge.net.embedded;

import forge.deck.Deck;
import forge.game.GameType;
import forge.game.GameView;
import forge.game.card.CardView;
import forge.game.phase.PhaseType;
import forge.game.player.PlayerView;
import forge.game.player.RegisteredPlayer;
import forge.gamemodes.match.HostedMatch;
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
        boolean playedLand;

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
            if (!playedLand && gv.getPhase() == PhaseType.MAIN1 && owner.equals(gv.getPlayerTurn())
                    && owner.getHand() != null && !owner.getHand().isEmpty()) {
                playedLand = true;
                final CardView land = owner.getHand().iterator().next();
                return () -> controller.selectCard(land, null, null);
            }
            if (playedLand && gv.getTurn() >= 3) {
                return controller::concede;
            }
            return controller::selectButtonOk;
        }

        private GuiGameEvent lastButtons() {
            synchronized (remote.log) {
                for (int i = remote.log.size() - 1; i >= 0; i--) {
                    if (remote.log.get(i).getMethod() == ProtocolMethod.updateButtons) {
                        return remote.log.get(i);
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
            humanDeck.getMain().add("Drifting Meadow", 40);
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
            assertTrue(remote.blockingCalls.stream().anyMatch(s -> s.startsWith("getAbilityToPlay")), "nested sendAndWait ran inside the pump");
            assertEquals(me.getBattlefield().size(), 1, "the land reached the battlefield");
        } finally {
            GuiBase.setInterface(previous);
        }
    }
}
