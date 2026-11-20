package forge.ai.ability;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class DamageAllAiTest extends AITest {
    // The AI has the better board on paper, but the opponent's flyers kill it next turn.
    @Test
    public void testSweepsWhenFacingLethalAttack() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        fillLibrary(ai, 10);
        fillLibrary(opp, 10);
        ai.setLife(4, null);
        addCards("Mountain", 9, ai);
        addCards("Grizzly Bears", 4, ai);
        addCards("Wind Drake", 2, opp);
        addCardToZone("Blasphemous Act", ai, ZoneType.Hand);

        moveToMain2(game, ai);
        playUntilPhase(game, PhaseType.END_OF_TURN);

        AssertJUnit.assertEquals(0, opp.getCreaturesInPlay().size());
    }
}
