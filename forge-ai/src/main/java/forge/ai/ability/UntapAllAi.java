package forge.ai.ability;

import forge.ai.SpellAbilityAi;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class UntapAllAi extends SpellAbilityAi {
    @Override
    protected boolean canPlayAI(Player aiPlayer, SpellAbility sa) {
        final Card source = sa.getHostCard();

        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            String valid = "";
            CardCollectionView list = aiPlayer.getGame().getCardsIn(ZoneType.Battlefield);
            if (sa.hasParam("ValidCards")) {
                valid = sa.getParam("ValidCards");
            }
            list = CardLists.getValidCards(list, valid.split(","), source.getController(), source);
            return !list.isEmpty();
        }
        return false;
    }

    @Override
    protected boolean doTriggerAINoCost(Player aiPlayer, SpellAbility sa, boolean mandatory) {
        return mandatory;
    }
}
