package forge.game.trigger;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;

import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.card.CardUtil;
import forge.game.spellability.SpellAbility;
import forge.util.Localizer;

public class TriggerDamageDoneOnce extends Trigger {

    public TriggerDamageDoneOnce(Map<String, String> params, Card host, boolean intrinsic) {
        super(params, host, intrinsic);

    }

    @SuppressWarnings("unchecked")
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

        if (hasParam("ValidSource")) {
            final Map<Card, Integer> damageMap = (Map<Card, Integer>) runParams.get(AbilityKey.DamageMap);

            if (getDamageAmount(damageMap) <= 0) {
                return false;
            }
        }

        if (!matchesValidParam("ValidTarget", runParams.get(AbilityKey.DamageTarget))) {
            return false;
        }

        return true;
    }

    @Override
    public void setTriggeringObjects(SpellAbility sa, Map<AbilityKey, Object> runParams) {
        @SuppressWarnings("unchecked")
        final Map<Card, Integer> damageMap = (Map<Card, Integer>) runParams.get(AbilityKey.DamageMap);

        Object target = runParams.get(AbilityKey.DamageTarget);
        if (target instanceof Card) {
            target = CardUtil.getLKICopy((Card)runParams.get(AbilityKey.DamageTarget));
        }
        sa.setTriggeringObject(AbilityKey.Target, target);
        sa.setTriggeringObject(AbilityKey.Sources, getDamageSources(damageMap));
        sa.setTriggeringObject(AbilityKey.DamageAmount, getDamageAmount(damageMap));
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

    public int getDamageAmount(Map<Card, Integer> damageMap) {
        int result = 0;
        for (Map.Entry<Card, Integer> e : damageMap.entrySet()) {
            if (matchesValidParam("ValidSource", e.getKey())) {
                result += e.getValue();
            }
        }
        return result;
    }

    public Set<Card> getDamageSources(Map<Card, Integer> damageMap) {
        if (!hasParam("ValidSource")) {
            return Sets.newHashSet(damageMap.keySet());
        }
        Set<Card> result = Sets.newHashSet();
        for (Card c : damageMap.keySet()) {
            if (matchesValid(c, getParam("ValidSource").split(","))) {
                result.add(c);
            }
        }
        return result;
    }
}
