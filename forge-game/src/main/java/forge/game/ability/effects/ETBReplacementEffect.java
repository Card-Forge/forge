package forge.game.ability.effects;

import java.util.Map;

import forge.game.ability.AbilityKey;
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
        final Card card = (Card) sa.getReplacingObject(AbilityKey.Card);
        Map<AbilityKey, Object> params = AbilityKey.newMap();
        params.put(AbilityKey.CardLKI, sa.getReplacingObject(AbilityKey.CardLKI));
        params.put(AbilityKey.ReplacementEffect, sa.getReplacementEffect());
        sa.getActivatingPlayer().getGame().getAction().moveToPlay(card, card.getController(), sa, params);
    }
}