package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.card.CardPlayOption.PayManaCost;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class AlurenTest extends AITest {
    private boolean canCastFreeWithAluren(Card c, Player p) {
        for (SpellAbility sa : c.getAllPossibleAbilities(p, true)) {
            if (sa.getMayPlayOption() != null && sa.getMayPlayOption().getPayManaCost() == PayManaCost.NO
                    && sa.getMayPlay().getHostCard().getName().equals("Aluren")) {
                return true;
            }
        }
        return false;
    }

    @Test
    public void everyPlayerMayCastWithAluren() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);

        addCard("Aluren", p);
        Card mine = addCardToZone("Grizzly Bears", p, ZoneType.Hand);
        Card stolen = addCard("Grizzly Bears", opp);
        Card taker = addCardToZone("Hostage Taker", p, ZoneType.Hand);
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertTrue(canCastFreeWithAluren(mine, p));

        // with no lands, p can only cast the exiled Bears through Aluren
        game.getAction().moveToPlay(taker, null, AbilityKey.newMap());
        gameLoopUntilNextPhase(game);
        stolen = game.getCardState(stolen);
        AssertJUnit.assertTrue(stolen.isInZone(ZoneType.Battlefield));
        AssertJUnit.assertEquals(p, stolen.getController());
    }
}
