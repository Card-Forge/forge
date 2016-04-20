package forge.game.ability.effects;

import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.player.Player;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.List;

public class UntapAllEffect extends SpellAbilityEffect {
    @Override
    protected String getStackDescription(SpellAbility sa) {
        if (sa instanceof AbilitySub) {
            return "Untap all valid cards.";
        }
        return sa.getParam("SpellDescription");
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card card = sa.getHostCard();

        String valid = "";
        CardCollectionView list;

        List<Player> tgtPlayers = getTargetPlayers(sa);

        if (sa.hasParam("ValidCards")) {
            valid = sa.getParam("ValidCards");
        }

        if (!sa.usesTargeting() && !sa.hasParam("Defined")) {
            list = sa.getActivatingPlayer().getGame().getCardsIn(ZoneType.Battlefield);
        } else {
            CardCollection list2 = new CardCollection();
            for (final Player p : tgtPlayers) {
                list2.addAll(p.getCardsIn(ZoneType.Battlefield));
            }
            list = list2;
        }
        list = CardLists.getValidCards(list, valid.split(","), card.getController(), card, sa);

        boolean remember = sa.hasParam("RememberUntapped");
        for (Card c : list) {
            c.untap();
            if (remember) {
                card.addRemembered(c);
            }
        }
    }
}
