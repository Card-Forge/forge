package forge.ai;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class CombatStepsTest extends AITest {

    /** CR 508.8: only an attack with no declared attackers skips the declare blockers and combat damage steps. */
    @Test
    public void attackerRemovedFromCombatDoesNotSkipBlockersAndDamage() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);
        Card bear = addCard("Grizzly Bears", p);
        bear.setSickness(false);

        Set<PhaseType> priority = EnumSet.noneOf(PhaseType.class);
        p.dangerouslySetController(new PlayerControllerAi(game, p, p.getLobbyPlayer()) {
            @Override
            public void declareAttackers(Player attacker, Combat combat) {
                combat.addAttacker(bear, opponent);
            }
            @Override
            public List<SpellAbility> chooseSpellAbilityToPlay() {
                PhaseHandler ph = game.getPhaseHandler();
                if (ph.inCombat()) {
                    priority.add(ph.getPhase());
                }
                return null;
            }
        });

        playUntilPhase(game, PhaseType.COMBAT_DECLARE_ATTACKERS);
        game.getCombat().removeFromCombat(bear);
        playUntilPhase(game, PhaseType.COMBAT_END);

        assertTrue(priority.contains(PhaseType.COMBAT_DECLARE_BLOCKERS), "declare blockers step skipped: " + priority);
        assertFalse(game.getPhaseHandler().skippedDeclareBlockers());
        assertEquals(opponent.getLife(), 20);
    }
}
