package forge.game.ability.effects;

import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import forge.game.Game;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.TokenCreateTable;
import forge.game.card.token.TokenInfo;
import forge.game.player.Player;
import forge.game.replacement.ReplacementEffect;
import forge.game.replacement.ReplacementResult;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Localizer;

public class ReplaceTokenEffect extends SpellAbilityEffect {

    @Override
    public void resolve(SpellAbility sa) {
        final Card card = sa.getHostCard();
        final Player p = sa.getActivatingPlayer();
        final Game game = card.getGame();
        SpellAbility repSA = sa;

        if (repSA.getReplacingObjects().isEmpty()) {
            repSA = sa.getRootAbility();
        }
        ReplacementEffect re = repSA.getReplacementEffect();
        // ReplaceToken Effect only applies to one Player
        Player affected = (Player) repSA.getReplacingObject(AbilityKey.Player);
        TokenCreateTable table = (TokenCreateTable) repSA.getReplacingObject(AbilityKey.Token);

        @SuppressWarnings("unchecked")
        Map<AbilityKey, Object> originalParams =
                (Map<AbilityKey, Object>) repSA.getReplacingObject(AbilityKey.OriginalParams);

        if ("Amount".equals(sa.getParam("Type"))) {
            final String mod = sa.getParamOrDefault("Amount", "Twice");
            for (Map.Entry<Card, Integer> e : table.row(affected).entrySet()) {
                if (!re.matchesValidParam("ValidToken", e.getKey())) {
                    continue;
                }
                int newAmt = AbilityUtils.doXMath(e.getValue(), mod, card, sa);
                table.put(affected, e.getKey(), newAmt);
            }
        } else if ("AddToken".equals(sa.getParam("Type"))) {
            long timestamp = game.getNextTimestamp();

            Map<Player, Integer> byController = Maps.newHashMap();
            for (Map.Entry<Card, Integer> e : table.row(affected).entrySet()) {
                if (!re.matchesValidParam("ValidToken", e.getKey())) {
                    continue;
                }
                Player controller = e.getKey().getController();
                int old = byController.getOrDefault(controller, 0);
                byController.put(controller, old + e.getValue());
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
                        token.setTokenSpawningAbility((SpellAbility)repSA.getReplacingObject(AbilityKey.Cause));
                        token.setController(e.getKey(), timestamp);
                        table.put(p, token, e.getValue());
                    }
                }
            }
        } else if ("ReplaceToken".equals(sa.getParam("Type"))) {
            Card chosen = null;
            if (sa.hasParam("ValidChoices")) {
                CardCollectionView choices = CardLists.getValidCards(game.getCardsIn(ZoneType.Battlefield), sa.getParam("ValidChoices"), p, card, sa);
                if (choices.isEmpty()) {
                    originalParams.put(AbilityKey.ReplacementResult, ReplacementResult.NotReplaced);
                    return;
                }
                chosen = p.getController().chooseSingleEntityForEffect(choices, sa, Localizer.getInstance().getMessage("lblChooseaCard"), false, null);
            }

            long timestamp = game.getNextTimestamp();

            Multimap<Player, Pair<Integer, Iterable<Object>>> toInsertMap = ArrayListMultimap.create();
            Set<Card> toRemoveSet = Sets.newHashSet();
            for (Map.Entry<Card, Integer> e : table.row(affected).entrySet()) {
                if (!re.matchesValidParam("ValidToken", e.getKey())) {
                    continue;
                }
                Player controller = e.getKey().getController();
                // TODO should still merge the amounts to avoid additional prototypes when sourceSA doesn't use ForEach
                //int old = ObjectUtils.defaultIfNull(toInsertMap.get(controller), 0);
                Pair<Integer, Iterable<Object>> tokenAmountPair = new ImmutablePair<>(e.getValue(), e.getKey().getRemembered());
                toInsertMap.put(controller, tokenAmountPair);
                toRemoveSet.add(e.getKey());
            }
            // remove replaced tokens
            table.row(affected).keySet().removeAll(toRemoveSet);

            // insert new tokens
            for (Map.Entry<Player, Pair<Integer, Iterable<Object>>> pe : toInsertMap.entries()) {
                int amt = pe.getValue().getLeft();
                if (amt <= 0) {
                    continue;
                }
                for (String script : sa.getParam("TokenScript").split(",")) {
                    final Card token;
                    if (script.equals("Chosen")) {
                        token = CopyPermanentEffect.getProtoType(sa, chosen, pe.getKey());
                        token.setCopiedPermanent(token);
                    } else {
                        token = TokenInfo.getProtoType(script, sa, pe.getKey());
                    }

                    if (token == null) {
                        throw new RuntimeException("don't find Token for TokenScript: " + script);
                    }

                    token.setTokenSpawningAbility((SpellAbility)repSA.getReplacingObject(AbilityKey.Cause));
                    token.setController(pe.getKey(), timestamp);
                    // if token is created from ForEach keep that
                    token.addRemembered(pe.getValue().getRight());
                    table.put(affected, token, amt);
                }
            }
        } else if ("ReplaceController".equals(sa.getParam("Type"))) {
            long timestamp = game.getNextTimestamp();
            Player newController = p;
            if (sa.hasParam("NewController")) {
                newController = AbilityUtils.getDefinedPlayers(sa.getHostCard(), sa.getParam("NewController"), sa).get(0);
            }
            for (Map.Entry<Card, Integer> c : table.row(affected).entrySet()) {
                if (!re.matchesValidParam("ValidToken", c.getKey())) {
                    continue;
                }
                c.getKey().setController(newController, timestamp);
            }
        }

        // effect was updated
        originalParams.put(AbilityKey.ReplacementResult, ReplacementResult.Updated);
    }

}
