package forge.ai.ability;


import forge.ai.*;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.Map;

public class LearnAi extends SpellAbilityAi {
    @Override
    protected AiAbilityDecision canPlay(Player aiPlayer, SpellAbility sa) {
        // For the time being, Learn is treated as universally positive due to being optional
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    @Override
    protected AiAbilityDecision doTriggerNoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        if (mandatory) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }
        return canPlay(aiPlayer, sa);
    }

    @Override
    public AiAbilityDecision chkDrawback(Player aiPlayer, SpellAbility sa) {
        return canPlay(aiPlayer, sa);
    }

    @Override
    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message, Map<String, Object> params) {
        return true;
    }

    public static Card chooseCardToLearn(CardCollection options, Player ai, SpellAbility sa) {
        CardCollection sideboard = CardLists.filter(options, CardPredicates.inZone(ZoneType.Sideboard));
        CardCollection hand = CardLists.filter(options, CardPredicates.inZone(ZoneType.Hand));
        hand.remove(sa.getHostCard()); // this card will be used in the process, don't consider it for discard

        CardCollection lessons = CardLists.getType(sideboard, "Lesson");
        CardCollection goodDiscards = ((PlayerControllerAi)ai.getController()).getAi().getCardsToDiscard(1, 1, hand, sa);

        if (!lessons.isEmpty()) {
            return ComputerUtilCard.getBestAI(lessons);
        } else if (goodDiscards != null && !goodDiscards.isEmpty()) {
            return ComputerUtilCard.getWorstAI(goodDiscards);
        }

        // Don't choose anything if there's no good option
        return null;
    }
}
