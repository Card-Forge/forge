package forge.game.ability;

import org.testng.annotations.Test;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

import static junit.framework.Assert.assertEquals;

/** Restarting the game ends everything the old game had scheduled for a later phase. */
public class RestartGameCommandsTest extends AITest {

    /** A command waiting for the beginning of combat (a "control that player's combat" grant) must not outlive a restart. */
    @Test
    public void aBeginningOfCombatCommandDoesNotSurviveARestart() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card host = addCard("Grizzly Bears", p);
        int[] ran = {0};
        game.getBeginOfCombat().addUntil(p, () -> ran[0]++);

        SpellAbility sa = AbilityFactory.getAbility("DB$ RestartGame | RestrictFromZone$ Exile", host);
        sa.setActivatingPlayer(p);
        AbilityUtils.resolve(sa);
        game.getBeginOfCombat().executeUntil(p);

        assertEquals("the old game's command did not run in the new game", 0, ran[0]);
    }
}
