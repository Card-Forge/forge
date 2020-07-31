package forge.ai.ability;

import forge.ai.*;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.card.CounterType;
import forge.game.cost.Cost;
import forge.game.cost.CostPart;
import forge.game.cost.CostRemoveCounter;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;

import java.util.List;

public class TapAi extends TapAiBase {
    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {

        final PhaseHandler phase = ai.getGame().getPhaseHandler();
        final Player turn = phase.getPlayerTurn();

        if (turn.isOpponentOf(ai) && phase.getPhase().isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS)) {
            // Tap things down if it's Human's turn
        } else if (turn.equals(ai)) {
            if (SpellAbilityAi.isSorcerySpeed(sa) && phase.getPhase().isBefore(PhaseType.COMBAT_BEGIN)) {
                // Cast it if it's a sorcery.
            } else if (phase.getPhase().isBefore(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
                // Aggro Brains are willing to use TapEffects aggressively instead of defensively
                AiController aic = ((PlayerControllerAi) ai.getController()).getAi();
                if (!aic.getBooleanProperty(AiProps.PLAY_AGGRO)) {
                    return false;
                }
            } else {
                // Don't tap down after blockers
                return false;
            }
        } else if (!SpellAbilityAi.playReusable(ai, sa)){
            // Generally don't want to tap things with an Instant during Players turn outside of combat
            return false;
        }

        // prevent run-away activations - first time will always return true
        if (ComputerUtil.preventRunAwayActivations(sa)) {
            return false;
        }

        final TargetRestrictions tgt = sa.getTargetRestrictions();
        final Card source = sa.getHostCard();
        final Cost abCost = sa.getPayCosts();
        if (abCost != null) {
            if (!ComputerUtilCost.checkDiscardCost(ai, abCost, source)) {
                return false;
            }
        }

        if (tgt == null) {
            final List<Card> defined = AbilityUtils.getDefinedCards(source, sa.getParam("Defined"), sa);

            boolean bFlag = false;
            for (final Card c : defined) {
                bFlag |= c.isUntapped();
            }

            return bFlag;
        } else {
            if ("TapForXCounters".equals(sa.getParam("AILogic"))) {
                // e.g. Waxmane Baku
                CounterType ctrType = CounterType.get(CounterEnumType.KI);
                for (CostPart part : sa.getPayCosts().getCostParts()) {
                    if (part instanceof CostRemoveCounter) {
                        ctrType = ((CostRemoveCounter)part).counter;
                        break;
                    }
                }

                int numTargetable = Math.min(sa.getHostCard().getCounters(ctrType), ai.getOpponents().getCreaturesInPlay().size());
                sa.setSVar("ChosenX", String.valueOf(numTargetable));
            }

            sa.resetTargets();
            return tapPrefTargeting(ai, source, tgt, sa, false);
        }

    }

}
