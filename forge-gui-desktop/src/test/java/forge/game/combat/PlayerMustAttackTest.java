package forge.game.combat;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

public class PlayerMustAttackTest extends AITest {

    @Test
    public void mustAttackThePlaneswalkerWhenAttackingThePlayerHasACost() {
        Game game = initAndCreateGame();
        Player attacker = game.getPlayers().get(1);
        Player defender = game.getPlayers().get(0);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, attacker);
        addCard("Trove of Temptation", defender);
        addCard("Propaganda", defender);
        Card jace = addCard("Jace Beleren", defender);
        jace.setCounters(CounterEnumType.LOYALTY, 3);
        Card bears = addCard("Grizzly Bears", attacker);
        bears.setSickness(false);
        game.getAction().checkStateEffects(true);

        Combat combat = new Combat(attacker);
        game.getPhaseHandler().setCombat(combat);
        AssertJUnit.assertFalse("attacking the planeswalker is free, so not attacking is illegal", CombatUtil.validateAttackers(combat));

        combat.addAttacker(bears, jace);
        AssertJUnit.assertTrue(CombatUtil.validateAttackers(combat));
    }

    @Test
    public void neverRequiredToPayForAnAttack() {
        Game game = initAndCreateGame();
        Player attacker = game.getPlayers().get(1);
        Player defender = game.getPlayers().get(0);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, attacker);
        addCard("Trove of Temptation", defender);
        addCard("Propaganda", defender);
        Card bears = addCard("Grizzly Bears", attacker);
        bears.setSickness(false);
        game.getAction().checkStateEffects(true);

        Combat combat = new Combat(attacker);
        game.getPhaseHandler().setCombat(combat);
        AssertJUnit.assertTrue(CombatUtil.validateAttackers(combat));
    }
}
