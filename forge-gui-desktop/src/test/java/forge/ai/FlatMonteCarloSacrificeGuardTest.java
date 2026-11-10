package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.simulation.ComboGuard;
import forge.ai.simulation.ComboPlan;
import forge.ai.simulation.DeckPlan;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.cost.CostSacrifice;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * A sacrifice cost (Wight of the Reliquary) must not throw away a piece of the deck's combos. Kept in a class of its
 * own so that it shares no state with the other Flat Monte Carlo tests.
 */
public class FlatMonteCarloSacrificeGuardTest extends AITest {
    private static final Object[] DEPTHS_DECK = { "Vampire Hexmage", 4, "Dark Depths", 4, "Thespian's Stage", 4,
            "Lightning Bolt", 4, "Thoughtseize", 4, "Inquisition of Kozilek", 4, "Lotus Petal", 4, "Swamp", 14,
            "Mountain", 4, "Dark Ritual", 4 };

    private Player deal(final Game game, final Object... list) {
        final Player p = game.getPlayers().get(1);
        for (int i = 0; i < list.length; i += 2) {
            for (int n = 0; n < (Integer) list[i + 1]; n++) {
                addCardToZone((String) list[i], p, ZoneType.Library);
            }
        }
        return p;
    }

    private static SpellAbility sacrificeAbility(final Card wight, final Player p) {
        for (final SpellAbility sa : wight.getSpellAbilities()) {
            if (sa.isActivatedAbility() && sa.getPayCosts() != null
                    && sa.getPayCosts().hasSpecificCostType(CostSacrifice.class)) {
                sa.setActivatingPlayer(p);
                return sa;
            }
        }
        throw new IllegalStateException("no sacrifice ability on " + wight);
    }

    @Test
    public void aSacrificeThatWouldThrowAwayAComboPieceIsRefused() {
        final Game game = initAndCreateGame();
        final Player p = deal(game, DEPTHS_DECK);
        final DeckPlan plan = DeckPlan.forSetting("auto", p);
        final SpellAbility ability = sacrificeAbility(addCard("Wight of the Reliquary", p), p);
        addCard("Vampire Hexmage", p);
        AssertJUnit.assertEquals("sacrifice-key", ComboGuard.problem(p, ability, null, plan.combos));
    }

    @Test
    public void aSacrificeOfOrdinaryFodderIsNotRefused() {
        final Game game = initAndCreateGame();
        final Player p = deal(game, DEPTHS_DECK);
        final DeckPlan plan = DeckPlan.forSetting("auto", p);
        final SpellAbility ability = sacrificeAbility(addCard("Wight of the Reliquary", p), p);
        addCard("Llanowar Elves", p);
        AssertJUnit.assertNull(ComboGuard.problem(p, ability, null, plan.combos));
    }

    @Test
    public void aDeckWithoutAComboPlanSacrificesAsBefore() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        final SpellAbility ability = sacrificeAbility(addCard("Wight of the Reliquary", p), p);
        addCard("Vampire Hexmage", p);
        AssertJUnit.assertNull(ComboGuard.problem(p, ability, null, ComboPlan.EMPTY));
    }
}
