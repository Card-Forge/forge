package forge.view;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.GameEndReason;
import forge.game.GameLogEntryType;
import forge.game.player.RegisteredPlayer;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

public class SimulationOutcomeClassificationTest extends AITest {

    @Test
    public void historicalDrawCleanupShapeIsReportedAsInvalidMultipleWinners() {
        final Game game = initAndCreateGame();
        game.setGameOver(GameEndReason.Draw);

        final SimulationOutcomeClassification result = SimulationOutcomeClassification.classify(game);

        assertEquals(result.getKind(), SimulationOutcomeClassification.Kind.INVALID_MULTIPLE_WINNERS);
        assertEquals(result.getWinnerSeats(), List.of(0, 1));
        assertFalse(result.getWinnerName().isPresent());
        assertEquals(result.canonical(), "INVALID_OUTCOME MULTIPLE_WINNERS [0,1]");
        assertFalse(SimulateMatch.reportableLogEntries(game, false).stream()
                .anyMatch(entry -> entry.type() == GameLogEntryType.MATCH_RESULTS));
    }

    @Test
    public void semanticWinnerReportingIsStableAcrossRegisteredPlayerIteration() {
        final Game first = initAndCreateGame();
        first.setGameOver(GameEndReason.Draw);
        final Game second = initAndCreateGame();
        second.setGameOver(GameEndReason.Draw);

        assertEquals(SimulationOutcomeClassification.classify(first).canonical(),
                SimulationOutcomeClassification.classify(second).canonical());
    }

    @Test
    public void gameOutcomeReceivesStableRegistrationOrderAndTeamAssignmentDoesNotReorderIt() {
        final Game game = initAndCreateGame();
        game.getRegisteredPlayers().get(0).setTeam(7);
        game.getRegisteredPlayers().get(1).setTeam(7);
        final List<RegisteredPlayer> registrationOrder = game.getRegisteredPlayers().stream()
                .map(player -> player.getRegisteredPlayer()).toList();

        game.setGameOver(GameEndReason.Draw);

        assertEquals(List.copyOf(game.getOutcome().getPlayerNames().keySet()), registrationOrder);
        assertEquals(SimulationOutcomeClassification.classify(game).getKind(),
                SimulationOutcomeClassification.Kind.MULTIPLE_WINNERS);
    }
}
