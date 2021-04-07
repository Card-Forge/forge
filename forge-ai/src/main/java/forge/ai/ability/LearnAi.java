package forge.ai.ability;


import forge.ai.ComputerUtilCard;
import forge.ai.PlayerControllerAi;
import forge.ai.SpellAbilityAi;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class LearnAi extends SpellAbilityAi {
    @Override
    protected boolean canPlayAI(Player aiPlayer, SpellAbility sa) {
        // For the time being, Learn is treated as universally positive due to being optional

        return true;
    }

    @Override
    protected boolean doTriggerAINoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        return mandatory || canPlayAI(aiPlayer, sa);
    }

    @Override
    public boolean chkAIDrawback(SpellAbility sa, Player aiPlayer) {
        return canPlayAI(aiPlayer, sa);
    }

    @Override
    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message) {
        return true;
    }

    public static Card chooseCardToLearn(CardCollection options, Player ai, SpellAbility sa) {
        CardCollection sideboard = CardLists.filter(options, CardPredicates.inZone(ZoneType.Sideboard));
        CardCollection hand = CardLists.filter(options, CardPredicates.inZone(ZoneType.Hand));
        hand.remove(sa.getHostCard()); // this card will be used in the process, don't consider it for discard

        CardCollection lessons = CardLists.filter(sideboard, CardPredicates.isType("Lesson"));
        CardCollection goodDiscards = ((PlayerControllerAi)ai.getController()).getAi().getCardsToDiscard(1, 1, hand, sa);

        if (!lessons.isEmpty()) {
            return ComputerUtilCard.getBestAI(lessons);
        } else if (!goodDiscards.isEmpty()) {
            return ComputerUtilCard.getWorstAI(goodDiscards);
        }

        // Don't choose anything if there's no good option
        return null;
    }
}
