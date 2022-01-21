package forge.game.ability.effects;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.ObjectUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardFactory;
import forge.game.card.TokenCreateTable;
import forge.game.card.token.TokenInfo;
import forge.game.player.Player;
import forge.game.replacement.ReplacementResult;
import forge.game.spellability.SpellAbility;

public class ReplaceTokenEffect extends SpellAbilityEffect {

    @Override
    public void resolve(SpellAbility sa) {
        final Card card = sa.getHostCard();
        final Player p = sa.getActivatingPlayer();
        final Game game = card.getGame();

        // ReplaceToken Effect only applies to one Player
        Player affected = (Player) sa.getReplacingObject(AbilityKey.Player);
        TokenCreateTable table = (TokenCreateTable) sa.getReplacingObject(AbilityKey.Token);

        @SuppressWarnings("unchecked")
        Map<AbilityKey, Object> originalParams = (Map<AbilityKey, Object>) sa
                .getReplacingObject(AbilityKey.OriginalParams);

        // currently the only ones that changes the amount does double it
        if ("Amount".equals(sa.getParam("Type"))) {
            for (Map.Entry<Card, Integer> e : table.row(affected).entrySet()) {
                if (!sa.matchesValidParam("ValidCard", e.getKey())) {
                    continue;
                }
                // currently the amount is only doubled
                table.put(affected, e.getKey(), e.getValue() * 2);
            }
        } else if ("AddToken".equals(sa.getParam("Type"))) {
            long timestamp = game.getNextTimestamp();

            Map<Player, Integer> byController = Maps.newHashMap();
            for (Map.Entry<Card, Integer> e : table.row(affected).entrySet()) {
                if (!sa.matchesValidParam("ValidCard", e.getKey())) {
                    continue;
                }
                Player contoller = e.getKey().getController();
                int old = ObjectUtils.defaultIfNull(byController.get(contoller), 0);
                byController.put(contoller, old + e.getValue());
            }

            if (!byController.isEmpty()) {
                // for Xorn, might matter if you could somehow create Treasure under multiple players control
                if (sa.hasParam("Amount")) {
                    int i = AbilityUtils.calculateAmount(card, sa.getParam("Amount"), sa);
                    for (Map.Entry<Player, Integer> e : byController.entrySet()) {
                        e.setValue(i);
                    }
                }
                for (Map.Entry<Player, Integer> e : byController.entrySet()) {
                    for (String script : sa.getParam("TokenScript").split(",")) {
                        final Card token = TokenInfo.getProtoType(script, sa, p);
    
                        if (token == null) {
                            throw new RuntimeException("don't find Token for TokenScript: " + script);
                        }
                        token.setController(e.getKey(), timestamp);
                        table.put(p, token, e.getValue());
                    }
                }
            }
        } else if ("ReplaceToken".equals(sa.getParam("Type"))) {
            long timestamp = game.getNextTimestamp();
            SpellAbility sourceSA = (SpellAbility) originalParams.get(AbilityKey.SourceSA);

            Map<Player, List<Integer>> toInsertMap = Maps.newHashMap();
            Map<Player, List<Card>> oldTokenMap = Maps.newHashMap();
            Set<Card> toRemoveSet = Sets.newHashSet();
            for (Map.Entry<Card, Integer> e : table.row(affected).entrySet()) {
                if (!sa.matchesValidParam("ValidCard", e.getKey())) {
                    continue;
                }
                Player controller = e.getKey().getController();
                if (toInsertMap.get(controller) == null) {
                    toInsertMap.put(controller, Lists.newArrayList());
                }
                toInsertMap.get(controller).add(e.getValue());
                if (oldTokenMap.get(controller) == null) {
                    oldTokenMap.put(controller, Lists.newArrayList());
                }
                oldTokenMap.get(controller).add(e.getKey());
                toRemoveSet.add(e.getKey());
            }
            // remove replaced tokens
            table.row(affected).keySet().removeAll(toRemoveSet);

            // insert new tokens
            for (Map.Entry<Player, List<Integer>> pe : toInsertMap.entrySet()) {
                List<Integer> amounts = transformAmounts(pe.getValue(), sourceSA);
                int oldIndex = -1;
                for (Integer amt : amounts) {
                    oldIndex++;
                    if (amt <= 0) {
                        continue;
                    }
                    for (String script : sa.getParam("TokenScript").split(",")) {
                        final Card token = TokenInfo.getProtoType(script, sa, pe.getKey());

                        if (token == null) {
                            throw new RuntimeException("don't find Token for TokenScript: " + script);
                        }

                        token.setController(pe.getKey(), timestamp);

                        // reapply state to new token
                        final Card newToken = CardFactory.copyCard(token, true);
                        newToken.setStates(CardFactory.getCloneStates(token, newToken, sourceSA));
                        // force update the now set State
                        newToken.setState(newToken.getCurrentStateName(), true, true);
                        // if token is created from ForEach keep that
                        newToken.addRemembered(oldTokenMap.get(pe.getKey()).get(oldIndex).getRemembered());
                        table.put(affected, newToken, amt);
                    }
                }
            }
        } else if ("ReplaceController".equals(sa.getParam("Type"))) {
            long timestamp = game.getNextTimestamp();
            Player newController = sa.getActivatingPlayer();
            if (sa.hasParam("NewController")) {
                newController = AbilityUtils.getDefinedPlayers(sa.getHostCard(), sa.getParam("NewController"), sa).get(0);
            }
            for (Map.Entry<Card, Integer> c : table.row(affected).entrySet()) {
                if (!sa.matchesValidParam("ValidCard", c.getKey())) {
                    continue;
                }
                c.getKey().setController(newController, timestamp);
            }
        }

        // effect was updated
        originalParams.put(AbilityKey.ReplacementResult, ReplacementResult.Updated);
    }

    private static List<Integer> transformAmounts(List<Integer> in, SpellAbility sa) {
        if (sa.hasParam("ForEach")) {
            return in;
        }
        // we can merge the amounts without losing information
        List<Integer> result = Lists.newArrayList();
        int sum = 0;
        for (int e : in) {
            sum += e;
        }
        result.add(sum);
        return result;
    }
}
