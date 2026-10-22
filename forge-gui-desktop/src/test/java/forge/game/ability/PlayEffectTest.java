package forge.game.ability;

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
import forge.game.zone.ZoneType;
import forge.util.collect.FCollectionView;

import static junit.framework.Assert.assertTrue;

/** AllowRepeats$ lets the same card be played several times, up to Amount. */
public class PlayEffectTest extends AITest {

    /** Counts how often a card is offered, and fails the test if the same one keeps coming back. */
    private static class CountingController extends PlayerControllerAi {
        int offered = 0;

        CountingController(Game game, Player p) {
            super(game, p, p.getLobbyPlayer());
        }

        @Override
        public <T extends GameEntity> T chooseSingleEntityForEffect(FCollectionView<T> optionList,
                DelayedReveal delayedReveal, SpellAbility sa, String title, boolean isOptional,
                Player targetedPlayer, Map<String, Object> params) {
            if (++offered > 10) {
                throw new AssertionError("offered " + offered + " times");
            }
            return super.chooseSingleEntityForEffect(optionList, delayedReveal, sa, title, isOptional,
                    targetedPlayer, params);
        }
    }

    /** Amount$ bounds the tries, so a card that can't be paid for is offered Amount times at most. */
    @Test(timeOut = 60000)
    public void anUnpayableCardIsOfferedNoMoreThanAmountTimes() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        CountingController controller = new CountingController(game, p);
        p.dangerouslySetController(controller);
        // no mana cost and no WithoutManaCost$, so there is nothing to pay
        Card vision = addCardToZone("Ancestral Vision", p, ZoneType.Graveyard);
        SpellAbility sa = AbilityFactory.getAbility(
                "DB$ Play | Defined$ Self | Controller$ You | AllowRepeats$ True | Amount$ 2", vision);
        sa.setActivatingPlayer(p);

        AbilityUtils.resolve(sa);

        assertTrue("offered " + controller.offered + " times for Amount$ 2", controller.offered <= 2);
    }
}
