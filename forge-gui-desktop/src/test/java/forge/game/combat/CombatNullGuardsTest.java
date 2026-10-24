package forge.game.combat;

import org.testng.annotations.Test;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

import static junit.framework.Assert.assertTrue;

/** Combat queries that can be asked about an attacker that is gone or a battle that has no protector. */
public class CombatNullGuardsTest extends AITest {

    /**
     * Brimaz puts a token onto the battlefield blocking an attacker. If that attacker has left combat by the
     * time the trigger resolves (CR 506.4), the token still enters but blocks nothing.
     */
    @Test
    public void aTokenBlockingAnAttackerThatLeftCombatBlocksNothing() {
        Game game = initAndCreateGame();
        Player attackerPlayer = game.getPlayers().get(1);
        Player defender = game.getPlayers().get(0);
        Combat combat = new Combat(attackerPlayer);
        game.getPhaseHandler().setCombat(combat);
        Card gone = addCard("Grizzly Bears", attackerPlayer);
        Card stays = addCard("Runeclaw Bear", attackerPlayer);
        combat.addAttacker(gone, defender);
        combat.addAttacker(stays, defender);
        combat.removeFromCombat(gone);

        SpellAbility sa = AbilityFactory.getAbility("DB$ Token | TokenScript$ w_1_1_cat_soldier_vigilance"
                + " | TokenOwner$ You | TokenBlocking$ Self", gone);
        sa.setActivatingPlayer(defender);
        AbilityUtils.resolve(sa);

        assertTrue("the token entered, but blocks nothing", combat.getAllBlockers().isEmpty());
    }
}
