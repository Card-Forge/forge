package forge.ai.ability;

import forge.ai.*;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.cost.Cost;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

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
        } else if (!SpellAbilityAi.playReusable(ai, sa)) {
            // Generally don't want to tap things with an Instant during Players turn outside of combat
            return false;
        }

        // prevent run-away activations - first time will always return true
        if (ComputerUtil.preventRunAwayActivations(sa)) {
            return false;
        }

        final Card source = sa.getHostCard();
        final Cost abCost = sa.getPayCosts();

        if ("GoblinPolkaBand".equals(sa.getParam("AILogic"))) {
            return SpecialCardAi.GoblinPolkaBand.consider(ai, sa);
        }

        if (!ComputerUtilCost.checkDiscardCost(ai, abCost, source, sa)) {
            return false;
        }

        if (!sa.usesTargeting()) {
            boolean bFlag = false;
            for (final Card c : AbilityUtils.getDefinedCards(source, sa.getParam("Defined"), sa)) {
                bFlag |= c.isUntapped();
            }

            return bFlag;
        } else {
            // X controls the minimum targets
            if ("X".equals(sa.getTargetRestrictions().getMinTargets()) && sa.getSVar("X").equals("Count$xPaid")) {
                // Set PayX here to maximum value.
                // TODO need to set XManaCostPaid for targets, maybe doesn't need PayX anymore?
                sa.setXManaCostPaid(ComputerUtilCost.getMaxXValue(sa, ai));
            }

            sa.resetTargets();
            return tapPrefTargeting(ai, source, sa, false);
        }
    }

}
