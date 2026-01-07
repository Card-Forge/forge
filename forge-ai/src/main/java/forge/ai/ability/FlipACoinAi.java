package forge.ai.ability;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtil;
import forge.ai.SpellAbilityAi;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

public class FlipACoinAi extends SpellAbilityAi {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#checkApiLogic(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected AiAbilityDecision checkApiLogic(Player ai, SpellAbility sa) {
        if (sa.hasParam("AILogic")) {
            String ailogic = sa.getParam("AILogic");
            if (ailogic.equals("PhaseOut")) {
                if (!ComputerUtil.predictThreatenedObjects(sa.getActivatingPlayer(), sa).contains(sa.getHostCard())) {
                    return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                }
            } else if (ailogic.equals("Bangchuckers")) {
                if (ai.getGame().getPhaseHandler().getPhase().isBefore(PhaseType.END_OF_TURN) ) {
                    return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
                }
                sa.resetTargets();
                for (Player o : ai.getOpponents()) {
                    if (sa.canTarget(o) && o.canLoseLife() && !o.cantLoseForZeroOrLessLife()) {
                        sa.getTargets().add(o);
                        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                    }
                }
                for (Card c : ai.getOpponents().getCreaturesInPlay()) {
                    if (sa.canTarget(c)) {
                        sa.getTargets().add(c);
                        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                    }
                }
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            } else if (ailogic.equals("KillOrcs")) {
            	if (ai.getGame().getPhaseHandler().getPhase().isBefore(PhaseType.END_OF_TURN) ) {
                    return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            	}
            	sa.resetTargets();
                for (Card c : ai.getOpponents().getCreaturesInPlay()) {
                    if (sa.canTarget(c)) {
                        sa.getTargets().add(c);
                        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
                    }
            	}
            	return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
        }
        if (sa.isTargetNumberValid()) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        } else {
            return new AiAbilityDecision(0, AiPlayDecision.TargetingFailed);
        }
    }

    @Override
    public AiAbilityDecision chkDrawback(Player ai, SpellAbility sa) {
        return canPlay(ai, sa);
    }
}
