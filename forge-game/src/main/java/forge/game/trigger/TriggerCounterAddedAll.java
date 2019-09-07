package forge.game.trigger;

import java.util.Map;

import com.google.common.collect.Lists;

import forge.game.GameEntity;
import forge.game.GameEntityCounterTable;
import forge.game.card.*;
import forge.game.spellability.SpellAbility;

public class TriggerCounterAddedAll extends Trigger {

    public TriggerCounterAddedAll(Map<String, String> params, Card host, boolean intrinsic) {
        super(params, host, intrinsic);
    }

    @Override
    public boolean performTest(Map<String, Object> runParams2) {
        final GameEntityCounterTable table = (GameEntityCounterTable) runParams2.get("Objects");

        return !filterTable(table).isEmpty();
    }

    @Override
    public void setTriggeringObjects(SpellAbility sa) {
        final GameEntityCounterTable table = (GameEntityCounterTable) getRunParams().get("Objects");

        Map<GameEntity, Integer> all = this.filterTable(table);

        int amount = 0;
        for (final Integer v : all.values()) {
            amount += v;
        }

        sa.setTriggeringObject("Objects", Lists.newArrayList(all.keySet()));
        sa.setTriggeringObject("Amount", amount);
    }

    @Override
    public String getImportantStackObjects(SpellAbility sa) {
        StringBuilder sb = new StringBuilder();
        sb.append("Amount: ").append(sa.getTriggeringObject("Amount"));
        return sb.toString();
    }

    private Map<GameEntity, Integer> filterTable(GameEntityCounterTable table) {
        CounterType counterType = CounterType.getType(getParam("CounterType"));
        String valid = getParam("Valid");

        return table.filterTable(counterType, valid, getHostCard(), null);
    }
}
