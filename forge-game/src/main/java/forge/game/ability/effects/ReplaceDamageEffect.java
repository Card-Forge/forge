package forge.game.ability.effects;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import forge.game.Game;
import forge.game.GameEntity;
import forge.game.GameLogEntryType;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardDamageMap;
import forge.game.replacement.ReplacementResult;
import forge.game.replacement.ReplacementType;
import forge.game.spellability.SpellAbility;
import forge.util.TextUtil;

/**
 * This class handles two kinds of prevention effect:
 * - Prevent next X damages. Those will use `Amount$ <SVar>`, and the `<SVar>` will have form `Number$ X`.
 *   That SVar will be updated after each prevention "shield" used up.
 * - Prevent X damages. Those will use `Amount$ N` or `Amount$ <SVar>`, where the `<SVar>` will have form other than
 *   `Number$ X`. These "shields" are not used up so won't be updated. */
public class ReplaceDamageEffect extends SpellAbilityEffect {

    @Override
    public void resolve(SpellAbility sa) {
        final Card card = sa.getHostCard();
        final Game game = card.getGame();

        // outside of Replacement Effect, unwanted result
        if (!sa.isReplacementAbility()) {
            return;
        }

        @SuppressWarnings("unchecked")
        Map<AbilityKey, Object> originalParams = (Map<AbilityKey, Object>) sa.getReplacingObject(AbilityKey.OriginalParams);
        Map<AbilityKey, Object> params = AbilityKey.newMap(originalParams);

        Integer dmg = (Integer) sa.getReplacingObject(AbilityKey.DamageAmount);
                
        String varValue = sa.getParamOrDefault("Amount", "1");
        int prevent = AbilityUtils.calculateAmount(card, varValue, sa);
        
        if (prevent > 0) {
            int n = Math.min(dmg, prevent);
            dmg -= n;
            prevent -= n;

            if (!StringUtils.isNumeric(varValue) && card.getSVar(varValue).startsWith("Number$")) {
                if (card.getType().hasStringType("Effect") && prevent <= 0) {
                    game.getAction().exile(card, null);
                } else {
                    card.setSVar(varValue, "Number$" + prevent);
                    card.updateAbilityTextForView();
                }
            }
            // Set PreventedDamage SVar
            card.setSVar("PreventedDamage", "Number$" + n);

            Card sourceLKI = (Card) sa.getReplacingObject(AbilityKey.Source);
            GameEntity target = (GameEntity) sa.getReplacingObject(AbilityKey.Target);

            // Set prevent map entry
            CardDamageMap preventMap = (CardDamageMap) originalParams.get(AbilityKey.PreventMap);
            preventMap.put(sourceLKI, target, n);

            // Following codes are commented out since DamagePrevented trigger is currently not used by any Card.
            // final Map<AbilityKey, Object> runParams = AbilityKey.newMap();
            // runParams.put(AbilityKey.DamageTarget, target);
            // runParams.put(AbilityKey.DamageAmount, dmg);
            // runParams.put(AbilityKey.DamageSource, sourceLKI);
            // runParams.put(AbilityKey.IsCombatDamage, originalParams.get(AbilityKey.IsCombat));
            // game.getTriggerHandler().runTrigger(TriggerType.DamagePrevented, runParams, false);
        }

        // no damage for original target anymore
        if (dmg <= 0) {
            originalParams.put(AbilityKey.ReplacementResult, ReplacementResult.Replaced);
            return;
        }
        params.put(AbilityKey.DamageAmount, dmg);

        // need to log Updated events there, or the log is wrong order
        String message = sa.getReplacementEffect().toString();
        if ( !StringUtils.isEmpty(message)) {
            message = TextUtil.fastReplace(message, "CARDNAME", card.getName());
            game.getGameLog().add(GameLogEntryType.EFFECT_REPLACED, message);
        }

        //try to call replacementHandler with new Params
        final ReplacementType event = sa.getReplacementEffect().getMode();
        ReplacementResult result = game.getReplacementHandler().run(event, params);
        switch (result) {
        case NotReplaced:
        case Updated: {
            for (Map.Entry<AbilityKey, Object> e : params.entrySet()) {
                originalParams.put(e.getKey(), e.getValue());
            }
            // effect was updated
            originalParams.put(AbilityKey.ReplacementResult, ReplacementResult.Updated);
            break;
        }
        default:
            // effect was replaced with something else
            originalParams.put(AbilityKey.ReplacementResult, result);
            break;
        }
    }

}
