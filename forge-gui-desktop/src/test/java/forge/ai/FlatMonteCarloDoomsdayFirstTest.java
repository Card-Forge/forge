package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.simulation.ComboGuard;
import forge.ai.simulation.ComboPlan;
import forge.ai.simulation.DeckPlan;
import forge.game.Game;
import forge.game.card.Card;
import forge.card.mana.ManaAtom;
import forge.game.keyword.Keyword;
import forge.game.mana.Mana;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Doomsday: with a winning Doomsday in hand nothing else spends the mana or draws first.
 *
 * <p>Report: the AI cast Dark Ritual and then Street Wraith as a five mana creature, and had nothing left for
 * Doomsday. The Wraith is in the hand to be cycled for two life once the pile is down, and Dark Ritual is there to
 * pay for Doomsday itself. The rule is {@code DoomsdayPile.mustWaitForDoomsday} ("doomsday-first").
 */
public class FlatMonteCarloDoomsdayFirstTest extends AITest {
    private Game game;
    private Player ai;

    private void setUp() {
        game = initAndCreateGame();
        ai = game.getPlayers().get(1);
        for (final String line : FlatMonteCarloVintageMetaTest.DOOMSDAY.split("\n")) {
            final int space = line.indexOf(' ');
            if (space > 0) {
                for (int n = Integer.parseInt(line.substring(0, space)); n > 0; n--) {
                    addCardToZone(line.substring(space + 1).trim(), ai, ZoneType.Library);
                }
            }
        }
    }

    private ComboPlan plan() {
        return DeckPlan.forSetting("auto", ai).combos;
    }

    /** The board of the report: two Underground Sea, an Island and a Lotus Petal. */
    private void board() {
        addCardToZone("Underground Sea", ai, ZoneType.Battlefield);
        addCardToZone("Underground Sea", ai, ZoneType.Battlefield);
        addCardToZone("Island", ai, ZoneType.Battlefield);
        addCardToZone("Lotus Petal", ai, ZoneType.Battlefield);
    }

    private Card inHand(final String name) {
        for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
            if (c.getName().equals(name)) {
                return c;
            }
        }
        throw new IllegalStateException(name + " is not in hand");
    }

    private String verdict(final SpellAbility sa) {
        sa.setActivatingPlayer(ai);
        return ComboGuard.problem(ai, sa, null, plan());
    }

    private SpellAbility spell(final String name) {
        return inHand(name).getFirstSpellAbility();
    }

    private SpellAbility cycling(final String name) {
        for (final SpellAbility sa : inHand(name).getSpellAbilities()) {
            if (sa.isKeyword(Keyword.CYCLING)) {
                return sa;
            }
        }
        throw new IllegalStateException("no cycling on " + name);
    }

    private void reportedHand() {
        board();
        addCardToZone("Doomsday", ai, ZoneType.Hand);
        addCardToZone("Misdirection", ai, ZoneType.Hand);
        addCardToZone("Dark Ritual", ai, ZoneType.Hand);
        addCardToZone("Street Wraith", ai, ZoneType.Hand);
    }

    @Test
    public void theCreatureWaitsForDoomsday() {
        setUp();
        reportedHand();
        AssertJUnit.assertEquals("Street Wraith for five mana before Doomsday", "doomsday-first", verdict(spell("Street Wraith")));
    }

    @Test
    public void theCyclingWaitsForDoomsday() {
        setUp();
        reportedHand();
        AssertJUnit.assertEquals("cycling the Wraith draws a card the pile should hold", "doomsday-first",
                verdict(cycling("Street Wraith")));
    }

    @Test
    public void doomsdayItselfAndItsManaAreNotHeldBack() {
        setUp();
        reportedHand();
        AssertJUnit.assertNull("Doomsday", verdict(spell("Doomsday")));
        AssertJUnit.assertNull("Dark Ritual pays for it", verdict(spell("Dark Ritual")));
        AssertJUnit.assertNull("a counterspell is not a mana sink", verdict(spell("Misdirection")));
    }

    @Test
    public void nothingWaitsWithoutDoomsdayInHand() {
        setUp();
        board();
        addCardToZone("Dark Ritual", ai, ZoneType.Hand);
        addCardToZone("Street Wraith", ai, ZoneType.Hand);
        AssertJUnit.assertNull("no Doomsday: the Wraith is judged as any creature", verdict(spell("Street Wraith")));
    }

    @Test
    public void nothingWaitsWhenNoPileWins() {
        setUp();
        addCardToZone("Swamp", ai, ZoneType.Battlefield);
        addCardToZone("Swamp", ai, ZoneType.Battlefield);
        addCardToZone("Swamp", ai, ZoneType.Battlefield);
        addCardToZone("Doomsday", ai, ZoneType.Hand);
        addCardToZone("Street Wraith", ai, ZoneType.Hand);
        AssertJUnit.assertNull("no blue for the Oracle: Doomsday is not the plan, so the Wraith is not held",
                verdict(spell("Street Wraith")));
    }

    @Test
    public void nothingWaitsWithTooLittleLifeForDoomsday() {
        setUp();
        reportedHand();
        ai.setLife(3, null);
        AssertJUnit.assertNull("Doomsday cannot be cast at 3 life", verdict(spell("Street Wraith")));
    }

    /**
     * The turn of the report, a step later: Dark Ritual is cast first (a Sea tapped, BBB floating, the Ritual gone from
     * the hand). The Wraith must still wait: the plan has the floating mana to pay for Doomsday.
     */
    @Test
    public void theCreatureStillWaitsOnceTheRitualHasBeenCast() {
        setUp();
        board();
        addCardToZone("Doomsday", ai, ZoneType.Hand);
        addCardToZone("Misdirection", ai, ZoneType.Hand);
        addCardToZone("Street Wraith", ai, ZoneType.Hand);
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            if (c.getName().equals("Underground Sea")) {
                c.setTapped(true); // paid for the Ritual
                break;
            }
        }
        final Card ritual = addCardToZone("Dark Ritual", ai, ZoneType.Graveyard);
        for (int i = 0; i < 3; i++) {
            ai.getManaPool().addMana(new Mana((byte) ManaAtom.BLACK, ritual, null, ai));
        }
        AssertJUnit.assertEquals("BBB floating, a Sea less: Doomsday is still payable, so nothing goes before it",
                "doomsday-first", verdict(spell("Street Wraith")));
        AssertJUnit.assertNull("and Doomsday itself is fine", verdict(spell("Doomsday")));
    }
}
