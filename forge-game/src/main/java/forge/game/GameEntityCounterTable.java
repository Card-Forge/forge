package forge.game;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.common.collect.ForwardingTable;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import com.google.common.collect.Table;

import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.card.CounterType;
import forge.game.player.Player;
import forge.game.replacement.ReplacementType;
import forge.game.spellability.AbilityStatic;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;

public class GameEntityCounterTable extends ForwardingTable<Optional<Player>, GameEntity, Multiset<CounterType>> {

    private Table<Optional<Player>, GameEntity, Multiset<CounterType>> dataMap = HashBasedTable.create();

    public GameEntityCounterTable() {
    }

    public GameEntityCounterTable(Table<Optional<Player>, GameEntity, Multiset<CounterType>> counterTable) {
        putAll(counterTable);
    }

    /*
     * (non-Javadoc)
     * @see com.google.common.collect.ForwardingTable#delegate()
     */
    @Override
    protected Table<Optional<Player>, GameEntity, Multiset<CounterType>> delegate() {
        return dataMap;
    }

    public int put(Player putter, GameEntity object, CounterType type, int value) {
        Optional<Player> o = Optional.ofNullable(putter);
        Multiset<CounterType> map = get(o, object);
        if (map == null) {
            map = HashMultiset.create();
            put(o, object, map);
        }
        if (value > 0) {
            return map.add(type, value);
        } else {
            return map.remove(type, -value);
        }
    }

    public int get(Player putter, GameEntity object, CounterType type) {
        Optional<Player> o = Optional.ofNullable(putter);
        Multiset<CounterType> map = get(o, object);
        if (map == null) {
            return 0;
        }
        return map.count(type);
    }

    public int totalValues() {
        return values().stream().collect(Collectors.summingInt(Multiset::size));
    }

    /*
     * returns the counters that can still be removed from game entity
     */
    public Multiset<CounterType> filterToRemove(GameEntity ge) {
        if (!containsColumn(ge)) {
            return HashMultiset.create(ge.getCounters());
        }
        Multiset<CounterType> alreadyRemoved = column(ge).get(Optional.<Player>empty());
        return HashMultiset.create(Multisets.difference(ge.getCounters(), alreadyRemoved));
    }

    public Map<GameEntity, Integer> filterTable(CounterType type, String valid, String validSource, Card host, CardTraitBase sa) {
        Map<GameEntity, Integer> result = columnMap().entrySet().stream().filter(gm -> gm.getKey().isValid(valid, host.getController(), host, sa))
            .collect(Collectors.groupingBy(gm -> gm.getKey(),
                            Collectors.summingInt(gm -> gm.getValue().entrySet().stream().
                                    filter(e -> validSource == null || (e.getKey().isPresent() && e.getKey().get().isValid(validSource, host.getController(), host, sa))).
                                    mapToInt(e -> type == null ? e.getValue().size() : e.getValue().count(type)).sum())));
        // entities that only received counters of other types must not count as matches
        result.values().removeIf(v -> v == 0);
        return result;
    }

    public void triggerCountersPutAll(final Game game) {
        if (isEmpty()) {
            return;
        }
        for (Cell<Optional<Player>, GameEntity, Multiset<CounterType>> c : cellSet()) {
            if (c.getValue().isEmpty()) {
                continue;
            }
            final Map<AbilityKey, Object> runParams = AbilityKey.newMap();
            runParams.put(AbilityKey.Source, c.getRowKey().get());
            runParams.put(AbilityKey.Object, c.getColumnKey());
            runParams.put(AbilityKey.CounterMap, c.getValue());
            game.getTriggerHandler().runTrigger(TriggerType.CounterPlayerAddedAll, runParams, false);
        }
        final Map<AbilityKey, Object> runParams = AbilityKey.newMap();
        runParams.put(AbilityKey.Objects, this);
        game.getTriggerHandler().runTrigger(TriggerType.CounterAddedAll, runParams, false);
    }

    public void replaceCounterEffect(final Game game, final SpellAbility cause) {
        replaceCounterEffect(game, cause, cause != null && !(cause instanceof AbilityStatic), false, null);
    }
    @SuppressWarnings("unchecked")
    public boolean replaceCounterEffect(final Game game, final SpellAbility cause, final boolean effect, final boolean etb, Map<AbilityKey, Object> params) {
        if (isEmpty()) {
            return false;
        }
        GameEntityCounterTable result = new GameEntityCounterTable();
        for (Map.Entry<GameEntity, Map<Optional<Player>, Multiset<CounterType>>> gm : columnMap().entrySet()) {
            Map<Optional<Player>, Multiset<CounterType>> values = gm.getValue();

            // ETB Counters are already handled in the Move Event
            if (!etb) {
                final Map<AbilityKey, Object> repParams = AbilityKey.mapFromAffected(gm.getKey());
                repParams.put(AbilityKey.Cause, cause);
                repParams.put(AbilityKey.EffectOnly, effect);
                repParams.put(AbilityKey.CounterMap, values);
                repParams.put(AbilityKey.ETB, etb);
                if (params != null) {
                    repParams.putAll(params);
                }

                switch (game.getReplacementHandler().run(ReplacementType.AddCounter, repParams)) {
                case NotReplaced:
                    break;
                case Updated: {
                    values = (Map<Optional<Player>, Multiset<CounterType>>) repParams.get(AbilityKey.CounterMap);
                    break;
                }
                default:
                    continue;
                }
            }

            // Add ETB flag
            Map<AbilityKey, Object> runParams = AbilityKey.newMap();
            runParams.put(AbilityKey.Cause, cause);
            if (params != null) {
                runParams.putAll(params);
            }

            boolean firstTime = false;
            if (gm.getKey() instanceof Card c) {
                firstTime = game.getCounterAddedThisTurn(null, c) == 0;
            }

            // Apply counter after replacement effect
            for (Map.Entry<Optional<Player>, Multiset<CounterType>> e : values.entrySet()) {
                boolean remember = cause != null && cause.hasParam("RememberPut");
                for (Multiset.Entry<CounterType> ec : e.getValue().entrySet()) {
                    int value = ec.getCount();
                    if (cause != null && cause.hasParam("MaxFromEffect")) {
                        value = Math.min(value, Integer.parseInt(cause.getParam("MaxFromEffect")) - gm.getKey().getCounters(ec.getElement()));
                    }
                    gm.getKey().addCounterInternal(ec.getElement(), value, e.getKey().orElse(null), true, result, runParams);
                    if (remember && value > 0) {
                        cause.getHostCard().addRemembered(gm.getKey());
                    }
                }
            }

            if (result.containsColumn(gm.getKey())) {
                runParams = AbilityKey.newMap();
                runParams.put(AbilityKey.Object, gm.getKey());
                runParams.put(AbilityKey.FirstTime, firstTime);
                game.getTriggerHandler().runTrigger(TriggerType.CounterTypeAddedAll, runParams, false);
            }
        }

        int totalAdded = totalValues();
        if (totalAdded > 0 && cause != null && cause.hasParam("RememberAmount")) {
            cause.getHostCard().addRemembered(totalAdded);
        }

        result.triggerCountersPutAll(game);
        return !result.isEmpty();
    }
}
