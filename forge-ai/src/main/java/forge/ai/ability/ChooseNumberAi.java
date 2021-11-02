package forge.ai.ability;

import forge.ai.AiAttackController;
import forge.ai.ComputerUtilCard;
import forge.ai.SpellAbilityAi;
import forge.game.ability.AbilityUtils;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.util.MyRandom;

public class ChooseNumberAi extends SpellAbilityAi {

    @Override
    protected boolean canPlayAI(Player aiPlayer, SpellAbility sa) {
        String aiLogic = sa.getParamOrDefault("AILogic", "");

        if (aiLogic.isEmpty()) {
            return false;
        } else if (aiLogic.equals("SweepCreatures")) {
            int maxChoiceLimit = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("Max"), sa);
            int ownCreatureCount = aiPlayer.getCreaturesInPlay().size();
            int oppMaxCreatureCount = 0;
            Player refOpp = null;
            for (Player opp : aiPlayer.getOpponents()) {
                int oppCreatureCount = Math.max(oppMaxCreatureCount, opp.getCreaturesInPlay().size());
                if (oppCreatureCount > oppMaxCreatureCount) {
                    oppMaxCreatureCount = oppCreatureCount;
                    refOpp = opp;
                }
            }

            if (refOpp == null) {
                return false; // no opponent has any creatures
            }

            int evalAI = ComputerUtilCard.evaluateCreatureList(aiPlayer.getCreaturesInPlay());
            int evalOpp = ComputerUtilCard.evaluateCreatureList(refOpp.getCreaturesInPlay());

            if (aiPlayer.getLifeLostLastTurn() + aiPlayer.getLifeLostThisTurn() == 0 && evalAI > evalOpp) {
                return false; // we're not pressured and our stuff seems better, don't do it yet
            }

            return ownCreatureCount > oppMaxCreatureCount + 2 || ownCreatureCount < Math.min(oppMaxCreatureCount, maxChoiceLimit);
        }

        TargetRestrictions tgt = sa.getTargetRestrictions();
        if (tgt != null) {
            sa.resetTargets();
            Player opp = AiAttackController.choosePreferredDefenderPlayer(aiPlayer);
            if (sa.canTarget(opp)) {
                sa.getTargets().add(opp);
            } else {
                return false;
            }
        }
        boolean chance = MyRandom.getRandom().nextFloat() <= Math.pow(.6667, sa.getActivationsThisTurn());
        return chance;
    }

    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
        return mandatory || canPlayAI(ai, sa);
    }

}
