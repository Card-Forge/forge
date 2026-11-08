package forge.ai;

import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * The AI on the opponent's turn, played through its own decision: a trick for the creature that blocked, and a creature
 * put onto the battlefield from the hand (Quicksilver Amulet) as a surprise, at the moment that makes it one: after the
 * attackers are declared, so that it can block, or in the opponent's end step. Each test names what a good player does.
 * A failure is a rule that is too strict or a rollout that cannot see the gain; the message gives the play chosen.
 */
public class FlatMonteCarloDefenseTest extends AITest {
    private static final int LONG = 240_000;

    private Game game;
    private Player ai;
    private Player opp;

    private void setUp() {
        game = initAndCreateGame();
        game.AI_FLAT_MONTE_CARLO = true;
        game.AI_TIMEOUT = 10;
        ai = game.getPlayers().get(1);
        opp = game.getPlayers().get(0);
        fillLibrary(ai, 20);
        fillLibrary(opp, 20);
    }

    /** The AI's decision at that phase of the turn of {@code turnOf}; the combat is set after the phase (devModeSet ends it). */
    private String nextPlay(final PhaseType phase, final Player turnOf, final Combat combat) {
        game.getPhaseHandler().devModeSet(phase, turnOf);
        if (combat != null) {
            game.getPhaseHandler().setCombat(combat);
        }
        game.getAction().checkStateEffects(true);
        final List<SpellAbility> chosen = ((PlayerControllerAi) ai.getController()).chooseSpellAbilityToPlay();
        return chosen == null || chosen.isEmpty() ? null : chosen.get(0).getHostCard().getName();
    }

    private Card ready(final String name, final Player owner) {
        final Card c = addCardToZone(name, owner, ZoneType.Battlefield);
        c.setSickness(false);
        return c;
    }

    /** The opponent's attacker is blocked by the AI's creature. */
    private Combat blocked(final Card attacker, final Card blocker) {
        final Combat combat = new Combat(opp);
        combat.addAttacker(attacker, ai);
        combat.addBlocker(attacker, blocker);
        combat.setBlocked(attacker, true);
        return combat;
    }

    // ------------------------------------------------------------------------------------------ after the block

    @Test(timeOut = LONG)
    public void theBlockerIsPumpedAfterTheBlock() {
        setUp();
        addCards("Forest", 1, ai);
        addCardToZone("Giant Growth", ai, ZoneType.Hand);
        final Card bears = ready("Grizzly Bears", ai);
        final Card giant = ready("Hill Giant", opp);
        final String play = nextPlay(PhaseType.COMBAT_DECLARE_BLOCKERS, opp, blocked(giant, bears));
        AssertJUnit.assertEquals("a 3/3 kills the Bears, a 5/5 Bears kill it", "Giant Growth", play);
    }

    @Test(timeOut = LONG)
    public void theTrickIsKeptWhenTheBlockerWinsAnyway() {
        setUp();
        addCards("Forest", 1, ai);
        addCardToZone("Giant Growth", ai, ZoneType.Hand);
        final Card wurm = ready("Craw Wurm", ai); // 6/4
        final Card bears = ready("Grizzly Bears", opp);
        final String play = nextPlay(PhaseType.COMBAT_DECLARE_BLOCKERS, opp, blocked(bears, wurm));
        AssertJUnit.assertNull("the Wurm eats the Bears without help (the AI chose " + play + ")", play);
    }

    // ------------------------------------------------------------------------------------------ the surprise

    @Test(timeOut = LONG)
    public void anAmuletPutsABlockerInAfterTheAttack() {
        setUp();
        addCards("Plains", 4, ai);
        ready("Quicksilver Amulet", ai);
        addCardToZone("Hill Giant", ai, ZoneType.Hand);
        final Card bears = ready("Grizzly Bears", opp);
        final Combat combat = new Combat(opp);
        combat.addAttacker(bears, ai);
        final String play = nextPlay(PhaseType.COMBAT_DECLARE_ATTACKERS, opp, combat);
        AssertJUnit.assertEquals("a 3/3 comes in and blocks the 2/2", "Quicksilver Amulet", play);
    }

    @Test(timeOut = LONG)
    public void anAmuletDoesNotBlockAFlyerWithABear() {
        setUp();
        addCards("Plains", 4, ai);
        ready("Quicksilver Amulet", ai);
        addCardToZone("Hill Giant", ai, ZoneType.Hand);
        final Card angel = ready("Serra Angel", opp);
        final Combat combat = new Combat(opp);
        combat.addAttacker(angel, ai);
        final String play = nextPlay(PhaseType.COMBAT_DECLARE_ATTACKERS, opp, combat);
        AssertJUnit.assertNull("the Giant cannot block a flyer: the end step is the moment (the AI chose " + play + ")", play);
    }

    @Test(timeOut = LONG)
    public void anAmuletComesInAtTheEndOfTheOpponentsTurn() {
        setUp();
        addCards("Plains", 4, ai);
        ready("Quicksilver Amulet", ai);
        addCardToZone("Craw Wurm", ai, ZoneType.Hand); // six mana: only the Amulet can put it in
        final String play = nextPlay(PhaseType.END_OF_TURN, opp, null);
        AssertJUnit.assertEquals("safe from a sorcery, and ready to attack on the AI's turn", "Quicksilver Amulet", play);
    }

    @Test(timeOut = LONG)
    public void anAmuletWaitsInTheMainPhase() {
        setUp();
        addCards("Plains", 4, ai);
        ready("Quicksilver Amulet", ai);
        addCardToZone("Craw Wurm", ai, ZoneType.Hand);
        final String play = nextPlay(PhaseType.MAIN2, ai, null);
        AssertJUnit.assertFalse("the opponent would see it, and a sorcery could answer it (the AI chose " + play + ")",
                "Quicksilver Amulet".equals(play));
    }

    @Test(timeOut = LONG)
    public void sneakAttackBeforeTheAttackForAHastyBeast() {
        setUp();
        addCards("Mountain", 1, ai);
        ready("Sneak Attack", ai);
        addCardToZone("Craw Wurm", ai, ZoneType.Hand);
        final String play = nextPlay(PhaseType.MAIN1, ai, null);
        AssertJUnit.assertEquals("a 6/4 with haste against an empty board", "Sneak Attack", play);
    }
}
