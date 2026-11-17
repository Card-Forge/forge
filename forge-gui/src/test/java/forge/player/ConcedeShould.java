package forge.player;

import com.google.common.eventbus.Subscribe;
import forge.ai.LobbyPlayerAi;
import forge.game.GameEndReason;
import forge.gamemodes.match.AbstractGuiGame;
import forge.gamemodes.match.NextGameDecision;
import forge.gamemodes.match.input.InputSynchronized;
import forge.gui.events.UiEventNextGameDecision;
import org.mockito.Mockito;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;

public class ConcedeShould {

    @Test
    public void letThePlayerWatchTheRemainingAiPlayersAndEndTheGameWhenTheyLeave() throws InterruptedException {
        HumanControlledGame g = new HumanControlledGame(new LobbyPlayerAi("AI 1", null), new LobbyPlayerAi("AI 2", null));
        AbstractGuiGame gui = guiFor(g);
        List<NextGameDecision> decisions = new ArrayList<>();
        g.game.getMatch().subscribeToEvents(new Object() {
            @Subscribe
            public void receive(final UiEventNextGameDecision event) {
                decisions.add(event.decision());
            }
        });

        onGameThread(gui::concede);

        assertThat(decisions).isEmpty();
        assertThat(g.game.isGameOver()).isFalse();

        onGameThread(gui::concede);

        assertThat(g.game.isGameOver()).isTrue();
        assertThat(g.game.getOutcome().getWinCondition()).isEqualTo(GameEndReason.AllHumansLost);
    }

    @Test
    public void releaseAWaitingRemotePlayerWhenTheHostLeaves() throws InterruptedException {
        HumanControlledGame g = new HumanControlledGame(HumanControlledGame.lobbyPlayer("Remote"), new LobbyPlayerAi("AI", null));
        AbstractGuiGame gui = guiFor(g);
        gui.setNetGame();
        PlayerControllerHuman remote = (PlayerControllerHuman) g.opponent.getController();
        // The input proxy would show the prompt through a GUI this test does not have
        remote.getInputQueue().deleteObservers();
        InputSynchronized remotePrompt = Mockito.mock(InputSynchronized.class);
        remote.getInputQueue().setInput(remotePrompt);

        onGameThread(gui::concede);

        assertThat(g.game.isGameOver()).isFalse();

        onGameThread(gui::concede);

        assertThat(g.game.isGameOver()).isTrue();
        Mockito.verify(remotePrompt).relaseLatchWhenGameIsOver();
    }

    private static AbstractGuiGame guiFor(final HumanControlledGame g) {
        AbstractGuiGame gui = Mockito.mock(AbstractGuiGame.class,
                Mockito.withSettings().useConstructor().defaultAnswer(Mockito.CALLS_REAL_METHODS));
        Mockito.doReturn(true).when(gui).showConfirmDialog(anyString(), anyString(), anyString(), anyString(), anyBoolean());
        gui.setGameView(g.game.getView());
        gui.setOriginalGameController(g.human.getView(), g.controller);
        return gui;
    }

    // The mobile game menu concedes on a game thread, where ending the game runs at once
    private static void onGameThread(final Runnable action) throws InterruptedException {
        Thread thread = new Thread(action, "Game-test");
        thread.start();
        thread.join();
    }
}
