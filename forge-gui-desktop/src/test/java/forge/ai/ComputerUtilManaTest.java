package forge.ai;

import forge.ai.simulation.SimulationTest;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostParser;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Auto payment should not tap every untapped source of one colour when the same cost can be paid
 * while leaving at least one source of each colour untapped. The number of sources used never changes.
 */
public class ComputerUtilManaTest extends SimulationTest {

    /**
     * Pays the cost in test mode with the given basic lands and returns how many of each land
     * name stay untapped. Land specs look like "Swamp:4".
     */
    private Map<String, Integer> payAndCountUntapped(final String cost, final int expectedTapped, final String... landSpecs) {
        Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, p);

        final Map<String, Integer> untapped = new HashMap<>();
        for (final String spec : landSpecs) {
            final String[] parts = spec.split(":");
            final int count = Integer.parseInt(parts[1]);
            for (int i = 0; i < count; i++) {
                addCard(parts[0], p);
            }
            untapped.put(parts[0], count);
        }

        // any spell will do as the host, only the cost matters
        final Card host = addCardToZone("Grizzly Bears", p, ZoneType.Hand);
        final SpellAbility sa = host.getFirstSpellAbility();
        sa.setActivatingPlayer(p);

        final ManaCostBeingPaid toPay = new ManaCostBeingPaid(new ManaCost(new ManaCostParser(cost)));
        final CardCollection used = ComputerUtilMana.getManaSourcesToPayCost(toPay, sa, p, false);
        assertNotNull(used, "cost " + cost + " should be payable");
        assertEquals(used.size(), expectedTapped, "number of sources tapped for " + cost);

        for (final Card c : used) {
            untapped.merge(c.getName(), -1, Integer::sum);
        }
        return untapped;
    }

    private static void assertLeft(final Map<String, Integer> untapped, final String... lands) {
        for (final String land : lands) {
            assertTrue(untapped.get(land) >= 1, "expected at least one untapped " + land + ", got " + untapped);
        }
    }

    @Test
    public void genericPartKeepsBothColoursAvailable() {
        // {3}{B} with 4 Swamp + 2 Island: 3 Swamp + 1 Island leaves one of each
        assertLeft(payAndCountUntapped("3 B", 4, "Swamp:4", "Island:2"), "Swamp", "Island");
    }

    @Test
    public void genericPartPrefersTheOtherColour() {
        // {1}{B} with 2 Swamp + 4 Island: must not use both Swamps
        assertLeft(payAndCountUntapped("1 B", 2, "Swamp:2", "Island:4"), "Swamp", "Island");
    }

    @Test
    public void hybridShardLeavesBothColoursAvailable() {
        // {U/B} with 1 Swamp + 2 Island: pay with an Island so the Swamp stays
        assertLeft(payAndCountUntapped("U/B", 1, "Swamp:1", "Island:2"), "Swamp", "Island");
    }

    @Test
    public void coloredShardDoesNotDrainItsColour() {
        // {3}{W} with 2 Plains + 4 Island: only one Plains is needed
        assertLeft(payAndCountUntapped("3 W", 4, "Plains:2", "Island:4"), "Plains", "Island");
    }

    @Test
    public void threeColoursAreAllKept() {
        // {2}{R} with 2 of each of three types: one Mountain for R, the generic from Forest and Plains
        assertLeft(payAndCountUntapped("2 R", 3, "Mountain:2", "Forest:2", "Plains:2"), "Mountain", "Forest", "Plains");
    }

    @Test
    public void unavoidableDrainStillPaysWithSameNumberOfLands() {
        // {2}{B} with 2 Swamp + 2 Island taps 3 of 4 lands, so one colour is gone either way
        final Map<String, Integer> untapped = payAndCountUntapped("2 B", 3, "Swamp:2", "Island:2");
        assertEquals(untapped.get("Swamp") + untapped.get("Island"), 1);
    }
}
