package forge.ai.ability;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.AITest;
import forge.ai.PlayerControllerAi;
import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

public class GoadAiTest extends AITest {
    private Card goadTarget(Game game, Player ai, Card host) {
        game.getAction().checkStateEffects(true);
        SpellAbility sa = AbilityFactory.getAbility("DB$ Goad | ValidTgts$ Creature", host);
        sa.setActivatingPlayer(ai);
        ((PlayerControllerAi) ai.getController()).getAi().doTrigger(sa, true);
        return sa.getTargets().getFirstTargetedCard();
    }

    @Test
    public void testGoadsOpponentCreatureItCanBlock() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card host = addCard("Craw Wurm", ai);
        addCard("Hill Giant", ai);
        Card theirs = addCard("Grizzly Bears", opp);

        AssertJUnit.assertEquals(theirs, goadTarget(game, ai, host));
    }

    @Test
    public void testMandatoryGoadPrefersOpponentCreature() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        Card host = addCard("Grizzly Bears", ai);
        Card theirs = addCard("Craw Wurm", opp);

        AssertJUnit.assertEquals(theirs, goadTarget(game, ai, host));
    }
}
