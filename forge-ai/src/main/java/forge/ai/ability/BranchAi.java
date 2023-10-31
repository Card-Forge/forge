package forge.ai.ability;


import com.google.common.base.Predicate;
import forge.ai.*;
import forge.game.GameEntity;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.combat.Combat;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.SpellAbility;
import forge.util.Expressions;

import java.util.Map;

public class BranchAi extends SpellAbilityAi {
    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected boolean canPlayAI(Player aiPlayer, SpellAbility sa) {
        final String aiLogic = sa.getParamOrDefault("AILogic", "");
        if ("GrislySigil".equals(aiLogic)) {
            return SpecialCardAi.GrislySigil.consider(aiPlayer, sa);
        } else if ("BranchCounter".equals(aiLogic)) {
            // TODO: this might need expanding/tweaking if more cards are added with different SA setups
            SpellAbility top = ComputerUtilAbility.getTopSpellAbilityOnStack(aiPlayer.getGame(), sa);
            if (top == null || !sa.canTarget(top)) {
                return false;
            }
            Card host = sa.getHostCard();

            // pre-target the object to calculate the branch condition SVar, then clean up before running the real check
            sa.getTargets().add(top);
            int value = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("BranchConditionSVar"), sa);
            sa.resetTargets();

            String branchCompare = sa.getParamOrDefault("BranchConditionSVarCompare", "GE1");
            String operator = branchCompare.substring(0, 2);
            String operand = branchCompare.substring(2);
            final int operandValue = AbilityUtils.calculateAmount(host, operand, sa);
            boolean conditionMet = Expressions.compare(value, operator, operandValue);

            SpellAbility falseSub = sa.getAdditionalAbility("FalseSubAbility"); // this ability has the UnlessCost part
            boolean willPlay = false;
            if (!conditionMet && falseSub.hasParam("UnlessCost")) {
                // FIXME: We're emulating the UnlessCost on the SA to run the proper checks.
                // This is hacky, but it works. Perhaps a cleaner way exists?
                sa.getMapParams().put("UnlessCost", falseSub.getParam("UnlessCost"));
                willPlay = SpellApiToAi.Converter.get(ApiType.Counter).canPlayAIWithSubs(aiPlayer, sa);
                sa.getMapParams().remove("UnlessCost");
            } else {
                willPlay = SpellApiToAi.Converter.get(ApiType.Counter).canPlayAIWithSubs(aiPlayer, sa);
            }
            return willPlay;
        } else if ("TgtAttacker".equals(aiLogic)) {
            final Combat combat = aiPlayer.getGame().getCombat();
            if (combat == null || combat.getAttackingPlayer() != aiPlayer) {
                return false;
            }

            final CardCollection attackers = combat.getAttackers();
            final CardCollection attackingBattle = CardLists.filter(attackers, new Predicate<Card>() {
                @Override
                public boolean apply(Card card) {
                    final GameEntity def = combat.getDefenderByAttacker(combat.getBandOfAttacker(card));
                    return def instanceof Card && ((Card)def).isBattle();
                }
            });

            if (!attackingBattle.isEmpty()) {
                sa.getTargets().add(ComputerUtilCard.getBestCreatureAI(attackingBattle));
            } else {
                sa.getTargets().add(ComputerUtilCard.getBestCreatureAI(attackers));
            }

            return sa.isTargetNumberValid();
        }

        // TODO: expand for other cases where the AI is needed to make a decision on a branch
        return true;
    }

    @Override
    protected boolean doTriggerAINoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        return canPlayAI(aiPlayer, sa) || mandatory;
    }

    @Override
    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message, Map<String, Object> params) {
        return true;
    }
}
