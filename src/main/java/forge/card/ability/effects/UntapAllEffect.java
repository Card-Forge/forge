package forge.card.ability.effects;

import java.util.ArrayList;
import java.util.List;

import forge.Card;
import forge.CardLists;
import forge.Singletons;
import forge.card.ability.SpellAbilityEffect;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class UntapAllEffect extends SpellAbilityEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#getStackDescription(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected String getStackDescription(SpellAbility sa) {
        if (sa instanceof AbilitySub) {
            return "Untap all valid cards.";
        } else {
            return sa.getParam("SpellDescription");
        }
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card card = sa.getSourceCard();

        String valid = "";
        List<Card> list = null;

        List<Player> tgtPlayers = getTargetPlayersEmptyAsDefault(sa);

        if (sa.hasParam("ValidCards")) {
            valid = sa.getParam("ValidCards");
        }

        if (tgtPlayers.isEmpty()) {
            list = Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield);
        } else {
            list = new ArrayList<Card>();
            for (final Player p : tgtPlayers) {
                list.addAll(p.getCardsIn(ZoneType.Battlefield));
            }
        }
        list = CardLists.getValidCards(list, valid.split(","), card.getController(), card);

        boolean remember = sa.hasParam("RememberUntapped");
        for (Card c : list) {
            c.untap();
            if (remember) {
                card.addRemembered(c);
            }
        }
    }

}
