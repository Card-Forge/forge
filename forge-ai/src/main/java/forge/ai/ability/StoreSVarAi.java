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
        //

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
        else if ("Tree of Redemption".equals(source.getName())) {
            if (!ai.canGainLife())
                return false;

            // someone controls "Rain of Gore" or "Sulfuric Vortex", lifegain is bad in that case
            if (game.isCardInPlay("Rain of Gore") || game.isCardInPlay("Sulfuric Vortex"))
                return false;

            // an opponent controls "Tainted Remedy", lifegain is bad in that case
            for (Player op : ai.getOpponents()) {
                if (op.isCardInPlay("Tainted Remedy"))
                    return false;
            }

            if (ComputerUtil.waitForBlocking(sa) || ai.getLife() + 1 >= source.getNetToughness()
                || (ai.getLife() > 5 && !ComputerUtilCombat.lifeInSeriousDanger(ai, ai.getGame().getCombat()))) {
                return false;
            }
        }
        else if ("Tree of Perdition".equals(source.getName())) {
            boolean shouldDo = false;

            if (ComputerUtil.waitForBlocking(sa))
                return false;

            for (Player op : ai.getOpponents()) {
                // if oppoent can't be targeted, or it can't lose life, try another one
                if (!op.canBeTargetedBy(sa) || !op.canLoseLife())
                    continue;
                // an opponent has more live than this toughness
                if (op.getLife() + 1 >= source.getNetToughness()) {
                    shouldDo = true;
                } else {
                    // opponent can't gain life, so "Tainted Remedy" should not work.
                    if (!op.canGainLife()) {
                        continue;
                    } else if (ai.isCardInPlay("Tainted Remedy")) { // or AI has Tainted Remedy 
                        shouldDo = true;
                    } else {
                        for (Player ally : ai.getAllies()) {
                            // if an Ally has Tainted Remedy and opponent is also opponent of ally
                            if (ally.isCardInPlay("Tainted Remedy") && op.isOpponentOf(ally))
                                shouldDo = true;
                        }
                    }

                }

                if (shouldDo)
                    break;
            }

            return shouldDo;
        }

        return true;
    }

    @Override
    protected boolean doTriggerAINoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {

        return true;
    }

}
