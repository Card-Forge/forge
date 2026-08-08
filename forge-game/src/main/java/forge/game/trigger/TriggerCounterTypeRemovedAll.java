package forge.game.trigger;

import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;

import java.util.Map;

public class TriggerCounterTypeRemovedAll extends Trigger {

    public TriggerCounterTypeRemovedAll(Map<String, String> params, Card host, boolean intrinsic) {
        super(params, host, intrinsic);
    }

    @Override
    public boolean performTest(Map<AbilityKey, Object> runParams) {
        return matchesValidParam("ValidObject", runParams.get(AbilityKey.Object));
    }

    @Override
    public void setTriggeringObjects(SpellAbility sa, Map<AbilityKey, Object> runParams) {
        sa.setTriggeringObjectsFrom(runParams, AbilityKey.Object);
    }

    @Override
    public String getImportantStackObjects(SpellAbility sa) {
        return String.valueOf(sa.getTriggeringObject(AbilityKey.Object));
    }

}
