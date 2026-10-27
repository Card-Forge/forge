package forge.game.combat;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

/** Rampaging Geoderm: "If it's attacking a battle" is about the creature the property is tested on. */
public class AttackingBattleTest extends AITest {

    @Test
    public void attackingBattleIsAboutTheTestedCreatureNotTheSource() {
        Game game = initAndCreateGame();
        Player attacker = game.getPlayers().get(1);
        Player defender = game.getPlayers().get(0);
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_ATTACKERS, attacker);
        Card geoderm = addCard("Rampaging Geoderm", attacker);
        Card battle = addCard("Invasion of Regatha", defender);
        Card atBattle = addCard("Grizzly Bears", attacker);
        Card atPlayer = addCard("Grizzly Bears", attacker);
        atBattle.setSickness(false);
        atPlayer.setSickness(false);
        game.getAction().checkStateEffects(true);

        Combat combat = new Combat(attacker);
        game.getPhaseHandler().setCombat(combat);
        combat.addAttacker(geoderm, defender);
        combat.addAttacker(atBattle, battle);
        combat.addAttacker(atPlayer, defender);

        AssertJUnit.assertTrue("a creature attacking a battle matches", atBattle.isValid("Card.attackingBattle", attacker, geoderm, null));
        AssertJUnit.assertFalse("a creature attacking a player does not", atPlayer.isValid("Card.attackingBattle", attacker, geoderm, null));
    }
}
