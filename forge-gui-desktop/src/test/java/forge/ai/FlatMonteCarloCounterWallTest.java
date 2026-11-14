package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.simulation.ComboGuard;
import forge.ai.simulation.ComboPlan;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Permanents that counter spells: Void Mirror counters a spell cast without colored mana (the first playtest of the
 * Lurrus Vault Key deck cast Black Lotus and Mox Emerald into its own), Chalice of the Void counters the spells whose
 * mana value is its number of charge counters. The guard refuses the casts that would be countered.
 */
public class FlatMonteCarloCounterWallTest extends AITest {
    private SpellAbility cast(final Player ai, final String name) {
        final Card c = addCardToZone(name, ai, ZoneType.Hand);
        final SpellAbility sa = c.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        return sa;
    }

    @Test
    public void aSpellThatCostsNothingIsRefusedWithAVoidMirrorInPlay() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        addCard("Void Mirror", ai);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals("void-mirror-counters-free-spell",
                ComboGuard.problem(ai, cast(ai, "Black Lotus"), null, ComboPlan.EMPTY));
        AssertJUnit.assertEquals("void-mirror-counters-free-spell",
                ComboGuard.problem(ai, cast(ai, "Mox Emerald"), null, ComboPlan.EMPTY));
    }

    @Test
    public void aSpellWithACostIsLeftToTheRolloutsAndWithoutTheMirrorNothingIsRefused() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        addCard("Void Mirror", ai);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertNull("a spell with a cost can be paid with colored mana",
                ComboGuard.problem(ai, cast(ai, "Sol Ring"), null, ComboPlan.EMPTY));
        final Game other = initAndCreateGame();
        final Player ai2 = other.getPlayers().get(1);
        AssertJUnit.assertNull("no mirror, no problem", ComboGuard.problem(ai2, cast(ai2, "Black Lotus"), null, ComboPlan.EMPTY));
    }

    private Card chalice(final Player ai, final int counters) {
        final Card c = addCard("Chalice of the Void", ai);
        c.addCounterInternal(forge.game.card.CounterType.getType("CHARGE"), counters, ai, false, null, null);
        return c;
    }

    @Test
    public void aChaliceWithNoCountersRefusesTheFreeSpells() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        chalice(ai, 0);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals("chalice-counters-spell", ComboGuard.problem(ai, cast(ai, "Mox Pearl"), null, ComboPlan.EMPTY));
        AssertJUnit.assertNull("a spell with mana value 1 is not countered by a Chalice at 0",
                ComboGuard.problem(ai, cast(ai, "Sol Ring"), null, ComboPlan.EMPTY));
    }

    @Test
    public void aChaliceAtOneRefusesTheSpellsWithManaValueOneOnly() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        chalice(ai, 1);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals("chalice-counters-spell", ComboGuard.problem(ai, cast(ai, "Sol Ring"), null, ComboPlan.EMPTY));
        AssertJUnit.assertNull("a free spell is fine against a Chalice at 1",
                ComboGuard.problem(ai, cast(ai, "Mox Pearl"), null, ComboPlan.EMPTY));
    }

    @Test
    public void anOpponentsChaliceCountersTheAisSpellsToo() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        chalice(game.getPlayers().get(0), 0);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertEquals("chalice-counters-spell", ComboGuard.problem(ai, cast(ai, "Black Lotus"), null, ComboPlan.EMPTY));
    }
}
