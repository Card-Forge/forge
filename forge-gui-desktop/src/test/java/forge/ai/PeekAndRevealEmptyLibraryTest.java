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

public class PeekAndRevealEmptyLibraryTest extends AITest {

    private void resolveDuelist(Player p, Player opp) {
        Card duelist = addCard("Keen Duelist", p);
        SpellAbility sa = AbilityFactory.getAbility(duelist, "TrigReveal");
        sa.setActivatingPlayer(p);
        sa.getTargets().add(opp);
        AbilityUtils.resolve(sa);
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
