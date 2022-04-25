package forge.ai.ability;

import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;

import forge.ai.SpellAbilityAi;
import forge.game.card.CardCollectionView;
import forge.game.card.CardPredicates;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class MeldAi extends SpellAbilityAi {
    @Override
    protected boolean checkApiLogic(Player aiPlayer, SpellAbility sa) {
        String primaryMeld = sa.getParam("Primary");
        String secondaryMeld = sa.getParam("Secondary");
        
        CardCollectionView cardsOTB = aiPlayer.getCardsIn(ZoneType.Battlefield);
        if (cardsOTB.isEmpty()) {
            return false;
        }
        
        boolean hasPrimaryMeld = Iterables.any(cardsOTB, Predicates.and(
                CardPredicates.nameEquals(primaryMeld), CardPredicates.isOwner(aiPlayer)));
        boolean hasSecondaryMeld = Iterables.any(cardsOTB, Predicates.and(
                CardPredicates.nameEquals(secondaryMeld), CardPredicates.isOwner(aiPlayer)));
        
        return hasPrimaryMeld && hasSecondaryMeld && sa.getHostCard().getName().equals(primaryMeld);
    }
    
    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
        return true;
    }
}