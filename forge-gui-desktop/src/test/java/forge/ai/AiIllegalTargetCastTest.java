package forge.ai;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

import org.testng.annotations.Test;

import com.google.common.eventbus.Subscribe;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.event.GameEventCardTapped;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * An AI cast with illegal targets must cost nothing: mana comes back, the spell goes back to the hand, nothing is left
 * on the stack, and the AI remembers the failure so it does not pick the same cast again and again.
 *
 * Runs through {@link PlayerControllerAi#playWithRollback}, the entry point the game loop uses, with the AI snapshot
 * rollback switched off ({@link PlayerControllerAi#AI_ROLLBACK}).
 *
 * Lives in package forge.ai because AiController.hasFailedToCast is package-private.
 */
public class AiIllegalTargetCastTest extends AITest {

    private static final String BOLT = "Lightning Bolt";
    private static final String MOUNTAIN = "Mountain";

    private static final class Fixture {
        final Game game;
        final Player ai;
        final Player opp;
        final PlayerControllerAi pc;
        final SpellAbility sa;

        Fixture(Game game, Player ai, Player opp, SpellAbility sa) {
            this.game = game;
            this.ai = ai;
            this.opp = opp;
            this.pc = (PlayerControllerAi) ai.getController();
            this.sa = sa;
        }
    }

    /** One untapped Mountain on the battlefield, a Lightning Bolt in hand, the AI's main phase. */
    private Fixture fixture() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);

        addCard(MOUNTAIN, ai);
        final Card bolt = addCardToZone(BOLT, ai, ZoneType.Hand);
        for (final Player p : game.getPlayers()) {
            fillLibrary(p, 20);
        }
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        game.getAction().checkStateEffects(true);

        assertTrue("the test needs an AI controller", ai.getController() instanceof PlayerControllerAi);
        final SpellAbility sa = bolt.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        return new Fixture(game, ai, opp, sa);
    }

    private static int count(final Player p, final ZoneType zone, final String name) {
        int n = 0;
        for (final Card c : p.getCardsIn(zone)) {
            if (c.getName().equals(name)) {
                n++;
            }
        }
        return n;
    }

    private static Card first(final Player p, final ZoneType zone, final String name) {
        for (final Card c : p.getCardsIn(zone)) {
            if (c.getName().equals(name)) {
                return c;
            }
        }
        return null;
    }

    private static boolean mountainTapped(final Player p) {
        return first(p, ZoneType.Battlefield, MOUNTAIN).isTapped();
    }

    /** Taps are only caused by paying here: when one happens the spell loses its targets, like a cost that changed them. */
    public static final class TargetSaboteur {
        private final SpellAbility sa;
        int tapEvents;

        TargetSaboteur(final SpellAbility sa) {
            this.sa = sa;
        }

        @Subscribe
        public void onTapped(final GameEventCardTapped e) {
            tapEvents++;
            sa.resetTargets();
        }
    }

    @Test
    public void aiRollbackSnapshotIsOff() {
        // the whole-game snapshot ran the phones out of memory; turning it on again should be a conscious decision
        assertFalse(PlayerControllerAi.AI_ROLLBACK);
    }

    @Test
    public void normalCastStillWorks() {
        final Fixture f = fixture();
        f.sa.getTargets().add(f.opp);

        final boolean played = f.pc.playWithRollback(f.sa, null);

        assertTrue("a legal cast goes on the stack", played);
        assertEquals("on the stack", 1, f.game.getStack().size());
        assertEquals("left the hand", 0, count(f.ai, ZoneType.Hand, BOLT));
        assertTrue("mana was paid", mountainTapped(f.ai));
        assertEquals(0, f.game.getStack().getIllegalAdditionCount(f.game.getStack().peekAbility().getHostCard()));
        assertFalse(f.pc.getAi().hasFailedToCast(f.sa));
    }

    @Test
    public void noTargetsMeansNothingIsPaidAndTheAiRemembers() {
        final Fixture f = fixture();
        f.sa.resetTargets(); // the AI got here without choosing any

        final boolean played = f.pc.playWithRollback(f.sa, null);

        assertFalse("not reported as cast", played);
        assertEquals("still in hand", 1, count(f.ai, ZoneType.Hand, BOLT));
        assertEquals("nothing on the stack", 0, f.game.getStack().size());
        assertEquals("nothing in the stack zone", 0, f.game.getCardsIn(ZoneType.Stack).size());
        assertFalse("nothing was paid", mountainTapped(f.ai));
        assertEquals("never got as far as adding it", 0,
                f.game.getStack().getIllegalAdditionCount(first(f.ai, ZoneType.Hand, BOLT)));
        assertTrue("remembered as failed", f.pc.getAi().hasFailedToCast(f.sa));

        // and it does not try again
        assertFalse(f.pc.playWithRollback(f.sa, null));
        assertEquals(1, count(f.ai, ZoneType.Hand, BOLT));
        assertFalse(mountainTapped(f.ai));
    }

    @Test
    public void deferredTargetChoiceThatFindsNothingReturnsTheSpell() {
        final Fixture f = fixture();

        // the targets are chosen after the spell moved to the stack zone: an empty choice has to send it back
        final boolean played = f.pc.playWithRollback(f.sa, s -> { });

        assertFalse(played);
        assertEquals("back in hand, not lost in the stack zone", 1, count(f.ai, ZoneType.Hand, BOLT));
        assertEquals(0, f.game.getCardsIn(ZoneType.Stack).size());
        assertEquals(0, f.game.getStack().size());
        assertFalse(mountainTapped(f.ai));
        assertTrue(f.pc.getAi().hasFailedToCast(f.sa));
    }

    @Test
    public void targetsThatTurnIllegalWhilePayingGiveEverythingBack() {
        final Fixture f = fixture();
        f.sa.getTargets().add(f.opp); // legal when the cast starts, so the check before paying lets it through
        final TargetSaboteur saboteur = new TargetSaboteur(f.sa);
        f.game.subscribeToEvents(saboteur);

        final boolean played = f.pc.playWithRollback(f.sa, null);

        assertTrue("harness: paying must have tapped the Mountain, or this test proves nothing", saboteur.tapEvents > 0);
        assertFalse("not reported as cast", played);
        assertEquals("the spell is back in hand", 1, count(f.ai, ZoneType.Hand, BOLT));
        assertEquals("not lost in the stack zone", 0, f.game.getCardsIn(ZoneType.Stack).size());
        assertEquals("nothing on the stack", 0, f.game.getStack().size());
        assertFalse("the mana was given back", mountainTapped(f.ai));
        assertEquals("counted as a failed addition", 1,
                f.game.getStack().getIllegalAdditionCount(first(f.ai, ZoneType.Hand, BOLT)));
        assertTrue("remembered even without a snapshot", f.pc.getAi().hasFailedToCast(f.sa));
    }
}