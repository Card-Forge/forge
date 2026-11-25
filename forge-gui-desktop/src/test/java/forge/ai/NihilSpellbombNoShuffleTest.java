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

/**
 * Nihil Spellbomb exiles all cards from target player's graveyard; it never shuffles that player's library.
 */
public class NihilSpellbombNoShuffleTest extends AITest {

    @Test
    public void exilingTheGraveyardKeepsTheLibraryOrder() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        fillLibrary(opp, 20);
        addCardToZone("Runeclaw Bear", opp, ZoneType.Graveyard);
        addCardToZone("Runeclaw Bear", opp, ZoneType.Graveyard);
        Card bomb = addCard("Nihil Spellbomb", p);
        List<Card> before = new ArrayList<>(opp.getCardsIn(ZoneType.Library));

        SpellAbility sa = bomb.getSpellAbilities().stream().filter(SpellAbility::isActivatedAbility).findFirst().get();
        sa.setActivatingPlayer(p);
        sa.getTargets().add(opp);
        AbilityUtils.resolve(sa);

        AssertJUnit.assertTrue(opp.getCardsIn(ZoneType.Graveyard).isEmpty());
        AssertJUnit.assertEquals(2, opp.getCardsIn(ZoneType.Exile).size());
        AssertJUnit.assertEquals(before, new ArrayList<>(opp.getCardsIn(ZoneType.Library)));
    }
}
