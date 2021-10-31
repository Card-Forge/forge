package forge.ai.ability;

import forge.ai.AiAttackController;
import forge.ai.SpellAbilityAi;
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
            int ownCreatureCount = aiPlayer.getCreaturesInPlay().size();
            int oppMaxCreatureCount = 0;
            for (Player opp : aiPlayer.getOpponents()) {
                oppMaxCreatureCount = Math.max(oppMaxCreatureCount, opp.getCreaturesInPlay().size());
            }

            // TODO: maybe check if the AI is actually pressured and/or check the total value of the creatures on both sides of the board
            return ownCreatureCount > oppMaxCreatureCount + 2 || ownCreatureCount < oppMaxCreatureCount;
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
