package forge.gamesimulationtests.priority;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * CR 502.4: no player receives priority during the untap step. A spell cast there (Panglacial Wurm, during a search)
 * stays on the stack until the upkeep, where the active player receives priority first (CR 117.3a).
 *
 * How the Wurm gets cast in a's untap step (turn 3): with Nature's Revolt all lands are creatures, so a's Phantom
 * Steed can exile Steam Vents. Cytoshape makes the Steed a copy of Undiscovered Paradise, and the Steed (hasted by
 * Fervor) taps for mana, so it returns to a's hand during a's next untap step. Steam Vents then returns to the
 * battlefield under its owner's control; its owner, at 2 life, pays 2 life as it enters. Enduring Angel replaces
 * that with their life total becoming 3, Lich turns that life gain into a draw, and Archmage Ascension turns the draw
 * into a search, during which they cast the Wurm.
 */
public class UntapStepCastTest extends PriorityTestBase {

    /** Sets everything up during turns 1 and 2; {@code caster} owns Steam Vents and casts the Wurm in turn 3's untap step. */
    private void castWurmInUntapOfTurn3(final Player caster, final boolean beanstalk) {
        addCard("Nature's Revolt", a);
        addCard("Fervor", a);
        final Card vents = addCard("Steam Vents", caster);
        lands(caster, 7); // the Wurm's mana: creatures from now on, so they are not summoning sick on turn 3
        if (caster != a) {
            addCard("Mox Sapphire", a);
            addCard("Mox Sapphire", a);
            addCard("Sol Ring", a);
            cast("Control Magic", a, vents);
            resolveAll();
            assertEquals(vents.getController(), a, "a controls b's Steam Vents\n" + log);
        }

        // Phantom Steed exiles Steam Vents, the only other creature a controls (the Forests are a's only if a casts)
        addCard("Mox Sapphire", a);
        addCard("Mox Jet", a);
        addCard("Sol Ring", a);
        cast("Phantom Steed", a);
        resolveAll();
        final Card steed = a.getCardsIn(ZoneType.Battlefield).stream().filter(c -> c.getName().equals("Phantom Steed")).findFirst().get();
        assertTrue(vents.isInZone(ZoneType.Exile), "Phantom Steed exiled Steam Vents\n" + log);

        // Cytoshape: the Steed becomes a copy of Undiscovered Paradise and taps for mana
        addCard("Mox Emerald", a);
        addCard("Mox Sapphire", a);
        addCard("Mox Pearl", a);
        cast("Cytoshape", a, steed);
        ctrl(a).prefer = addCard("Undiscovered Paradise", a);
        resolveAll();
        assertEquals(steed.getName(), "Undiscovered Paradise", "the Steed is a copy of Undiscovered Paradise\n" + log);
        final SpellAbility paradiseMana = steed.getManaAbilities().get(0);
        ctrl(a).actions.add(paradiseMana);
        runUntil(steed::isTapped, "the Steed tapping for mana");

        // the rest of the chain, added at the end of turn 2 so nothing draws or searches before turn 3's untap step
        runUntilPhase(2, PhaseType.END_OF_TURN);
        addCard("Lich", caster);
        addCard("Enduring Angel", caster);
        archmageAscension(caster);
        addCardToZone(WURM, caster, ZoneType.Library);
        caster.setLife(2, null);
        if (beanstalk) {
            addCard("Up the Beanstalk", caster);
        }
    }

    private void assertWurmResolvesInUpkeep(final String expectedTop) {
        final Priority upkeep = runUntilPriorityIn(3, PhaseType.UPKEEP);
        assertTrue(priorities(3, PhaseType.UNTAP).isEmpty(), "no player may receive priority in the untap step\n" + log);
        assertEquals(upkeep.player(), a, "the active player gets priority first in upkeep\n" + log);
        assertEquals(upkeep.stackTop(), expectedTop, log.toString());
        assertTrue(count(WURM, ZoneType.Stack) == 1, "the Wurm was cast in the untap step and is still on the stack\n" + log);
        runUntil(() -> count(WURM, ZoneType.Battlefield) == 1, "the Wurm resolving");
        assertEquals(turn(), 3, log.toString());
        assertEquals(game.getPhaseHandler().getPhase(), PhaseType.UPKEEP, "the Wurm resolves in upkeep, not untap\n" + log);
    }

    @Test(timeOut = 60000)
    public void wurmCastByActivePlayerInUntap() {
        setUpGame();
        castWurmInUntapOfTurn3(a, false);
        assertWurmResolvesInUpkeep(WURM);
    }

    /** b owns Steam Vents (a took it with Control Magic), so it returns to b, who pays the life and casts the Wurm. */
    @Test(timeOut = 60000)
    public void wurmCastByNonActivePlayerInUntap() {
        setUpGame();
        castWurmInUntapOfTurn3(b, false);
        assertWurmResolvesInUpkeep(WURM);
    }

    /** The Wurm's cast triggers Up the Beanstalk: the trigger waits for the upkeep instead of the game loop spinning forever. */
    @Test(timeOut = 60000)
    public void wurmCastWithPendingTriggerInUntapDoesNotHang() {
        setUpGame();
        castWurmInUntapOfTurn3(a, true);
        assertWurmResolvesInUpkeep("Up the Beanstalk");
    }
}
