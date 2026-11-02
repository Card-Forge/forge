package forge.ai.ability;

import org.testng.annotations.Test;

import forge.ai.AITest;
import forge.card.CardStateName;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;

import static org.testng.Assert.assertTrue;

// Ashnod's Altar (sacrifice a creature: add {C}{C}) + Funeral Room (whenever a creature you control
// dies, each opponent loses 1 life and you gain 1 life) + Trudge Garden (whenever you gain life, you may pay {2}: 4/4
// Fungus Beast). The altar's mana pays the next creature: endless drain. Four 0/8
// walls stop the attack, no land.
public class SacManaLoop152Test extends AITest {

    private Player altarGame(boolean garden) {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);
        addCard("Ashnod's Altar", ai);
        Card room = addCard("Funeral Room // Awakening Hall", ai);
        room.unlockRoom(ai, CardStateName.LeftSplit);
        if (garden) {
            addCard("Trudge Garden", ai);
        }
        addCard("Grizzly Bears", ai).setSickness(false);
        for (int i = 0; i < 4; i++) {
            addCard("Wall of Stone", opponent);
        }
        fillLibrary(ai, 20);
        fillLibrary(opponent, 20);
        ai.addLandPlayedThisTurn();
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        playUntilNextTurn(game);
        return opponent;
    }

    @Test(timeOut = 120000)
    public void testAltarRoomGardenLoopKills() {
        Player opponent = altarGame(true);
        assertTrue(opponent.getLife() <= 0, "life " + opponent.getLife());
    }

    // without Trudge Garden there is no refund: the bear stays (no loop to run), at most one life
    @Test(timeOut = 120000)
    public void testWithoutGardenNoLoop() {
        Player opponent = altarGame(false);
        assertTrue(opponent.getLife() >= 19, "life " + opponent.getLife());
    }
}
