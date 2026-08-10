package forge.game.decision;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class BlockDeclarationNeutralityTest extends AITest {
    @Test
    public void generationAndReplayConsumeNoRngAndDoNotMutateForgeState() {
        final Game game = initAndCreateGame();
        final Player attacker = game.getPlayers().get(0);
        final Player defender = game.getPlayers().get(1);
        final Card attackingCreature = addCardToZone("Grizzly Bears", attacker, ZoneType.Battlefield);
        final Card blocker = addCardToZone("Runeclaw Bear", defender, ZoneType.Battlefield);
        attackingCreature.setSickness(false);
        final Combat combat = new Combat(attacker);
        combat.addAttacker(attackingCreature, defender);
        game.getPhaseHandler().setCombat(combat);
        final BlockDeclarationAdapter adapter = new BlockDeclarationAdapter();

        final BlockDeclarationAdapter.Capture capture = NeutralityAssertions.assertGameAndRngNeutral(
                "BLOCK generation", game, () -> adapter.begin(defender, defender, combat));
        assertEquals(capture.getStatus(), BlockDeclarationAdapter.Status.SUPPORTED);

        combat.addBlocker(attackingCreature, blocker);
        final BlockDeclarationAdapter.Replay replay = NeutralityAssertions.assertGameAndRngNeutral(
                "BLOCK replay", game, () -> adapter.replay(capture, combat));

        assertEquals(replay.getStatus(), BlockDeclarationAdapter.ReplayStatus.COMPLETE);
        assertEquals(replay.getSteps().size(), 3);
        assertEquals(combat.getBlockers(attackingCreature).size(), 1);
    }
}
