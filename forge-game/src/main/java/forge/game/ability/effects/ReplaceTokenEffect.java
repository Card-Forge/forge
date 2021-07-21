package forge.game.ability.effects;

import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.ObjectUtils;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
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

            Map<Player, Integer> toInsertMap = Maps.newHashMap();
            Set<Card> toRemoveSet = Sets.newHashSet();
            for (Map.Entry<Card, Integer> e : table.row(affected).entrySet()) {
                if (!sa.matchesValidParam("ValidCard", e.getKey())) {
                    continue;
                }
                Player controller = e.getKey().getController();
                int old = ObjectUtils.defaultIfNull(toInsertMap.get(controller), 0);
                toInsertMap.put(controller, old + e.getValue());
                toRemoveSet.add(e.getKey());
            }
            // remove replaced tokens
            table.row(affected).keySet().removeAll(toRemoveSet);

            // insert new tokens
            for (Map.Entry<Player, Integer> pe : toInsertMap.entrySet()) {
                if (pe.getValue() <= 0) {
                    continue;
                }
                for (String script : sa.getParam("TokenScript").split(",")) {
                    final Card token = TokenInfo.getProtoType(script, sa, pe.getKey());

                    if (token == null) {
                        throw new RuntimeException("don't find Token for TokenScript: " + script);
                    }

                    token.setController(pe.getKey(), timestamp);
                    table.put(affected, token, pe.getValue());
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

}
