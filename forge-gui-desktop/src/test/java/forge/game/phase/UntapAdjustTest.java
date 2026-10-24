package forge.game.phase;

import java.util.Map;

import org.testng.annotations.Test;

import forge.ai.AITest;
import forge.ai.PlayerControllerAi;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.card.Card;
import forge.game.player.DelayedReveal;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.collect.FCollectionView;

import static junit.framework.Assert.assertTrue;

/** Winter Orb limits the untap step to one land, which the player picks one land at a time. */
public class UntapAdjustTest extends AITest {

    /** Declines every choice, and fails the test if it is asked again and again. */
    private static class DecliningController extends PlayerControllerAi {
        private int asked = 0;

        DecliningController(Game game, Player p) {
            super(game, p, p.getLobbyPlayer());
        }

        @Override
        public <T extends GameEntity> T chooseSingleEntityForEffect(FCollectionView<T> optionList,
                DelayedReveal delayedReveal, SpellAbility sa, String title, boolean isOptional,
                Player targetedPlayer, Map<String, Object> params) {
            if (++asked > 10) {
                throw new AssertionError("asked " + asked + " times to choose a land and declined each time");
            }
            return null;
        }
    }

    /** A choice that never comes must end the untap step, not repeat the question forever. */
    @Test(timeOut = 60000)
    public void aDeclinedChoiceEndsTheUntapStep() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        addCard("Winter Orb", p);
        Card first = addCard("Mountain", p);
        Card second = addCard("Mountain", p);
        first.setTapped(true);
        second.setTapped(true);
        game.getAction().checkStateEffects(true);
        p.dangerouslySetController(new DecliningController(game, p));

        new Untap(game).executeAt();

        assertTrue("no land was chosen, so none untaps", first.isTapped() && second.isTapped());
    }
}
