package forge.card.abilityfactory.ai;

import java.util.List;
import java.util.Random;

import forge.Card;
import forge.Singletons;
import forge.card.abilityfactory.AbilityUtils;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.AIPlayer;
import forge.game.player.Player;
import forge.util.MyRandom;

public class TapAi extends TapAiBase {
    @Override
    protected boolean canPlayAI(AIPlayer ai, SpellAbility sa) {
        final Target tgt = sa.getTarget();
        final Card source = sa.getSourceCard();

        final Random r = MyRandom.getRandom();
        boolean randomReturn = r.nextFloat() <= Math.pow(.6667, sa.getActivationsThisTurn());

        final PhaseHandler phase = Singletons.getModel().getGame().getPhaseHandler();
        final Player turn = phase.getPlayerTurn();

        if (turn.isOpponentOf(ai) && phase.getPhase().isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS)) {
            // Tap things down if it's Human's turn
        } else if (turn == ai && phase.getPhase().isBefore(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
            // Tap creatures down if in combat -- handled in tapPrefTargeting().
        } else if (AbilityUtils.isSorcerySpeed(sa)) {
            // Cast it if it's a sorcery.
        } else {
            // Generally don't want to tap things with an Instant during AI turn outside of combat
            return false;
        }

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
            tgt.resetTargets();
            if (!tapPrefTargeting(ai, source, tgt, sa, false)) {
                return false;
            }
        }

        return randomReturn;
    }

    @Override
    protected boolean doTriggerAINoCost(AIPlayer ai, SpellAbility sa, boolean mandatory) {

        final Target tgt = sa.getTarget();
        final Card source = sa.getSourceCard();

        if (tgt == null) {
            if (mandatory) {
                return true;
            }

            // TODO: use Defined to determine, if this is an unfavorable result

            return true;
        } else {
            if (tapPrefTargeting(ai, source, tgt, sa, mandatory)) {
                return true;
            } else if (mandatory) {
                // not enough preferred targets, but mandatory so keep going:
                return tapUnpreferredTargeting(ai, sa, mandatory);
            }
        }

        return false;
    }

    @Override
    public boolean chkAIDrawback(SpellAbility sa, AIPlayer ai) {
        final Target tgt = sa.getTarget();
        final Card source = sa.getSourceCard();

        boolean randomReturn = true;

        if (tgt == null) {
            // either self or defined, either way should be fine
        } else {
            // target section, maybe pull this out?
            tgt.resetTargets();
            if (!tapPrefTargeting(ai, source, tgt, sa, false)) {
                return false;
            }
        }

        return randomReturn;
    }

}
