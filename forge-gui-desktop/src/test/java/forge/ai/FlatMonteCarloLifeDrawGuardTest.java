package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.simulation.ComboGuard;
import forge.ai.simulation.ComboPlan;
import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Griselbrand's "pay 7 life: draw seven cards" is only used while the life and the library can take it. Kept in a class
 * of its own: it shares no state with the other Flat Monte Carlo tests.
 */
public class FlatMonteCarloLifeDrawGuardTest extends AITest {

    private static SpellAbility drawAbility(final Card griselbrand, final Player ai) {
        for (final SpellAbility sa : griselbrand.getSpellAbilities()) {
            if (sa.isActivatedAbility() && sa.getApi() == ApiType.Draw) {
                sa.setActivatingPlayer(ai);
                return sa;
            }
        }
        throw new IllegalStateException("no draw ability on " + griselbrand);
    }

    protected void fillLibrary(final Player p, final int cards) {
        for (int i = 0; i < cards; i++) {
            addCardToZone("Island", p, ZoneType.Library);
        }
    }

    @Test
    public void aDrawThatLeavesTheAiAlmostDeadIsRefused() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        fillLibrary(ai, 20);
        final SpellAbility draw = drawAbility(addCard("Griselbrand", ai), ai);
        ai.setLife(10, null);
        AssertJUnit.assertEquals("life-draw-reckless", ComboGuard.problem(ai, draw, null, ComboPlan.EMPTY));
    }

    @Test
    public void aDrawWithLifeAndLibraryToSpareIsLeftToTheRollouts() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        fillLibrary(ai, 20);
        final SpellAbility draw = drawAbility(addCard("Griselbrand", ai), ai);
        ai.setLife(20, null);
        AssertJUnit.assertNull(ComboGuard.problem(ai, draw, null, ComboPlan.EMPTY));
    }

    @Test
    public void aDrawThatWouldEmptyTheLibraryIsRefused() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        fillLibrary(ai, 5);
        final SpellAbility draw = drawAbility(addCard("Griselbrand", ai), ai);
        ai.setLife(20, null);
        AssertJUnit.assertEquals("life-draw-reckless", ComboGuard.problem(ai, draw, null, ComboPlan.EMPTY));
    }

    @Test
    public void aDrawIsRefusedWhenTheOpponentsCreaturesCanFinishWhatIsLeft() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        fillLibrary(ai, 20);
        final SpellAbility draw = drawAbility(addCard("Griselbrand", ai), ai);
        ai.setLife(20, null);
        for (int i = 0; i < 5; i++) {
            addCard("Hill Giant", opp);
        }
        AssertJUnit.assertEquals("life-draw-reckless", ComboGuard.problem(ai, draw, null, ComboPlan.EMPTY));
    }
}
