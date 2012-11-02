package forge.card.abilityfactory.effects;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import forge.Card;
import forge.CardLists;
import forge.Singletons;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellEffect;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class UntapAllEffect extends SpellEffect {
    
    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(Map<String, String> params, SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        if (sa instanceof AbilitySub) {
            sb.append(" ");
            sb.append("Untap all valid cards.");
        } else {
            sb.append(sa.getSourceCard()).append(" - ");
            sb.append(params.get("SpellDescription"));
        }
        return sb.toString();
    }

    @Override
    public void resolve(java.util.Map<String,String> params, SpellAbility sa) {
        final Card card = sa.getSourceCard();
    
        String valid = "";
        List<Card> list = null;
    
        ArrayList<Player> tgtPlayers = null;
    
        if (params.containsKey("ValidCards")) {
            valid = params.get("ValidCards");
        }
    
        final Target tgt = sa.getTarget();
        if (tgt != null) {
            tgtPlayers = tgt.getTargetPlayers();
        } else if (params.containsKey("Defined")) {
            // use it
            tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
        }
    
        if ((tgtPlayers == null) || tgtPlayers.isEmpty()) {
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