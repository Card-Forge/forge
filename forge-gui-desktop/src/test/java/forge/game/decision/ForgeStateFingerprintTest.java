package forge.game.decision;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.player.Player;
import forge.util.DeterminismAuditRandom;
import forge.util.MyRandom;
import org.testng.annotations.Test;

import java.util.Random;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;

public class ForgeStateFingerprintTest extends AITest {

    @Test
    public void repeatedFingerprintingIsStableAndConsumesNoGameplayRng() {
        final Game game = initAndCreateGame();
        final Random original = MyRandom.getRandom();
        final DeterminismAuditRandom random = new DeterminismAuditRandom(20260810L);
        MyRandom.setRandom(random);
        try {
            final String first = ForgeStateFingerprint.canonical(game);
            final String second = ForgeStateFingerprint.canonical(game);

            assertEquals(second, first);
            assertEquals(random.getDrawCount(), 0L);
        } finally {
            MyRandom.setRandom(original);
        }
    }

    @Test
    public void observableGameplayMutationChangesTheFingerprint() {
        final Game game = initAndCreateGame();
        final Player player = game.getPlayers().get(0);
        final String before = ForgeStateFingerprint.canonical(game);

        player.setLife(player.getLife() - 1, null);

        assertNotEquals(ForgeStateFingerprint.canonical(game), before);
    }
}
