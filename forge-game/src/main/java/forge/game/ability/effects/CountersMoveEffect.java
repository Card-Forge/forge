package forge.game.ability.effects;

import forge.game.Game;
import forge.game.GameEntityCounterTable;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CounterType;
import forge.game.player.Player;
import forge.game.player.PlayerController;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import forge.util.TextUtil;

public class CountersMoveEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();

        Card source = null;
        List<Card> srcCards = getDefinedCardsOrTargeted(sa, "Source");
        
        if (srcCards.size() > 0) {
            source = srcCards.get(0);
        }
        final List<Card> tgtCards = getDefinedCardsOrTargeted(sa);
        final String countername = sa.getParam("CounterType");
        final int amount = AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("CounterNum"), sa);

        sb.append("Move ");
        if ("Any".matches(countername)) {
            if (amount == 1) {
                sb.append("a counter");
            } else {
                sb.append(amount).append(" ").append(" counter");
            }
        } else {   
            sb.append(amount).append(" ").append(countername).append(" counter");
        }
        if (amount != 1) {
            sb.append("s");
        }
        sb.append(" from ").append(source).append(" to ");
        try{
            sb.append(tgtCards.get(0));
        } catch(final IndexOutOfBoundsException exception) {
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
        try {
            cType = AbilityUtils.getCounterType(counterName, sa);
        } catch (Exception e) {
            if (!counterName.matches("Any")) {
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

            // target cant receive this counter type
            if (!dest.canReceiveCounters(cType)) {
                return;
            }

            Card cur = game.getCardState(dest, null);
            if (cur == null || !cur.equalsWithTimestamp(dest)) {
                // Test to see if the card we're trying to add is in the expected state
                return;
            }
            dest = cur;

            int csum = 0;

            // only select cards if the counterNum is any
            if (counterNum.equals("Any")) {
                StringBuilder sb = new StringBuilder();
                sb.append("Choose cards to take ").append(cType.getName()).append(" counters from");

                srcCards = player.getController().chooseCardsForEffect(srcCards, sa, sb.toString(), 0, srcCards.size(), true);
            }

            for (Card src : srcCards) {
                // rule 121.5: If the first and second objects are the same object, nothing happens
                if (src.equals(dest)) {
                    continue;
                }

                int cmax = src.getCounters(cType);
                if (cmax <= 0) {
                    continue;
                }

                int cnum = 0;
                if (counterNum.equals("All")) {
                    cnum = cmax;
                } else if (counterNum.equals("Any")) {
                    Map<String, Object> params = Maps.newHashMap();
                    params.put("CounterType", cType);
                    params.put("Source", src);
                    params.put("Target", dest);
                    StringBuilder sb = new StringBuilder();
                    sb.append("Take how many ").append(cType.getName());
                    sb.append(" counters from ").append(src).append("?");
                    cnum = player.getController().chooseNumber(sa, sb.toString(), 0, cmax, params);
                } else {
                    cnum = AbilityUtils.calculateAmount(host, counterNum, sa);
                }
                if(cnum > 0) {
                    src.subtractCounter(cType, cnum);
                    game.updateLastStateForCard(src);
                    csum += cnum;
                }
            }

            if (csum > 0) {
                dest.addCounter(cType, csum, player, true, table);
                game.updateLastStateForCard(dest);
                table.triggerCountersPutAll(game);
            }
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
            CardCollectionView tgtCards = game.getCardsIn(ZoneType.Battlefield);
            tgtCards = CardLists.getValidCards(tgtCards, sa.getParam("ValidDefined"), player, host, sa);

            if (counterNum.equals("Any")) {
                StringBuilder sb = new StringBuilder();
                sb.append("Choose cards to get ").append(cType.getName());
                sb.append(" counters from ").append(source).append(".");

                tgtCards = player.getController().chooseCardsForEffect(
                        tgtCards, sa, sb.toString(), 0, tgtCards.size(), true);
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

                Map<String, Object> params = Maps.newHashMap();
                params.put("CounterType", cType);
                params.put("Source", source);
                params.put("Target", cur);
                StringBuilder sb = new StringBuilder();
                sb.append("Put how many ").append(cType.getName()).append(" counters on ").append(cur).append("?");
                int cnum = player.getController().chooseNumber(sa, sb.toString(), 0, source.getCounters(cType), params);

                if (cnum > 0) {
                    source.subtractCounter(cType, cnum);
                    cur.addCounter(cType, cnum, player, true, table);
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
        }

        Card source = null;
        int cntToMove = 0;
        List<Card> srcCards = getDefinedCardsOrTargeted(sa, "Source");
        if (srcCards.size() > 0) {
            source = srcCards.get(0);
        }

        // source doesn't has any counters to move
        if (!source.hasCounters()) {
            return;
        }

        if (!counterNum.equals("All") && !counterNum.equals("Any")) {
            cntToMove = AbilityUtils.calculateAmount(host, counterNum, sa);
        } else {
            cntToMove = source.getCounters(cType);
        }
        List<Card> tgtCards = getDefinedCardsOrTargeted(sa);

        for (final Card dest : tgtCards) {
            if (null != source && null != dest) {
                // rule 121.5: If the first and second objects are the same object, nothing happens
                if (source.equals(dest)) {
                    continue;
                }
                Card cur = game.getCardState(dest, null);
                if (cur == null || !cur.equalsWithTimestamp(dest)) {
                    // Test to see if the card we're trying to add is in the expected state
                    continue;
                }

                if (!"Any".matches(counterName)) {
                    if (!cur.canReceiveCounters(cType)) {
                        continue;
                    }

                    if (counterNum.equals("Any")) {
                        Map<String, Object> params = Maps.newHashMap();
                        params.put("CounterType", cType);
                        params.put("Source", source);
                        params.put("Target", cur);
                        StringBuilder sb = new StringBuilder();
                        sb.append("Take how many ").append(cType.getName());
                        sb.append(" counters from ").append(source).append("?");
                        cntToMove = pc.chooseNumber(sa, sb.toString(), 0, cntToMove, params);
                    }

                    if (source.getCounters(cType) >= cntToMove) {
                        source.subtractCounter(cType, cntToMove);
                        cur.addCounter(cType, cntToMove, player, true, table);
                        game.updateLastStateForCard(cur);
                    }
                } else {
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
                    String title = "Select type counters to remove";
                    CounterType chosenType = pc.chooseCounterType(typeChoices, sa, title, params);

                    params = Maps.newHashMap();
                    params.put("CounterType", chosenType);
                    params.put("Source", source);
                    params.put("Target", dest);
                    StringBuilder sb = new StringBuilder();
                    sb.append("Take how many ").append(chosenType.getName()).append(" counters?");
                    int chosenAmount = pc.chooseNumber(
                            sa, sb.toString(), 0, Math.min(tgtCounters.get(chosenType), cntToMove), params);

                    if (chosenAmount > 0) {
                        dest.addCounter(chosenType, chosenAmount, player, true, table);
                        source.subtractCounter(chosenType, chosenAmount);
                        game.updateLastStateForCard(dest);
                        cntToMove -= chosenAmount;
                    }
                }
            }
        }
        // update source
        game.updateLastStateForCard(source);
        table.triggerCountersPutAll(game);
    } // moveCounterResolve
}
