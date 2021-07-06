package forge.game.ability.effects;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import forge.game.Game;
import forge.game.GameEntityCounterTable;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.card.CounterType;
import forge.game.player.Player;
import forge.game.player.PlayerController;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.CardTranslation;
import forge.util.Localizer;
import forge.util.TextUtil;

public class CountersMoveEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        final List<Card> tgtCards = getDefinedCardsOrTargeted(sa);

        Card source = null;
        if (sa.usesTargeting() && sa.getMinTargets() == 2) {
            if (tgtCards.size() < 2) {
                return "";
            }
            source = tgtCards.remove(0);
        } else {
            List<Card> srcCards = getDefinedCardsOrTargeted(sa, "Source");

            if (srcCards.size() > 0) {
                source = srcCards.get(0);
            }
        }
        final String countername = sa.getParam("CounterType");
        final String counterAmount = sa.getParam("CounterNum");
        int amount = 0;
        if (!"Any".equals(counterAmount) && !"All".equals(counterAmount)) {
            amount = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("CounterNum"), sa);
        }

        sb.append("Move ");
        if ("Any".matches(countername)) {
            if (amount == 1) {
                sb.append("a counter");
            } else {
                sb.append(amount).append(" ").append(" counter");
            }
        } else if ("All".equals(countername)) {
            sb.append("all counter");
        } else {
            sb.append(amount).append(" ").append(countername).append(" counter");
        }
        if (amount != 1) {
            sb.append("s");
        }
        sb.append(" from ").append(source).append(" to ");
        try {
            sb.append(tgtCards.get(0));
        } catch (final IndexOutOfBoundsException exception) {
            System.out.println(TextUtil.concatWithSpace("Somehow this is missing targets?", source.toString()));
        }

        sb.append(".");
        return sb.toString();
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Card host = sa.getHostCard();
        final String counterName = sa.getParam("CounterType");
        final String counterNum = sa.getParam("CounterNum");
        final Player player = sa.getActivatingPlayer();
        final PlayerController pc = player.getController();
        final Game game = host.getGame();

        CounterType cType = null;
        if (!counterName.matches("Any") && !counterName.matches("All")) {
            try {
                cType = AbilityUtils.getCounterType(counterName, sa);
            } catch (Exception e) {
                System.out.println("Counter type doesn't match, nor does an SVar exist with the type name.");
                return;
            }
        }

        GameEntityCounterTable table = new GameEntityCounterTable();

        // uses for multi sources -> one defined/target
        // this needs given counter type
        if (sa.hasParam("ValidSource")) {
            CardCollectionView srcCards = game.getCardsIn(ZoneType.Battlefield);
            srcCards = CardLists.getValidCards(srcCards, sa.getParam("ValidSource"), player, host, sa);
            List<Card> tgtCards = getDefinedCardsOrTargeted(sa);

            if (tgtCards.isEmpty()) {
                return;
            }
            Card dest = tgtCards.get(0);

            Card cur = game.getCardState(dest, null);
            if (cur == null || !cur.equalsWithTimestamp(dest)) {
                // Test to see if the card we're trying to add is in the expected state
                return;
            }
            dest = cur;

            Map<String, Object> params = Maps.newHashMap();
            params.put("Target", dest);

            if ("All".equals(counterName)) {
                // only select cards if the counterNum is any
                if (counterNum.equals("Any")) {
                    srcCards = CardLists.filter(srcCards, CardPredicates.hasCounters());
                    srcCards = player.getController().chooseCardsForEffect(srcCards, sa,
                            Localizer.getInstance().getMessage("lblChooseTakeCountersCard", "any"), 0,
                            srcCards.size(), true, params);
                }
            } else {
                // target cant receive this counter type
                if (!dest.canReceiveCounters(cType)) {
                    return;
                }
                srcCards = CardLists.filter(srcCards, CardPredicates.hasCounter(cType));

                // only select cards if the counterNum is any
                if (counterNum.equals("Any")) {
                    params.put("CounterType", cType);
                    srcCards = player.getController().chooseCardsForEffect(srcCards, sa,
                            Localizer.getInstance().getMessage("lblChooseTakeCountersCard", cType.getName()), 0,
                            srcCards.size(), true, params);
                }
            }

            Map<CounterType, Integer> countersToAdd = Maps.newHashMap();

            for (Card src : srcCards) {
                // rule 121.5: If the first and second objects are the same object, nothing happens
                if (src.equals(dest)) {
                    continue;
                }

                if ("All".equals(counterName)) {
                    final Map<CounterType, Integer> tgtCounters = Maps.newHashMap(src.getCounters());
                    for (Map.Entry<CounterType, Integer> e : tgtCounters.entrySet()) {
                        removeCounter(sa, src, dest, e.getKey(), counterNum, countersToAdd);
                    }
                } else {
                    removeCounter(sa, src, dest, cType, counterNum, countersToAdd);
                }
            }
            for (Map.Entry<CounterType, Integer> e : countersToAdd.entrySet()) {
                dest.addCounter(e.getKey(), e.getValue(), player, sa, true, table);
            }

            game.updateLastStateForCard(dest);
            table.triggerCountersPutAll(game);
            return;
        } else if (sa.hasParam("ValidDefined")) {
            // one Source to many Targets
            // need given CounterType
            // currently used for Forgotten Ancient
            List<Card> srcCards = getDefinedCardsOrTargeted(sa, "Source");
            if (srcCards.isEmpty()) {
                return;
            }
            Card source = srcCards.get(0);

            if (source.getCounters(cType) <= 0) {
                return;
            }
            Map<String, Object> params = Maps.newHashMap();
            params.put("CounterType", cType);
            params.put("Source", source);

            CardCollectionView tgtCards = game.getCardsIn(ZoneType.Battlefield);
            tgtCards = CardLists.getValidCards(tgtCards, sa.getParam("ValidDefined"), player, host, sa);

            if (counterNum.equals("Any")) {
                tgtCards = player.getController().chooseCardsForEffect(
                        tgtCards, sa, Localizer.getInstance().getMessage("lblChooseCardToGetCountersFrom",
                                cType.getName(), CardTranslation.getTranslatedName(source.getName())),
                        0, tgtCards.size(), true, params);
            }

            boolean updateSource = false;

            for (final Card dest : tgtCards) {
                // rule 121.5: If the first and second objects are the same object, nothing happens
                if (source.equals(dest)) {
                    continue;
                }
                if (!dest.canReceiveCounters(cType)) {
                    continue;
                }

                Card cur = game.getCardState(dest, null);
                if (cur == null || !cur.equalsWithTimestamp(dest)) {
                    // Test to see if the card we're trying to add is in the expected state
                    continue;
                }

                params = Maps.newHashMap();
                params.put("CounterType", cType);
                params.put("Source", source);
                params.put("Target", cur);
                int cnum = player.getController().chooseNumber(sa,
                        Localizer.getInstance().getMessage("lblPutHowManyTargetCounterOnCard", cType.getName(),
                                CardTranslation.getTranslatedName(cur.getName())),
                        0, source.getCounters(cType), params);

                if (cnum > 0) {
                    source.subtractCounter(cType, cnum);
                    cur.addCounter(cType, cnum, player, sa, true, table);
                    game.updateLastStateForCard(cur);
                    updateSource = true;
                }
            }
            if (updateSource) {
                // update source
                game.updateLastStateForCard(source);
                table.triggerCountersPutAll(game);
            }
            return;
        } else {
            Card source = null;
            List<Card> tgtCards = getDefinedCardsOrTargeted(sa);
            // special logic for moving from Target to Target
            if (sa.usesTargeting() && sa.getMinTargets() == 2) {
                if (tgtCards.size() < 2) {
                    return;
                }
                source = tgtCards.remove(0);
            } else {
                List<Card> srcCards = getDefinedCardsOrTargeted(sa, "Source");
                if (srcCards.size() > 0) {
                    source = srcCards.get(0);
                }
            }
            if (source == null) {
                return;
            }

            // source doesn't has any counters to move
            if (!source.hasCounters()) {
                return;
            }

            for (final Card dest : tgtCards) {
                if (null != dest) {
                    // rule 121.5: If the first and second objects are the same object, nothing happens
                    if (source.equals(dest)) {
                        continue;
                    }
                    Card cur = game.getCardState(dest, null);
                    if (cur == null || !cur.equalsWithTimestamp(dest)) {
                        // Test to see if the card we're trying to add is in the expected state
                        continue;
                    }

                    Map<CounterType, Integer> countersToAdd = Maps.newHashMap();
                    if ("All".equals(counterName)) {
                        final Map<CounterType, Integer> tgtCounters = Maps.newHashMap(source.getCounters());
                        for (Map.Entry<CounterType, Integer> e : tgtCounters.entrySet()) {
                            removeCounter(sa, source, cur, e.getKey(), counterNum, countersToAdd);
                        }

                    } else if ("Any".equals(counterName)) {
                        // any counterType currently only Leech Bonder
                        final Map<CounterType, Integer> tgtCounters = source.getCounters();

                        final List<CounterType> typeChoices = Lists.newArrayList();
                        // get types of counters
                        for (CounterType ct : tgtCounters.keySet()) {
                            if (dest.canReceiveCounters(ct)) {
                                typeChoices.add(ct);
                            }
                        }
                        if (typeChoices.isEmpty()) {
                            return;
                        }

                        Map<String, Object> params = Maps.newHashMap();
                        params.put("Source", source);
                        params.put("Target", dest);
                        String title = Localizer.getInstance().getMessage("lblSelectRemoveCounterType");
                        CounterType chosenType = pc.chooseCounterType(typeChoices, sa, title, params);

                        removeCounter(sa, source, cur, chosenType, counterNum, countersToAdd);
                    } else {
                        if (!cur.canReceiveCounters(cType)) {
                            continue;
                        }

                        removeCounter(sa, source, cur, cType, counterNum, countersToAdd);
                    }

                    for (Map.Entry<CounterType, Integer> e : countersToAdd.entrySet()) {
                        cur.addCounter(e.getKey(), e.getValue(), player, sa, true, table);
                    }
                    game.updateLastStateForCard(cur);
                }
            }
            // update source
            game.updateLastStateForCard(source);
        }
        table.triggerCountersPutAll(game);
    } // moveCounterResolve

    protected void removeCounter(SpellAbility sa, final Card src, final Card dest, CounterType cType, String counterNum, Map<CounterType, Integer> countersToAdd) {
        final Card host = sa.getHostCard();
        //final String counterNum = sa.getParam("CounterNum");
        final Player player = sa.getActivatingPlayer();
        final PlayerController pc = player.getController();
        final Game game = host.getGame();

        // rule 121.5: If the first and second objects are the same object, nothing happens
        if (src.equals(dest)) {
            return;
        }

        if (!dest.canReceiveCounters(cType)) {
            return;
        }

        int cmax = src.getCounters(cType);
        if (cmax <= 0) {
            return;
        }

        int cnum = 0;
        if (counterNum.equals("All")) {
            cnum = cmax;
        } else if (counterNum.equals("Any")) {
            Map<String, Object> params = Maps.newHashMap();
            params.put("CounterType", cType);
            params.put("Source", src);
            params.put("Target", dest);
            cnum = pc.chooseNumber(
                    sa, Localizer.getInstance().getMessage("lblTakeHowManyTargetCounterFromCard",
                            cType.getName(), CardTranslation.getTranslatedName(src.getName())),
                    0, cmax, params);
        } else {
            cnum = Math.min(cmax, AbilityUtils.calculateAmount(host, counterNum, sa));
        }
        if (cnum > 0) {
            src.subtractCounter(cType, cnum);
            game.updateLastStateForCard(src);
            countersToAdd.put(cType, (countersToAdd.containsKey(cType) ? countersToAdd.get(cType) : 0) + cnum);
        }
    }
}
