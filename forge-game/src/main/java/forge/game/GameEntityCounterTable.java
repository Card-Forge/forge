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
        Integer old = contains(rowKey, columnKey) ? get(rowKey, columnKey) : 0;
        return super.put(rowKey, columnKey, old + value);
    }

    public Map<GameEntity, Integer> filterTable(CounterType type, String valid, Card host, SpellAbility sa) {
        Map<GameEntity, Integer> result = Maps.newHashMap();

        for (Map.Entry<GameEntity, Integer> e : column(type).entrySet()) {
            if (e.getKey().isValid(valid, host.getController(), host, sa)) {
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
