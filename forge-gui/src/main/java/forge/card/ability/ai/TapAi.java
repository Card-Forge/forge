package forge.card.ability.ai;

import java.util.List;

import forge.Card;
import forge.card.ability.AbilityUtils;
import forge.card.ability.SpellAbilityAi;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.TargetRestrictions;
import forge.game.ai.ComputerUtil;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;

public class TapAi extends TapAiBase {
    @Override
    protected boolean canPlayAI(Player ai, SpellAbility sa) {

        final PhaseHandler phase = ai.getGame().getPhaseHandler();
        final Player turn = phase.getPlayerTurn();

        if (turn.isOpponentOf(ai) && phase.getPhase().isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS)) {
            // Tap things down if it's Human's turn
        } else if (turn == ai && phase.getPhase().isBefore(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
            // Tap creatures down if in combat -- handled in tapPrefTargeting().
        } else if (SpellAbilityAi.isSorcerySpeed(sa)) {
            // Cast it if it's a sorcery.
        } else if (!SpellAbilityAi.playReusable(ai, sa)){
            // Generally don't want to tap things with an Instant during AI turn outside of combat
            return false;
        }

        // prevent run-away activations - first time will always return true
        if (ComputerUtil.preventRunAwayActivations(sa)) {
            return false;
        }

        final TargetRestrictions tgt = sa.getTargetRestrictions();
        final Card source = sa.getSourceCard();

        if (tgt == null) {
            final List<Card> defined = AbilityUtils.getDefinedCards(source, sa.getParam("Defined"), sa);

            boolean bFlag = false;
            for (final Card c : defined) {
                bFlag |= c.isUntapped();
            }

            if (!bFlag) {
                return false;
            }
        } else {
            sa.resetTargets();
            if (!tapPrefTargeting(ai, source, tgt, sa, false)) {
                return false;
            }
        }

        return true;
    }

}
