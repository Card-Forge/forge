package forge.game.ability;

import org.testng.annotations.Test;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

import static junit.framework.Assert.assertEquals;

/** A random retarget keeps the damage a divided ability was dividing. */
public class ChangeTargetsDividedTest extends AITest {

    /** The whole division moves to the new target; it must not be read after the old targets were cleared. */
    @Test
    public void aRandomRetargetKeepsTheDividedTotal() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card host = addCard("Grizzly Bears", p);
        SpellAbility divided = AbilityFactory.getAbility(
                "AB$ DealDamage | Cost$ 0 | ValidTgts$ Any | NumDmg$ 3 | TargetMin$ 1 | TargetMax$ 3 | DividedAsYouChoose$ 3",
                host);
        divided.setActivatingPlayer(p);
        divided.getTargets().add(p);
        divided.addDividedAllocation(p, 3);
        game.getStack().add(divided);
        // an activated ability goes on the stack as a copy
        SpellAbility onStack = game.getStack().peekAbility();

        SpellAbility sa = AbilityFactory.getAbility(
                "DB$ ChangeTargets | TargetType$ Activated | ValidTgts$ Card | RandomTarget$ True", host);
        sa.setActivatingPlayer(p);
        sa.getTargets().add(onStack);

        assertEquals("setup", 3, onStack.getTotalDividedValue());
        AbilityUtils.resolve(sa);

        assertEquals("the three points stay allocated", 3, onStack.getTotalDividedValue());
    }
}
