package forge.ai;

import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.simulation.ActivationGuard;
import forge.ai.simulation.FlatMonteCarloPicker;
import forge.ai.simulation.XChoice;
import forge.game.Game;
import forge.game.GameObject;
import forge.game.card.Card;
import forge.game.card.CounterType;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Planeswalker abilities under Flat Monte Carlo.
 *
 * <p>Report: the AI does not activate the first and second abilities of Sorin, Imperious Bloodlord ("target creature you
 * control gains deathtouch and lifelink", "you may sacrifice a Vampire"). The heuristics turn them down (a pump with no
 * attack in sight, an optional sacrifice), and discovery only added card draw, removal and developments, so no rollout
 * ever saw the loyalty they add. A loyalty ability that is not idle is now discovered, and aimed when it has a target.
 */
public class FlatMonteCarloPlaneswalkerTest extends AITest {
    private Game game;
    private Player ai;

    private void setUp() {
        game = initAndCreateGame();
        game.AI_FLAT_MONTE_CARLO = true;
        game.AI_TIMEOUT = 10;
        ai = game.getPlayers().get(1);
    }

    private Card sorin() {
        final Card c = addCardToZone("Sorin, Imperious Bloodlord", ai, ZoneType.Battlefield);
        c.addCounterInternal(CounterType.getType("LOYALTY"), 4, ai, false, null, null);
        return c;
    }

    private SpellAbility ability(final Card c, final String api) {
        for (final SpellAbility sa : c.getSpellAbilities()) {
            if (sa.isActivatedAbility() && sa.getApi() != null && sa.getApi().name().equals(api)) {
                sa.setActivatingPlayer(ai);
                return sa;
            }
        }
        throw new IllegalStateException("no " + api + " ability on " + c);
    }

    @Test
    public void sorinsPumpIsDiscoverable() {
        setUp();
        AssertJUnit.assertTrue("+1: deathtouch and lifelink", FlatMonteCarloPicker.isDiscoverable(ability(sorin(), "Pump")));
    }

    @Test
    public void sorinsSacrificeIsDiscoverable() {
        setUp();
        AssertJUnit.assertTrue("+1: you may sacrifice a Vampire", FlatMonteCarloPicker.isDiscoverable(ability(sorin(), "Sacrifice")));
    }

    @Test
    public void sorinsPumpIsAimedAtTheAisBestCreature() {
        setUp();
        final SpellAbility pump = ability(sorin(), "Pump");
        addCardToZone("Grizzly Bears", ai, ZoneType.Battlefield);
        final Card nighthawk = addCardToZone("Vampire Nighthawk", ai, ZoneType.Battlefield);
        addCardToZone("Hill Giant", game.getPlayers().get(0), ZoneType.Battlefield);
        final GameObject target = FlatMonteCarloPicker.loyaltyTarget(ai, pump);
        AssertJUnit.assertEquals("the best of its own creatures, never the opponent's", nighthawk, target);
    }

    @Test
    public void sorinsPumpWithoutACreatureHasNoTarget() {
        setUp();
        final SpellAbility pump = ability(sorin(), "Pump");
        addCardToZone("Hill Giant", game.getPlayers().get(0), ZoneType.Battlefield);
        AssertJUnit.assertNull("only the opponent's creature is there", FlatMonteCarloPicker.loyaltyTarget(ai, pump));
    }

    @Test
    public void anAbilityThatIsNotAPlaneswalkerIsNotDiscoveredByThisRule() {
        setUp();
        final Card bears = addCardToZone("Grizzly Bears", ai, ZoneType.Battlefield);
        for (final SpellAbility sa : bears.getSpellAbilities()) {
            AssertJUnit.assertFalse(sa.isActivatedAbility() && FlatMonteCarloPicker.isDiscoverable(sa));
        }
    }

    private String firstPlayHost() {
        fillLibrary(ai, 20);
        fillLibrary(game.getPlayers().get(0), 20);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        final List<SpellAbility> chosen = ((PlayerControllerAi) ai.getController()).chooseSpellAbilityToPlay();
        return chosen == null || chosen.isEmpty() ? null : chosen.get(0).getHostCard().getName();
    }

    @Test(timeOut = 240_000)
    public void flatMcActivatesSorinWithAVampireInPlay() {
        setUp();
        sorin();
        addCardToZone("Vampire Nighthawk", ai, ZoneType.Battlefield);
        AssertJUnit.assertEquals("a loyalty ability and a creature to aim it at", "Sorin, Imperious Bloodlord", firstPlayHost());
    }

    @Test(timeOut = 240_000)
    public void flatMcActivatesSorinEvenWithoutACreature() {
        setUp();
        sorin();
        AssertJUnit.assertEquals("the second +1 needs no target: the loyalty is the point", "Sorin, Imperious Bloodlord",
                firstPlayHost());
    }

    // ------------------------------------------------------------------------------------------ one of each kind

    private Card walker(final String name, final int loyalty) {
        final Card c = addCardToZone(name, ai, ZoneType.Battlefield);
        c.addCounterInternal(CounterType.getType("LOYALTY"), loyalty, ai, false, null, null);
        return c;
    }

    private Card theirs(final String name) {
        return addCardToZone(name, game.getPlayers().get(0), ZoneType.Battlefield);
    }

    /** A plus ability that draws, with no target: Teferi, Hero of Dominaria. */
    @Test
    public void aDrawingPlusAbilityIsDiscoverable() {
        setUp();
        AssertJUnit.assertTrue(FlatMonteCarloPicker.isDiscoverable(ability(walker("Teferi, Hero of Dominaria", 4), "Draw")));
    }

    /** A zero ability that makes a token: Gideon, Ally of Zendikar. It adds no loyalty, and is still a play. */
    @Test
    public void aTokenMakingZeroAbilityIsDiscoverableAndNotIdle() {
        setUp();
        final SpellAbility token = ability(walker("Gideon, Ally of Zendikar", 4), "Token");
        AssertJUnit.assertTrue(FlatMonteCarloPicker.isDiscoverable(token));
        AssertJUnit.assertNull("a token is something", ActivationGuard.problem(ai, token));
    }

    /** A plus ability aimed at one's own creature: Elspeth, Knight-Errant. */
    @Test
    public void anEncouragingAbilityIsAimedAtTheAisOwnCreature() {
        setUp();
        final SpellAbility pump = ability(walker("Elspeth, Knight-Errant", 4), "Pump");
        theirs("Hill Giant");
        final Card nighthawk = addCardToZone("Vampire Nighthawk", ai, ZoneType.Battlefield);
        AssertJUnit.assertTrue(FlatMonteCarloPicker.isDiscoverable(pump));
        AssertJUnit.assertEquals(nighthawk, FlatMonteCarloPicker.loyaltyTarget(ai, pump));
    }

    @Test
    public void anEncouragingAbilityWithOnlyTheirCreaturesHasNoTarget() {
        setUp();
        final SpellAbility pump = ability(walker("Elspeth, Knight-Errant", 4), "Pump");
        theirs("Hill Giant");
        AssertJUnit.assertNull(FlatMonteCarloPicker.loyaltyTarget(ai, pump));
    }

    /** A minus ability that removes a creature: Chandra, Torch of Defiance. */
    @Test
    public void aRemovalAbilityIsAimedAtTheirCreature() {
        setUp();
        final SpellAbility burn = ability(walker("Chandra, Torch of Defiance", 4), "DealDamage");
        addCardToZone("Vampire Nighthawk", ai, ZoneType.Battlefield);
        final Card giant = theirs("Hill Giant");
        AssertJUnit.assertTrue(FlatMonteCarloPicker.isDiscoverable(burn));
        AssertJUnit.assertEquals("never its own creature", giant, FlatMonteCarloPicker.loyaltyTarget(ai, burn));
    }

    @Test
    public void aRemovalAbilityWithOnlyItsOwnCreaturesHasNoTarget() {
        setUp();
        final SpellAbility burn = ability(walker("Chandra, Torch of Defiance", 4), "DealDamage");
        addCardToZone("Vampire Nighthawk", ai, ZoneType.Battlefield);
        AssertJUnit.assertNull(FlatMonteCarloPicker.loyaltyTarget(ai, burn));
    }

    @Test
    public void aRemovalAbilityWithNoCreatureAtAllIsIdle() {
        setUp();
        final SpellAbility burn = ability(walker("Chandra, Torch of Defiance", 4), "DealDamage");
        AssertJUnit.assertEquals("no-valid-targets", ActivationGuard.problem(ai, burn));
        theirs("Hill Giant");
        AssertJUnit.assertNull(ActivationGuard.problem(ai, burn));
    }

    /** A minus ability aimed at a player: Liliana of the Veil makes the opponent sacrifice. */
    @Test
    public void aPlayerTargetingAbilityIsAimedAtTheOpponent() {
        setUp();
        final SpellAbility edict = ability(walker("Liliana of the Veil", 3), "Sacrifice");
        theirs("Hill Giant");
        AssertJUnit.assertEquals(game.getPlayers().get(0), FlatMonteCarloPicker.loyaltyTarget(ai, edict));
    }

    /** Two targets (Garruk's untap of two lands) cannot be aimed with one: discovered, but left to the heuristics. */
    @Test
    public void anAbilityWithTwoTargetsIsNotAimedHere() {
        setUp();
        final SpellAbility untap = ability(walker("Garruk Wildspeaker", 3), "Untap");
        addCardToZone("Forest", ai, ZoneType.Battlefield);
        addCardToZone("Forest", ai, ZoneType.Battlefield);
        AssertJUnit.assertTrue(FlatMonteCarloPicker.isDiscoverable(untap));
        AssertJUnit.assertNull(FlatMonteCarloPicker.loyaltyTarget(ai, untap));
    }

    /** A token maker that costs loyalty: Garruk's -1. */
    @Test
    public void aMinusTokenMakerIsDiscoverable() {
        setUp();
        AssertJUnit.assertTrue(FlatMonteCarloPicker.isDiscoverable(ability(walker("Garruk Wildspeaker", 3), "Token")));
    }

    /** An ultimate that costs X is never discovered: Chandra, Flamecaller. */
    @Test
    public void anAbilityWithXInItsLoyaltyCostIsNotDiscovered() {
        setUp();
        AssertJUnit.assertFalse(FlatMonteCarloPicker.isDiscoverable(ability(walker("Chandra, Flamecaller", 4), "DamageAll")));
    }

    /** Card selection: Chandra, Torch of Defiance's +1 digs. */
    @Test
    public void aDiggingPlusAbilityIsDiscoverable() {
        setUp();
        AssertJUnit.assertTrue(FlatMonteCarloPicker.isDiscoverable(ability(walker("Chandra, Torch of Defiance", 4), "Dig")));
    }

    @Test(timeOut = 240_000)
    public void flatMcActivatesElspethWithACreatureToEncourage() {
        setUp();
        walker("Elspeth, Knight-Errant", 4);
        addCardToZone("Vampire Nighthawk", ai, ZoneType.Battlefield);
        AssertJUnit.assertEquals("Elspeth, Knight-Errant", firstPlayHost());
    }

    @Test(timeOut = 240_000)
    public void flatMcActivatesTeferi() {
        setUp();
        walker("Teferi, Hero of Dominaria", 4);
        AssertJUnit.assertEquals("Teferi, Hero of Dominaria", firstPlayHost());
    }

    // ------------------------------------------------------------------------------------------ ultimates and the rest

    private SpellAbility withCost(final Card c, final String cost) {
        for (final SpellAbility sa : c.getSpellAbilities()) {
            if (sa.isActivatedAbility() && sa.hasParam("Cost") && sa.getParam("Cost").startsWith(cost)) {
                sa.setActivatingPlayer(ai);
                return sa;
            }
        }
        throw new IllegalStateException("no ability with cost " + cost + " on " + c);
    }

    /**
     * An emblem ultimate (Chandra, Torch of Defiance): discovered and not idle, but the rollouts cannot see what an
     * emblem is worth, only the loyalty it costs, so it is played when the heuristics approve it and not otherwise.
     */
    @Test
    public void anEmblemUltimateIsAPlayForTheRollouts() {
        setUp();
        final SpellAbility ultimate = withCost(walker("Chandra, Torch of Defiance", 7), "SubCounter<7/LOYALTY>");
        AssertJUnit.assertTrue(FlatMonteCarloPicker.isDiscoverable(ultimate));
        AssertJUnit.assertNull(ActivationGuard.problem(ai, ultimate));
    }

    /** A minus ability that puts a card from the hand into play: Sorin, Imperious Bloodlord's -3. */
    @Test
    public void aPutFromHandAbilityNeedsTheCardInHand() {
        setUp();
        final SpellAbility put = ability(walker("Sorin, Imperious Bloodlord", 4), "ChangeZone");
        AssertJUnit.assertEquals("no Vampire in hand", "nothing-to-affect", ActivationGuard.problem(ai, put));
        addCardToZone("Vampire Nighthawk", ai, ZoneType.Hand);
        AssertJUnit.assertNull("a Vampire in hand", ActivationGuard.problem(ai, put));
        AssertJUnit.assertTrue(FlatMonteCarloPicker.isDiscoverable(put));
    }

    /** An animate: Gideon, Ally of Zendikar's +1 becomes a creature. */
    @Test
    public void anAnimatingPlusAbilityIsDiscoverable() {
        setUp();
        final SpellAbility animate = ability(walker("Gideon, Ally of Zendikar", 4), "Animate");
        AssertJUnit.assertTrue(FlatMonteCarloPicker.isDiscoverable(animate));
        AssertJUnit.assertNull(ActivationGuard.problem(ai, animate));
    }

    /** Each player discards: Liliana of the Veil's +1. */
    @Test
    public void aDiscardPlusAbilityIsDiscoverable() {
        setUp();
        AssertJUnit.assertTrue(FlatMonteCarloPicker.isDiscoverable(ability(walker("Liliana of the Veil", 3), "Discard")));
    }

    // ------------------------------------------------------------------------------------------ X loyalty, carefully

    private SpellAbility chandraNalaarX(final int loyalty) {
        return withCost(walker("Chandra Nalaar", loyalty), "SubCounter<X/LOYALTY>");
    }

    @Test
    public void xDamageIsExactlyWhatKillsTheBestCreatureItCan() {
        setUp();
        final SpellAbility xDamage = chandraNalaarX(6);
        final Card giant = theirs("Hill Giant"); // 3/3
        theirs("Grizzly Bears");                 // 2/2
        final XChoice.LoyaltyPlan plan = XChoice.loyaltyPlan(ai, xDamage);
        AssertJUnit.assertNotNull(plan);
        AssertJUnit.assertEquals("the toughness of the Giant, no more", 3, plan.x);
        AssertJUnit.assertEquals(giant, plan.target);
        AssertJUnit.assertTrue("so it is a play for the rollouts", FlatMonteCarloPicker.isDiscoverable(xDamage));
    }

    @Test
    public void xDamageNeverKillsThePlaneswalker() {
        setUp();
        final SpellAbility xDamage = chandraNalaarX(2); // at most 1 may be paid
        theirs("Grizzly Bears");                        // needs 2
        AssertJUnit.assertNull(XChoice.loyaltyPlan(ai, xDamage));
        AssertJUnit.assertFalse(FlatMonteCarloPicker.isDiscoverable(xDamage));
    }

    @Test
    public void xDamageIsNeverAimedAtItsOwnCreature() {
        setUp();
        final SpellAbility xDamage = chandraNalaarX(6);
        addCardToZone("Grizzly Bears", ai, ZoneType.Battlefield);
        AssertJUnit.assertNull("nothing of the opponent's to kill", XChoice.loyaltyPlan(ai, xDamage));
    }

    @Test
    public void xDamageIgnoresACreatureTooBigToKill() {
        setUp();
        final SpellAbility xDamage = chandraNalaarX(4); // at most 3
        theirs("Craw Wurm");                            // 6/4
        AssertJUnit.assertNull(XChoice.loyaltyPlan(ai, xDamage));
    }

    private SpellAbility flamecallerX() {
        return withCost(walker("Chandra, Flamecaller", 4), "SubCounter<X/LOYALTY>");
    }

    @Test
    public void aSweepTakesTheSmallestXThatKillsTheirs() {
        setUp();
        final SpellAbility sweep = flamecallerX();
        theirs("Grizzly Bears");
        theirs("Grizzly Bears");
        final XChoice.LoyaltyPlan plan = XChoice.loyaltyPlan(ai, sweep);
        AssertJUnit.assertNotNull(plan);
        AssertJUnit.assertEquals("two toughness kills both Bears", 2, plan.x);
        AssertJUnit.assertNull("no single target", plan.target);
        AssertJUnit.assertTrue(FlatMonteCarloPicker.isDiscoverable(sweep));
    }

    @Test
    public void aSweepThatKillsMoreOfTheAisThanOfTheirsIsNotPlayed() {
        setUp();
        final SpellAbility sweep = flamecallerX();
        theirs("Grizzly Bears");
        addCardToZone("Grizzly Bears", ai, ZoneType.Battlefield);
        addCardToZone("Grizzly Bears", ai, ZoneType.Battlefield);
        AssertJUnit.assertNull("two of its own for one of theirs", XChoice.loyaltyPlan(ai, sweep));
        AssertJUnit.assertFalse(FlatMonteCarloPicker.isDiscoverable(sweep));
    }

    @Test
    public void aSweepWithNothingToKillIsNotPlayed() {
        setUp();
        final SpellAbility sweep = flamecallerX();
        theirs("Craw Wurm"); // toughness 4 > the 3 it may pay
        AssertJUnit.assertNull(XChoice.loyaltyPlan(ai, sweep));
    }

    @Test
    public void otherXAbilitiesAreLeftToTheHeuristics() {
        setUp();
        final SpellAbility ugin = ability(walker("Ugin, the Spirit Dragon", 7), "ChangeZoneAll");
        theirs("Grizzly Bears");
        AssertJUnit.assertNull("no plan for an exile by mana value", XChoice.loyaltyPlan(ai, ugin));
        AssertJUnit.assertFalse(FlatMonteCarloPicker.isDiscoverable(ugin));
    }
}
