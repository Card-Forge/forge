package forge.ai;

import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * The AI's own decisions, through the controller, for the plays of the activated-abilities work: forecast, plot, foretell
 * and suspend, morphs, hand tutors, landcycling, sacrifice outlets, mana left for a spell, tricks for several creatures
 * and an X of mana.
 * <p>
 * These are scenarios, not rules: a failure is a rule that is too strict, a rollout that cannot see the gain, or a
 * heuristic that overrides it. The message gives the play the AI chose.
 */
public class FlatMonteCarloSpecialScenarioTest extends AITest {
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

    /** What the AI does at that phase of the turn of {@code turnOf}; the combat is set after the phase (devModeSet ends it). */
    private SpellAbility choose(final PhaseType phase, final Player turnOf, final Combat combat) {
        game.getPhaseHandler().devModeSet(phase, turnOf);
        if (combat != null) {
            game.getPhaseHandler().setCombat(combat);
        }
        game.getAction().checkStateEffects(true);
        final List<SpellAbility> chosen = ((PlayerControllerAi) ai.getController()).chooseSpellAbilityToPlay();
        return chosen == null || chosen.isEmpty() ? null : chosen.get(0);
    }

    private static String show(final SpellAbility sa) {
        return sa == null ? "nothing" : sa.getHostCard().getName() + ": " + sa.getDescription();
    }

    private static boolean isHost(final SpellAbility sa, final String name) {
        return sa != null && name.equals(sa.getHostCard().getName());
    }

    private Card ready(final String name, final Player owner) {
        final Card c = addCardToZone(name, owner, ZoneType.Battlefield);
        c.setSickness(false);
        return c;
    }

    // ------------------------------------------------------------------------------------------------------ forecast

    @Test(timeOut = LONG)
    public void theUpkeepIsForTheForecastAbilityAndNothingElse() {
        setUp();
        addCards("Mountain", 2, ai);
        addCardToZone("Sky Hussar", ai, ZoneType.Hand);
        addCardToZone("Lightning Bolt", ai, ZoneType.Hand);
        for (int i = 0; i < 2; i++) {
            addCardToZone("Savannah Lions", ai, ZoneType.Battlefield).setSickness(true);
        }
        ready("Hill Giant", opp);
        final SpellAbility play = choose(PhaseType.UPKEEP, ai, null);
        AssertJUnit.assertTrue("the burn spell waits for the main phase: " + show(play), play == null || isHost(play, "Sky Hussar"));
    }

    @Test(timeOut = LONG)
    public void aForecastDoesNotTapTheCreaturesThatAttack() {
        setUp();
        addCardToZone("Sky Hussar", ai, ZoneType.Hand);
        ready("Savannah Lions", ai);
        ready("Savannah Lions", ai);
        final SpellAbility play = choose(PhaseType.UPKEEP, ai, null);
        AssertJUnit.assertFalse("two attackers against an empty board stay untapped: " + show(play), isHost(play, "Sky Hussar"));
    }

    // ------------------------------------------------------------------------------------- plot, foretell, suspend

    @Test(timeOut = LONG)
    public void aCardThatCannotBeCastIsForetold() {
        setUp();
        addCards("Island", 2, ai);
        addCardToZone("Behold the Multiverse", ai, ZoneType.Hand);
        final SpellAbility play = choose(PhaseType.MAIN2, ai, null);
        AssertJUnit.assertTrue("{2} now for a {1}{U} cast later: " + show(play), play != null && play.isForetelling());
    }

    @Test(timeOut = LONG)
    public void aCardThatCannotBeCastIsSuspended() {
        setUp();
        addCards("Mountain", 1, ai);
        addCardToZone("Rift Bolt", ai, ZoneType.Hand);
        ready("Hill Giant", opp);
        final SpellAbility play = choose(PhaseType.MAIN2, ai, null);
        AssertJUnit.assertTrue("{R} now for a free Rift Bolt next turn: " + show(play),
                play != null && play.isKeyword(Keyword.SUSPEND));
    }

    @Test(timeOut = LONG)
    public void aForetellDoesNotTakeTheManaOfACreatureThatIsCast() {
        setUp();
        addCards("Island", 2, ai);
        addCardToZone("Behold the Multiverse", ai, ZoneType.Hand);
        addCardToZone("Merfolk Looter", ai, ZoneType.Hand);
        final SpellAbility play = choose(PhaseType.MAIN2, ai, null);
        AssertJUnit.assertFalse("the two lands cast the Looter: " + show(play), play != null && play.isForetelling());
    }

    // ----------------------------------------------------------------------------------------------- morph

    @Test(timeOut = LONG)
    public void aMorphIsTurnedUpAfterTheBlockNotBefore() {
        setUp();
        addCards("Plains", 4, ai);
        final Card angel = ready("Exalted Angel", ai);
        angel.turnFaceDown();
        angel.setSickness(false);
        final SpellAbility before = choose(PhaseType.MAIN1, ai, null);
        AssertJUnit.assertFalse("the opponent would see it before the attack: " + show(before),
                before != null && before.isTurnFaceUp());
        final Card giant = ready("Hill Giant", opp);
        final Combat combat = new Combat(ai);
        combat.addAttacker(angel, opp);
        combat.addBlocker(angel, giant);
        combat.setBlocked(angel, true);
        final SpellAbility after = choose(PhaseType.COMBAT_DECLARE_BLOCKERS, ai, combat);
        AssertJUnit.assertTrue("a 4/5 lifelink flyer kills the blocking 3/3 and lives: " + show(after),
                after != null && after.isTurnFaceUp());
    }

    // ------------------------------------------------------------------------------------- tutors and land searches

    @Test(timeOut = LONG)
    public void transmuteIsNotUsedWithoutAPlanTarget() {
        setUp();
        addCards("Swamp", 3, ai);
        addCardToZone("Dimir Infiltrator", ai, ZoneType.Hand);
        for (int i = 0; i < 5; i++) {
            addCardToZone("Grizzly Bears", ai, ZoneType.Library);
        }
        final SpellAbility play = choose(PhaseType.MAIN1, ai, null);
        AssertJUnit.assertFalse("a card for a card and the mana: " + show(play),
                play != null && play.isActivatedAbility() && play.getApi() == ApiType.ChangeZone);
    }

    @Test(timeOut = LONG)
    public void aHandWithoutALandCyclesForOne() {
        setUp();
        addCards("Swamp", 2, ai);
        addCardToZone("Absorb Vis", ai, ZoneType.Hand);
        for (int i = 0; i < 5; i++) {
            addCardToZone("Swamp", ai, ZoneType.Library);
        }
        final SpellAbility play = choose(PhaseType.MAIN1, ai, null);
        AssertJUnit.assertTrue("basic landcycling for the land drop: " + show(play),
                isHost(play, "Absorb Vis") && play.getApi() == ApiType.ChangeZone);
    }

    @Test(timeOut = LONG)
    public void aHandWithALandDoesNotCycle() {
        setUp();
        addCards("Swamp", 2, ai);
        addCardToZone("Swamp", ai, ZoneType.Hand);
        addCardToZone("Absorb Vis", ai, ZoneType.Hand);
        for (int i = 0; i < 5; i++) {
            addCardToZone("Swamp", ai, ZoneType.Library);
        }
        final SpellAbility play = choose(PhaseType.MAIN1, ai, null);
        AssertJUnit.assertFalse("the land in hand is the land drop: " + show(play), isHost(play, "Absorb Vis"));
    }

    // -------------------------------------------------------------------------------------- outlets and mana

    @Test(timeOut = LONG)
    public void anOutletDoesNotEatTheOnlyGoodCreature() {
        setUp();
        addCardToZone("Goblin Bombardment", ai, ZoneType.Battlefield);
        ready("Serra Angel", ai);
        ready("Hill Giant", opp);
        final SpellAbility play = choose(PhaseType.MAIN1, ai, null);
        AssertJUnit.assertFalse("a Serra Angel for one damage: " + show(play), isHost(play, "Goblin Bombardment"));
    }

    @Test(timeOut = LONG)
    public void anEquipDoesNotTakeTheManaOfACreatureInHand() {
        setUp();
        addCards("Plains", 2, ai);
        addCardToZone("Short Sword", ai, ZoneType.Battlefield);
        ready("Savannah Lions", ai);
        addCardToZone("Silvercoat Lion", ai, ZoneType.Hand);
        final SpellAbility play = choose(PhaseType.MAIN1, ai, null);
        AssertJUnit.assertFalse("the two lands cast the Lion: " + show(play), isHost(play, "Short Sword"));
    }

    // ------------------------------------------------------------------------------------------- tricks

    @Test(timeOut = LONG)
    public void aTrickForTwoCreaturesIsPlayedWhenBothFightsAreEven() {
        setUp();
        addCards("Plains", 4, ai);
        final Card bears1 = ready("Grizzly Bears", ai);
        final Card bears2 = ready("Grizzly Bears", ai);
        final Card block1 = ready("Grizzly Bears", opp);
        final Card block2 = ready("Grizzly Bears", opp);
        addCardToZone("Appeal to Eirdu", ai, ZoneType.Hand);
        final Combat combat = new Combat(ai);
        combat.addAttacker(bears1, opp);
        combat.addAttacker(bears2, opp);
        combat.addBlocker(bears1, block1);
        combat.addBlocker(bears2, block2);
        combat.setBlocked(bears1, true);
        combat.setBlocked(bears2, true);
        final SpellAbility play = choose(PhaseType.COMBAT_DECLARE_BLOCKERS, ai, combat);
        AssertJUnit.assertTrue("+2/+1 wins both trades: " + show(play), isHost(play, "Appeal to Eirdu"));
        AssertJUnit.assertFalse("aimed at a creature in the fight", play.getTargets().isEmpty());
    }

    // ------------------------------------------------------------------------------------------- X of mana

    @Test(timeOut = LONG)
    public void anXAbilityKillsTheCreatureTheManaPaysFor() {
        setUp();
        addCards("Mountain", 4, ai);
        ready("Cinder Elemental", ai);
        ready("Hill Giant", opp);
        final SpellAbility play = choose(PhaseType.MAIN1, ai, null);
        AssertJUnit.assertTrue("X is 3 for a Hill Giant: " + show(play), isHost(play, "Cinder Elemental"));
    }

    @Test(timeOut = LONG)
    public void anXAbilityWaitsWhenTheManaIsForASpell() {
        setUp();
        addCards("Mountain", 4, ai);
        ready("Cinder Elemental", ai);
        ready("Hill Giant", opp);
        addCardToZone("Hill Giant", ai, ZoneType.Hand);
        final SpellAbility play = choose(PhaseType.MAIN1, ai, null);
        AssertJUnit.assertFalse("the four lands cast the Giant: " + show(play), isHost(play, "Cinder Elemental"));
    }

    // ------------------------------------------------------------------------------------------ copies and targets

    @Test(timeOut = LONG)
    public void aCopyOfABounceSpellIsAimedAtAnotherCreature() {
        setUp();
        final Card giant = ready("Hill Giant", opp);
        final Card bears = ready("Grizzly Bears", opp);
        final Card unsummon = addCardToZone("Unsummon", ai, ZoneType.Hand);
        ai.getZone(ZoneType.Hand).remove(unsummon);
        final SpellAbility original = unsummon.getFirstSpellAbility();
        original.setActivatingPlayer(ai);
        original.resetTargets();
        original.getTargets().add(giant);
        game.getStack().add(original);
        final SpellAbility copy = original.copy(ai);
        copy.resetTargets();
        copy.getTargets().add(giant);
        ((PlayerControllerAi) ai.getController()).chooseNewTargetsForCopy(copy);
        AssertJUnit.assertEquals("the copy bounces the other creature, as the AI's own choice for a copy", bears,
                copy.getTargets().getFirstTargetedCard());
    }

    @Test(timeOut = LONG)
    public void aTrickForTwoCreaturesWithOneInTheFightIsAimedAtThatOne() {
        setUp();
        addCards("Plains", 4, ai);
        final Card bears = ready("Grizzly Bears", ai);
        ready("Grizzly Bears", ai); // attacks too, but nobody blocks it
        final Card block = ready("Grizzly Bears", opp);
        addCardToZone("Appeal to Eirdu", ai, ZoneType.Hand);
        final Combat combat = new Combat(ai);
        combat.addAttacker(bears, opp);
        combat.addBlocker(bears, block);
        combat.setBlocked(bears, true);
        final SpellAbility play = choose(PhaseType.COMBAT_DECLARE_BLOCKERS, ai, combat);
        AssertJUnit.assertTrue("+2/+1 wins the one fight: " + show(play), isHost(play, "Appeal to Eirdu"));
        AssertJUnit.assertTrue("aimed at the blocked creature", play.getTargets().getTargetCards().contains(bears));
    }
}
