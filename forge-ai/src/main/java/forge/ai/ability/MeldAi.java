package forge.ai.ability;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.SpellAbilityAi;
import forge.game.card.CardCollectionView;
import forge.game.card.CardPredicates;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class MeldAi extends SpellAbilityAi {
    @Override
    protected AiAbilityDecision checkApiLogic(Player aiPlayer, SpellAbility sa) {
        String primaryMeld = sa.getParam("Primary");
        String secondaryMeld = sa.getParam("Secondary");
        
        CardCollectionView cardsOTB = aiPlayer.getCardsIn(ZoneType.Battlefield);
        if (cardsOTB.isEmpty()) {
            return new AiAbilityDecision(0, AiPlayDecision.MissingNeededCards);
        }

        boolean hasPrimaryMeld = cardsOTB.anyMatch(CardPredicates.nameEquals(primaryMeld).and(CardPredicates.isOwner(aiPlayer)));
        boolean hasSecondaryMeld = cardsOTB.anyMatch(CardPredicates.nameEquals(secondaryMeld).and(CardPredicates.isOwner(aiPlayer)));
        if (hasPrimaryMeld && hasSecondaryMeld && sa.getHostCard().getName().equals(primaryMeld)) {
            // If the primary meld card is on the battlefield and both meld cards are owned by the AI, play the ability
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        } else {
            // If the secondary meld card is on the battlefield and it is the one being activated, play the ability
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
    }
    
    @Override
    protected AiAbilityDecision doTriggerNoCost(Player ai, SpellAbility sa, boolean mandatory) {
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }
}