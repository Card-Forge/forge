/**
 * 
 */
package forge.game.card;

import java.util.Map;

import com.google.common.collect.ForwardingTable;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;

import forge.game.Game;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;

public class CardZoneTable extends ForwardingTable<ZoneType, ZoneType, CardCollection> {
    // TODO use EnumBasedTable if exist
    private Table<ZoneType, ZoneType, CardCollection> dataMap = HashBasedTable.create();

    /**
     * special put logic, add Card to Card Collection
     */
    public CardCollection put(ZoneType rowKey, ZoneType columnKey, Card value) {
        CardCollection old;
        if (contains(rowKey, columnKey)) {
            old = get(rowKey, columnKey);
            old.add(value);
        } else {
            old = new CardCollection(value);
            dataMap.put(rowKey, columnKey, old);
        }
        return old;
    }

    @Override
    protected Table<ZoneType, ZoneType, CardCollection> delegate() {
        return dataMap;
    }

    public void triggerChangesZoneAll(final Game game) {
        if (!isEmpty()) {
            final Map<String, Object> runParams = Maps.newHashMap();
            runParams.put("Cards", this);
            game.getTriggerHandler().runTrigger(TriggerType.ChangesZoneAll, runParams, false);
        }
    }
}
