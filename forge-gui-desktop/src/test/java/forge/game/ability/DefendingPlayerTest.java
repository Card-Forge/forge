package forge.game.ability;

import java.util.List;

import org.testng.annotations.Test;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

import static junit.framework.Assert.assertTrue;

/** Defined$ DefendingPlayer is the defending player of the combat the source is in (CR 506.2). */
public class DefendingPlayerTest extends AITest {

    /** There is no defending player outside combat, so the list is empty rather than an error. */
    @Test
    public void noDefendingPlayerOutsideCombat() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card bears = addCard("Grizzly Bears", p);
        SpellAbility sa = AbilityFactory.getAbility("DB$ GainLife | Defined$ DefendingPlayer | LifeAmount$ 1", bears);
        sa.setActivatingPlayer(p);

        List<Player> players = AbilityUtils.getDefinedPlayers(bears, "DefendingPlayer", sa);

        assertTrue("no combat, no defending player", players.isEmpty());
    }
}
