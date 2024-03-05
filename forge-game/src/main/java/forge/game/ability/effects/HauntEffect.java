package forge.game.ability.effects;

import java.util.Map;

import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardZoneTable;
import forge.game.spellability.SpellAbility;

public class HauntEffect extends SpellAbilityEffect {

    @Override
    public void resolve(SpellAbility sa) {
        Card host = sa.getHostCard();
        if (host.isPermanent()) {
            // get new version instead of battlefield lki
            host = (Card) sa.getTriggeringObject(AbilityKey.NewCard);
        }
        final Game game = host.getGame();
        Card card = game.getCardState(host, null);
        if (card == null) {
            return;
        } else if (sa.usesTargeting() && !card.isToken() && host.equalsWithTimestamp(card)) {
            // haunt target but only if card is no token and still in grave
            Map<AbilityKey, Object> moveParams = AbilityKey.newMap();
            CardZoneTable zoneMovements = AbilityKey.addCardZoneTableParams(moveParams, sa);
            final Card moved = game.getAction().exile(card, sa, moveParams);
            sa.getTargetCard().addHauntedBy(moved);
            zoneMovements.triggerChangesZoneAll(game, sa);
        } else if (!sa.usesTargeting() && card.getHaunting() != null) {
            // unhaunt
            card.getHaunting().removeHauntedBy(card);
            card.setHaunting(null);
        }
    }

}
