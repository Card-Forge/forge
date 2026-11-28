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

public class CombatDamageStepsTest extends AITest {

    /** Plays one attack with the given card and returns the combat steps in which its controller got priority. */
    private Set<PhaseType> attackAndRecordPriority(Game game, Player p, Card attacker, Player opponent) {
        Set<PhaseType> priority = EnumSet.noneOf(PhaseType.class);
        p.dangerouslySetController(new PlayerControllerAi(game, p, p.getLobbyPlayer()) {
            @Override
            public void declareAttackers(Player who, Combat combat) {
                combat.addAttacker(attacker, opponent);
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
        playUntilPhase(game, PhaseType.COMBAT_END);
        return priority;
    }

    /** CR 510.3, 510.4: both damage steps happen with a double striker, even one that assigns no damage. */
    @Test
    public void zeroPowerDoubleStrikerGetsPriorityInBothDamageSteps() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);
        Card ace = addCard("Fencing Ace", p);
        ace.setSickness(false);
        ace.setBasePower(0);

        Set<PhaseType> priority = attackAndRecordPriority(game, p, ace, opponent);

        assertTrue(priority.contains(PhaseType.COMBAT_FIRST_STRIKE_DAMAGE), "no priority in the first-strike damage step: " + priority);
        assertTrue(priority.contains(PhaseType.COMBAT_DAMAGE), "no priority in the combat damage step: " + priority);
        assertEquals(opponent.getLife(), 20);
    }

    /** CR 510.4: with no first or double striker in combat there is no first-strike damage step. */
    @Test
    public void noFirstStrikerNoFirstStrikeDamageStep() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);
        Card bear = addCard("Grizzly Bears", p);
        bear.setSickness(false);

        Set<PhaseType> priority = attackAndRecordPriority(game, p, bear, opponent);

        assertFalse(priority.contains(PhaseType.COMBAT_FIRST_STRIKE_DAMAGE), "priority in a first-strike damage step that should not happen: " + priority);
        assertTrue(priority.contains(PhaseType.COMBAT_DAMAGE), "no priority in the combat damage step: " + priority);
        assertEquals(opponent.getLife(), 18);
    }
}
