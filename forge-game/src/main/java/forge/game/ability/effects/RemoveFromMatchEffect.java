package forge.game.ability.effects;

import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.event.GameEventRandomLog;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;

public class RemoveFromMatchEffect extends SpellAbilityEffect {
    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        CardCollection toRemove;

        if (sa.hasParam("RemoveType")) {
            CardCollection cards = (CardCollection) host.getOwner().getGame().getCardsInGame();
            if (sa.hasParam("IncludeSideboard")) {
                CardCollection sideboard = (CardCollection) host.getGame().getCardsIn(ZoneType.Sideboard);
                cards.addAll(sideboard);
            }
            toRemove = (CardCollection) AbilityUtils.filterListByType(cards, sa.getParam("RemoveType"), sa);
        } else {
            toRemove = getTargetCards(sa);
        }
        String logMessage = sa.getParamOrDefault("LogMessage", "Removed from match");
        String remove = toRemove.toString().replace("[","").replace("]","");
        host.getController().getGame().fireEvent(new GameEventRandomLog(logMessage + ": " + remove));
        for (final Card tgtC : toRemove) {
            tgtC.getGame().getAction().ceaseToExist(tgtC, true);
            PaperCard rem = (PaperCard) tgtC.getPaperCard();
            host.getGame().getMatch().removeCard(rem);
        }
    }
}