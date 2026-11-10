package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.simulation.SurpriseEntry;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Quicksilver Amulet, Elvish Piper, Sneak Attack: the moments at which putting a creature from the hand onto the
 * battlefield is a surprise, and the creatures worth it at each (see {@link SurpriseEntry}).
 */
public class FlatMonteCarloSurpriseEntryTest extends AITest {
    private Game game;
    private Player ai;
    private Player opp;

    private void setUp() {
        game = initAndCreateGame();
        ai = game.getPlayers().get(1);
        opp = game.getPlayers().get(0);
    }

    private SpellAbility ability(final String name) {
        final Card c = addCardToZone(name, ai, ZoneType.Battlefield);
        for (final SpellAbility sa : c.getSpellAbilities()) {
            if (sa.isActivatedAbility() && !sa.isManaAbility()) {
                sa.setActivatingPlayer(ai);
                return sa;
            }
        }
        throw new IllegalStateException("no activated ability on " + name);
    }

    /** The opponent attacks the AI with a creature (flying or not) and the blockers are not declared yet. */
    private void opponentAttacks(final String attackerName) {
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_ATTACKERS, opp);
        final Card attacker = addCardToZone(attackerName, opp, ZoneType.Battlefield);
        final Combat combat = new Combat(opp);
        combat.addAttacker(attacker, ai);
        game.getPhaseHandler().setCombat(combat); // after devModeSet, which ends the combat
    }

    @Test
    public void theAbilitiesThatPutACreatureOntoTheBattlefieldAreRecognized() {
        setUp();
        AssertJUnit.assertTrue(SurpriseEntry.isEntryAbility(ability("Quicksilver Amulet")));
        AssertJUnit.assertTrue(SurpriseEntry.isEntryAbility(ability("Elvish Piper")));
        AssertJUnit.assertTrue(SurpriseEntry.isEntryAbility(ability("Sneak Attack")));
        AssertJUnit.assertFalse("a pinger is not one", SurpriseEntry.isEntryAbility(ability("Prodigal Sorcerer")));
    }

    @Test
    public void sneakAttackGivesHaste() {
        setUp();
        final SpellAbility sneak = ability("Sneak Attack");
        final Card bears = addCardToZone("Grizzly Bears", ai, ZoneType.Hand);
        AssertJUnit.assertTrue(SurpriseEntry.hasteOnEntry(sneak, bears));
        AssertJUnit.assertFalse(SurpriseEntry.hasteOnEntry(ability("Quicksilver Amulet"), bears));
    }

    @Test
    public void theEndStepOfTheOpponentIsAlwaysAMoment() {
        setUp();
        final SpellAbility amulet = ability("Quicksilver Amulet");
        addCardToZone("Grizzly Bears", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.END_OF_TURN, opp);
        AssertJUnit.assertEquals(SurpriseEntry.Window.OPPONENT_END_STEP, SurpriseEntry.window(ai));
        AssertJUnit.assertTrue(SurpriseEntry.offered(ai, amulet));
    }

    @Test
    public void nothingIsOfferedWithoutACreatureInHand() {
        setUp();
        final SpellAbility amulet = ability("Quicksilver Amulet");
        addCardToZone("Lightning Bolt", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.END_OF_TURN, opp);
        AssertJUnit.assertFalse(SurpriseEntry.offered(ai, amulet));
    }

    @Test
    public void inTheMainPhaseAnAmuletWaitsForTheOpponentsTurn() {
        setUp();
        final SpellAbility amulet = ability("Quicksilver Amulet");
        addCardToZone("Grizzly Bears", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        AssertJUnit.assertFalse("the creature could not attack, and the opponent would see it", SurpriseEntry.offered(ai, amulet));
    }

    @Test
    public void beforeOwnCombatOnlyForACreatureThatAttacksAtOnce() {
        setUp();
        final SpellAbility amulet = ability("Quicksilver Amulet");
        final SpellAbility sneak = ability("Sneak Attack");
        addCardToZone("Grizzly Bears", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        AssertJUnit.assertFalse("the Amulet gives no haste", SurpriseEntry.offered(ai, amulet));
        AssertJUnit.assertTrue("Sneak Attack does", SurpriseEntry.offered(ai, sneak));
    }

    @Test
    public void afterTheAttackersAreDeclaredACreatureThatCanBlockIsAMoment() {
        setUp();
        final SpellAbility piper = ability("Elvish Piper");
        addCardToZone("Grizzly Bears", ai, ZoneType.Hand);
        opponentAttacks("Hill Giant");
        AssertJUnit.assertEquals(SurpriseEntry.Window.OPPONENT_ATTACKS, SurpriseEntry.window(ai));
        AssertJUnit.assertTrue(SurpriseEntry.offered(ai, piper));
    }

    @Test
    public void aCreatureThatCannotBlockAFlyerIsNotAMoment() {
        setUp();
        final SpellAbility piper = ability("Elvish Piper");
        addCardToZone("Grizzly Bears", ai, ZoneType.Hand);
        opponentAttacks("Serra Angel");
        AssertJUnit.assertFalse("the Bears cannot block a flyer", SurpriseEntry.offered(ai, piper));
        addCardToZone("Pegasus Courser", ai, ZoneType.Hand); // a flyer
        AssertJUnit.assertTrue("a flyer can", SurpriseEntry.offered(ai, piper));
    }

    @Test
    public void noAttackNoMoment() {
        setUp();
        addCardToZone("Grizzly Bears", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_ATTACKERS, opp);
        AssertJUnit.assertEquals(SurpriseEntry.Window.NONE, SurpriseEntry.window(ai));
    }
}
