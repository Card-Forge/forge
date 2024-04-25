package forge.ai.ability;


import forge.ai.AiController;
import forge.ai.AiProps;
import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilMana;
import forge.ai.PlayerControllerAi;
import forge.ai.SpellAbilityAi;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.Map;

public class ExploreAi extends SpellAbilityAi {
    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected boolean canPlayAI(Player aiPlayer, SpellAbility sa) {
        // Explore with a target (e.g. Enter the Unknown)
        if (sa.usesTargeting()) {
            Card bestCreature = ComputerUtilCard.getBestCreatureAI(aiPlayer.getCardsIn(ZoneType.Battlefield));
            if (bestCreature == null) {
                return false;
            }

            sa.resetTargets();
            sa.getTargets().add(bestCreature);
        }

        return true;
    }

    public static boolean shouldPutInGraveyard(Card topCard, Player ai) {
        int predictedMana = ComputerUtilMana.getAvailableManaSources(ai, false).size();
        CardCollectionView cardsOTB = ai.getCardsIn(ZoneType.Battlefield);
        CardCollectionView cardsInHand = ai.getCardsIn(ZoneType.Hand);
        CardCollection landsOTB = CardLists.filter(cardsOTB, CardPredicates.Presets.LANDS_PRODUCING_MANA);
        CardCollection landsInHand = CardLists.filter(cardsInHand, CardPredicates.Presets.LANDS_PRODUCING_MANA);

        int maxCMCDiff = 1;
        int numLandsToStillNeedMore = 2;

        if (ai.getController().isAI()) {
            AiController aic = ((PlayerControllerAi)ai.getController()).getAi();
            maxCMCDiff = aic.getIntProperty(AiProps.EXPLORE_MAX_CMC_DIFF_TO_PUT_IN_GRAVEYARD);
            numLandsToStillNeedMore = aic.getIntProperty(AiProps.EXPLORE_NUM_LANDS_TO_STILL_NEED_MORE);
        }

        if (landsInHand.isEmpty() && landsOTB.size() <= numLandsToStillNeedMore) {
            // We need more lands to improve our mana base, explore away the non-lands
            return true;
        } else if (topCard.getCMC() - maxCMCDiff >= predictedMana && !topCard.hasSVar("DoNotDiscardIfAble")) {
            // We're not casting this in foreseeable future, put it in the graveyard
            return true;
        }

        // Put on top of the library (do not mark the card for placement in the graveyard)
        return false;
    }

    @Override
    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message, Map<String, Object> params) {
        return shouldPutInGraveyard((Card)params.get("RevealedCard"), player);
    }

    @Override
    protected boolean doTriggerAINoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        if (sa.usesTargeting()) {
            CardCollection list = CardLists.getValidCards(aiPlayer.getGame().getCardsIn(ZoneType.Battlefield),
                    sa.getTargetRestrictions().getValidTgts(), aiPlayer, sa.getHostCard(), sa);

            if (!list.isEmpty()) {
                sa.getTargets().add(ComputerUtilCard.getBestCreatureAI(list));
                return true;
            }

            return false;
        }

        return canPlayAI(aiPlayer, sa) || mandatory;
    }
}
