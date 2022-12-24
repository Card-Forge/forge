package forge.game.ability.effects;

import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

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
        CardCollectionView list;
        final String valid = sa.getParamOrDefault("ValidCards", "");

        if (!sa.usesTargeting() && !sa.hasParam("Defined")) {
            list = sa.getActivatingPlayer().getGame().getCardsIn(ZoneType.Battlefield);
        } else {
            list = getTargetPlayers(sa).getCardsIn(ZoneType.Battlefield);
        }
        list = CardLists.getValidCards(list, valid, sa.getActivatingPlayer(), card, sa);

        boolean remember = sa.hasParam("RememberUntapped");
        for (Card c : list) {
            c.untap(true);
            if (remember) {
                card.addRemembered(c);
            }
        }
    }
}
