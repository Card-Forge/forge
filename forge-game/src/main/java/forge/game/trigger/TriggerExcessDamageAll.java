package forge.game.trigger;

import java.util.Map;

import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.spellability.SpellAbility;
import forge.util.Localizer;

public class TriggerExcessDamageAll extends Trigger {

    public TriggerExcessDamageAll(Map<String, String> params, Card host, boolean intrinsic) {
        super(params, host, intrinsic);
    }

    @Override
    public boolean performTest(Map<AbilityKey, Object> runParams) {
        if (hasParam("CombatDamage")) {
            if (getParam("CombatDamage").equalsIgnoreCase("True") != (Boolean) runParams.get(AbilityKey.IsCombatDamage)) {
                return false;
            }
        }
        if (getDamageTargets((CardCollection) runParams.get(AbilityKey.DamageTargets)).isEmpty()) {
            return false;
        }

        return true;
    }

    @Override
    public void setTriggeringObjects(SpellAbility sa, Map<AbilityKey, Object> runParams) {
        sa.setTriggeringObject(AbilityKey.Targets, getDamageTargets((CardCollection) runParams.get(AbilityKey.DamageTargets)));
    }

    @Override
    public String getImportantStackObjects(SpellAbility sa) {
        StringBuilder sb = new StringBuilder();
        sb.append(Localizer.getInstance().getMessage("lblDamaged")).append(": ").append(sa.getTriggeringObject(AbilityKey.Targets));
        return sb.toString();
    }

    public CardCollection getDamageTargets(CardCollection damageTargets) {
        if (!hasParam("ValidTarget")) {
            return damageTargets;
        }
        CardCollection result = new CardCollection();
        for (Card c : damageTargets) {
            if (matchesValidParam("ValidTarget", c)) {
                result.add(c);
            }
        }
        return result;
    }
}
