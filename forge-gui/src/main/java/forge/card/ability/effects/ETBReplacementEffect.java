package forge.card.ability.effects;

import forge.card.ability.SpellAbilityEffect;
import forge.card.spellability.SpellAbility;
import forge.game.card.Card;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class ETBReplacementEffect extends SpellAbilityEffect {
    @Override
    public void resolve(SpellAbility sa) {
        sa.getActivatingPlayer().getGame().getAction().moveToPlay(((Card) sa.getReplacingObject("Card")));
    }
}