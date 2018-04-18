package forge.game.ability.effects;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Maps;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.replacement.ReplacementResult;
import forge.game.spellability.SpellAbility;

public class ReplaceDamageEffect extends SpellAbilityEffect {

    @Override
    public void resolve(SpellAbility sa) {
        final Card card = sa.getHostCard();
        final Game game = card.getGame();

        // outside of Replacement Effect, unwanted result
        if (!sa.getRootAbility().isReplacementAbility()) {
            return;
        }
        
        String varValue = sa.getParamOrDefault("VarName", "1");

        @SuppressWarnings("unchecked")
        Map<String, Object> originalParams = (Map<String, Object>) sa.getReplacingObject("OriginalParams");
        Map<String, Object> params = Maps.newHashMap(originalParams);
        
        Integer dmg = (Integer) sa.getReplacingObject("DamageAmount");
                
        int prevent = AbilityUtils.calculateAmount(card, varValue, sa);
        
        // Currently it does reduce damage by amount, need second mode for Setting Damage
        
        if (prevent > 0) {
            int n = Math.min(dmg, prevent);
            dmg -= n;
            prevent -= n;

            if (card.getType().hasStringType("Effect") && prevent <= 0) {
               game.getAction().exile(card, null);
            } else if (!StringUtils.isNumeric(varValue)) {
                card.setSVar(varValue, "Number$" + prevent);
            }
        }

        // no damage for original target anymore
        if (dmg <= 0) {
            originalParams.put("ReplacementResult", ReplacementResult.Replaced);
            return;
        }
        params.put("DamageAmount", dmg);


        //try to call replacementHandler with new Params
        ReplacementResult result = game.getReplacementHandler().run(params); 
        switch (result) {
        case NotReplaced:
        case Updated: {
            for (Map.Entry<String, Object> e : params.entrySet()) {
                originalParams.put(e.getKey(), e.getValue());
            }
            // effect was updated
            originalParams.put("ReplacementResult", ReplacementResult.Updated);
            break;
        }
        default:
            // effect was replaced with something else
            originalParams.put("ReplacementResult", result);
            break;
        }
    }

}
