package forge.game.trigger;

import java.util.Map;
import java.util.Set;

import forge.game.GameEntity;
import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;
import forge.util.Localizer;

public class TriggerDamageDoneOnce extends Trigger {

    public TriggerDamageDoneOnce(Map<String, String> params, Card host, boolean intrinsic) {
        super(params, host, intrinsic);

    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean performTest(Map<AbilityKey, Object> runParams) {
        final Set<Card> srcs = (Set<Card>) runParams.get(AbilityKey.DamageSources);
        final GameEntity tgt = (GameEntity) runParams.get(AbilityKey.DamageTarget);

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
        
        if (hasParam("ValidSource")) {
            boolean valid = false;
            for (Card c : srcs) {
                if (c.isValid(getParam("ValidSource").split(","), this.getHostCard().getController(),this.getHostCard(), null)) {
                    valid = true;
                }
            }
            if (!valid) {
                return false;
            }
        }
        
        if (hasParam("ValidTarget")) {
            if (!matchesValid(tgt, getParam("ValidTarget").split(","), this.getHostCard())) {
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
            sb.append(Localizer.getInstance().getMessage("lblDamaged")).append(": ").append(sa.getTriggeringObject(AbilityKey.Target)).append(", ");
        }
        sb.append(Localizer.getInstance().getMessage("lblAmount")).append(": ").append(sa.getTriggeringObject(AbilityKey.DamageAmount));
        return sb.toString();
    }

}
