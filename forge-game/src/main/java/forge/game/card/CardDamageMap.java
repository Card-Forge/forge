/**
 * 
 */
package forge.game.card;

import java.util.Map;

import com.google.common.collect.ForwardingTable;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;

import forge.game.GameEntity;
import forge.game.trigger.TriggerType;

public class CardDamageMap extends ForwardingTable<Card, GameEntity, Integer> {
    private Table<Card, GameEntity, Integer> dataMap = HashBasedTable.create();

    // common function to gain life for lifelink
    public void dealLifelinkDamage() {
        for (Map.Entry<Card, Map<GameEntity, Integer>> e : this.rowMap().entrySet()) {
            final Card sourceLKI = e.getKey();
            int damageSum = 0;
            for (final Integer i : e.getValue().values()) {
                damageSum += i;
            }
            if (damageSum > 0 && sourceLKI.hasKeyword("Lifelink")) {
                sourceLKI.getController().gainLife(damageSum, sourceLKI);
            }
        }
    }   
    
    public void triggerPreventDamage(boolean isCombat) {
        for (Map.Entry<GameEntity, Map<Card, Integer>> e : this.columnMap().entrySet()) {
            int sum = 0;
            for (final int i : e.getValue().values()) {
                sum += i;
            }
            if (sum > 0) {
                final GameEntity ge = e.getKey();
                final Map<String, Object> runParams = Maps.newHashMap();
                runParams.put("DamageTarget", ge);
                runParams.put("DamageAmount", sum);
                runParams.put("IsCombatDamage", isCombat);
                
                ge.getGame().getTriggerHandler().runTrigger(TriggerType.DamagePreventedOnce, runParams, false);
            }
        }
    }

    /**
     * special put logic, sum the values
     */
    @Override
    public Integer put(Card rowKey, GameEntity columnKey, Integer value) {
        Integer old = contains(rowKey, columnKey) ? get(rowKey, columnKey) : 0;
        return dataMap.put(rowKey, columnKey, value + old);
    }

    @Override
    protected Table<Card, GameEntity, Integer> delegate() {
        return dataMap;
    }

}
