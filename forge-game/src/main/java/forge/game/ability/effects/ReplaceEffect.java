package forge.game.ability.effects;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Maps;

import forge.game.Game;
import forge.game.GameLogEntryType;
import forge.game.GameObject;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.token.TokenInfo;
import forge.game.player.Player;
import forge.game.replacement.ReplacementResult;
import forge.game.replacement.ReplacementType;
import forge.game.spellability.SpellAbility;
import forge.util.TextUtil;

public class ReplaceEffect extends SpellAbilityEffect {

    @Override
    public void resolve(SpellAbility sa) {
        final Card card = sa.getHostCard();
        final Game game = card.getGame();

        final AbilityKey varName = AbilityKey.fromString(sa.getParam("VarName"));
        final String varValue = sa.getParam("VarValue");
        final String type = sa.getParamOrDefault("VarType", "amount");
        final ReplacementType retype = sa.getReplacementEffect().getMode();

        @SuppressWarnings("unchecked")
        Map<AbilityKey, Object> originalParams = (Map<AbilityKey, Object>) sa.getReplacingObject(AbilityKey.OriginalParams);
        Map<AbilityKey, Object> params = Maps.newHashMap(originalParams);

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

        if (params.containsKey(AbilityKey.EffectOnly)) {
            params.put(AbilityKey.EffectOnly, true);
        }

        if (retype == ReplacementType.DamageDone) {
            for (Map.Entry<AbilityKey, Object> e : params.entrySet()) {
                originalParams.put(e.getKey(), e.getValue());
            }
            originalParams.put(AbilityKey.ReplacementResult, ReplacementResult.Updated);
            return;
        }

        // need to log Updated events there, or the log is wrong order
        String message = sa.getReplacementEffect().toString();
        if ( !StringUtils.isEmpty(message)) {
            message = TextUtil.fastReplace(message, "CARDNAME", card.getName());
            game.getGameLog().add(GameLogEntryType.EFFECT_REPLACED, message);
        }

        //try to call replacementHandler with new Params
        ReplacementResult result = game.getReplacementHandler().run(retype, params);
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
