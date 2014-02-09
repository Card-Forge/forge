package forge.game.ability.effects;

import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.ArrayList;
import java.util.List;

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

        List<Player> tgtPlayers = getTargetPlayers(sa);

        if (sa.hasParam("ValidCards")) {
            valid = sa.getParam("ValidCards");
        }

        if (!sa.usesTargeting() && !sa.hasParam("Defined")) {
            list = sa.getActivatingPlayer().getGame().getCardsIn(ZoneType.Battlefield);
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
