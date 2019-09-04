/**
 * 
 */
package forge.game.card;

import java.util.Map;

import com.google.common.collect.ForwardingTable;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;

import forge.game.GameEntity;
import forge.game.keyword.Keyword;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;

public class CardDamageMap extends ForwardingTable<Card, GameEntity, Integer> {
    private Table<Card, GameEntity, Integer> dataMap = HashBasedTable.create();
    
    public CardDamageMap(Table<Card, GameEntity, Integer> damageMap) {
        this.putAll(damageMap);
    }

    public CardDamageMap() {
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
                
                ge.getGame().getTriggerHandler().runTriggerOld(TriggerType.DamagePreventedOnce, runParams, false);
            }
        }
    }

    public void triggerDamageDoneOnce(boolean isCombat, final SpellAbility sa) {
        // Source -> Targets
        for (Map.Entry<Card, Map<GameEntity, Integer>> e : this.rowMap().entrySet()) {
            final Card sourceLKI = e.getKey();
            int sum = 0;
            for (final Integer i : e.getValue().values()) {
                sum += i;
            }
            if (sum > 0) {
                final Map<String, Object> runParams = Maps.newHashMap();
                runParams.put("DamageSource", sourceLKI);
                runParams.put("DamageTargets", Sets.newHashSet(e.getValue().keySet()));
                runParams.put("DamageAmount", sum);
                runParams.put("IsCombatDamage", isCombat);
                
                sourceLKI.getGame().getTriggerHandler().runTriggerOld(TriggerType.DamageDealtOnce, runParams, false);
                
                if (sourceLKI.hasKeyword(Keyword.LIFELINK)) {
                    sourceLKI.getController().gainLife(sum, sourceLKI, sa);
                }
            }
        }
        // Targets -> Source
        for (Map.Entry<GameEntity, Map<Card, Integer>> e : this.columnMap().entrySet()) {
            int sum = 0;
            for (final int i : e.getValue().values()) {
                sum += i;
            }
            if (sum > 0) {
                final GameEntity ge = e.getKey();
                final Map<String, Object> runParams = Maps.newHashMap();
                runParams.put("DamageTarget", ge);
                runParams.put("DamageSources", Sets.newHashSet(e.getValue().keySet()));
                runParams.put("DamageAmount", sum);
                runParams.put("IsCombatDamage", isCombat);
                
                ge.getGame().getTriggerHandler().runTriggerOld(TriggerType.DamageDoneOnce, runParams, false);
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
