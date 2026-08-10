package forge.game.decision;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import org.testng.annotations.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class AttackDeclarationAdapterTest extends AITest {
    private final AttackDeclarationAdapter adapter = new AttackDeclarationAdapter();

    @Test
    public void emptyAiDeclarationReplaysThroughDoneWithoutTouchingCombat() {
        final Game game = initAndCreateGame();
        final Player attacker = game.getPlayers().get(1);
        final Card creature = addCardToZone("Grizzly Bears", attacker, ZoneType.Battlefield);
        creature.setSickness(false);
        final Combat combat = new Combat(attacker);
        final AttackDeclarationAdapter.Replay replay = NeutralityAssertions.assertGameAndRngNeutral(
                "ATTACK generation/replay", game, () -> {
                    final AttackDeclarationAdapter.Capture capture = adapter.begin(attacker, attacker, combat);
                    return adapter.replay(capture, Map.of());
                });
        final AttackDeclarationAdapter.Capture capture = adapter.begin(attacker, attacker, combat);

        assertEquals(capture.getStatus(), AttackDeclarationAdapter.Status.SUPPORTED);
        assertEquals(replay.getStatus(), AttackDeclarationAdapter.ReplayStatus.COMPLETE);
        assertEquals(replay.getSteps().size(), 1);
        assertEquals(replay.getSteps().get(0).getRequest().getCandidates().size(), 2);
        assertTrue(replay.getCompletedAssignments().isEmpty());
        assertTrue(combat.getAttackers().isEmpty());
    }

    @Test
    public void aiAssignmentReplaysDeterministicallyAndLeavesOriginalMapUnchanged() {
        final Game game = initAndCreateGame();
        final Player attacker = game.getPlayers().get(1);
        final Card first = addCardToZone("Grizzly Bears", attacker, ZoneType.Battlefield);
        final Card second = addCardToZone("Runeclaw Bear", attacker, ZoneType.Battlefield);
        first.setSickness(false);
        second.setSickness(false);
        final Combat combat = new Combat(attacker);
        final AttackDeclarationAdapter.Capture capture = adapter.begin(attacker, attacker, combat);
        final Map<Card, forge.game.GameEntity> aiResult = new LinkedHashMap<>();
        aiResult.put(second, game.getPlayers().get(0));
        aiResult.put(first, game.getPlayers().get(0));

        final AttackDeclarationAdapter.Replay replay = adapter.replay(capture, aiResult);

        assertEquals(replay.getStatus(), AttackDeclarationAdapter.ReplayStatus.COMPLETE);
        assertEquals(replay.getSteps().size(), 3);
        assertEquals(replay.getCompletedAssignments().get(0).getCard().getCardId(), first.getId());
        assertEquals(replay.getCompletedAssignments().get(0).getCard().getGameTimestamp(), first.getGameTimestamp());
        assertEquals(replay.getCompletedAssignments().get(1).getCard().getCardId(), second.getId());
        assertEquals(replay.getCompletedAssignments().get(1).getCard().getGameTimestamp(), second.getGameTimestamp());
        assertEquals(aiResult.size(), 2);
        assertSame(aiResult.keySet().iterator().next(), second);
        assertTrue(combat.getAttackers().isEmpty());
    }

    @Test
    public void unmappableAiAssignmentFailsOpenForDiagnostics() {
        final Game game = initAndCreateGame();
        final Player attacker = game.getPlayers().get(1);
        final Card creature = addCardToZone("Grizzly Bears", attacker, ZoneType.Battlefield);
        creature.setSickness(false);
        final Combat combat = new Combat(attacker);
        final AttackDeclarationAdapter.Capture capture = adapter.begin(attacker, attacker, combat);
        final Card notCaptured = addCardToZone("Runeclaw Bear", attacker, ZoneType.Battlefield);
        notCaptured.setSickness(false);
        final Map<Card, forge.game.GameEntity> aiResult = Map.of(notCaptured, game.getPlayers().get(0));

        final AttackDeclarationAdapter.Replay replay = adapter.replay(capture, aiResult);

        assertEquals(replay.getStatus(), AttackDeclarationAdapter.ReplayStatus.MAPPING_FAILED);
        assertTrue(combat.getAttackers().isEmpty());
    }
}
