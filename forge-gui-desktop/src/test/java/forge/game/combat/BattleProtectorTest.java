package forge.game.combat;

import org.testng.annotations.Test;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.card.Card;
import forge.game.player.Player;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

public class BattleProtectorTest extends AITest {

    /** A battle that has no protector is not a defender anyone can attack; asking must not throw. */
    @Test
    public void aBattleWithoutAProtectorIsNotAPossibleDefender() {
        Game game = initAndCreateGame();
        Player attackerPlayer = game.getPlayers().get(1);
        Card battle = addCard("Invasion of Ikoria", game.getPlayers().get(0));
        assertTrue("setup: the battle has no protector", battle.getProtectingPlayer() == null);

        boolean offered = false;
        for (GameEntity defender : CombatUtil.getAllPossibleDefenders(attackerPlayer)) {
            offered |= defender == battle;
        }

        assertFalse("a battle nobody protects is not offered as an attack target", offered);
    }
}
