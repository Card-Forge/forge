package forge.card;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

public class ClayGolemTest extends AITest {

    @Test
    public void testClayGolemHasDefaultSVarXAndResolvesMonstrosityAmount() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        Card clayGolem = addCard("Clay Golem", p);
        AssertJUnit.assertNotNull("Clay Golem should load", clayGolem);
        AssertJUnit.assertEquals("Default SVar X should be 1", "1", clayGolem.getSVar("X"));

        SpellAbility monstrositySa = null;
        for (SpellAbility sa : clayGolem.getAllSpellAbilities()) {
            if (sa.hasParam("Monstrosity")) {
                monstrositySa = sa;
                break;
            }
        }
        AssertJUnit.assertNotNull("Clay Golem should have Monstrosity ability", monstrositySa);
        AssertJUnit.assertEquals("X", monstrositySa.getParam("Monstrosity"));

        int amount = AbilityUtils.calculateAmount(clayGolem, "X", monstrositySa);
        AssertJUnit.assertEquals("Monstrosity amount X should evaluate to default SVar:X of 1", 1, amount);
    }
}
