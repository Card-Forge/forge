package forge.card.abilityfactory.effects;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import forge.Card;
import forge.Singletons;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellEffect;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class TapAllEffect extends SpellEffect {
    
    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(Map<String, String> params, SpellAbility sa) {
        if (sa instanceof AbilitySub) {
            return "Tap all valid cards.";
        } else {
            return params.get("SpellDescription");
        }
    }

    @Override
    public void resolve(java.util.Map<String,String> params, SpellAbility sa) {
        final Card card = sa.getSourceCard();
        final boolean remTapped = params.containsKey("RememberTapped");
        if (remTapped) {
            card.clearRemembered();
        }
    
        List<Card> cards = null;
    
        final List<Player> tgtPlayers = getTargetPlayersEmptyAsDefault(sa, params);
    
        if ((tgtPlayers == null) || tgtPlayers.isEmpty()) {
            cards = Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield);
        } else {
            cards = new ArrayList<Card>();
            for( final Player p : tgtPlayers )
                cards.addAll(p.getCardsIn(ZoneType.Battlefield));
        }
    
        cards = AbilityFactory.filterListByType(cards, params.get("ValidCards"), sa);
    
        for (final Card c : cards) {
            if (remTapped) {
            card.addRemembered(c);
            }
            c.tap();
        }
    }

}