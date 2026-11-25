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

/**
 * Keen Duelist: "you and target opponent each reveal the top card of your library. You each lose life equal to
 * the mana value of the card revealed by the other player." An empty library for one player means only that that
 * player reveals nothing; the other player still reveals the top card of their own library.
 */
public class PeekAndRevealEmptyLibraryTest extends AITest {

    private void resolveDuelist(Player p, Player opp) {
        Card duelist = addCard("Keen Duelist", p);
        SpellAbility sa = AbilityFactory.getAbility(duelist, "TrigReveal");
        sa.setActivatingPlayer(p);
        sa.getTargets().add(opp);
        AbilityUtils.resolve(sa);
    }

    @Test
    public void opponentsEmptyLibraryDoesNotHideTheControllersReveal() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCardToZone("Runeclaw Bear", p, ZoneType.Library);

        resolveDuelist(p, opp);

        // the opponent loses life equal to the mana value of the controller's revealed card (Runeclaw Bear, 2)
        AssertJUnit.assertEquals(18, opp.getLife());
        AssertJUnit.assertEquals(20, p.getLife());
    }

    @Test
    public void controllersEmptyLibraryDoesNotHideTheOpponentsReveal() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        addCardToZone("Runeclaw Bear", opp, ZoneType.Library);

        resolveDuelist(p, opp);

        AssertJUnit.assertEquals(18, p.getLife());
        AssertJUnit.assertEquals(20, opp.getLife());
    }
}
