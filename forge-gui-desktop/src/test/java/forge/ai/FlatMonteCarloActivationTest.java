package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.simulation.ActivationGuard;
import forge.ai.simulation.ComboGuard;
import forge.ai.simulation.ComboPlan;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Activated abilities with nothing to act on are not discovered by Flat Monte Carlo.
 *
 * <p>Report: the AI tapped Oran-Rief, the Vastwood every turn with no green creature that entered this turn. The
 * heuristics refuse it, but discovery re-adds counter effects and the rollouts cannot see the wasted tap.
 */
public class FlatMonteCarloActivationTest extends AITest {
    private Game game;
    private Player ai;

    private void setUp() {
        game = initAndCreateGame();
        game.AI_FLAT_MONTE_CARLO = true;
        game.AI_TIMEOUT = 10;
        ai = game.getPlayers().get(1);
    }

    private SpellAbility ability(final Card c) {
        for (final SpellAbility sa : c.getSpellAbilities()) {
            if (sa.isActivatedAbility() && !sa.isManaAbility()) {
                sa.setActivatingPlayer(ai);
                return sa;
            }
        }
        throw new IllegalStateException("no activated ability on " + c);
    }

    private SpellAbility oran() {
        return ability(addCardToZone("Oran-Rief, the Vastwood", ai, ZoneType.Battlefield));
    }

    @Test
    public void noCreaturesAtAll() {
        setUp();
        AssertJUnit.assertEquals("nothing-to-affect", ActivationGuard.problem(ai, oran()));
    }

    @Test
    public void aNonGreenCreatureThatEnteredThisTurn() {
        setUp();
        final SpellAbility sa = oran();
        addCardToZone("Hill Giant", ai, ZoneType.Battlefield).setSickness(true);
        AssertJUnit.assertEquals("nothing-to-affect", ActivationGuard.problem(ai, sa));
    }

    @Test
    public void aGreenCreatureOfTheOpponentIsNotAHelp() {
        setUp();
        final SpellAbility sa = oran();
        addCardToZone("Grizzly Bears", game.getPlayers().get(0), ZoneType.Battlefield);
        AssertJUnit.assertEquals("nothing-to-affect", ActivationGuard.problem(ai, sa));
    }

    @Test
    public void aGreenCreatureThatJustEnteredIsPlayed() {
        setUp();
        final SpellAbility sa = oran();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        addCardToZone("Grizzly Bears", ai, ZoneType.Battlefield);
        AssertJUnit.assertNull("a Bears that entered this turn", ActivationGuard.problem(ai, sa));
    }

    @Test
    public void theComboGuardRefusesTheIdleAbility() {
        setUp();
        AssertJUnit.assertEquals("nothing-to-affect", ComboGuard.problem(ai, oran(), null, ComboPlan.EMPTY));
    }

    @Test
    public void aTargetedAbilityWithNoTargetIsIdle() {
        setUp();
        // Prodigal Sorcerer: tap to deal 1 damage to any target, always has a player to hit: never idle
        final SpellAbility ping = ability(addCardToZone("Prodigal Sorcerer", ai, ZoneType.Battlefield));
        AssertJUnit.assertNull("a player is always a legal target", ActivationGuard.problem(ai, ping));
        // Icy Manipulator-like: tap target artifact, creature or land - there is always a land? use a creature-only one
        final SpellAbility tapper = ability(addCardToZone("Master Decoy", ai, ZoneType.Battlefield));
        // the only creature is the Decoy itself, which is a legal target of "tap target creature"
        AssertJUnit.assertNull(ActivationGuard.problem(ai, tapper));
    }

    @Test
    public void spellsAndManaAbilitiesAreNotJudged() {
        setUp();
        final Card forest = addCardToZone("Forest", ai, ZoneType.Battlefield);
        AssertJUnit.assertFalse(ActivationGuard.isActivated(forest.getManaAbilities().get(0)));
    }

    /** Cards put straight into play have no counters: a Planeswalker its loyalty, a Carnifex Demon its -1/-1 counters. */
    private Card withCounters(final Card c, final String type, final int amount) {
        c.addCounterInternal(forge.game.card.CounterType.getType(type), amount, ai, false, null, null);
        return c;
    }

    private Card theirs(final String name) {
        return addCardToZone(name, game.getPlayers().get(0), ZoneType.Battlefield);
    }

    private SpellAbility mine(final String name) {
        return ability(addCardToZone(name, ai, ZoneType.Battlefield));
    }

    // ------------------------------------------------------------------------------------------ targeted abilities

    @Test
    public void royalAssassinWithNoTappedCreatureHasNoTarget() {
        setUp();
        final SpellAbility sa = mine("Royal Assassin");
        theirs("Hill Giant"); // untapped: not a legal target of "target tapped creature"
        AssertJUnit.assertEquals("no-valid-targets", ActivationGuard.problem(ai, sa));
    }

    @Test
    public void royalAssassinWithATappedCreatureIsPlayed() {
        setUp();
        final SpellAbility sa = mine("Royal Assassin");
        theirs("Hill Giant").setTapped(true);
        AssertJUnit.assertNull(ActivationGuard.problem(ai, sa));
    }

    @Test
    public void royalAssassinIsRefusedByTheComboGuardToo() {
        setUp();
        AssertJUnit.assertEquals("no-valid-targets", ComboGuard.problem(ai, mine("Royal Assassin"), null, ComboPlan.EMPTY));
    }

    // ------------------------------------------------------------------------------------------ damage to the opposing side

    @Test
    public void ashenFirebeastWithNothingToHurtIsIdle() {
        setUp();
        final SpellAbility sa = mine("Ashen Firebeast");
        AssertJUnit.assertEquals("only itself (the AI's own) is hit", "nothing-to-affect", ActivationGuard.problem(ai, sa));
    }

    @Test
    public void ashenFirebeastIgnoresAFlyer() {
        setUp();
        final SpellAbility sa = mine("Ashen Firebeast");
        theirs("Wind Drake");
        AssertJUnit.assertEquals("a flyer is not hit", "nothing-to-affect", ActivationGuard.problem(ai, sa));
    }

    @Test
    public void ashenFirebeastWithAnOpposingGroundCreatureIsPlayed() {
        setUp();
        final SpellAbility sa = mine("Ashen Firebeast");
        theirs("Grizzly Bears");
        AssertJUnit.assertNull(ActivationGuard.problem(ai, sa));
    }

    // ------------------------------------------------------------------------------------------ tap all

    @Test
    public void thundercloudElementalNeedsAnUntappedSmallOpposingCreature() {
        setUp();
        final SpellAbility sa = mine("Thundercloud Elemental");
        theirs("Hill Giant"); // toughness 3
        AssertJUnit.assertEquals("too big to be tapped by it", "nothing-to-affect", ActivationGuard.problem(ai, sa));
        final Card bears = theirs("Grizzly Bears");
        bears.setTapped(true);
        AssertJUnit.assertEquals("already tapped", "nothing-to-affect", ActivationGuard.problem(ai, sa));
        bears.setTapped(false);
        AssertJUnit.assertNull("an untapped Bears", ActivationGuard.problem(ai, sa));
    }

    // ------------------------------------------------------------------------------------------ untap all

    @Test
    public void myrGalvanizerNeedsAnotherTappedMyr() {
        setUp();
        final SpellAbility sa = mine("Myr Galvanizer");
        AssertJUnit.assertEquals("no other Myr", "nothing-to-affect", ActivationGuard.problem(ai, sa));
        final Card other = addCardToZone("Myr Galvanizer", ai, ZoneType.Battlefield);
        AssertJUnit.assertEquals("another Myr, but untapped", "nothing-to-affect", ActivationGuard.problem(ai, sa));
        other.setTapped(true);
        AssertJUnit.assertNull("a tapped Myr to untap", ActivationGuard.problem(ai, sa));
    }

    // ------------------------------------------------------------------------------------------ a boost and a curse

    @Test
    public void aBoostForOwnCreaturesIsPlayedWhenThereIsOne() {
        setUp();
        // Akroan Phalanx is itself a creature the AI controls: Creature.YouCtrl always matches it
        AssertJUnit.assertNull(ActivationGuard.problem(ai, mine("Akroan Phalanx")));
    }

    @Test
    public void aBoostIgnoresTheOpponentsCreatures() {
        setUp();
        final SpellAbility sa = mine("Akroan Phalanx");
        AssertJUnit.assertNull("its own source is enough", ActivationGuard.problem(ai, sa));
        theirs("Hill Giant");
        AssertJUnit.assertNull("their creature changes nothing", ActivationGuard.problem(ai, sa));
    }

    @Test
    public void aCurseNeedsAnOpposingCreature() {
        setUp();
        final SpellAbility sa = ability(withCounters(addCardToZone("Carnifex Demon", ai, ZoneType.Battlefield), "M1M1", 2));
        AssertJUnit.assertEquals("no other creature at all", "nothing-to-affect", ActivationGuard.problem(ai, sa));
        addCardToZone("Grizzly Bears", ai, ZoneType.Battlefield);
        AssertJUnit.assertEquals("only the AI's own creature would get the -1/-1", "nothing-to-affect",
                ActivationGuard.problem(ai, sa));
        theirs("Hill Giant");
        AssertJUnit.assertNull("an opposing creature", ActivationGuard.problem(ai, sa));
    }

    // ------------------------------------------------------------------------------------------ read from the ability

    /** The activated ability of the card whose cost script starts with the given text (a loyalty cost, say). */
    private SpellAbility withCost(final Card c, final String cost) {
        for (final SpellAbility sa : c.getSpellAbilities()) {
            if (sa.isActivatedAbility() && sa.hasParam("Cost") && sa.getParam("Cost").startsWith(cost)) {
                sa.setActivatingPlayer(ai);
                return sa;
            }
        }
        throw new IllegalStateException("no ability with cost " + cost + " on " + c);
    }

    @Test
    public void aPlusAbilityIsWorthItsLoyaltyEvenWithNothingToTarget() {
        setUp();
        final Card ajani = withCounters(addCardToZone("Ajani, Caller of the Pride", ai, ZoneType.Battlefield), "LOYALTY", 4);
        AssertJUnit.assertNull("+1 on up to one creature, none in play: the loyalty is the point",
                ActivationGuard.problem(ai, withCost(ajani, "AddCounter<1/LOYALTY>")));
    }

    @Test
    public void aMinusAbilityWithNothingToDoIsIdle() {
        setUp();
        final Card ajani = withCounters(addCardToZone("Ajani Goldmane", ai, ZoneType.Battlefield), "LOYALTY", 4);
        final SpellAbility counters = withCost(ajani, "SubCounter<1/LOYALTY>");
        AssertJUnit.assertEquals("-1: a counter on each creature, and there is none", "nothing-to-affect",
                ActivationGuard.problem(ai, counters));
        addCardToZone("Grizzly Bears", ai, ZoneType.Battlefield);
        AssertJUnit.assertNull("with a creature of the AI's", ActivationGuard.problem(ai, counters));
    }

    @Test
    public void aPlusAbilityWithNoEffectAtAllStillGainsLife() {
        setUp();
        final Card ajani = withCounters(addCardToZone("Ajani Goldmane", ai, ZoneType.Battlefield), "LOYALTY", 4);
        AssertJUnit.assertNull("+1: gain 2 life", ActivationGuard.problem(ai, withCost(ajani, "AddCounter<1/LOYALTY>")));
    }

    @Test
    public void anUpToAbilityWithNoCreatureAndAnEffectOnWhatItTargetedIsIdle() {
        setUp();
        final Card ajani = withCounters(addCardToZone("Ajani, Sleeper Agent", ai, ZoneType.Battlefield), "LOYALTY", 4);
        final SpellAbility minus = withCost(ajani, "SubCounter<3/LOYALTY>");
        AssertJUnit.assertEquals("up to three creatures, none in play; the vigilance goes to those targets too",
                "no-valid-targets", ActivationGuard.problem(ai, minus));
        addCardToZone("Grizzly Bears", ai, ZoneType.Battlefield);
        AssertJUnit.assertNull("a creature to put counters on", ActivationGuard.problem(ai, minus));
    }

    @Test
    public void anAbilityThatNeedsTwoTargetsNeedsTwoCandidates() {
        setUp();
        final SpellAbility untap = mine("Argothian Elder");
        addCardToZone("Forest", ai, ZoneType.Battlefield);
        AssertJUnit.assertEquals("one land: not the two it untaps", "no-valid-targets", ActivationGuard.problem(ai, untap));
        addCardToZone("Forest", ai, ZoneType.Battlefield);
        AssertJUnit.assertNull("two lands", ActivationGuard.problem(ai, untap));
    }

    @Test
    public void aCostThatNeedsAnotherCreatureIsRefusedWithoutOne() {
        setUp();
        final SpellAbility sa = mine("Blood Host");
        AssertJUnit.assertEquals("sacrifice another creature, and there is none", "cost-unpayable",
                ActivationGuard.problem(ai, sa));
        addCardToZone("Grizzly Bears", ai, ZoneType.Battlefield);
        AssertJUnit.assertNull("with a creature to sacrifice", ActivationGuard.problem(ai, sa));
    }

    @Test
    public void aSacrificeWithNothingToSacrificeAndNothingToReturnIsIdle() {
        setUp();
        final Card daretti = withCounters(addCardToZone("Daretti, Scrap Savant", ai, ZoneType.Battlefield), "LOYALTY", 3);
        final SpellAbility minus = withCost(daretti, "SubCounter<2/LOYALTY>");
        AssertJUnit.assertEquals("no artifact to sacrifice, none in the graveyard to return", "nothing-to-affect",
                ActivationGuard.problem(ai, minus));
        addCardToZone("Sol Ring", ai, ZoneType.Battlefield);
        AssertJUnit.assertNull("an artifact to sacrifice", ActivationGuard.problem(ai, minus));
    }

    // ------------------------------------------------------------------------------------------ outside its remit

    @Test
    public void aSpellInHandIsNotJudged() {
        setUp();
        final Card bolt = addCardToZone("Lightning Bolt", ai, ZoneType.Hand);
        final SpellAbility sa = bolt.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        AssertJUnit.assertFalse(ActivationGuard.isActivated(sa));
        AssertJUnit.assertNull(ActivationGuard.problem(ai, sa));
    }

    @Test
    public void anAbilityInHandIsNotJudged() {
        setUp();
        final Card firebeast = addCardToZone("Ashen Firebeast", ai, ZoneType.Hand);
        for (final SpellAbility sa : firebeast.getSpellAbilities()) {
            if (sa.isActivatedAbility()) {
                AssertJUnit.assertFalse("only battlefield abilities are judged", ActivationGuard.isActivated(sa));
            }
        }
    }

    @Test
    public void aNullAiOrAbilityIsHarmless() {
        setUp();
        AssertJUnit.assertNull(ActivationGuard.problem(null, null));
        AssertJUnit.assertNull(ActivationGuard.problem(ai, null));
        AssertJUnit.assertFalse(ActivationGuard.isActivated(null));
    }

    @Test(timeOut = 240_000)
    public void flatMcDoesNotTapOranRiefForNothing() {
        setUp();
        fillLibrary(ai, 20);
        fillLibrary(game.getPlayers().get(0), 20);
        addCardToZone("Oran-Rief, the Vastwood", ai, ZoneType.Battlefield);
        addCard("Hill Giant", ai);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        game.getAction().checkStateEffects(true);
        final java.util.List<SpellAbility> chosen = ((PlayerControllerAi) ai.getController()).chooseSpellAbilityToPlay();
        final String play = chosen == null || chosen.isEmpty() ? null : chosen.get(0).getHostCard().getName();
        AssertJUnit.assertFalse("no green creature entered: the AI chose " + play, "Oran-Rief, the Vastwood".equals(play));
    }
}
