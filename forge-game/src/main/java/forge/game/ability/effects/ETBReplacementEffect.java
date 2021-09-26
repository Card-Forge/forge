package forge.game.ability.effects;

import java.util.Map;

import forge.game.Game;
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
        final Game game = sa.getActivatingPlayer().getGame();
        final Card card = (Card) sa.getReplacingObject(AbilityKey.Card);

        Map<AbilityKey, Object> params = AbilityKey.newMap();
        params.put(AbilityKey.CardLKI, sa.getReplacingObject(AbilityKey.CardLKI));
        params.put(AbilityKey.ReplacementEffect, sa.getReplacementEffect());
        params.put(AbilityKey.LastStateBattlefield, sa.getReplacingObject(AbilityKey.LastStateBattlefield));
        params.put(AbilityKey.LastStateGraveyard, sa.getReplacingObject(AbilityKey.LastStateGraveyard));

        final SpellAbility root = sa.getRootAbility();
        SpellAbility cause = (SpellAbility) root.getReplacingObject(AbilityKey.Cause);

        game.getAction().moveToPlay(card, card.getController(), cause, params);
    }
}