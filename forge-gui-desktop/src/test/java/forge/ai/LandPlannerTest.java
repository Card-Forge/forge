package forge.ai;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import forge.ai.simulation.FlatMcTestBase;
import forge.card.mana.ManaAtom;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.zone.ZoneType;

/**
 * LandPlanner (PR #12508): which land to play so that the hand can be cast as early as the colours allow.
 * <p>
 * Part 1 tests the arithmetic with hand-made sources and spells (no game, no cards): payment matching and the
 * schedule score. Part 2 reads sources off real cards. Part 3 asks the planner for real decisions.
 * The planner must stay a refinement: it answers only when one land is clearly better and otherwise returns null.
 */
public class LandPlannerTest extends FlatMcTestBase {
    private static final byte W = ManaAtom.WHITE, U = ManaAtom.BLUE, B = ManaAtom.BLACK, R = ManaAtom.RED,
            G = ManaAtom.GREEN, C = ManaAtom.COLORLESS;
    private static final double[] NO_DEMAND = new double[5];

    private static LandPlanner.Src src(final int mask, final int from) {
        return new LandPlanner.Src((byte) mask, from);
    }

    private static LandPlanner.Spell spell(final double weight, final int generic, final int... coloured) {
        final byte[] arr = new byte[coloured.length];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = (byte) coloured[i];
        }
        return new LandPlanner.Spell("test", arr, generic, weight);
    }

    private static List<byte[]> costs(final int... masks) {
        final List<byte[]> out = new ArrayList<>();
        for (final int m : masks) {
            out.add(new byte[] { (byte) m });
        }
        return out;
    }

    // ===================================================================================== canPay

    @Test
    public void canPayOneColouredPip() {
        assertTrue(LandPlanner.canPay(costs(R), 0, Arrays.asList(src(R, 0))));
        assertFalse(LandPlanner.canPay(costs(R), 0, Arrays.asList(src(G, 0))), "wrong colour");
        assertFalse(LandPlanner.canPay(costs(R), 0, Collections.<LandPlanner.Src>emptyList()), "no source");
    }

    @Test
    public void colourlessSourceCannotPayAColouredPip() {
        assertFalse(LandPlanner.canPay(costs(R), 0, Arrays.asList(src(C, 0))));
        assertTrue(LandPlanner.canPay(costs(), 1, Arrays.asList(src(C, 0))), "but it pays generic mana");
    }

    @Test
    public void aDualLandPaysOnlyOneOfItsColoursAtATime() {
        final List<LandPlanner.Src> oneDual = Arrays.asList(src(R | G, 0));
        assertTrue(LandPlanner.canPay(costs(R), 0, oneDual));
        assertTrue(LandPlanner.canPay(costs(G), 0, oneDual));
        assertFalse(LandPlanner.canPay(costs(R, G), 0, oneDual), "{R}{G} needs two sources, a dual land is one");
    }

    @Test
    public void twoDualLandsCanPayTwoColours() {
        assertTrue(LandPlanner.canPay(costs(R, G), 0, Arrays.asList(src(R | G, 0), src(R | G, 0))));
    }

    @Test
    public void matchingFindsTheAssignmentThatWorks() {
        // R/G dual and a Forest for {R}{G}: the dual must pay R although it could also pay G
        assertTrue(LandPlanner.canPay(costs(R, G), 0, Arrays.asList(src(R | G, 0), src(G, 0))));
        // the other order must work as well
        assertTrue(LandPlanner.canPay(costs(R, G), 0, Arrays.asList(src(G, 0), src(R | G, 0))));
        // R/G dual and a Mountain for {G}{G}: only one G source exists
        assertFalse(LandPlanner.canPay(costs(G, G), 0, Arrays.asList(src(R | G, 0), src(R, 0))));
    }

    @Test
    public void hybridPipIsPaidByEitherColour() {
        final List<byte[]> hybrid = new ArrayList<>();
        hybrid.add(new byte[] { (byte) (G | W) });
        assertTrue(LandPlanner.canPay(hybrid, 0, Arrays.asList(src(W, 0))));
        assertTrue(LandPlanner.canPay(hybrid, 0, Arrays.asList(src(G, 0))));
        assertFalse(LandPlanner.canPay(hybrid, 0, Arrays.asList(src(B, 0))));
    }

    @Test
    public void genericManaUsesWhatTheColouredPartsLeaveOver() {
        // {2}{R}: the Mountain pays R, two other sources pay the generic part
        assertTrue(LandPlanner.canPay(costs(R), 2, Arrays.asList(src(R, 0), src(G, 0), src(U, 0))));
        // the same three sources cannot pay {3}{R}
        assertFalse(LandPlanner.canPay(costs(R), 3, Arrays.asList(src(R, 0), src(G, 0), src(U, 0))));
        // the only red source cannot be used for both the pip and the generic mana
        assertFalse(LandPlanner.canPay(costs(R), 1, Arrays.asList(src(R, 0))));
    }

    @Test
    public void severalSpellsMustShareTheSources() {
        // {R} and {R} with one Mountain: not together
        assertFalse(LandPlanner.canPay(costs(R, R), 0, Arrays.asList(src(R, 0), src(G, 0))));
        assertTrue(LandPlanner.canPay(costs(R, R), 0, Arrays.asList(src(R, 0), src(R, 0))));
    }

    // ===================================================================================== score (the schedule)

    @Test
    public void spellIsWorthItsWeightTimesTheDecayOfTheTurnItIsCastOn() {
        final List<LandPlanner.Spell> bolt = Arrays.asList(spell(100, 0, R));
        assertEquals(LandPlanner.score(Arrays.asList(src(R, 0)), bolt, NO_DEMAND), 100.0, 1e-9, "this turn");
        assertEquals(LandPlanner.score(Arrays.asList(src(R, 1)), bolt, NO_DEMAND), 70.0, 1e-9, "next turn");
        assertEquals(LandPlanner.score(Arrays.asList(src(R, 2)), bolt, NO_DEMAND), 50.0, 1e-9, "the turn after");
        assertEquals(LandPlanner.score(Arrays.asList(src(R, 3)), bolt, NO_DEMAND), 0.0, 1e-9, "beyond the horizon");
        assertEquals(LandPlanner.score(Arrays.asList(src(G, 0)), bolt, NO_DEMAND), 0.0, 1e-9, "wrong colour: never");
    }

    @Test
    public void aLandThatEntersTappedCostsTheTurn() {
        final List<LandPlanner.Spell> bolt = Arrays.asList(spell(100, 0, R));
        final double untapped = LandPlanner.score(Arrays.asList(src(R, 0)), bolt, NO_DEMAND);
        final double tapped = LandPlanner.score(Arrays.asList(src(R, 1)), bolt, NO_DEMAND);
        assertTrue(untapped - tapped >= LandPlanner.MIN_MARGIN, "the planner must be able to see the difference");
    }

    @Test
    public void twoSpellsOnOneSourceAreCastOnDifferentTurns() {
        final List<LandPlanner.Spell> two = Arrays.asList(spell(100, 0, R), spell(80, 0, R));
        assertEquals(LandPlanner.score(Arrays.asList(src(R, 0)), two, NO_DEMAND), 100 + 80 * 0.7, 1e-9,
                "one Mountain: the second {R} spell waits a turn");
        assertEquals(LandPlanner.score(Arrays.asList(src(R, 0), src(R, 0)), two, NO_DEMAND), 180.0, 1e-9,
                "two Mountains: both this turn");
    }

    @Test
    public void genericCostsAreIncluded() {
        final List<LandPlanner.Spell> s = Arrays.asList(spell(100, 2, R)); // {2}{R}
        assertEquals(LandPlanner.score(Arrays.asList(src(R, 0), src(C, 0), src(C, 0)), s, NO_DEMAND), 100.0, 1e-9);
        assertEquals(LandPlanner.score(Arrays.asList(src(R, 0), src(C, 0)), s, NO_DEMAND), 0.0, 1e-9);
        assertEquals(LandPlanner.score(Arrays.asList(src(R, 0), src(C, 0), src(C, 1)), s, NO_DEMAND), 70.0, 1e-9,
                "the third source arrives a turn later");
    }

    @Test
    public void libraryColourBonusFavoursFirstSourceOfADemandedColour() {
        final double[] wantsRed = { 0, 0, 0, 1, 0 };
        final List<LandPlanner.Spell> none = Collections.emptyList();
        assertEquals(LandPlanner.score(Arrays.asList(src(R, 0)), none, wantsRed), 18.0, 1e-9, "first source: 60%");
        assertEquals(LandPlanner.score(Arrays.asList(src(R, 0), src(R, 0)), none, wantsRed), 30.0, 1e-9, "second: full");
        assertEquals(LandPlanner.score(Arrays.asList(src(R, 0), src(R, 0), src(R, 0)), none, wantsRed), 30.0, 1e-9,
                "a third source adds nothing");
        assertEquals(LandPlanner.score(Arrays.asList(src(G, 0)), none, wantsRed), 0.0, 1e-9, "a colour nobody asks for");
    }

    @Test
    public void aDualLandCountsForBothColours() {
        final double[] wantsBoth = { 0, 0, 0, 1, 1 };
        final double dual = LandPlanner.score(Arrays.asList(src(R | G, 0)), Collections.<LandPlanner.Spell>emptyList(), wantsBoth);
        final double single = LandPlanner.score(Arrays.asList(src(R, 0)), Collections.<LandPlanner.Spell>emptyList(), wantsBoth);
        assertEquals(dual, 36.0, 1e-9);
        assertTrue(dual > single);
    }

    @Test
    public void libraryBonusOfAFirstSourceIsLargeEnoughToBreakATie() {
        // two lands that do the same for the hand but differ in library colours must be decidable by the library alone
        final double[] wantsRed = { 0, 0, 0, 1, 0 };
        final List<LandPlanner.Spell> none = Collections.emptyList();
        final double gain = LandPlanner.score(Arrays.asList(src(R, 0)), none, wantsRed)
                - LandPlanner.score(Arrays.asList(src(G, 0)), none, wantsRed);
        assertTrue(gain >= LandPlanner.MIN_MARGIN, "gain " + gain + " must reach the margin " + LandPlanner.MIN_MARGIN);
    }

    // ===================================================================================== reading the sources

    @Test
    public void basicLandIsOneSourceOfItsColour() {
        setUpGame();
        put("Forest", ai, ZoneType.Battlefield);
        final List<LandPlanner.Src> s = LandPlanner.battlefieldSources(ai);
        assertEquals(s.size(), 1);
        assertEquals(s.get(0).mask, G);
        assertEquals(s.get(0).from, 0);
    }

    @Test
    public void dualLandIsOneSourceThatMayBeEitherColour() {
        setUpGame();
        put("Tranquil Cove", ai, ZoneType.Battlefield); // W/U
        final List<LandPlanner.Src> s = LandPlanner.battlefieldSources(ai);
        assertEquals(s.size(), 1);
        assertEquals(s.get(0).mask, (byte) (W | U));
    }

    @Test
    public void bounceLandIsTwoSourcesAtOnce() {
        setUpGame();
        put("Azorius Chancery", ai, ZoneType.Battlefield); // {T}: add {W}{U}
        final List<LandPlanner.Src> s = LandPlanner.battlefieldSources(ai);
        assertEquals(s.size(), 2, "a bounce land makes both colours at the same time");
        assertTrue((s.get(0).mask | s.get(1).mask) == (W | U) && s.get(0).mask != s.get(1).mask);
    }

    @Test
    public void tappedLandCountsFromNextTurn() {
        setUpGame();
        final Card forest = put("Forest", ai, ZoneType.Battlefield);
        forest.setTapped(true);
        assertEquals(LandPlanner.battlefieldSources(ai).get(0).from, 1);
    }

    @Test
    public void summoningSickManaCreatureCountsFromNextTurn() {
        setUpGame();
        final Card elves = put("Llanowar Elves", ai, ZoneType.Battlefield);
        elves.setSickness(true);
        final List<LandPlanner.Src> s = LandPlanner.battlefieldSources(ai);
        assertEquals(s.size(), 1);
        assertEquals(s.get(0).from, 1);
    }

    @Test
    public void permanentsWithoutManaAbilitiesAreNoSources() {
        setUpGame();
        put("Grizzly Bears", ai, ZoneType.Battlefield);
        assertTrue(LandPlanner.battlefieldSources(ai).isEmpty());
    }

    // ===================================================================================== reading the spells

    @Test
    public void spellsAreSortedBestFirstAndCoverTheHand() {
        setUpGame();
        put("Lightning Bolt", ai, ZoneType.Hand);
        put("Serra Angel", ai, ZoneType.Hand);
        put("Mountain", ai, ZoneType.Hand);
        final List<LandPlanner.Spell> s = LandPlanner.spellsWanted(ai);
        assertEquals(s.size(), 2, "lands are not spells");
        assertTrue(s.get(0).weight > s.get(1).weight, "the 5 mana angel is worth more than the bolt");
    }

    @Test
    public void costShardsBecomeMasksAndGenericMana() {
        setUpGame();
        put("Serra Angel", ai, ZoneType.Hand); // {3}{W}{W}
        final LandPlanner.Spell s = LandPlanner.spellsWanted(ai).get(0);
        assertEquals(s.generic, 3);
        assertEquals(s.coloured.length, 2);
        assertEquals(s.coloured[0], W);
    }

    @Test
    public void phyrexianAndXShardsAreNotPlannedAsColouredMana() {
        setUpGame();
        put("Gitaxian Probe", ai, ZoneType.Hand); // {U/P}
        final LandPlanner.Spell probe = LandPlanner.spellsWanted(ai).get(0);
        assertEquals(probe.coloured.length, 0, "paid with life");
        setUpGame();
        put("Fireball", ai, ZoneType.Hand); // {X}{R}
        final LandPlanner.Spell fireball = LandPlanner.spellsWanted(ai).get(0);
        assertEquals(fireball.coloured.length, 1, "X is planned as 0");
        assertEquals(fireball.coloured[0], R);
    }

    @Test
    public void twoGenericHybridIsPlannedAsTheExpensiveWay() {
        setUpGame();
        put("Spectral Procession", ai, ZoneType.Hand); // {2/W}{2/W}{2/W}
        final LandPlanner.Spell s = LandPlanner.spellsWanted(ai).get(0);
        assertEquals(s.coloured.length, 0);
        assertEquals(s.generic, 6, "never promises more than it can deliver");
    }

    @Test
    public void colourHybridPipAcceptsEitherColour() {
        setUpGame();
        put("Kitchen Finks", ai, ZoneType.Hand); // {1}{G/W}{G/W}
        final LandPlanner.Spell s = LandPlanner.spellsWanted(ai).get(0);
        assertEquals(s.coloured.length, 2);
        assertEquals(s.coloured[0], (byte) (G | W));
    }

    @Test
    public void libraryDemandCountsColoursOfTheSpellsLeftInTheLibrary() {
        setUpGame();
        put("Lightning Bolt", 2, ai, ZoneType.Library);
        put("Grizzly Bears", 2, ai, ZoneType.Library);
        put("Mountain", 5, ai, ZoneType.Library); // lands do not count
        final double[] d = LandPlanner.libraryDemand(ai);
        assertEquals(d[3], 1.0, 1e-9, "red is in half of the spells: full demand");
        assertEquals(d[4], 1.0, 1e-9, "green too");
        assertEquals(d[0], 0.0, 1e-9, "nobody asks for white");
    }

    @Test
    public void libraryDemandScalesDown() {
        setUpGame();
        put("Lightning Bolt", 1, ai, ZoneType.Library);
        put("Grizzly Bears", 3, ai, ZoneType.Library);
        assertEquals(LandPlanner.libraryDemand(ai)[3], 0.5, 1e-9, "red in 1 of 4 spells");
    }

    @Test
    public void libraryDemandOfAnEmptyLibraryIsZero() {
        setUpGame();
        for (final double d : LandPlanner.libraryDemand(ai)) {
            assertEquals(d, 0.0, 1e-9);
        }
    }

    // ===================================================================================== choose()

    @Test
    public void playsTheUntappedLandWhenTheSpellCanBeCastNow() {
        setUpGame();
        put("Forest", ai, ZoneType.Battlefield);
        final Card cove = put("Tranquil Cove", ai, ZoneType.Hand);
        final Card forest = put("Forest", ai, ZoneType.Hand);
        put("Grizzly Bears", ai, ZoneType.Hand); // {1}{G}
        final Card chosen = LandPlanner.choose(ai, collection(cove, forest), c -> c == cove);
        assertSame(chosen, forest, "the tapland would cost the Bears a turn");
    }

    @Test
    public void playsTheLandThatMakesTheColourTheHandNeeds() {
        setUpGame();
        put("Island", ai, ZoneType.Battlefield);
        final Card mountain = put("Mountain", ai, ZoneType.Hand);
        final Card forest = put("Forest", ai, ZoneType.Hand);
        put("Lightning Bolt", ai, ZoneType.Hand);
        assertSame(LandPlanner.choose(ai, collection(mountain, forest), c -> false), mountain);
        assertSame(LandPlanner.choose(ai, collection(forest, mountain), c -> false), mountain, "order does not matter");
    }

    @Test
    public void libraryColoursDoNotDecideTheOrderWhenBothLandsGetPlayedAnyway() {
        setUpGame();
        final Card mountain = put("Mountain", ai, ZoneType.Hand);
        final Card forest = put("Forest", ai, ZoneType.Hand);
        put("Lightning Bolt", 6, ai, ZoneType.Library);
        // Both lands will be on the battlefield within the planner's horizon whichever comes first, so the library
        // colours cannot separate them: no opinion, and the caller's own rule decides.
        assertNull(LandPlanner.choose(ai, collection(mountain, forest), c -> false));
    }

    @Test
    public void saysNothingWhenTheLandsAreTooCloseToCall() {
        setUpGame();
        final Card mountain = put("Mountain", ai, ZoneType.Hand);
        final Card forest = put("Forest", ai, ZoneType.Hand);
        assertNull(LandPlanner.choose(ai, collection(mountain, forest), c -> false),
                "nothing in hand or library to decide on: the caller's own rule decides");
    }

    @Test
    public void saysNothingForTwoCopiesOfTheSameLand() {
        setUpGame();
        final Card a = put("Forest", ai, ZoneType.Hand);
        final Card b = put("Forest", ai, ZoneType.Hand);
        put("Grizzly Bears", ai, ZoneType.Hand);
        assertNull(LandPlanner.choose(ai, collection(a, b), c -> false));
    }

    @Test
    public void saysNothingForFewerThanTwoLands() {
        setUpGame();
        final Card a = put("Forest", ai, ZoneType.Hand);
        assertNull(LandPlanner.choose(ai, collection(a), c -> false));
        assertNull(LandPlanner.choose(ai, new CardCollection(), c -> false));
        assertNull(LandPlanner.choose(ai, null, c -> false));
    }

    @Test
    public void aBrokenPredicateNeverBreaksTheTurn() {
        setUpGame();
        final Card a = put("Forest", ai, ZoneType.Hand);
        final Card b = put("Mountain", ai, ZoneType.Hand);
        put("Lightning Bolt", ai, ZoneType.Hand);
        assertNull(LandPlanner.choose(ai, collection(a, b), c -> {
            throw new IllegalStateException("boom");
        }), "a refinement only: an exception means no opinion");
    }

    @Test
    public void landPlannerIsOnByDefault() {
        setUpGame();
        assertTrue(brain().getBoolProperty(AiProps.LAND_PLANNER));
    }
}