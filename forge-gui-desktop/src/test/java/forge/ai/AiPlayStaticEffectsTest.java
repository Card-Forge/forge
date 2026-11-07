package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

/**
 * Lets the AI play whole turns with permanent blanket static effects in play. The AI is not deterministic, so this
 * only checks that the game keeps going, the AI actually plays cards, and the blanket effects apply to what it plays.
 */
public class AiPlayStaticEffectsTest extends AITest {

    @Test
    public void testAiPlaysTurnsWithBlanketStaticEffects() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);

        addCard("Teferi, Mage of Zhalfir", p);
        addCard("Urborg, Tomb of Yawgmoth", p);
        addCard("Enchanted Evening", p);
        for (int i = 0; i < 3; i++) {
            addCardToZone("Forest", p, ZoneType.Hand);
            addCardToZone("Runeclaw Bear", p, ZoneType.Hand);
        }
        for (int i = 0; i < 15; i++) {
            addCardToZone("Plains", p, ZoneType.Library);
            addCardToZone("Plains", opp, ZoneType.Library);
        }
        game.getAction().checkStateEffects(true);
        int permanentsBefore = p.getCardsIn(ZoneType.Battlefield).size();

        playUntilNextTurn(game);
        playUntilNextTurn(game);
        playUntilNextTurn(game);

        AssertJUnit.assertFalse("the game must not have ended", game.isGameOver());
        int permanentsAfter = p.getCardsIn(ZoneType.Battlefield).size();
        AssertJUnit.assertTrue("the AI should have played at least one card, had " + permanentsBefore + " now "
                + permanentsAfter, permanentsAfter > permanentsBefore);
        for (Card c : p.getCardsIn(ZoneType.Battlefield)) {
            AssertJUnit.assertTrue(c.getName() + " is an enchantment", c.isEnchantment());
            if (c.isLand()) {
                AssertJUnit.assertTrue(c.getName() + " is a Swamp", c.getType().hasSubtype("Swamp"));
            }
        }
    }
}
