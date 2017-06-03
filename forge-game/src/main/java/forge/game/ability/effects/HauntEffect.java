package forge.game.ability.effects;

import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;

public class HauntEffect extends SpellAbilityEffect {

    @Override
    public void resolve(SpellAbility sa) {
        final Card card = sa.getHostCard();
        if (sa.usesTargeting() && !card.isToken()) {
            // haunt target but only if card is no token
            final Card copy = card.getGame().getAction().exile(card, sa);
            sa.getTargets().getFirstTargetedCard().addHauntedBy(copy);
        } else if (!sa.usesTargeting() && card.getHaunting() != null) {
            // unhaunt
            card.getHaunting().removeHauntedBy(card);
            card.setHaunting(null);
        }
    }

}
