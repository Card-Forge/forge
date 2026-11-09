package forge.ai;

import java.util.regex.Pattern;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.simulation.ComboGuard;
import forge.ai.simulation.ComboPlan;
import forge.ai.simulation.DeckPlan;
import forge.ai.simulation.DredgeLine;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Breakthrough with Lion's Eye Diamond for the LED Dredge list (src/test/resources/ai/legacy-meta/led-dredge.dck): the
 * deck is read as a dredge deck, the Diamond is recognised as a mana ability that discards the hand, holding the whole
 * setup is worth more than holding part of it, and the Diamond is never cracked unless a refill spell of the AI is on
 * the stack.
 */
public class FlatMonteCarloDredgeLineTest extends AITest {
    private static final Pattern LINE = Pattern.compile("^(\\d+)\\s+(.+?)\\s*$");

    private Player deal(final Game game, final Object... list) {
        final Player p = game.getPlayers().get(1);
        for (int i = 0; i < list.length; i += 2) {
            for (int n = 0; n < (Integer) list[i + 1]; n++) {
                addCardToZone((String) list[i], p, ZoneType.Library);
            }
        }
        return p;
    }

    private static final Object[] DREDGE_DECK = { "Lion's Eye Diamond", 4, "Bridge from Below", 3, "Golgari Thug", 3,
        "Golgari Grave-Troll", 4, "Faithless Looting", 4, "Ox of Agonas", 2, "Cabal Therapy", 4, "Otherworldly Gaze", 4,
        "Anger", 1, "Poxwalkers", 3, "Narcomoeba", 4, "Stinkweed Imp", 4, "Underground Sea", 1, "Breakthrough", 3,
        "Island", 1, "Cephalid Coliseum", 4, "Mountain", 1, "Volcanic Island", 2, "Dread Return", 2, "Flooded Strand", 1,
        "Ashen Rider", 1, "Scalding Tarn", 4 };

    private Player dealLedDredge() {
        final Game game = initAndCreateGame();
        final Player p = deal(game, DREDGE_DECK);
        return p;
    }

    @Test
    public void ledDredgeIsReadAsADredgeDeck() {
        final Player p = dealLedDredge();
        final DeckPlan plan = DeckPlan.forSetting("auto", p);
        AssertJUnit.assertTrue(plan.combos.summary(), plan.combos.has(ComboPlan.Kind.DREDGE));
    }

    @Test
    public void theDiamondIsAManaAbilityThatDiscardsTheHandAndPetalIsNot() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        final Card diamond = addCardToZone("Lion's Eye Diamond", p, ZoneType.Hand);
        final Card petal = addCardToZone("Lotus Petal", p, ZoneType.Hand);
        boolean diamondFound = false;
        for (final SpellAbility sa : diamond.getManaAbilities()) {
            diamondFound |= DredgeLine.isHandDiscardMana(sa);
        }
        boolean petalFound = false;
        for (final SpellAbility sa : petal.getManaAbilities()) {
            petalFound |= DredgeLine.isHandDiscardMana(sa);
        }
        AssertJUnit.assertTrue(diamondFound);
        AssertJUnit.assertFalse(petalFound);
    }

    @Test
    public void theWholeSetupIsWorthMoreThanPartOfIt() {
        final Player p = dealLedDredge();
        final DeckPlan plan = DeckPlan.forSetting("auto", p);
        AssertJUnit.assertTrue(plan.combos.summary(), plan.combos.has(ComboPlan.Kind.DREDGE));
        addCardToZone("Golgari Grave-Troll", p, ZoneType.Hand);
        final int dredgerOnly = plan.combos.term(p);
        addCardToZone("Breakthrough", p, ZoneType.Hand);
        final int withRefill = plan.combos.term(p);
        addCardToZone("Lion's Eye Diamond", p, ZoneType.Battlefield);
        final int withDiamond = plan.combos.term(p);
        AssertJUnit.assertTrue("refill " + withRefill + " vs " + dredgerOnly, withRefill > dredgerOnly);
        AssertJUnit.assertTrue("diamond " + withDiamond + " vs " + withRefill, withDiamond > withRefill);
    }

    @Test
    public void theDiamondIsNotCrackedWithNothingOnTheStack() {
        final Player p = dealLedDredge();
        final DeckPlan plan = DeckPlan.forSetting("auto", p);
        addCardToZone("Lion's Eye Diamond", p, ZoneType.Battlefield);
        addCardToZone("Golgari Grave-Troll", p, ZoneType.Hand);
        addCardToZone("Breakthrough", p, ZoneType.Hand);
        AssertJUnit.assertNull(DredgeLine.response(p, plan.combos));
    }

    /** Casts Breakthrough as far as the stack: the card leaves the hand and its spell is the AI's own top item. */
    private SpellAbility castBreakthrough(final Game game, final Player p) {
        final Card spell = addCardToZone("Breakthrough", p, ZoneType.Hand);
        p.getZone(ZoneType.Hand).remove(spell);
        final SpellAbility sa = spell.getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        game.getStack().add(sa);
        return sa;
    }

    @Test
    public void theDiamondIsCrackedWithBreakthroughOnTheStack() {
        final Game game = initAndCreateGame();
        final Player p = deal(game, DREDGE_DECK);
        final DeckPlan plan = DeckPlan.forSetting("auto", p);
        final Card diamond = addCardToZone("Lion's Eye Diamond", p, ZoneType.Battlefield);
        addCardToZone("Golgari Grave-Troll", p, ZoneType.Hand);
        addCardToZone("Narcomoeba", p, ZoneType.Hand);
        castBreakthrough(game, p);
        final SpellAbility crack = DredgeLine.response(p, plan.combos);
        AssertJUnit.assertNotNull("the Diamond must be cracked in response", crack);
        AssertJUnit.assertEquals(diamond, crack.getHostCard());
    }

    @Test
    public void theDiamondIsNotCrackedWithoutADredgerOrWithoutAnythingToDump() {
        final Game game = initAndCreateGame();
        final Player p = deal(game, DREDGE_DECK);
        final DeckPlan plan = DeckPlan.forSetting("auto", p);
        addCardToZone("Lion's Eye Diamond", p, ZoneType.Battlefield);
        addCardToZone("Island", p, ZoneType.Hand);
        castBreakthrough(game, p);
        AssertJUnit.assertNull(DredgeLine.response(p, plan.combos));
    }

    @Test
    public void breakthroughIsHeldBackUntilTheLineIsComplete() {
        final Game game = initAndCreateGame();
        final Player p = deal(game, DREDGE_DECK);
        final DeckPlan plan = DeckPlan.forSetting("auto", p);
        final Card breakthrough = addCardToZone("Breakthrough", p, ZoneType.Hand);
        final SpellAbility cast = breakthrough.getFirstSpellAbility();
        cast.setActivatingPlayer(p);
        AssertJUnit.assertTrue("no dredger, no Diamond", DredgeLine.mustWait(p, plan.combos, cast));
        addCardToZone("Golgari Grave-Troll", p, ZoneType.Hand);
        AssertJUnit.assertTrue("no Diamond in play", DredgeLine.mustWait(p, plan.combos, cast));
        addCardToZone("Lion's Eye Diamond", p, ZoneType.Battlefield);
        AssertJUnit.assertFalse("the line is complete", DredgeLine.mustWait(p, plan.combos, cast));
    }

    /**
     * canPlaySa holds Breakthrough back with AnotherTime, but a fuel play with that answer is still offered to the
     * rollouts as a discovered play: the guard is what stops it there, and it is what the AI really asks (aim is null).
     */
    @Test
    public void theGuardRefusesBreakthroughUntilTheLineIsComplete() {
        final Game game = initAndCreateGame();
        final Player p = deal(game, DREDGE_DECK);
        final DeckPlan plan = DeckPlan.forSetting("auto", p);
        final Card breakthrough = addCardToZone("Breakthrough", p, ZoneType.Hand);
        final SpellAbility cast = breakthrough.getFirstSpellAbility();
        cast.setActivatingPlayer(p);
        AssertJUnit.assertEquals("no dredger, no Diamond", "dredge-line-incomplete",
                ComboGuard.problem(p, cast, null, plan.combos));
        addCardToZone("Golgari Grave-Troll", p, ZoneType.Hand);
        AssertJUnit.assertEquals("no Diamond in play", "dredge-line-incomplete",
                ComboGuard.problem(p, cast, null, plan.combos));
        addCardToZone("Lion's Eye Diamond", p, ZoneType.Battlefield);
        AssertJUnit.assertNull("the line is complete", ComboGuard.problem(p, cast, null, plan.combos));
    }

    @Test
    public void theDiamondInPlayIsWorthMoreThanTheOneInHand() {
        final Player p = dealLedDredge();
        final DeckPlan plan = DeckPlan.forSetting("auto", p);
        addCardToZone("Golgari Grave-Troll", p, ZoneType.Hand);
        addCardToZone("Breakthrough", p, ZoneType.Hand);
        final Card diamond = addCardToZone("Lion's Eye Diamond", p, ZoneType.Hand);
        final int inHand = plan.combos.term(p);
        p.getZone(ZoneType.Hand).remove(diamond);
        p.getZone(ZoneType.Battlefield).add(diamond);
        final int inPlay = plan.combos.term(p);
        AssertJUnit.assertTrue("play " + inPlay + " vs hand " + inHand, inPlay > inHand);
    }
}
