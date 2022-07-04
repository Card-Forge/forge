package forge.ai.ability;


import java.util.Map;

import forge.ai.AiCardMemory;
import forge.ai.AiController;
import forge.ai.AiProps;
import forge.ai.ComputerUtilMana;
import forge.ai.PlayerControllerAi;
import forge.ai.SpellAbilityAi;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.player.PlayerCollection;
import forge.game.player.PlayerPredicates;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;

public class RearrangeTopOfLibraryAi extends SpellAbilityAi {
    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected boolean canPlayAI(Player aiPlayer, SpellAbility sa) {
        // Specific details of ordering cards are handled by PlayerControllerAi#orderMoveToZoneList
        final PhaseHandler ph = aiPlayer.getGame().getPhaseHandler();
        final Card source = sa.getHostCard();

        if (!sa.isTrigger()) {
            if (source.isPermanent() && !sa.getRestrictions().isSorcerySpeed()
                    && (sa.getPayCosts().hasTapCost() || sa.getPayCosts().hasManaCost())) {
                // If it has an associated cost, try to only do this before own turn
                if (!(ph.is(PhaseType.END_OF_TURN) && ph.getNextTurn() == aiPlayer)) {
                    return false;
                }
            }

            // Do it once per turn, generally (may be improved later)
            if (AiCardMemory.isRememberedCardByName(aiPlayer, source.getName(), AiCardMemory.MemorySet.ACTIVATED_THIS_TURN)) {
                return false;
            }
        }

        if (sa.usesTargeting()) {
            // ability is targeted
            sa.resetTargets();

            PlayerCollection targetableOpps = aiPlayer.getOpponents().filter(PlayerPredicates.isTargetableBy(sa));
            Player opp = targetableOpps.min(PlayerPredicates.compareByLife());
            final boolean canTgtAI = sa.canTarget(aiPlayer);
            final boolean canTgtHuman = sa.canTarget(opp);

            if (canTgtHuman && canTgtAI) {
                // TODO: maybe some other consideration rather than random?
                Player preferredTarget = MyRandom.percentTrue(50) ? aiPlayer : opp;
                sa.getTargets().add(preferredTarget);
            } else if (canTgtAI) {
                sa.getTargets().add(aiPlayer);
            } else if (canTgtHuman) {
                sa.getTargets().add(opp);
            } else {
                return false; // could not find a valid target
            }

            if (!canTgtHuman || !canTgtAI) {
                // can't target another player anyway, remember for no second activation this turn
                AiCardMemory.rememberCard(aiPlayer, source, AiCardMemory.MemorySet.ACTIVATED_THIS_TURN);
            }
        } else {
            // if it's just defined, no big deal
            AiCardMemory.rememberCard(aiPlayer, source, AiCardMemory.MemorySet.ACTIVATED_THIS_TURN);
        }

        return true;
    }

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#doTriggerAINoCost(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility, boolean)
     */
    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
        // Specific details of ordering cards are handled by PlayerControllerAi#orderMoveToZoneList
        return canPlayAI(ai, sa) || mandatory;
    }

    /* (non-Javadoc)
     * @see forge.card.ability.SpellAbilityAi#confirmAction(forge.game.player.Player, forge.card.spellability.SpellAbility, forge.game.player.PlayerActionConfirmMode, java.lang.String)
     */
    @Override
    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message, Map<String, Object> params) {
        // Confirming this action means shuffling the library if asked.

        // First, let's check if we can play the top card of the library
        PlayerCollection pc = sa.usesTargeting() ? new PlayerCollection(sa.getTargets().getTargetPlayers())
                : AbilityUtils.getDefinedPlayers(sa.getHostCard(), sa.getParam("Defined"), sa);

        Player p = pc.getFirst(); // currently always a single target spell
        Card top = p.getCardsIn(ZoneType.Library).isEmpty() ? null : p.getCardsIn(ZoneType.Library).getFirst();
        if (top == null) {
            return false;
        }

        int uncastableCMCThreshold = 2;
        int minLandsToScryLandsAway = 4;
        if (player.getController().isAI()) {
            AiController aic = ((PlayerControllerAi)player.getController()).getAi();
            minLandsToScryLandsAway = aic.getIntProperty(AiProps.SCRY_NUM_LANDS_TO_NOT_NEED_MORE);
            uncastableCMCThreshold = aic.getIntProperty(AiProps.SCRY_IMMEDIATELY_UNCASTABLE_CMC_DIFF);
        }

        int landsOTB = CardLists.count(p.getCardsIn(ZoneType.Battlefield), CardPredicates.Presets.LANDS_PRODUCING_MANA);
        int cmc = top.isSplitCard() ? Math.min(top.getCMC(Card.SplitCMCMode.LeftSplitCMC), top.getCMC(Card.SplitCMCMode.RightSplitCMC))
                : top.getCMC();
        int maxCastable = ComputerUtilMana.getAvailableManaEstimate(p, false);

        if (!top.isLand() && cmc - maxCastable >= uncastableCMCThreshold) {
            // Can't cast in the foreseeable future. Shuffle if doing it to ourselves or an ally, otherwise keep it
            return !p.isOpponentOf(player);
        } else if (top.isLand() && landsOTB <= minLandsToScryLandsAway) {
            // We don't want to give the opponent a free land if his land count is low
            return p.isOpponentOf(player);
        }

        // Usually we don't want to shuffle if we arranged things carefully
        return false;
    }
}
