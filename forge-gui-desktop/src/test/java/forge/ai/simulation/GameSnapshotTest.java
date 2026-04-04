package forge.ai.simulation;

import forge.game.Game;
import forge.game.GameSnapshot;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

public class GameSnapshotTest extends SimulationTest {

    @Test
    public void testSnapshotRestorePreservesCardCount() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCards("Plains", 3, p);
        addCard("Grizzly Bears", p);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        int cardsBefore = game.getCardsIn(ZoneType.Battlefield).size();

        game.EXPERIMENTAL_RESTORE_SNAPSHOT = true;
        GameSnapshot snapshot = new GameSnapshot(game);
        snapshot.makeCopy();
        snapshot.restoreGameState(game);

        int cardsAfter = game.getCardsIn(ZoneType.Battlefield).size();
        AssertJUnit.assertEquals("Card count should be preserved after restore", cardsBefore, cardsAfter);
    }

    @Test
    public void testSnapshotRestoreUntapsLands() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        Card plains = addCard("Plains", p);
        addCard("Grizzly Bears", p);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        // Snapshot with land untapped
        game.EXPERIMENTAL_RESTORE_SNAPSHOT = true;
        GameSnapshot snapshot = new GameSnapshot(game);
        snapshot.makeCopy();

        // Tap the land (simulating mana payment)
        plains.setTapped(true);
        AssertJUnit.assertTrue("Land should be tapped", plains.isTapped());

        // Restore — land should be untapped again
        snapshot.restoreGameState(game);
        AssertJUnit.assertFalse("Land should be untapped after restore", plains.isTapped());
    }

    @Test
    public void testMultiStepUndo() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        Card plains1 = addCard("Plains", p);
        Card plains2 = addCard("Plains", p);
        addCard("Grizzly Bears", p);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);
        game.EXPERIMENTAL_RESTORE_SNAPSHOT = true;

        // Simulate two actions with stash/commit pattern
        // Action 1: tap plains1
        game.stashGameState();
        plains1.setTapped(true);
        game.commitGameState();

        // Action 2: tap plains2
        game.stashGameState();
        plains2.setTapped(true);
        game.commitGameState();

        AssertJUnit.assertTrue(plains1.isTapped());
        AssertJUnit.assertTrue(plains2.isTapped());

        // Undo action 2 — plains2 should untap
        AssertJUnit.assertTrue(game.undoToLastSnapshot());
        AssertJUnit.assertTrue("plains1 still tapped after first undo", plains1.isTapped());
        AssertJUnit.assertFalse("plains2 untapped after first undo", plains2.isTapped());

        // Undo action 1 — plains1 should untap
        AssertJUnit.assertTrue(game.undoToLastSnapshot());
        AssertJUnit.assertFalse("plains1 untapped after second undo", plains1.isTapped());
        AssertJUnit.assertFalse("plains2 still untapped", plains2.isTapped());

        // No more undo available
        AssertJUnit.assertFalse(game.undoToLastSnapshot());
    }

    @Test
    public void testUndoThenNewActionThenUndo() {
        // Regression: after undo, the pending snapshot must be refreshed.
        // Otherwise the next commit pushes a stale snapshot.
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        Card plains1 = addCard("Plains", p);
        Card plains2 = addCard("Plains", p);
        Card plains3 = addCard("Plains", p);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);
        game.EXPERIMENTAL_RESTORE_SNAPSHOT = true;

        // Action 1: tap plains1
        game.stashGameState();
        plains1.setTapped(true);
        game.commitGameState();

        // Action 2: tap plains2
        game.stashGameState();
        plains2.setTapped(true);
        game.commitGameState();

        // Undo action 2
        AssertJUnit.assertTrue(game.undoToLastSnapshot());
        AssertJUnit.assertTrue(plains1.isTapped());
        AssertJUnit.assertFalse("plains2 untapped after undo", plains2.isTapped());

        // New action 3: tap plains3 (instead of plains2)
        // stashGameState was already called by undoToLastSnapshot, so no need to call again
        plains3.setTapped(true);
        game.commitGameState();

        // Undo action 3 — should go back to state with only plains1 tapped
        AssertJUnit.assertTrue(game.undoToLastSnapshot());
        AssertJUnit.assertTrue("plains1 still tapped", plains1.isTapped());
        AssertJUnit.assertFalse("plains2 NOT tapped (was never re-played)", plains2.isTapped());
        AssertJUnit.assertFalse("plains3 untapped after undo", plains3.isTapped());
    }

    @Test
    public void testSnapshotRestorePreservesLife() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCards("Plains", 2, p);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);

        int lifeBefore = p.getLife();

        game.EXPERIMENTAL_RESTORE_SNAPSHOT = true;
        GameSnapshot snapshot = new GameSnapshot(game);
        snapshot.makeCopy();

        // Change life
        p.setLife(lifeBefore - 5, null);
        AssertJUnit.assertEquals(lifeBefore - 5, p.getLife());

        // Restore
        snapshot.restoreGameState(game);
        AssertJUnit.assertEquals("Life should be restored", lifeBefore, p.getLife());
    }
}
