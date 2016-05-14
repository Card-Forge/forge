package forge.ai.ability;

import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilMana;
import forge.ai.SpellAbilityAi;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.ZoneType;

public class MillAi extends SpellAbilityAi {

    @Override
    protected boolean checkAiLogic(final Player ai, final SpellAbility sa, final String aiLogic) {
        if (aiLogic.equals("Main1")) {
            if (ai.getGame().getPhaseHandler().getPhase().isBefore(PhaseType.MAIN2) && !sa.hasParam("ActivationPhases")
                    && !ComputerUtil.castSpellInMain1(ai, sa)) {
                return false;
            }
        }
        return true;
    }
    
    @Override
    protected boolean checkPhaseRestrictions(final Player ai, final SpellAbility sa, final PhaseHandler ph) {
        if ("You".equals(sa.getParam("Defined")) && !SpellAbilityAi.isSorcerySpeed(sa)
                && !ph.is(PhaseType.END_OF_TURN, ai.getOpponent())) {
            return false;   // only self-mill at opponent EOT
        }
        return true;
    }
    
    @Override
    protected boolean checkApiLogic(final Player ai, final SpellAbility sa) {
        /*
         * TODO:
         * - logic in targetAI looks dodgy
         * - decide whether to self-mill (eg. delirium, dredge, bad top card)
         * - interrupt opponent's top card (eg. Brainstorm, good top card)
         * - check for Laboratory Maniac effect (needs to check for actual
         * effect due to possibility of "lose abilities" effect)
         */
        final Card source = sa.getHostCard();
        if (ComputerUtil.preventRunAwayActivations(sa)) {
            return false;   // prevents mill 0 infinite loop?
        }
        
        if (("You".equals(sa.getParam("Defined")) || "Player".equals(sa.getParam("Defined")))
                && ai.getCardsIn(ZoneType.Library).size() < 10) {
            return false;   // prevent self and each player mill when library is small
        }
        
        if (sa.getTargetRestrictions() != null && !targetAI(ai, sa, false)) {
            return false;
        }

        if ((sa.getParam("NumCards").equals("X") || sa.getParam("NumCards").equals("Z"))
                && source.getSVar("X").startsWith("Count$xPaid")) {
            // Set PayX here to maximum value.
            final int cardsToDiscard = Math.min(ComputerUtilMana.determineLeftoverMana(sa, ai),
                    ai.getOpponent().getCardsIn(ZoneType.Library).size());
            source.setSVar("PayX", Integer.toString(cardsToDiscard));
            if (cardsToDiscard <= 0) {
                return false;
            }
        }
        return true;
    }

    private boolean targetAI(final Player ai, final SpellAbility sa, final boolean mandatory) {
        final TargetRestrictions tgt = sa.getTargetRestrictions();
        Player opp = ai.getOpponent();

        if (tgt != null) {
            sa.resetTargets();
            if (!sa.canTarget(opp)) {
                if (mandatory && sa.canTarget(ai)) {
                    sa.getTargets().add(ai);
                    return true;
                }
                return false;
            }

            final int numCards = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("NumCards"), sa);

            final CardCollectionView pLibrary = opp.getCardsIn(ZoneType.Library);

            if (pLibrary.isEmpty()) { // deck already empty, no need to mill
                if (!mandatory) {
                    return false;
                }

                sa.getTargets().add(opp);
                return true;
            }

            if (numCards >= pLibrary.size()) {
                // Can Mill out Human's deck? Do it!
                sa.getTargets().add(opp);
                return true;
            }

            // Obscure case when you know what your top card is so you might?
            // want to mill yourself here
            // if (AI wants to mill self)
            // sa.getTargets().add(AllZone.getComputerPlayer());
            // else
            sa.getTargets().add(opp);
        }
        return true;
    }

    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player aiPlayer) {
        return targetAI(aiPlayer, sa, true);
    }


    @Override
    protected boolean doTriggerAINoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        if (!targetAI(aiPlayer, sa, mandatory)) {
            return false;
        }

        final Card source = sa.getHostCard();
        if (sa.getParam("NumCards").equals("X") && source.getSVar("X").equals("Count$xPaid")) {
            // Set PayX here to maximum value.
            final int cardsToDiscard = Math.min(ComputerUtilMana.determineLeftoverMana(sa, aiPlayer), aiPlayer.getOpponent()
                    .getCardsIn(ZoneType.Library).size());
            source.setSVar("PayX", Integer.toString(cardsToDiscard));
        }

        return true;
    }
    /* (non-Javadoc)
     * @see forge.card.ability.SpellAbilityAi#confirmAction(forge.game.player.Player, forge.card.spellability.SpellAbility, forge.game.player.PlayerActionConfirmMode, java.lang.String)
     */
    @Override
    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message) {
        return true;
    }
}
