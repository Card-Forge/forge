package forge.card.ability.ai;

import java.util.List;

import forge.Card;
import forge.CardLists;
import forge.card.ability.SpellAbilityAi;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class UntapAllAi extends SpellAbilityAi {

    @Override
    protected boolean canPlayAI(Player aiPlayer, SpellAbility sa) {
        final Card source = sa.getSourceCard();

        final AbilitySub abSub = sa.getSubAbility();
        if (abSub != null) {
            String valid = "";
            List<Card> list = aiPlayer.getGame().getCardsIn(ZoneType.Battlefield);
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
