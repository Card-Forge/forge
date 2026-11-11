package forge.gamesimulationtests.priority;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

import forge.ai.ComputerUtil;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * CR 608.2g: a spell cast during resolution gives nobody priority; CR 117.3b: after the resolving object finishes,
 * the active player receives priority. The same holds for a copy put onto the stack during resolution (CR 707.10).
 */
public class CastDuringResolutionPriorityTest extends PriorityTestBase {

    /** {@code caster} resolves Worldly Tutor and casts Panglacial Wurm during the search; it is {@code a}'s turn. */
    private Priority wurmCastDuringTutor(final Player caster) {
        giveWurm(caster);
        lands(caster, 1);
        cast("Worldly Tutor", caster);
        runUntil(() -> log.stream().anyMatch(p -> WURM.equals(p.stackTop())), "priority with the Wurm on the stack");
        return log.stream().filter(p -> WURM.equals(p.stackTop())).findFirst().get();
    }

    @Test(timeOut = 60000)
    public void nonActivePlayerCastsDuringResolution() {
        setUpGame();
        final Priority after = wurmCastDuringTutor(b);
        assertEquals(after.player(), a, "after Worldly Tutor resolves the active player gets priority first, not the Wurm's caster\n" + log);
        runUntil(() -> count(WURM, ZoneType.Battlefield) == 1, "the Wurm resolving");
        assertTrue(log.stream().filter(p -> WURM.equals(p.stackTop())).anyMatch(p -> p.player() == b),
                "both players had priority with the Wurm on the stack before it resolved\n" + log);
    }

    @Test(timeOut = 60000)
    public void activePlayerCastsDuringOwnResolution() {
        setUpGame();
        final Priority after = wurmCastDuringTutor(a);
        assertEquals(after.player(), a, log.toString());
    }

    /** b's Reverberate copies a's Opt: after Reverberate resolves, the active player a gets priority first. */
    @Test(timeOut = 60000)
    public void copyPutOnStackDuringResolution() {
        setUpGame();
        lands("Island", a, 1);
        lands("Mountain", b, 2);
        cast("Opt", a);
        final SpellAbility opt = game.getStack().peekAbility();
        final Card reverberate = addCardToZone("Reverberate", b, ZoneType.Hand);
        final SpellAbility copy = reverberate.getFirstSpellAbility();
        copy.setActivatingPlayer(b);
        copy.getTargets().add(opt);
        assertTrue(ComputerUtil.playStack(copy, b, game), "could not cast Reverberate");
        runUntil(() -> log.stream().anyMatch(p -> p.stackSize() == 2 && "Opt".equals(p.stackTop())), "priority with the copy on the stack");
        final Priority after = log.stream().filter(p -> p.stackSize() == 2 && "Opt".equals(p.stackTop())).findFirst().get();
        assertEquals(after.player(), a, "after Reverberate resolves the active player gets priority first, not the copy's controller\n" + log);
    }

    /**
     * a casts two Opts, then b casts Grapeshot on a's turn: b's storm trigger puts two copies on the stack as it
     * resolves. After the trigger resolves the active player a gets priority first, not the copies' controller.
     */
    @Test(timeOut = 60000)
    public void stormCopiesOnOpponentsTurn() {
        setUpGame();
        lands("Island", a, 2);
        cast("Opt", a);
        cast("Opt", a);
        runUntil(() -> game.getStack().isEmpty() && count("Opt", ZoneType.Graveyard) == 2, "both Opts resolving");

        lands("Mountain", b, 2);
        final SpellAbility grapeshot = addCardToZone("Grapeshot", b, ZoneType.Hand).getFirstSpellAbility();
        grapeshot.setActivatingPlayer(b);
        grapeshot.getTargets().add(a);
        assertTrue(ComputerUtil.playStack(grapeshot, b, game), "could not cast Grapeshot");

        // Grapeshot and its storm trigger make 2 objects; Grapeshot and its two copies make 3
        runUntil(() -> log.stream().anyMatch(p -> p.stackSize() == 3), "priority with the storm copies on the stack");
        final Priority after = log.stream().filter(p -> p.stackSize() == 3).findFirst().get();
        assertEquals(after.stackTop(), "Grapeshot", log.toString());
        assertEquals(after.player(), a, "after the storm trigger resolves the active player gets priority first, not the copies' controller\n" + log);
    }

    /**
     * a's Word of Command makes b cast the Opt in their hand during the resolution (CR 724.2, 608.2g). After Word of
     * Command resolves the active player a gets priority first (CR 117.3b), not the controlled player who cast Opt.
     */
    @Test(timeOut = 60000)
    public void spellCastByControlledPlayerDuringWordOfCommand() {
        setUpGame();
        lands("Swamp", a, 2);
        lands("Island", b, 1);
        addCardToZone("Opt", b, ZoneType.Hand);
        final SpellAbility word = addCardToZone("Word of Command", a, ZoneType.Hand).getFirstSpellAbility();
        word.setActivatingPlayer(a);
        word.getTargets().add(b);
        assertTrue(ComputerUtil.playStack(word, a, game), "could not cast Word of Command");

        runUntil(() -> log.stream().anyMatch(p -> "Opt".equals(p.stackTop())), "priority with Opt on the stack");
        final Priority after = log.stream().filter(p -> "Opt".equals(p.stackTop())).findFirst().get();
        assertEquals(after.player(), a, "after Word of Command resolves the active player gets priority first, not the controlled player\n" + log);
        runUntil(() -> count("Opt", ZoneType.Graveyard) == 1, "Opt resolving");
        assertTrue(game.getStack().isEmpty() && !b.isControlled(), "b is no longer controlled once Opt has resolved\n" + log);
    }
}
