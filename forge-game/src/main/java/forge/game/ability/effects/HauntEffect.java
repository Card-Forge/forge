package forge.game.ability.effects;

import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
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
            final Card copy = game.getAction().exile(new CardCollection(card), sa, null).get(0);
            sa.getTargetCard().addHauntedBy(copy);
        } else if (!sa.usesTargeting() && card.getHaunting() != null) {
            // unhaunt
            card.getHaunting().removeHauntedBy(card);
            card.setHaunting(null);
        }
    }

}
