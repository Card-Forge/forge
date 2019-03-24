package forge.game.ability.effects;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;

import forge.game.Game;
import forge.game.GameObject;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.token.TokenInfo;
import forge.game.player.Player;
import forge.game.replacement.ReplacementResult;
import forge.game.spellability.SpellAbility;

public class ReplaceEffect extends SpellAbilityEffect {

    @Override
    public void resolve(SpellAbility sa) {
        final Card card = sa.getHostCard();
        final Game game = card.getGame();

        final String varName = sa.getParam("VarName");
        final String varValue = sa.getParam("VarValue");
        final String type = sa.getParamOrDefault("VarType", "amount");

        @SuppressWarnings("unchecked")
        Map<String, Object> originalParams = (Map<String, Object>) sa.getReplacingObject("OriginalParams");
        Map<String, Object> params = Maps.newHashMap(originalParams);

        if ("Card".equals(type)) {
            List<Card> list = AbilityUtils.getDefinedCards(card, varValue, sa);
            if (list.size() > 0) {
                params.put(varName, list.get(0));
            }
        } else if ("Player".equals(type)) {
            List<Player> list = AbilityUtils.getDefinedPlayers(card, varValue, sa);
            if (list.size() > 0) {
                params.put(varName, list.get(0));
            }
        } else if ("GameEntity".equals(type)) {
            List<GameObject> list = AbilityUtils.getDefinedObjects(card, varValue, sa);
            if (list.size() > 0) {
                params.put(varName, list.get(0));
            }
        } else if ("TokenScript".equals(type)) {
            final Card protoType = TokenInfo.getProtoType(varValue, sa);
            if (protoType != null) {
                params.put(varName, protoType);
            }
        } else {
            params.put(varName, AbilityUtils.calculateAmount(card, varValue, sa));
        }

        if (params.containsKey("EffectOnly")) {
            params.put("EffectOnly", true);
        }

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
