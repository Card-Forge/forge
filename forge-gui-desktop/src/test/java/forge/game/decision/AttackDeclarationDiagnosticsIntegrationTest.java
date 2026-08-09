package forge.game.decision;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import org.testng.annotations.Test;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/** Exercises the enabled diagnostic seam while preserving Forge's live declaration. */
public class AttackDeclarationDiagnosticsIntegrationTest extends AITest {
    @Test
    public void enabledReplayCannotChangeTheControllerDeclaration() {
        final boolean enabled = !System.getProperty(PriorityActionDiagnostics.OUTPUT_PATH_PROPERTY, "").isBlank();
        final Game game = initAndCreateGame();
        final Player attacker = game.getPlayers().get(1);
        final Player defender = game.getPlayers().get(0);
        final Card creature = addCardToZone("Grizzly Bears", attacker, ZoneType.Battlefield);
        creature.setSickness(false);
        final Combat combat = new Combat(attacker);

        final PriorityActionDiagnostics.AttackDeclarationCapture capture =
                PriorityActionDiagnostics.captureAttackDeclaration(attacker, attacker, combat);
        if (!enabled) {
            assertTrue(capture == null);
            return;
        }

        assertNotNull(capture);
        combat.addAttacker(creature, defender);
        PriorityActionDiagnostics.recordAttackDeclaration(capture, combat,
                PriorityActionDiagnostics.startNativeCallback());

        assertTrue(combat.isAttacking(creature));
    }

    @Test
    public void enabledMappingFailureIsFailOpenForLiveCombat() {
        final boolean enabled = !System.getProperty(PriorityActionDiagnostics.OUTPUT_PATH_PROPERTY, "").isBlank();
        final Game game = initAndCreateGame();
        final Player attacker = game.getPlayers().get(1);
        final Player defender = game.getPlayers().get(0);
        final Card captured = addCardToZone("Grizzly Bears", attacker, ZoneType.Battlefield);
        captured.setSickness(false);
        final Combat combat = new Combat(attacker);

        final PriorityActionDiagnostics.AttackDeclarationCapture capture =
                PriorityActionDiagnostics.captureAttackDeclaration(attacker, attacker, combat);
        if (!enabled) {
            assertTrue(capture == null);
            return;
        }

        final Card notCaptured = addCardToZone("Runeclaw Bear", attacker, ZoneType.Battlefield);
        notCaptured.setSickness(false);
        assertNotNull(capture);
        combat.addAttacker(notCaptured, defender);
        PriorityActionDiagnostics.recordAttackDeclaration(capture, combat,
                PriorityActionDiagnostics.startNativeCallback());

        assertTrue(combat.isAttacking(notCaptured));
        assertTrue(!combat.isAttacking(captured));
    }
}
