package forge.game.trigger;

import java.util.Map;

import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.card.CardDamageMap;
import forge.game.spellability.SpellAbility;
import forge.util.Localizer;

public class TriggerDamageAll extends Trigger {

    public TriggerDamageAll(Map<String, String> params, Card host, boolean intrinsic) {
        super(params, host, intrinsic);
    }

    @Override
    public boolean performTest(Map<AbilityKey, Object> runParams) {

        if (hasParam("CombatDamage")) {
            if (getParam("CombatDamage").equals("True")) {
                if (!((Boolean) runParams.get(AbilityKey.IsCombatDamage))) {
                    return false;
                }
            } else if (getParam("CombatDamage").equals("False")) {
                if (((Boolean) runParams.get(AbilityKey.IsCombatDamage))) {
                    return false;
                }
            }
        }
        final CardDamageMap table = (CardDamageMap) runParams.get(AbilityKey.DamageMap);
        return filterTable(table) > 0;
    }

    @Override
    public void setTriggeringObjects(SpellAbility sa, Map<AbilityKey, Object> runParams) {
        final CardDamageMap table = (CardDamageMap) runParams.get(AbilityKey.DamageMap);

        sa.setTriggeringObject(AbilityKey.DamageAmount, filterTable(table));
    }

    @Override
    public String getImportantStackObjects(SpellAbility sa) {
        StringBuilder sb = new StringBuilder();
        sb.append(Localizer.getInstance().getMessage("lblAmount")).append(": ").append(sa.getTriggeringObject(AbilityKey.DamageAmount));
        return sb.toString();
    }

    private int filterTable(CardDamageMap table) {
        return table.filteredAmount(getParam("ValidSource"), getParam("ValidTarget"), getHostCard(), null);
    }
}
