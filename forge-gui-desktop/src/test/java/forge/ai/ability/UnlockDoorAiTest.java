package forge.ai.ability;

import forge.ai.AITest;
import forge.card.CardStateName;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

public class UnlockDoorAiTest extends AITest {

    private Card addRoom(Game game, Player p) {
        return addCard("Bottomless Pool", p);
    }

    /**
     * Keys to the House pays {3}, taps and sacrifices itself to "lock or unlock a door of target
     * Room you control". When every door of the Room is already unlocked the effect can only lock
     * one, so activating it throws the artifact away to shut off the AI's own Room.
     */
    @Test
    public void doesNotSacrificeItselfJustToLockItsOwnRoom() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);

        Card room = addRoom(game, ai);
        room.setUnlockedRooms(java.util.EnumSet.of(CardStateName.LeftSplit, CardStateName.RightSplit));

        addCard("Keys to the House", ai);
        for (int i = 0; i < 5; i++) {
            addCard("Island", ai);
        }
        game.getAction().checkStateEffects(true);

        playUntilStackClear(game);

        AssertJUnit.assertEquals("Keys to the House should not have been sacrificed", 1,
                countCardsWithName(game, "Keys to the House"));
        AssertJUnit.assertTrue("Both doors should still be unlocked",
                room.getUnlockedRooms().containsAll(
                        java.util.EnumSet.of(CardStateName.LeftSplit, CardStateName.RightSplit)));
    }

    /** With a door still locked the ability is worth its cost, so the AI should use it. */
    @Test
    public void unlocksARoomWhenADoorIsStillLocked() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);

        Card room = addRoom(game, ai);
        room.setUnlockedRooms(java.util.EnumSet.of(CardStateName.LeftSplit));

        addCard("Keys to the House", ai);
        for (int i = 0; i < 5; i++) {
            addCard("Island", ai);
        }
        game.getAction().checkStateEffects(true);

        playUntilStackClear(game);

        AssertJUnit.assertTrue("AI should have unlocked the remaining door, not locked the open one",
                room.getUnlockedRooms().contains(CardStateName.RightSplit));
    }

    /**
     * With both doors locked there is a choice to make. Bottomless Pool (the LeftSplit face) has a
     * "when you unlock this door" trigger that bounces a creature; Locker Room (RightSplit) only
     * has a combat damage trigger, so opening it pays out nothing right now. The AI should take
     * the door that actually does something.
     */
    @Test
    public void prefersTheDoorWithAnUnlockTrigger() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);

        Card room = addRoom(game, ai);
        room.setUnlockedRooms(java.util.EnumSet.noneOf(CardStateName.class));

        addCard("Keys to the House", ai);
        for (int i = 0; i < 5; i++) {
            addCard("Island", ai);
        }
        // something for the unlock trigger to target
        addCard("Grizzly Bears", game.getPlayers().get(0));
        game.getAction().checkStateEffects(true);

        playUntilStackClear(game);

        AssertJUnit.assertTrue("AI should have opened the door carrying the unlock trigger",
                room.getUnlockedRooms().contains(CardStateName.LeftSplit));
    }

    /**
     * Derelict Attic's unlock trigger draws two cards and loses 2 life, so opening that door at
     * 2 life kills the AI. Checking that a trigger merely exists is not enough - the AI has to ask
     * whether it actually wants to run it, which is what doTrigger does.
     */
    @Test
    public void doesNotOpenADoorWhoseTriggerWouldKillIt() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);

        Card room = addCard("Derelict Attic", ai);
        room.setUnlockedRooms(java.util.EnumSet.noneOf(CardStateName.class));
        for (int i = 0; i < 20; i++) {
            addCardToZone("Swamp", ai, forge.game.zone.ZoneType.Library);
        }

        addCard("Keys to the House", ai);
        for (int i = 0; i < 5; i++) {
            addCard("Island", ai);
        }
        ai.setLife(2, null);
        game.getAction().checkStateEffects(true);

        playUntilStackClear(game);

        AssertJUnit.assertFalse("AI should not have opened the door that would have killed it",
                room.getUnlockedRooms().contains(CardStateName.LeftSplit));
        AssertJUnit.assertTrue("AI should have opened the harmless Widow's Walk half instead",
                room.getUnlockedRooms().contains(CardStateName.RightSplit));
        AssertJUnit.assertTrue("AI should still be alive", ai.getLife() > 0);
    }
}
