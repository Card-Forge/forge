package forge.game.combat;

import org.testng.annotations.Test;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCopyService;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

/**
 * Sorrow's Path and General Jarkeld swap blockers between attackers. The attacker is player 1, the
 * defender (who blocks) is player 0.
 */
public class SwitchBlockEffectTest extends AITest {
    private Game game;
    private Player attackerPlayer;
    private Player defenderPlayer;
    private Combat combat;

    private void startCombat() {
        game = initAndCreateGame();
        attackerPlayer = game.getPlayers().get(1);
        defenderPlayer = game.getPlayers().get(0);
        combat = new Combat(attackerPlayer);
        game.getPhaseHandler().setCombat(combat);
    }

    private Card attacker(String name) {
        Card c = addCard(name, attackerPlayer);
        c.setSickness(false);
        combat.addAttacker(c, defenderPlayer);
        return c;
    }

    /** Declares a block the way PhaseHandler does, history included. */
    private Card blocker(String name, Card... attackers) {
        Card b = addCard(name, defenderPlayer);
        for (Card a : attackers) {
            combat.addBlocker(a, b);
            b.addBlockedThisTurn(CardCopyService.getLKICopy(a));
            a.addBlockedByThisTurn(CardCopyService.getLKICopy(b));
        }
        return b;
    }

    private void activate(String cardName, Player activator, Card first, Card second) {
        Card source = addCard(cardName, activator);
        SpellAbility sa = null;
        for (SpellAbility candidate : source.getAllSpellAbilities()) {
            if (candidate.getApi() == ApiType.SwitchBlock) {
                sa = candidate;
            }
        }
        sa.setActivatingPlayer(activator);
        sa.resetTargets();
        sa.getTargets().add(first);
        sa.getTargets().add(second);
        AbilityUtils.resolve(sa);
    }

    /**
     * Dwarven Soldier has a Blocks trigger naming Orcs. Moved onto an Orc by Sorrow's Path it blocks one
     * again, so the trigger has to fire (ruling on Sorrow's Path, 2009).
     */
    @Test
    public void sorrowsPathReblockFiresBlocksTriggerNamingTheAttacker() {
        startCombat();
        Card orc1 = attacker("Ironclaw Orcs");
        Card orc2 = attacker("Ironclaw Orcs");
        Card soldier = blocker("Dwarven Soldier", orc1);
        Card bears = blocker("Grizzly Bears", orc2);
        game.getAction().checkStateEffects(true);
        assertTrue("nothing has triggered yet", game.getStack().isEmpty());

        activate("Sorrow's Path", attackerPlayer, soldier, bears);

        assertTrue("the soldier now blocks the second Orc", combat.isBlocking(soldier, orc2));
        game.getAction().checkStateEffects(true);
        game.getStack().addAllTriggeredAbilitiesToStack();
        assertFalse("blocking an Orc again triggers Dwarven Soldier", game.getStack().isEmpty());
    }

    /**
     * Sorrow's Path finds the attackers it moves through the blocked-by record of the targeted blockers. A
     * blocker that was switched onto another attacker must be on that record, or a second activation
     * misses the attacker it now blocks.
     */
    @Test
    public void sorrowsPathSecondSwitchSeesTheBlockTheFirstOneMade() {
        startCombat();
        Card attacker1 = attacker("Grizzly Bears");
        Card attacker2 = attacker("Runeclaw Bear");
        Card attacker3 = attacker("Hill Giant");
        Card blocker1 = blocker("Goblin Chariot", attacker1);
        Card blocker2 = blocker("Savannah Lions", attacker2);
        Card blocker3 = blocker("Pearled Unicorn", attacker3);

        activate("Sorrow's Path", attackerPlayer, blocker1, blocker2);
        assertTrue("first switch: blocker1 now blocks attacker2", combat.isBlocking(blocker1, attacker2));
        assertTrue("first switch: blocker2 now blocks attacker1", combat.isBlocking(blocker2, attacker1));

        activate("Sorrow's Path", attackerPlayer, blocker1, blocker3);

        assertTrue("blocker1 takes over attacker3", combat.isBlocking(blocker1, attacker3));
        assertTrue("blocker3 takes over the attacker blocker1 was blocking", combat.isBlocking(blocker3, attacker2));
        assertFalse(combat.isBlocking(blocker1, attacker2));
        assertFalse(combat.isBlocking(blocker3, attacker3));
    }
}
