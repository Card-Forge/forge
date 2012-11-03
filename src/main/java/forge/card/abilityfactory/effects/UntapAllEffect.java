package forge.card.abilityfactory.effects;

import java.util.List;
import java.util.Map;

import forge.Card;
import forge.CardLists;
import forge.Singletons;
import forge.card.abilityfactory.SpellEffect;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class UntapAllEffect extends SpellEffect {
    
    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(Map<String, String> params, SpellAbility sa) {
        if (sa instanceof AbilitySub) {
            return "Untap all valid cards.";
        } else {
            return params.get("SpellDescription");
        }
    }

    @Override
    public void resolve(java.util.Map<String,String> params, SpellAbility sa) {
        final Card card = sa.getSourceCard();
    
        String valid = "";
        List<Card> list = null;
    
        List<Player> tgtPlayers = getTargetPlayers(sa, params);
    
        if (params.containsKey("ValidCards")) {
            valid = params.get("ValidCards");
        }
    
        if (tgtPlayers.isEmpty()) {
            list = Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield);
        } else {
            list = tgtPlayers.get(0).getCardsIn(ZoneType.Battlefield);
        }
        list = CardLists.getValidCards(list, valid.split(","), card.getController(), card);
    
        boolean remember = params.containsKey("RememberUntapped");
        for(Card c : list) {
            c.untap();
            if (remember) {
                card.addRemembered(c);
            }
        }
    }

}