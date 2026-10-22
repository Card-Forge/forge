package forge.game.player;

import org.testng.annotations.Test;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.zone.ZoneType;

import static junit.framework.Assert.assertTrue;

public class PlaneswalkTest extends AITest {

    /** With an empty planar deck there is no plane to planeswalk to. */
    @Test
    public void planeswalkingWithAnEmptyPlanarDeckDoesNothing() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        assertTrue("setup: the planar deck is empty", p.getZone(ZoneType.PlanarDeck).isEmpty());

        p.planeswalk(null);

        assertTrue("no plane appeared", p.getZone(ZoneType.Command).isEmpty());
    }
}
