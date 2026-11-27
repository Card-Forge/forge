package forge.game.trigger;

import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;

import java.util.Map;

public class TriggerPlaneswalk extends Trigger {

    public TriggerPlaneswalk(final Map<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }

    @Override
    public boolean performTest(Map<AbilityKey, Object> runParams) {
        return matchesValidParam("ValidPlayer", runParams.get(AbilityKey.Player))
                && matchesValidParam("ValidCause", runParams.get(AbilityKey.Cause));
    }

    @Override
    public void setTriggeringObjects(Map<AbilityKey, Object> objects, Map<AbilityKey, Object> runParams) {
        AbilityKey.copyKeys(runParams, objects, AbilityKey.Player, AbilityKey.Cause);
    }

    @Override
    public String getImportantStackObjects(SpellAbility sa) {
        return "";
    }
}
