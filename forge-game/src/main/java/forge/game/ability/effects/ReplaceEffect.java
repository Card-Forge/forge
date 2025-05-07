package forge.game.ability.effects;

import java.util.List;
import java.util.Map;
import java.util.Set;

import forge.game.GameObject;
import forge.game.PlanarDice;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.replacement.ReplacementResult;
import forge.game.spellability.SpellAbility;

public class ReplaceEffect extends SpellAbilityEffect {

    @Override
    public void resolve(SpellAbility sa) {
        final Card card = sa.getHostCard();

        final AbilityKey varName = AbilityKey.fromString(sa.getParam("VarName"));
        final String varValue = sa.getParam("VarValue");
        final String type = sa.getParamOrDefault("VarType", "amount");

        @SuppressWarnings("unchecked")
        Map<AbilityKey, Object> params = (Map<AbilityKey, Object>) sa.getReplacingObject(AbilityKey.OriginalParams);

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
        } else if ("PlanarDice".equals(type)) {
            params.put(varName, PlanarDice.smartValueOf(varValue));
        } else if ("Map".equals(type)) {
            Map<Player, Integer> m = (Map<Player, Integer>) sa.getReplacingObject(varName);
            for (Player key : AbilityUtils.getDefinedPlayers(card, sa.getParam("VarKey"), sa)) {
                m.put(key, m.getOrDefault(key, 0) + AbilityUtils.calculateAmount(card, varValue, sa));
            }
        } else if ("CardSet".equals(type)) {
            Set<Card> cards = (Set<Card>) params.get(varName);
            List<Card> list = AbilityUtils.getDefinedCards(card, varValue, sa);
            if (!list.isEmpty()) {
                cards.add(list.get(0));
            }
        } else if (varName != null) {
            params.put(varName, AbilityUtils.calculateAmount(card, varValue, sa));
        }

        params.put(AbilityKey.ReplacementResult, ReplacementResult.Updated);
    }

}
