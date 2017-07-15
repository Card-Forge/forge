package forge.ai.ability;

import forge.ai.SpellAbilityAi;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class MeldAi extends SpellAbilityAi {
    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected boolean checkApiLogic(Player aiPlayer, SpellAbility sa) {
        String primaryMeld = sa.getParam("Primary");
        String secondaryMeld = sa.getParam("Secondary");
        
        CardCollectionView cardsOTB = aiPlayer.getCardsIn(ZoneType.Battlefield);
        
        boolean hasPrimaryMeld = !CardLists.filter(cardsOTB, CardPredicates.nameEquals(primaryMeld)).isEmpty();
        boolean hasSecondaryMeld = !CardLists.filter(cardsOTB, CardPredicates.nameEquals(secondaryMeld)).isEmpty();
        
        return hasPrimaryMeld && hasSecondaryMeld;
    }
    
    @Override
    protected boolean doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
        return true;
    }
}