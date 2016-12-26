package forge.game.ability.effects;

import java.util.Map;

import com.google.common.collect.Maps;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;

public class ReplaceEffect extends SpellAbilityEffect {

    @Override
    public void resolve(SpellAbility sa) {
        final Card card = sa.getHostCard();
        final Game game = card.getGame();

        final String varName = sa.getParam("VarName");
        final String varValue = sa.getParam("VarValue");

        @SuppressWarnings("unchecked")
        Map<String, Object> originalParams = (Map<String, Object>) sa.getReplacingObject("OriginalParams");
        Map<String, Object> params = Maps.newHashMap(originalParams);

        params.put(varName, AbilityUtils.calculateAmount(card, varValue, sa));

        //try to call replacementHandler with new Params
        switch (game.getReplacementHandler().run(params)) {
        case NotReplaced:
        case Updated: {
            for (Map.Entry<String, Object> e : params.entrySet()) {
                originalParams.replace(e.getKey(), e.getValue());
            }
        }
        default:
            break;
        }
    }

}
