package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class DigForgetOtherRememberedTest extends AITest {

    @Test
    public void emptyLibraryStillForgetsThePreviousPass() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card surge = addCardToZone("Primal Surge", p, ZoneType.Hand);
        Card previous = addCard("Runeclaw Bear", p);
        surge.addRemembered(previous);
        AssertJUnit.assertTrue(p.getCardsIn(ZoneType.Library).isEmpty());

        SpellAbility dig = AbilityFactory.getAbility("DB$ Dig | DigNum$ 1 | ChangeNum$ All | ChangeValid$ Card"
                + " | DestinationZone$ Exile | ForgetOtherRemembered$ True | RememberChanged$ True", surge);
        dig.setActivatingPlayer(p);
        AbilityUtils.resolve(dig);

        AssertJUnit.assertFalse(surge.hasRemembered());
    }
}
