package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.undo.UndoManager;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.MagicStack;
import forge.game.zone.ZoneType;

/**
 * A spell or ability whose targets are not legal when it is put on the stack ("Couldn't add to stack, failed to
 * target") used to leave the card in the stack zone and on no stack: it was gone from the game, mana included.
 * It has to go back where it was cast from, with its mana, and the AI must not try the same cast over and over.
 * <p>
 * Every test casts on purpose without choosing a target, which is what the AI did with Cabal Therapy (its target is
 * on the discard sub ability, not on the spell itself).
 */
public class MagicStackIllegalTargetTest extends AITest {

    private static int count(final Game game, final ZoneType zone, final String name) {
        int n = 0;
        for (final Card c : game.getCardsIn(zone)) {
            if (c.getName().equals(name)) {
                n++;
            }
        }
        return n;
    }

    /** The card is only ever in one place. */
    private static void assertSomewhereExactlyOnce(final Game game, final String name, final ZoneType expected) {
        int total = 0;
        // real zones only: Flashback is a view of graveyard cards, it would count the same card twice
        for (final ZoneType zone : new ZoneType[] { ZoneType.Hand, ZoneType.Library, ZoneType.Graveyard,
                ZoneType.Battlefield, ZoneType.Exile, ZoneType.Stack, ZoneType.Command }) {
            total += count(game, zone, name);
        }
        AssertJUnit.assertEquals(name + " must not be lost or duplicated", 1, total);
        AssertJUnit.assertEquals(name + " must be in " + expected, 1, count(game, expected, name));
    }

    @Test
    public void spellWithoutLegalTargetGoesBackToHand() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        addCard("Mountain", ai);
        final Card bolt = addCardToZone("Lightning Bolt", ai, ZoneType.Hand);
        game.getAction().checkStateEffects(true);

        final SpellAbility sa = bolt.getSpellAbilities().get(0);
        sa.setActivatingPlayer(ai);
        // the AI path: the card is moved to the stack, paid for, then added, with no target chosen
        ComputerUtil.playStack(sa, ai, game);

        AssertJUnit.assertTrue("nothing can be on the stack", game.getStack().isEmpty());
        AssertJUnit.assertEquals(0, count(game, ZoneType.Stack, "Lightning Bolt"));
        assertSomewhereExactlyOnce(game, "Lightning Bolt", ZoneType.Hand);
    }

    @Test
    public void theManaIsGivenBack() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card mountain = addCard("Mountain", ai);
        final Card bolt = addCardToZone("Lightning Bolt", ai, ZoneType.Hand);
        game.getAction().checkStateEffects(true);

        final SpellAbility sa = bolt.getSpellAbilities().get(0);
        sa.setActivatingPlayer(ai);
        ComputerUtil.playStack(sa, ai, game);

        AssertJUnit.assertTrue("the land untaps, or its mana floats again (it was not simply lost)",
                !mountain.isTapped() || ai.getManaPool().totalMana() >= 1);
    }

    @Test
    public void humanPathAlsoReturnsTheSpell() {
        // addAndUnfreeze moves the card to the stack zone itself when it is not there yet
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card bolt = addCardToZone("Lightning Bolt", ai, ZoneType.Hand);
        game.getAction().checkStateEffects(true);

        final SpellAbility sa = bolt.getSpellAbilities().get(0);
        sa.setActivatingPlayer(ai);
        game.getStack().addAndUnfreeze(sa);

        AssertJUnit.assertTrue(game.getStack().isEmpty());
        AssertJUnit.assertFalse("the stack must not stay frozen", game.getStack().isFrozen());
        assertSomewhereExactlyOnce(game, "Lightning Bolt", ZoneType.Hand);
    }

    @Test
    public void cabalTherapyIsNotLostFromHand() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        addCard("Swamp", ai);
        final Card therapy = addCardToZone("Cabal Therapy", ai, ZoneType.Hand);
        game.getAction().checkStateEffects(true);

        final SpellAbility sa = therapy.getSpellAbilities().get(0);
        sa.setActivatingPlayer(ai);
        ComputerUtil.playStack(sa, ai, game);

        AssertJUnit.assertTrue(game.getStack().isEmpty());
        assertSomewhereExactlyOnce(game, "Cabal Therapy", ZoneType.Hand);
    }

    @Test
    public void cabalTherapyFlashbackGoesBackToTheGraveyard() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        addCard("Runeclaw Bear", ai);
        final Card therapy = addCardToZone("Cabal Therapy", ai, ZoneType.Graveyard);
        game.getAction().checkStateEffects(true);

        SpellAbility flashback = null;
        for (final SpellAbility sa : therapy.getAllPossibleAbilities(ai, false)) {
            if (sa.isFlashback()) {
                flashback = sa;
            }
        }
        AssertJUnit.assertNotNull("Cabal Therapy has flashback", flashback);
        flashback.setActivatingPlayer(ai);
        game.getStack().addAndUnfreeze(flashback);

        AssertJUnit.assertTrue(game.getStack().isEmpty());
        // the sacrificed creature is not given back (that cost can't be reversed without the original payment),
        // but the card itself must still be castable from where it was cast from, not exiled or gone
        assertSomewhereExactlyOnce(game, "Cabal Therapy", ZoneType.Graveyard);
    }

    @Test
    public void aCommanderGoesBackToTheCommandZone() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        // a stand in for a commander: any card cast from the command zone is returned there
        final Card commander = addCardToZone("Lightning Bolt", ai, ZoneType.Command);

        final SpellAbility sa = commander.getSpellAbilities().get(0);
        sa.setActivatingPlayer(ai);
        game.getStack().addAndUnfreeze(sa);

        AssertJUnit.assertTrue(game.getStack().isEmpty());
        assertSomewhereExactlyOnce(game, "Lightning Bolt", ZoneType.Command);
    }

    @Test
    public void anAbilityThatCannotBeAddedDoesNotTakeItsSourceAway() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card pyromancer = addCard("Prodigal Pyromancer", ai);
        pyromancer.setSickness(false);
        game.getAction().checkStateEffects(true);

        SpellAbility ping = null;
        for (final SpellAbility sa : pyromancer.getSpellAbilities()) {
            if (sa.isAbility() && sa.usesTargeting()) {
                ping = sa;
            }
        }
        AssertJUnit.assertNotNull(ping);
        ping.setActivatingPlayer(ai);
        game.getStack().addAndUnfreeze(ping);

        AssertJUnit.assertTrue(game.getStack().isEmpty());
        AssertJUnit.assertEquals("the creature stays on the battlefield", 1,
                count(game, ZoneType.Battlefield, "Prodigal Pyromancer"));
    }

    @Test
    public void theAiChecksTheTargetsBeforePayingAndDoesNotTryAgain() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card mountain = addCard("Mountain", ai);
        final Card bolt = addCardToZone("Lightning Bolt", ai, ZoneType.Hand);
        game.getAction().checkStateEffects(true);

        final SpellAbility sa = bolt.getSpellAbilities().get(0);
        sa.setActivatingPlayer(ai);
        final AiController controller = ((PlayerControllerAi) ai.getController()).getAi();

        AssertJUnit.assertFalse(ComputerUtil.handlePlayingSpellAbility(ai, sa, null));
        AssertJUnit.assertFalse("nothing was paid for it", mountain.isTapped());
        assertSomewhereExactlyOnce(game, "Lightning Bolt", ZoneType.Hand);
        AssertJUnit.assertTrue("not tried again this turn", controller.hasFailedToCast(sa));
        AssertJUnit.assertEquals("the AI does not even consider it", AiPlayDecision.CantPlayAi, controller.canPlaySa(sa));
    }

    @Test
    public void aCastAbandonedAfterTheSpellLeftTheHandGoesBack() {
        // the chosen targets are not valid: the spell was already moved to the stack zone and used to stay there
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        addCard("Mountain", ai);
        final Card bolt = addCardToZone("Lightning Bolt", ai, ZoneType.Hand);
        game.getAction().checkStateEffects(true);

        final SpellAbility sa = bolt.getSpellAbilities().get(0);
        sa.setActivatingPlayer(ai);
        AssertJUnit.assertFalse(ComputerUtil.handlePlayingSpellAbility(ai, sa, chosen -> { /* chooses nothing */ }));

        AssertJUnit.assertTrue(game.getStack().isEmpty());
        AssertJUnit.assertEquals(0, count(game, ZoneType.Stack, "Lightning Bolt"));
        assertSomewhereExactlyOnce(game, "Lightning Bolt", ZoneType.Hand);
    }

    @Test
    public void aLegalTargetedCastStillWorksWithTheSafetyNet() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        addCard("Mountain", ai);
        final Card bolt = addCardToZone("Lightning Bolt", ai, ZoneType.Hand);
        game.getAction().checkStateEffects(true);

        final SpellAbility sa = bolt.getSpellAbilities().get(0);
        sa.setActivatingPlayer(ai);
        final boolean played = ((PlayerControllerAi) ai.getController()).playWithRollback(sa, chosen -> {
            chosen.resetTargets();
            chosen.getTargets().add(opp);
        });

        AssertJUnit.assertTrue(played);
        AssertJUnit.assertEquals("on the stack, not rolled back", 1, game.getStack().size());
        AssertJUnit.assertEquals(1, count(game, ZoneType.Stack, "Lightning Bolt"));
    }

    @Test
    public void aRollbackSnapshotPutsTheGameBack() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card mountain = addCard("Mountain", ai);
        final Card bolt = addCardToZone("Lightning Bolt", ai, ZoneType.Hand);
        game.getAction().checkStateEffects(true);

        final UndoManager.Mark mark = game.getUndoManager().markForRollback(game);
        AssertJUnit.assertNotNull(mark);

        mountain.tap(true, null, null);
        game.getAction().moveToGraveyard(bolt, null);
        AssertJUnit.assertEquals(1, count(game, ZoneType.Graveyard, "Lightning Bolt"));

        AssertJUnit.assertTrue(game.getUndoManager().rollbackTo(game, mark));
        AssertJUnit.assertFalse("the land is untapped again", mountain.isTapped());
        assertSomewhereExactlyOnce(game, "Lightning Bolt", ZoneType.Hand);
    }

    @Test
    public void noSnapshotIsTakenWhenUndoIsOff() {
        final Game game = initAndCreateGame();
        game.getUndoManager().setMode(UndoManager.Mode.OFF);
        AssertJUnit.assertNull(game.getUndoManager().markForRollback(game));
        AssertJUnit.assertFalse(game.getUndoManager().rollbackTo(game, null));
    }

    @Test
    public void theFailureCountStartsOverEachTurn() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card bolt = addCardToZone("Lightning Bolt", ai, ZoneType.Hand);
        game.getAction().checkStateEffects(true);

        for (int i = 0; i < MagicStack.MAX_ILLEGAL_ADDITIONS_PER_TURN; i++) {
            final Card inHand = ai.getCardsIn(ZoneType.Hand).get(0);
            final SpellAbility sa = inHand.getSpellAbilities().get(0);
            sa.setActivatingPlayer(ai);
            game.getStack().addAndUnfreeze(sa);
        }
        AssertJUnit.assertTrue(game.getStack().hasRepeatedlyFailedToAdd(bolt));

        game.getStack().onNextTurn();
        AssertJUnit.assertFalse(game.getStack().hasRepeatedlyFailedToAdd(bolt));
    }
}
