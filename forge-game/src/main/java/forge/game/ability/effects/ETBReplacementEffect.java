package forge.game.ability.effects;

import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;

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