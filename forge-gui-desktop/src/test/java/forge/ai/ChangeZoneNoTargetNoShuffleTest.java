package forge.ai;

import java.util.ArrayList;
import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class ChangeZoneNoTargetNoShuffleTest extends AITest {

    @Test
    public void zeroTargetsShuffleNoLibrary() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        fillLibrary(p, 20);
        Card spell = addCardToZone("Turn the Earth", p, ZoneType.Hand);
        List<Card> before = new ArrayList<>(p.getCardsIn(ZoneType.Library));

        SpellAbility sa = spell.getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        AssertJUnit.assertTrue(sa.getTargets().isEmpty());
        AbilityUtils.resolve(sa);

        AssertJUnit.assertEquals(22, p.getLife());
        AssertJUnit.assertEquals(before, new ArrayList<>(p.getCardsIn(ZoneType.Library)));
    }
}
