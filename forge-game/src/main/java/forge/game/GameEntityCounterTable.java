package forge.game;

import java.util.Map;

import org.apache.commons.lang3.ObjectUtils;

import com.google.common.base.Optional;
import com.google.common.collect.ForwardingTable;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;

import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.card.CounterType;
import forge.game.player.Player;
import forge.game.replacement.ReplacementType;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;

public class GameEntityCounterTable extends ForwardingTable<Optional<Player>, GameEntity, Map<CounterType, Integer>> {

    private Table<Optional<Player>, GameEntity, Map<CounterType, Integer>> dataMap = HashBasedTable.create();

    public GameEntityCounterTable() {
    }

    public GameEntityCounterTable(Table<Optional<Player>, GameEntity, Map<CounterType, Integer>> counterTable) {
        putAll(counterTable);
    }

    /*
     * (non-Javadoc)
     * @see com.google.common.collect.ForwardingTable#delegate()
     */
    @Override
    protected Table<Optional<Player>, GameEntity, Map<CounterType, Integer>> delegate() {
        return dataMap;
    }

    public Integer put(Player putter, GameEntity object, CounterType type, Integer value) {
        Optional<Player> o = Optional.fromNullable(putter);
        Map<CounterType, Integer> map = get(o, object);
        if (map == null) {
            map = Maps.newHashMap();
            put(o, object, map);
        }
        return map.put(type, ObjectUtils.firstNonNull(map.get(type), 0) + value);
    }

    public int get(Player putter, GameEntity object, CounterType type) {
        Optional<Player> o = Optional.fromNullable(putter);
        Map<CounterType, Integer> map = get(o, object);
        if (map == null || !map.containsKey(type)) {
            return 0;
        }
        return ObjectUtils.firstNonNull(map.get(type), 0);
    }

    public int totalValues() {
        int result = 0;
        for (Map<CounterType, Integer> m : values()) {
            for (Integer i : m.values()) {
                result += i;
            }
        }
        return result;
    }

    /*
     * returns the counters that can still be removed from game entity
     */
    public Map<CounterType, Integer> filterToRemove(GameEntity ge) {
        Map<CounterType, Integer> result = Maps.newHashMap();
        if (!containsColumn(ge)) {
            result.putAll(ge.getCounters());
            return result;
        }
        Map<CounterType, Integer> alreadyRemoved = column(ge).get(Optional.absent());
        for (Map.Entry<CounterType, Integer> e : ge.getCounters().entrySet()) {
            int rest = e.getValue() - (alreadyRemoved.getOrDefault(e.getKey(), 0));
            if (rest > 0) {
                result.put(e.getKey(), rest);
            }
        }
        return result;
    }

    public Map<GameEntity, Integer> filterTable(CounterType type, String valid, Card host, CardTraitBase sa) {
        Map<GameEntity, Integer> result = Maps.newHashMap();

        for (Map.Entry<GameEntity, Map<Optional<Player>, Map<CounterType, Integer>>> gm : columnMap().entrySet()) {
            if (gm.getKey().isValid(valid, host.getController(), host, sa)) {
                for (Map<CounterType, Integer> cm : gm.getValue().values()) {
                    Integer old = ObjectUtils.firstNonNull(result.get(gm.getKey()), 0);
                    Integer v = ObjectUtils.firstNonNull(cm.get(type), 0);
                    if (old + v > 0) {
                        result.put(gm.getKey(), old + v);
                    }
                }
            }
        }
        return result;
    }

    public void triggerCountersPutAll(final Game game) {
        if (isEmpty()) {
            return;
        }
        for (Cell<Optional<Player>, GameEntity, Map<CounterType, Integer>> c : cellSet()) {
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

    public void replaceCounterEffect(final Game game, final SpellAbility cause, final boolean effect) {
        replaceCounterEffect(game, cause, effect, false, null);
    }

    @SuppressWarnings("unchecked")
    public boolean replaceCounterEffect(final Game game, final SpellAbility cause, final boolean effect, final boolean etb, Map<AbilityKey, Object> params) {
        if (isEmpty()) {
            return false;
        }
        GameEntityCounterTable result = new GameEntityCounterTable();
        for (Map.Entry<GameEntity, Map<Optional<Player>, Map<CounterType, Integer>>> gm : columnMap().entrySet()) {
            Map<Optional<Player>, Map<CounterType, Integer>> values = gm.getValue();

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
                    values = (Map<Optional<Player>, Map<CounterType, Integer>>) repParams.get(AbilityKey.CounterMap);
                    break;
                }
                default:
                    continue;
                }
            }

            // Add ETB flag
            final Map<AbilityKey, Object> runParams = AbilityKey.newMap();
            runParams.put(AbilityKey.Cause, cause);
            if (params != null) {
                runParams.putAll(params);
            }

            // Apply counter after replacement effect
            for (Map.Entry<Optional<Player>, Map<CounterType, Integer>> e : values.entrySet()) {
                boolean remember = cause != null && cause.hasParam("RememberPut");
                for (Map.Entry<CounterType, Integer> ec : e.getValue().entrySet()) {
                    Integer value = ec.getValue();
                    if (value == null) {
                        continue;
                    }
                    if (cause != null && cause.hasParam("MaxFromEffect")) {
                        value = Math.min(value, Integer.parseInt(cause.getParam("MaxFromEffect")) - gm.getKey().getCounters(ec.getKey()));
                    }
                    gm.getKey().addCounterInternal(ec.getKey(), value, e.getKey().orNull(), true, result, runParams);
                    if (remember && ec.getValue() >= 1) {
                        cause.getHostCard().addRemembered(gm.getKey());
                    }
                }
            }
        }
        result.triggerCountersPutAll(game);
        return !result.isEmpty();
    }
}
