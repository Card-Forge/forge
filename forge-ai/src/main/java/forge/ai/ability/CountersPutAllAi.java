package forge.ai.ability;

import com.google.common.base.Predicate;
import forge.ai.ComputerUtilCost;
import forge.ai.ComputerUtilMana;
import forge.ai.SpellAbilityAi;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.cost.Cost;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;

import java.util.List;
import java.util.Random;

public class CountersPutAllAi extends SpellAbilityAi {
    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {
        // AI needs to be expanded, since this function can be pretty complex
        // based on what
        // the expected targets could be
        final Random r = MyRandom.getRandom();
        final Cost abCost = sa.getPayCosts();
        final Card source = sa.getHostCard();
        List<Card> hList;
        List<Card> cList;
        final String type = sa.getParam("CounterType");
        final String amountStr = sa.getParam("CounterNum");
        final String valid = sa.getParam("ValidCards");
        final boolean curse = sa.isCurse();
        final TargetRestrictions tgt = sa.getTargetRestrictions();

        hList = CardLists.getValidCards(ai.getOpponent().getCardsIn(ZoneType.Battlefield), valid, source.getController(), source);
        cList = CardLists.getValidCards(ai.getCardsIn(ZoneType.Battlefield), valid, source.getController(), source);

        if (abCost != null) {
            // AI currently disabled for these costs
            if (!ComputerUtilCost.checkLifeCost(ai, abCost, source, 8, null)) {
                return false;
            }

            if (!ComputerUtilCost.checkDiscardCost(ai, abCost, source)) {
                return false;
            }

            if (!ComputerUtilCost.checkSacrificeCost(ai, abCost, source)) {
                return false;
            }
        }

        if (tgt != null) {
            Player pl = curse ? ai.getOpponent() : ai;
            sa.getTargets().add(pl);

            hList = CardLists.filterControlledBy(hList, pl);
            cList = CardLists.filterControlledBy(cList, pl);
        }

        // TODO improve X value to don't overpay when extra mana won't do
        // anything more useful
        final int amount;
        if (amountStr.equals("X") && source.getSVar(amountStr).equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            amount = ComputerUtilMana.determineLeftoverMana(sa, ai);
            source.setSVar("PayX", Integer.toString(amount));
        } else {
            amount = AbilityUtils.calculateAmount(sa.getHostCard(), amountStr, sa);
        }

        // prevent run-away activations - first time will always return true
        boolean chance = r.nextFloat() <= Math.pow(.6667, sa.getActivationsThisTurn());

        if (curse) {
            if (type.equals("M1M1")) {
                final List<Card> killable = CardLists.filter(hList, new Predicate<Card>() {
                    @Override
                    public boolean apply(final Card c) {
                        return c.getNetToughness() <= amount;
                    }
                });
                if (!(killable.size() > 2)) {
                    return false;
                }
            } else {
                // make sure compy doesn't harm his stuff more than human's
                // stuff
                if (cList.size() > hList.size()) {
                    return false;
                }
            }
        } else {
            // human has more things that will benefit, don't play
            if (hList.size() >= cList.size()) {
                return false;
            }

            //Check for cards that could profit from the ability
            PhaseHandler phase = ai.getGame().getPhaseHandler();
            if (type.equals("P1P1") && sa.isAbility() && source.isCreature()
                    && sa.getPayCosts() != null && sa.getPayCosts().hasTapCost()
                    && sa instanceof AbilitySub
                    && (!phase.getNextTurn().equals(ai)
                    || phase.getPhase().isBefore(PhaseType.COMBAT_DECLARE_BLOCKERS))) {
                boolean combatants = false;
                for (Card c : hList) {
                    if (!c.equals(source) && c.isUntapped()) {
                        combatants = true;
                        break;
                    }
                }
                if (!combatants) {
                    return false;
                }
            }
        }

        if (SpellAbilityAi.playReusable(ai, sa)) {
            return chance;
        }

        return ((r.nextFloat() < .6667) && chance);
    }

    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player ai) {
        return canPlayAI(ai, sa);
    }
    /* (non-Javadoc)
     * @see forge.card.ability.SpellAbilityAi#confirmAction(forge.game.player.Player, forge.card.spellability.SpellAbility, forge.game.player.PlayerActionConfirmMode, java.lang.String)
     */
    @Override
    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message) {
        return player.getCreaturesInPlay().size() >= player.getOpponent().getCreaturesInPlay().size();
    }
}
