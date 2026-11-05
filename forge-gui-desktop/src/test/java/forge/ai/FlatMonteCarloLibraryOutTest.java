package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.simulation.ComboPlan;
import forge.ai.simulation.DeckPlan;
import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Library-out decks (Oops! All Spells, Cephalid Breakfast): the deck is recognised, the library is only worth
 * emptying while the winner can be brought back (in hand, or in the graveyard with Dread Return), and Balustrade Spy
 * and Undercity Informer aim at their own controller.
 */
public class FlatMonteCarloLibraryOutTest extends AITest {
    private static final Object[] OOPS_DECK = { "Balustrade Spy", 4, "Undercity Informer", 1, "Thassa's Oracle", 1,
        "Narcomoeba", 4, "Dread Return", 4, "Bridge from Below", 4, "Cabal Therapy", 4, "Lotus Petal", 4,
        "Dark Ritual", 4, "Elvish Spirit Guide", 4, "Simian Spirit Guide", 4, "Poxwalkers", 4, "Tireless Tracker", 1,
        "Street Wraith", 3, "Lion's Eye Diamond", 4 };

    private Player dealOops(final Game game) {
        final Player p = game.getPlayers().get(1);
        for (int i = 0; i < OOPS_DECK.length; i += 2) {
            for (int n = 0; n < (Integer) OOPS_DECK[i + 1]; n++) {
                addCardToZone((String) OOPS_DECK[i], p, ZoneType.Library);
            }
        }
        return p;
    }

    /** The activated ability of a creature: getFirstSpellAbility() is the spell that casts the creature, not this. */
    private static SpellAbility millAbility(final Card c) {
        for (final SpellAbility sa : c.getSpellAbilities()) {
            if (sa.getApi() == ApiType.DigUntil) {
                return sa;
            }
        }
        throw new AssertionError(c + " has no DigUntil ability");
    }

    /** Leaves this many cards in the library. */
    private static void shrinkLibrary(final Player p, final int keep) {
        while (p.getCardsIn(ZoneType.Library).size() > keep) {
            p.getZone(ZoneType.Library).remove(p.getCardsIn(ZoneType.Library).get(0));
        }
    }

    @Test
    public void oopsIsReadAsALibraryOutDeck() {
        final Game game = initAndCreateGame();
        final Player p = dealOops(game);
        final DeckPlan plan = DeckPlan.forSetting("auto", p);
        AssertJUnit.assertTrue(plan.combos.summary(), plan.combos.has(ComboPlan.Kind.LIBRARY_OUT));
    }

    @Test
    public void aSmallLibraryIsPenalisedWhenTheWinnerCannotComeBack() {
        final Game game = initAndCreateGame();
        final Player p = dealOops(game);
        final DeckPlan plan = DeckPlan.forSetting("auto", p);
        shrinkLibrary(p, 4);
        AssertJUnit.assertTrue(plan.combos.term(p) < 0);
    }

    @Test
    public void theOracleInTheGraveyardWithDreadReturnInHandIsNotPenalised() {
        final Game game = initAndCreateGame();
        final Player p = dealOops(game);
        final DeckPlan plan = DeckPlan.forSetting("auto", p);
        shrinkLibrary(p, 4);
        final int without = plan.combos.term(p);
        addCardToZone("Thassa's Oracle", p, ZoneType.Graveyard);
        addCardToZone("Dread Return", p, ZoneType.Hand);
        final int with = plan.combos.term(p);
        AssertJUnit.assertTrue("with " + with + " vs without " + without, with > without);
    }

    @Test
    public void theOracleInHandRewardsAnEmptierLibrary() {
        final Game game = initAndCreateGame();
        final Player p = dealOops(game);
        final DeckPlan plan = DeckPlan.forSetting("auto", p);
        addCardToZone("Thassa's Oracle", p, ZoneType.Hand);
        shrinkLibrary(p, 20);
        final int full = plan.combos.term(p);
        shrinkLibrary(p, 8);
        final int small = plan.combos.term(p);
        AssertJUnit.assertTrue("small " + small + " vs " + full, small > full);
    }

    @Test
    public void undercityInformerAimsAtItsOwnerInALibraryOutDeck() {
        final Game game = initAndCreateGame();
        final Player p = dealOops(game);
        DeckPlan.forSetting("auto", p);
        final Card informer = addCardToZone("Undercity Informer", p, ZoneType.Battlefield);
        final SpellAbility mill = millAbility(informer);
        mill.setActivatingPlayer(p);
        // the verdict also depends on mana and on the sacrifice cost, which this board does not have: only the aim counts
        new forge.ai.ability.DigUntilAi().canPlayWithSubs(p, mill);
        AssertJUnit.assertTrue("it must aim at its owner", mill.getTargets().contains(p));
    }

    @Test
    public void undercityInformerStillAimsAtTheOpponentInAnOrdinaryDeck() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        for (int n = 0; n < 24; n++) {
            addCardToZone("Island", p, ZoneType.Library);
        }
        DeckPlan.forSetting("auto", p);
        final Card informer = addCardToZone("Undercity Informer", p, ZoneType.Battlefield);
        final SpellAbility mill = millAbility(informer);
        mill.setActivatingPlayer(p);
        new forge.ai.ability.DigUntilAi().canPlayWithSubs(p, mill);
        AssertJUnit.assertFalse("a deck that is not library-out never mills itself", mill.getTargets().contains(p));
        AssertJUnit.assertTrue("it keeps aiming at the opponent", mill.getTargets().contains(game.getPlayers().get(0)));
    }
}
