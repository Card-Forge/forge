package forge.game.trigger;

import java.util.Map;
import java.util.Set;

import forge.game.GameEntity;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;

public class TriggerDamageDoneOnce extends Trigger {

    public TriggerDamageDoneOnce(Map<String, String> params, Card host, boolean intrinsic) {
        super(params, host, intrinsic);

    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean performTest(Map<String, Object> runParams2) {
        final Set<Card> srcs = (Set<Card>) runParams2.get("DamageSources");
        final GameEntity tgt = (GameEntity) runParams2.get("DamageTarget");

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
        
        if (this.mapParams.containsKey("ValidSource")) {
            boolean valid = false;
            for (Card c : srcs) {
                if (c.isValid(this.mapParams.get("ValidSource").split(","), this.getHostCard().getController(),this.getHostCard(), null)) {
                    valid = true;
                }
            }
            if (!valid) {
                return false;
            }
        }
        
        if (this.mapParams.containsKey("ValidTarget")) {
            if (!matchesValid(tgt, this.mapParams.get("ValidTarget").split(","), this.getHostCard())) {
                return false;
            }
        }
        

        
        return true;
    }

    @Override
    public void setTriggeringObjects(SpellAbility sa) {
        if (this.getRunParams().containsKey("DamageTarget")) {
            sa.setTriggeringObject("Target", this.getRunParams().get("DamageTarget"));
        }
        if (this.getRunParams().containsKey("DamageSources")) {
            sa.setTriggeringObject("Sources", this.getRunParams().get("DamageSources"));
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
