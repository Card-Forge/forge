package forge.game.trigger;

import java.util.Map;

import forge.game.card.Card;
import forge.game.spellability.SpellAbility;

public class TriggerDamageDoneOnce extends Trigger {

    public TriggerDamageDoneOnce(Map<String, String> params, Card host, boolean intrinsic) {
        super(params, host, intrinsic);

    }

    @Override
    public boolean performTest(Map<String, Object> runParams2) {
        final Object tgt = runParams2.get("DamageTarget");
        if (this.mapParams.containsKey("ValidTarget")) {
            if (!matchesValid(tgt, this.mapParams.get("ValidTarget").split(","), this.getHostCard())) {
                return false;
            }
        }
        

        if (this.mapParams.containsKey("CombatDamage")) {
            if (this.mapParams.get("CombatDamage").equals("True")) {
                if (!((Boolean) runParams2.get("IsCombatDamage"))) {
                    return false;
                }
            } else if (this.mapParams.get("CombatDamage").equals("False")) {
                if (((Boolean) runParams2.get("IsCombatDamage"))) {
                    return false;
                }
            }
        }
        
        return true;
    }

    @Override
    public void setTriggeringObjects(SpellAbility sa) {

        if (this.getRunParams().containsKey("DamageTarget")) {
            sa.setTriggeringObject("Target", this.getRunParams().get("DamageTarget"));
        }
        sa.setTriggeringObject("DamageAmount", this.getRunParams().get("DamageAmount"));
        

    }

    @Override
    public String getImportantStackObjects(SpellAbility sa) {
        StringBuilder sb = new StringBuilder();
        if (sa.getTriggeringObject("Target") != null) {
            sb.append("Damaged: ").append(sa.getTriggeringObject("Target")).append(", ");
        }
        sb.append("Amount: ").append(sa.getTriggeringObject("DamageAmount"));
        return sb.toString();
    }

}
