package forge.ai.ability;

import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilCost;
import forge.card.mana.ManaCost;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardUtil;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** 
 * AbilityFactory for Creature Spells.
 *
 */
public class PermanentCreatureAi extends PermanentAi {

    /**
     * Checks if the AI will play a SpellAbility with the specified AiLogic
     */
    @Override
    protected boolean checkAiLogic(final Player ai, final SpellAbility sa, final String aiLogic) {
        final Game game = ai.getGame();

        if ("Never".equals(aiLogic)) {
            return false;
        } else if ("ZeroToughness".equals(aiLogic)) {
            // If Creature has Zero Toughness, make sure some static ability is in play
            // That will grant a toughness bonus

            final Card copy = CardUtil.getLKICopy(sa.getHostCard());

            ComputerUtilCard.applyStaticContPT(game, copy, null);

            if (copy.getNetToughness() <= 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if the AI will play a SpellAbility based on its phase restrictions
     */
    @Override
    protected boolean checkPhaseRestrictions(final Player ai, final SpellAbility sa, final PhaseHandler ph) {

        final Card card = sa.getHostCard();
        final Game game = ai.getGame();

        // FRF Dash Keyword
        if (sa.isDash()) {
            //only checks that the dashed creature will attack
            if (ph.isPlayerTurn(ai) && ph.getPhase().isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS)) {
                if (ai.hasKeyword("Skip your next combat phase."))
                    return false;
                if (ComputerUtilCost.canPayCost(sa.getHostCard().getSpellPermanent(), ai)) {
                    //do not dash if creature can be played normally
                    return false;
                }
                Card dashed = CardUtil.getLKICopy(sa.getHostCard());
                dashed.setSickness(false);
                return ComputerUtilCard.doesSpecifiedCreatureAttackAI(ai, dashed);
            } else {
                return false;
            }
        }

        // Prevent the computer from summoning Ball Lightning type creatures
        // after attacking
        if (card.hasSVar("EndOfTurnLeavePlay")
                && (!ph.isPlayerTurn(ai) || ph.getPhase().isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS)
                || ai.hasKeyword("Skip your next combat phase."))) {
            // AiPlayDecision.AnotherTime
            return false;
        }

        // save cards with flash for surprise blocking
        if (card.hasKeyword("Flash")
                && (ai.isUnlimitedHandSize() || ai.getCardsIn(ZoneType.Hand).size() <= ai.getMaxHandSize()
                        || ph.getPhase().isBefore(PhaseType.END_OF_TURN))
                && ai.getManaPool().totalMana() <= 0
                && (ph.isPlayerTurn(ai) || ph.getPhase().isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS))
                && (!card.hasETBTrigger(true) || card.hasSVar("AmbushAI")) && game.getStack().isEmpty()
                && !ComputerUtil.castPermanentInMain1(ai, sa)) {
            // AiPlayDecision.AnotherTime;
            return false;
        }

        return super.checkPhaseRestrictions(ai, sa, ph);
    }

    @Override
    protected boolean checkApiLogic(Player ai, SpellAbility sa) {

        if (!super.checkApiLogic(ai, sa)) {
            return false;
        }

        final Card card = sa.getHostCard();
        final ManaCost mana = card.getManaCost();
        final Game game = ai.getGame();

        /*
         * Checks if the creature will have non-positive toughness after
         * applying static effects. Exceptions: 1. has "etbCounter" keyword (eg.
         * Endless One) 2. paid non-zero for X cost 3. has ETB trigger 4. has
         * ETB replacement 5. has NoZeroToughnessAI svar (eg. Veteran Warleader)
         * 
         * 1. and 2. should probably be merged and applied on the card after
         * checking for effects like Doubling Season for getNetToughness to see
         * the true value. 3. currently allows the AI to suicide creatures as
         * long as it has an ETB. Maybe it should check if said ETB is actually
         * worth it. Not sure what 4. is for. 5. needs to be updated to ensure
         * that the net toughness is still positive after static effects.
         */
        final Card copy = CardUtil.getLKICopy(sa.getHostCard());
        ComputerUtilCard.applyStaticContPT(game, copy, null);
        if (copy.getNetToughness() <= 0 && !copy.hasStartOfKeyword("etbCounter") && mana.countX() == 0
                && !copy.hasETBTrigger(false) && !copy.hasETBReplacement() && !copy.hasSVar("NoZeroToughnessAI")) {
            // AiPlayDecision.WouldBecomeZeroToughnessCreature
            return false;
        }

        return true;
    }

}
