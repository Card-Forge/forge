package forge.game.trigger;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.GameEndReason;
import forge.game.ability.AbilityKey;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

public class MandatoryLoopTest extends AITest {

    @Test
    public void endlessMandatoryTriggersDrawTheGame() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        addCard("Gremlin Tamer", p);
        addCard("Secret Arcade", p);
        addCard("Night of Souls' Betrayal", p);
        game.getAction().checkStateEffects(true);

        game.getAction().moveToPlay(addCardToZone("Sterling Grove", p, ZoneType.Hand), null, AbilityKey.newMap());
        for (int i = 0; i < 1000 && !game.isGameOver(); i++) {
            game.getPhaseHandler().mainLoopStep();
        }

        AssertJUnit.assertTrue(game.isGameOver());
        AssertJUnit.assertEquals(GameEndReason.Draw, game.getOutcome().getWinCondition());
    }
}
