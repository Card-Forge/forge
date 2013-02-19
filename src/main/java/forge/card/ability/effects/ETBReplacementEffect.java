package forge.card.ability.effects;

import forge.Card;
import forge.card.ability.SpellAbilityEffect;
import forge.card.spellability.SpellAbility;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class ETBReplacementEffect extends SpellAbilityEffect {
    @Override
    public void resolve(SpellAbility sa) {
        forge.Singletons.getModel().getGame().getAction().moveToPlay(((Card) sa.getReplacingObject("Card")));
    }
}