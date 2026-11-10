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
 * Scenarios for the activated abilities of Flat Monte Carlo, played through the AI's own decision ({@code
 * chooseSpellAbilityToPlay}): does it use them when they pay, and keep them when they do not? Each test names what a
 * good player would do. A failure is either a rule that is too strict or a rollout that cannot see the gain; the
 * message gives the play the AI chose.
 */
public class FlatMonteCarloActivatedScenarioTest extends AITest {
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

    private String nextPlay(final PhaseType phase) {
        return nextPlay(phase, null);
    }

    /** The combat is set after the phase: devModeSet ends the combat it finds. */
    private String nextPlay(final PhaseType phase, final Combat combat) {
        game.getPhaseHandler().devModeSet(phase, ai);
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

    // ------------------------------------------------------------------------------------------ after the blocks

    /** The Bears are blocked by a Giant: Giant Growth makes them win the fight. */
    @Test(timeOut = LONG)
    public void aTrickWinsABlockedFight() {
        setUp();
        addCards("Forest", 1, ai);
        addCardToZone("Giant Growth", ai, ZoneType.Hand);
        final Card bears = ready("Grizzly Bears", ai);
        final Card giant = ready("Hill Giant", opp);
        final Combat combat = new Combat(ai);
        combat.addAttacker(bears, opp);
        combat.addBlocker(bears, giant);
        combat.setBlocked(bears, true);
        final String play = nextPlay(PhaseType.COMBAT_DECLARE_BLOCKERS, combat);
        AssertJUnit.assertEquals("the Bears die to the Giant without it", "Giant Growth", play);
    }

    /** The Wurm is blocked by a Bears and wins anyway: the trick stays in the hand. */
    @Test(timeOut = LONG)
    public void aTrickIsKeptWhenTheAttackerWinsAnyway() {
        setUp();
        addCards("Forest", 1, ai);
        addCardToZone("Giant Growth", ai, ZoneType.Hand);
        final Card wurm = ready("Craw Wurm", ai);
        final Card bears = ready("Grizzly Bears", opp);
        final Combat combat = new Combat(ai);
        combat.addAttacker(wurm, opp);
        combat.addBlocker(wurm, bears);
        combat.setBlocked(wurm, true);
        final String play = nextPlay(PhaseType.COMBAT_DECLARE_BLOCKERS, combat);
        AssertJUnit.assertNull("a 6/4 beats a 2/2 without help, the Growth is worth more later (the AI chose " + play + ")", play);
    }

    /** Firebreathing after the block turns a lost fight into a won one. */
    @Test(timeOut = LONG)
    public void firebreathingAfterTheBlock() {
        setUp();
        addCards("Mountain", 4, ai);
        final Card dragon = ready("Shivan Dragon", ai); // 5/5 flyer
        final Card blocker = ready("Pegasus Courser", opp); // 1/3 flyer: the dragon wins anyway
        final Combat combat = new Combat(ai);
        combat.addAttacker(dragon, opp);
        combat.addBlocker(dragon, blocker);
        combat.setBlocked(dragon, true);
        final String play = nextPlay(PhaseType.COMBAT_DECLARE_BLOCKERS, combat);
        AssertJUnit.assertNull("the dragon already wins, pumping only spends mana (the AI chose " + play + ")", play);
    }

    // ------------------------------------------------------------------------------------------ the main phase

    @Test(timeOut = LONG)
    public void unearthWhenTheManaIsSpareAndTheWayIsClear() {
        setUp();
        addCards("Swamp", 2, ai);
        addCardToZone("Dregscape Zombie", ai, ZoneType.Graveyard);
        AssertJUnit.assertEquals("a haste 2/1 against an empty board", "Dregscape Zombie", nextPlay(PhaseType.MAIN1));
    }

    @Test(timeOut = LONG)
    public void unearthWaitsWhenACreatureIsToBeCast() {
        setUp();
        addCards("Swamp", 3, ai);
        addCardToZone("Dregscape Zombie", ai, ZoneType.Graveyard);
        addCardToZone("Vampire Nighthawk", ai, ZoneType.Hand);
        final String play = nextPlay(PhaseType.MAIN1);
        AssertJUnit.assertFalse("the three mana are for the Nighthawk, not the unearth (the AI chose " + play + ")",
                "Dregscape Zombie".equals(play));
    }

    @Test(timeOut = LONG)
    public void embalmWithNothingElseToDo() {
        setUp();
        addCards("Plains", 1, ai);
        addCardToZone("Sacred Cat", ai, ZoneType.Graveyard);
        AssertJUnit.assertEquals("a lifelink token for {W}", "Sacred Cat", nextPlay(PhaseType.MAIN1));
    }

    @Test(timeOut = LONG)
    public void eternalizeWithFiveLands() {
        setUp();
        addCards("Plains", 5, ai);
        addCardToZone("Adorned Pouncer", ai, ZoneType.Graveyard);
        AssertJUnit.assertEquals("a 4/4 double striker for five", "Adorned Pouncer", nextPlay(PhaseType.MAIN1));
    }

    @Test(timeOut = LONG)
    public void scavengeOntoACreature() {
        setUp();
        ready("Grizzly Bears", ai);
        addCardToZone("Slitherhead", ai, ZoneType.Graveyard);
        AssertJUnit.assertEquals("two +1/+1 counters for {0}", "Slitherhead", nextPlay(PhaseType.MAIN1));
    }

    @Test(timeOut = LONG)
    public void aPingerKillsAWeakCreature() {
        setUp();
        ready("Prodigal Sorcerer", ai);
        ready("Llanowar Elves", opp);
        AssertJUnit.assertEquals("the Elves die to the ping", "Prodigal Sorcerer", nextPlay(PhaseType.MAIN1));
    }

    @Test(timeOut = LONG)
    public void firebreathingIsNotUsedInTheMainPhase() {
        setUp();
        addCards("Mountain", 4, ai);
        ready("Shivan Dragon", ai);
        final String play = nextPlay(PhaseType.MAIN1);
        AssertJUnit.assertFalse("the opponent would see the pump before the block (the AI chose " + play + ")",
                "Shivan Dragon".equals(play));
    }

    @Test(timeOut = LONG)
    public void aTokenMakerSpendsSpareManaAtTheEnd() {
        setUp();
        addCards("Forest", 2, ai);
        ready("Ant Queen", ai);
        AssertJUnit.assertEquals("{1}{G}: a 1/1, nothing else to spend it on", "Ant Queen", nextPlay(PhaseType.MAIN2));
    }
}
