package forge.gamesimulationtests.priority;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.List;

import org.testng.annotations.Test;

import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * CR 707.10: copying a spell puts the copy onto the stack right away, during the resolution that makes it. CR 405.3:
 * objects put on the stack at the same time go on in APNAP order (the active player's lowest), and a player orders
 * their own (CR 707.10d).
 */
public class CopyOrderOnStackTest extends PriorityTestBase {

    /**
     * a casts two Opts, then b's Grapeshot has storm 2: as the storm trigger resolves, b puts two copies on the stack
     * in the order b chose, which is the order they end up in (not reversed).
     */
    @Test(timeOut = 60000)
    public void copiesFromOneResolutionKeepTheirControllersOrder() {
        setUpGame();
        lands("Island", a, 2);
        cast("Opt", a);
        cast("Opt", a);
        runUntil(() -> game.getStack().isEmpty() && count("Opt", ZoneType.Graveyard) == 2, "both Opts resolving");
        lands("Mountain", b, 2);
        cast("Grapeshot", b, a);

        runUntil(() -> log.stream().anyMatch(p -> p.stackSize() == 3), "priority with the storm copies on the stack");
        final List<List<SpellAbility>> orders = ctrl(b).orderedSimultaneous;
        final List<SpellAbility> copies = orders.get(orders.size() - 1);
        assertEquals(copies.size(), 2, "b put the two storm copies on the stack together\n" + log);
        final List<SpellAbility> stack = stackTopDown();
        // the AI controller adds them in the order it chose, so the last one ends on top
        assertTrue(stack.get(0) == copies.get(1) && stack.get(1) == copies.get(0),
                "the copies are on the stack in the order b put them there: " + stack + " vs " + copies + "\n" + log);
    }

    /**
     * Three players; a is active, then b, then c. a casts Opt while Hive Mind is on the battlefield: each other player
     * copies it as Hive Mind's trigger resolves. b's copy goes on before c's (APNAP), so c's copy is on top.
     */
    @Test(timeOut = 60000)
    public void copiesForSeveralPlayersGoOnInApnapOrder() {
        setUpThreePlayerGame();
        addCard("Hive Mind", a);
        lands("Island", a, 1);
        cast("Opt", a);

        runUntil(() -> log.stream().anyMatch(p -> p.stackSize() == 3), "priority with the Hive Mind copies on the stack");
        final List<SpellAbility> stack = stackTopDown();
        assertEquals(stack.get(0).getActivatingPlayer(), c, "c's copy is on top (CR 405.3)\n" + stack + "\n" + log);
        assertEquals(stack.get(1).getActivatingPlayer(), b, "b's copy is below c's\n" + stack + "\n" + log);
        assertEquals(stack.get(2).getActivatingPlayer(), a, "a's Opt is at the bottom\n" + stack + "\n" + log);
    }
}
