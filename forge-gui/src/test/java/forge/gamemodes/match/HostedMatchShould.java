package forge.gamemodes.match;

import forge.game.Game;
import forge.gui.GuiBase;
import forge.gui.interfaces.IGuiBase;
import forge.player.PlayerControllerHuman;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Queue;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

/** Pins both origin checks without starting a game or a network connection. */
public class HostedMatchShould {
    @Test
    public void ignoreQuitFromAnAlreadyReplacedGame() throws Exception {
        try (Fixture fixture = new Fixture()) {
            fixture.replaceGame();
            fixture.quit();
            assertTrue(fixture.edt.isEmpty(), "stale origin is rejected before posting to the EDT");
            fixture.assertNotEnded();
        }
    }

    @Test
    public void recheckQuitOriginWhenTheEdtConsumesIt() throws Exception {
        try (Fixture fixture = new Fixture()) {
            fixture.quit();
            assertEquals(fixture.edt.size(), 1, "the current game's QUIT is actually queued");
            Game replacement = fixture.replaceGame();
            fixture.edt.remove().run();
            assertSame(fixture.hosted.getGame(), replacement);
            fixture.assertNotEnded();
        }
    }

    @Test
    public void executeQueuedQuitWhenItsGameIsStillCurrent() throws Exception {
        try (Fixture fixture = new Fixture()) {
            fixture.quit();
            assertEquals(fixture.edt.size(), 1);
            fixture.edt.remove().run();
            assertEquals(fixture.ended, 1);
            assertEquals(fixture.notified, 1);
            assertTrue(fixture.hosted.isMatchOver());
        }
    }

    private static final class Fixture implements AutoCloseable {
        private final IGuiBase previousGui = GuiBase.getInterface();
        private final Queue<Runnable> edt = new ArrayDeque<>();
        private final PlayerControllerHuman controller = mock(PlayerControllerHuman.class);
        private final HostedMatch hosted = new HostedMatch() {
            @Override public void endCurrentGame() { ended++; }
        };
        private int ended;
        private int notified;

        private Fixture() throws Exception {
            IGuiBase gui = mock(IGuiBase.class);
            when(gui.isGuiThread()).thenReturn(false);
            doAnswer(invocation -> { edt.add(invocation.getArgument(0)); return null; })
                    .when(gui).invokeInEdtLater(any(Runnable.class));
            Game origin = replaceGame();
            when(controller.getGame()).thenReturn(origin);
            hosted.setOnMatchOver(() -> notified++);
            GuiBase.setInterface(gui);
        }

        private Game replaceGame() throws Exception {
            Game replacement = mock(Game.class);
            Field game = HostedMatch.class.getDeclaredField("game");
            game.setAccessible(true);
            game.set(hosted, replacement);
            return replacement;
        }

        private void quit() throws Exception {
            Method decision = HostedMatch.class.getDeclaredMethod("addNextGameDecision",
                    PlayerControllerHuman.class, NextGameDecision.class);
            decision.setAccessible(true);
            decision.invoke(hosted, controller, NextGameDecision.QUIT);
        }

        private void assertNotEnded() {
            assertEquals(ended, 0);
            assertEquals(notified, 0);
            assertFalse(hosted.isMatchOver());
        }

        @Override public void close() { GuiBase.setInterface(previousGui); }
    }
}
