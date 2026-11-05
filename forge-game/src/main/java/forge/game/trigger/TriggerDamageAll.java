package forge.game.trigger;

import java.util.Map;

import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.card.CardDamageTable;
import forge.game.spellability.SpellAbility;
import forge.util.Localizer;

public class TriggerDamageAll extends Trigger {

    public TriggerDamageAll(Map<String, String> params, Card host, boolean intrinsic) {
        super(params, host, intrinsic);
    }

    @Override
    public boolean performTest(Map<AbilityKey, Object> runParams) {
        if (hasParam("CombatDamage")) {
            if (getParam("CombatDamage").equals("True") != (Boolean) runParams.get(AbilityKey.IsCombatDamage)) {
                return false;
            }
        }
        final CardDamageTable table = (CardDamageTable) runParams.get(AbilityKey.DamageMap);
        return !table.filteredMap(getParam("ValidSource"), getParam("ValidTarget"), getHostCard(), this).isEmpty();
    }

    @Override
    public void setTriggeringObjects(Map<AbilityKey, Object> objects, Map<AbilityKey, Object> runParams) {
        CardDamageTable table = (CardDamageTable) runParams.get(AbilityKey.DamageMap);
        table = table.filteredMap(getParam("ValidSource"), getParam("ValidTarget"), getHostCard(), this);

        objects.put(AbilityKey.DamageAmount, table.totalAmount());
        objects.put(AbilityKey.Sources, table.rowKeySet());
        objects.put(AbilityKey.Targets, table.columnKeySet());
    }

    @Override
    public String getImportantStackObjects(SpellAbility sa) {
        StringBuilder sb = new StringBuilder();
        sb.append(Localizer.getInstance().getMessage("lblDamageSource")).append(": ").append(sa.getTriggeringObject(AbilityKey.Sources)).append(", ");
        sb.append(Localizer.getInstance().getMessage("lblDamaged")).append(": ").append(sa.getTriggeringObject(AbilityKey.Targets)).append(", ");
        sb.append(Localizer.getInstance().getMessage("lblAmount")).append(": ").append(sa.getTriggeringObject(AbilityKey.DamageAmount));
        return sb.toString();
    }
}
