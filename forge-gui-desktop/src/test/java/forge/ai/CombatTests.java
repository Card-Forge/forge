package forge.ai;

import forge.game.Game;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import org.testng.AssertJUnit;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

public class CombatTests extends AITest {

    @Test
    public void testSwingForLethal() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        p.setTeam(0);
        addCard("Nest Robber", p);
        addCard("Nest Robber", p);

        Player opponent = game.getPlayers().get(0);
        opponent.setTeam(1);

        addCard("Runeclaw Bear", opponent);
        opponent.setLife(2, null);

        this.playUntilPhase(game, PhaseType.END_OF_TURN);

        AssertJUnit.assertTrue(game.isGameOver());
    }

    @Ignore
    @Test
    public void testClearForLethal() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        p.setTeam(0);
        addCard("Brazen Scourge", p);
        addCard("Brazen Scourge", p);
        addCard("Mountain", p);
        addCardToZone("Shock", p, ZoneType.Hand);

        Player opponent = game.getPlayers().get(0);
        opponent.setTeam(1);

        addCard("Runeclaw Bear", opponent);
        opponent.setLife(6, null);

        this.playUntilPhase(game, PhaseType.END_OF_TURN);
        System.out.println(this.gameStateToString(game));

        AssertJUnit.assertTrue(game.isGameOver());
    }

}
