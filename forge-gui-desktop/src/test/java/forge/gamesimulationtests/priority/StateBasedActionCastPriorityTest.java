package forge.gamesimulationtests.priority;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

import forge.ai.ComputerUtil;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * CR 117.5, 704.3: a spell cast during a state-based action check does not take priority for its caster (CR 601.2i):
 * whoever was about to receive priority still does.
 */
public class StateBasedActionCastPriorityTest extends PriorityTestBase {

    /**
     * b is at 0 life behind Marina Vendrell's Grimoire and has cast Stunning Reversal. a's Shatter destroys the
     * Grimoire; in the state-based action check before a receives priority, b would lose, draws seven instead, and
     * casts Panglacial Wurm through Archmage Ascension. a, the active player, still receives priority first.
     */
    @Test(timeOut = 60000)
    public void castDuringTheCheckBeforePriority() {
        setUpGame();
        final Card grimoire = stunningReversalEngine(b);
        lands("Mountain", a, 2);
        cast("Shatter", a, grimoire);
        runUntil(() -> log.stream().anyMatch(p -> WURM.equals(p.stackTop())), "priority with the Wurm on the stack");
        final Priority first = log.stream().filter(p -> WURM.equals(p.stackTop())).findFirst().get();
        assertEquals(first.player(), a, "a was about to receive priority and still does\n" + log);
    }

    /**
     * CR 514.3a: a state-based action in the cleanup step gives priority, the active player first.
     * <ul>
     * <li>a's Donate gives b a's Archmage Ascension: a owns it, so Lich's Mirror won't shuffle it away.</li>
     * <li>b takes a's Platinum Angel with Ray of Command until end of turn and is at 0 life.</li>
     * <li>In a's end step b taps seven Forests for green; Omnath, Locus of Mana keeps it in b's pool into cleanup.</li>
     * <li>In a's cleanup step the Angel returns to a (CR 514.2), so b would lose to the state-based action check.
     * b's Lich's Mirror shuffles everything b owns away (Omnath too; the mana stays until the step ends) and b
     * draws seven instead; Archmage Ascension turns a draw into a search and b casts the Wurm with the floating
     * mana.</li>
     * </ul>
     */
    @Test(timeOut = 60000)
    public void castDuringTheCleanupCheck() {
        setUpGame();
        final Card archmage = archmageAscension(a);
        lands("Island", a, 3);
        final SpellAbility donate = addCardToZone("Donate", a, ZoneType.Hand).getFirstSpellAbility();
        donate.setActivatingPlayer(a);
        donate.getTargets().add(b);
        donate.getSubAbility().getTargets().add(archmage);
        assertTrue(ComputerUtil.playStack(donate, a, game), "could not cast Donate");
        resolveAll();
        assertEquals(archmage.getController(), b, "b controls a's Archmage Ascension\n" + log);

        final Card angel = addCard("Platinum Angel", a);
        lands("Island", b, 4);
        cast("Ray of Command", b, angel);
        resolveAll();
        assertEquals(angel.getController(), b, "b controls Platinum Angel until end of turn\n" + log);
        b.setLife(0, null);

        addCard("Lich's Mirror", b);
        addCard("Omnath, Locus of Mana", b);
        addCardToZone(WURM, b, ZoneType.Library);
        ctrl(b).actIn = PhaseType.END_OF_TURN;
        for (final Card forest : lands(b, 7)) {
            ctrl(b).actions.add(forest.getManaAbilities().get(0));
        }

        final Priority first = runUntilPriorityIn(1, PhaseType.CLEANUP);
        assertTrue(count(WURM, ZoneType.Stack) == 1, "b cast the Wurm during the cleanup check\n" + log);
        assertEquals(first.player(), a, "the active player gets priority first, not the Wurm's caster\n" + log);
    }
}
