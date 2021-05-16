package forge.game.ability.effects;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import forge.game.Game;
import forge.game.GameEntity;
import forge.game.GameEntityCounterTable;
import forge.game.GameObject;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardDamageMap;
import forge.game.replacement.ReplacementResult;
import forge.game.replacement.ReplacementType;
import forge.game.spellability.SpellAbility;

public class ReplaceSplitDamageEffect extends SpellAbilityEffect {

    @Override
    public void resolve(SpellAbility sa) {
        final Card card = sa.getHostCard();
        final Game game = card.getGame();

        // outside of Replacement Effect, unwanted result
        if (!sa.isReplacementAbility()) {
            return;
        }

        final ReplacementType event = sa.getReplacementEffect().getMode();
        
        String varValue = sa.getParamOrDefault("VarName", "1");

        @SuppressWarnings("unchecked")
        Map<AbilityKey, Object> originalParams = (Map<AbilityKey , Object>) sa.getReplacingObject(AbilityKey.OriginalParams);
        Map<AbilityKey, Object> params = AbilityKey.newMap(originalParams);
        
        Integer dmg = (Integer) sa.getReplacingObject(AbilityKey.DamageAmount);
        
        
        int prevent = AbilityUtils.calculateAmount(card, varValue, sa);
        
        List<GameObject> list = AbilityUtils.getDefinedObjects(card, sa.getParam("DamageTarget"), sa);

        if (prevent > 0 && list.size() > 0 && list.get(0) instanceof GameEntity) {
            int n = Math.min(dmg, prevent);
            dmg -= n;
            prevent -= n;

            if (card.getType().hasStringType("Effect") && prevent <= 0) {
                game.getAction().exile(card, null);
            } else if (!StringUtils.isNumeric(varValue)) {
                sa.setSVar(varValue, "Number$" + prevent);
                card.updateAbilityTextForView();
            }
            
            Card sourceLKI = (Card) sa.getReplacingObject(AbilityKey.Source);
            GameEntity target = (GameEntity) sa.getReplacingObject(AbilityKey.Target);

            GameEntity obj = (GameEntity) list.get(0);
            boolean isCombat = (Boolean) originalParams.get(AbilityKey.IsCombat);
            CardDamageMap damageMap = (CardDamageMap) originalParams.get(AbilityKey.DamageMap);
            CardDamageMap preventMap = (CardDamageMap) originalParams.get(AbilityKey.PreventMap);
            GameEntityCounterTable counterTable = (GameEntityCounterTable) originalParams.get(AbilityKey.CounterTable);
            SpellAbility cause = (SpellAbility) originalParams.get(AbilityKey.Cause);
            damageMap.put(sourceLKI, obj, n);
            obj.replaceDamage(n, sourceLKI, isCombat, damageMap, preventMap, counterTable, cause);
        }

        // no damage for original target anymore
        if (dmg <= 0) {
            originalParams.put(AbilityKey.ReplacementResult, ReplacementResult.Replaced);
            return;
        }
        params.put(AbilityKey.DamageAmount, dmg);

        //try to call replacementHandler with new Params
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
