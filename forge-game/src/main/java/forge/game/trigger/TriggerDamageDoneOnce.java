package forge.game.trigger;

import java.util.Map;
import java.util.Set;

import forge.game.GameEntity;
import forge.game.ability.AbilityKey;
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
        sa.setTriggeringObject(AbilityKey.Target, getFromRunParams(AbilityKey.DamageTarget));
        sa.setTriggeringObject(AbilityKey.Sources, getFromRunParams(AbilityKey.DamageSources));
        sa.setTriggeringObjectsFrom(this, AbilityKey.DamageAmount);
    }

    @Override
    public String getImportantStackObjects(SpellAbility sa) {
        StringBuilder sb = new StringBuilder();
        if (sa.getTriggeringObject(AbilityKey.Target) != null) {
            sb.append("Damaged: ").append(sa.getTriggeringObject(AbilityKey.Target)).append(", ");
        }
        sb.append("Amount: ").append(sa.getTriggeringObject(AbilityKey.DamageAmount));
        return sb.toString();
    }

}
