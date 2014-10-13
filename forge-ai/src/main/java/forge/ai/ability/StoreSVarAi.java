package forge.ai.ability;

import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilCombat;
import forge.ai.ComputerUtilMana;
import forge.ai.SpellAbilityAi;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates.Presets;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

import java.util.ArrayList;
import java.util.List;

public class StoreSVarAi extends SpellAbilityAi {

    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        //Tree of Redemption

        final Card source = sa.getHostCard();
        final Game game = ai.getGame();
        final Combat combat = game.getCombat();
        final PhaseHandler ph = game.getPhaseHandler();
        final Player opp = ai.getOpponents().get(0);

        if (sa.hasParam("AILogic")) {
            if (sa.getPayCosts().getTotalMana().countX() > 0 && source.getSVar("X").equals("Count$xPaid")) {
                // Set PayX here to half the remaining mana to allow for Main 2 and other combat shenanigans.
                final int xPay = ComputerUtilMana.determineLeftoverMana(sa, ai) / 2;
                if (xPay == 0) { return false; }
                source.setSVar("PayX", Integer.toString(xPay));
            }

            final String logic = sa.getParam("AILogic");
            if (logic.equals("RestrictBlocking")) {
                if (!ph.isPlayerTurn(ai) || ph.getPhase().isBefore(PhaseType.COMBAT_BEGIN)
                        || ph.getPhase().isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS)) {
                    return false;
                }

                List<Card> possibleAttackers = ai.getCreaturesInPlay();
                List<Card> possibleBlockers = opp.getCreaturesInPlay();
                possibleBlockers = CardLists.filter(possibleBlockers, Presets.UNTAPPED);
                int oppLife = opp.getLife();
                int potentialDmg = 0;
                List<Card> currentAttackers = new ArrayList<Card>();

                if (possibleBlockers.size() == 0) { return false; }

                for (final Card creat : possibleAttackers) {
                    if (CombatUtil.canAttack(creat, opp) && possibleBlockers.size() > 1) {
                        potentialDmg += creat.getCurrentPower();
                        if (potentialDmg >= oppLife) { return true; }
                    }
                    if (combat != null && combat.isAttacking(creat)) {
                        currentAttackers.add(creat);
                    }
                }

                return currentAttackers.size() > possibleBlockers.size();
            }
            return false;
        }
        else if (ComputerUtil.waitForBlocking(sa) || ai.getLife() + 1 >= source.getNetToughness()
                || (ai.getLife() > 5 && !ComputerUtilCombat.lifeInSeriousDanger(ai, ai.getGame().getCombat()))) {
            return false;
        }

        return true;
    }

    @Override
    protected boolean doTriggerAINoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {

        return true;
    }

}
