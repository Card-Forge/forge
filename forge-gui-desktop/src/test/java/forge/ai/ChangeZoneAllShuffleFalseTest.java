package forge.ai;

import java.util.ArrayList;
import java.util.List;

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
 * A ChangeZoneAll with Shuffle$ False must not shuffle any library. Wakanda Forever!'s last step
 * ("Put the rest into your graveyard") carries Shuffle$ False; the effect tested only that the
 * parameter was present, so it shuffled every player's library.
 */
public class ChangeZoneAllShuffleFalseTest extends AITest {

    @Test
    public void shuffleFalseLeavesLibrariesInOrder() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        fillLibrary(p, 20);
        fillLibrary(opp, 20);
        Card wakanda = addCardToZone("Wakanda Forever!", p, ZoneType.Hand);
        List<Card> before = new ArrayList<>(p.getCardsIn(ZoneType.Library));
        List<Card> oppBefore = new ArrayList<>(opp.getCardsIn(ZoneType.Library));

        // nothing is remembered, so the step moves no card
        SpellAbility rest = AbilityFactory.getAbility(wakanda, "DBRestGrave");
        rest.setActivatingPlayer(p);
        AbilityUtils.resolve(rest);

        AssertJUnit.assertEquals(before, new ArrayList<>(p.getCardsIn(ZoneType.Library)));
        AssertJUnit.assertEquals(oppBefore, new ArrayList<>(opp.getCardsIn(ZoneType.Library)));
    }
}
