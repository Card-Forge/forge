package forge.game;

import java.util.Map;

import com.google.common.collect.ForwardingTable;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;

import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.card.CounterType;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;

public class GameEntityCounterTable extends ForwardingTable<GameEntity, CounterType, Integer> {

    private Table<GameEntity, CounterType, Integer> dataMap = HashBasedTable.create();

    /*
     * (non-Javadoc)
     * @see com.google.common.collect.ForwardingTable#delegate()
     */
    @Override
    protected Table<GameEntity, CounterType, Integer> delegate() {
        return dataMap;
    }

    /* (non-Javadoc)
     * @see com.google.common.collect.ForwardingTable#put(java.lang.Object, java.lang.Object, java.lang.Object)
     */
    @Override
    public Integer put(GameEntity rowKey, CounterType columnKey, Integer value) {
        return super.put(rowKey, columnKey, get(rowKey, columnKey) + value);
    }


    @Override
    public Integer get(Object rowKey, Object columnKey) {
        if (!contains(rowKey, columnKey)) {
            return 0; // helper to not return null value
        }
        return super.get(rowKey, columnKey);
    }


    /*
     * returns the counters that can still be removed from game entity
     */
    public Map<CounterType, Integer> filterToRemove(GameEntity ge) {
        Map<CounterType, Integer> result = Maps.newHashMap();
        if (!containsRow(ge)) {
            result.putAll(ge.getCounters());
            return result;
        }
        Map<CounterType, Integer> alreadyRemoved = row(ge);
        for (Map.Entry<CounterType, Integer> e : ge.getCounters().entrySet()) {
            Integer rest = e.getValue() - (alreadyRemoved.containsKey(e.getKey()) ? alreadyRemoved.get(e.getKey()) : 0);
            if (rest > 0) {
                result.put(e.getKey(), rest);
            }
        }
        return result;
    }

    public Map<GameEntity, Integer> filterTable(CounterType type, String valid, Card host, SpellAbility sa) {
        Map<GameEntity, Integer> result = Maps.newHashMap();

        for (Map.Entry<GameEntity, Integer> e : column(type).entrySet()) {
            if (e.getValue() > 0 && e.getKey().isValid(valid, host.getController(), host, sa)) {
                result.put(e.getKey(), e.getValue());
            }
        }
        return result;
    }

    public void triggerCountersPutAll(final Game game) {
        if (!isEmpty()) {
            final Map<AbilityKey, Object> runParams = AbilityKey.newMap();
            runParams.put(AbilityKey.Objects, this);
            game.getTriggerHandler().runTrigger(TriggerType.CounterAddedAll, runParams, false);
        }
    }
}
