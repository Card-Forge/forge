package forge.game.trigger;

import java.util.Map;

import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;

public class TriggerManifestDread extends Trigger {

    public TriggerManifestDread(Map<String, String> params, Card host, boolean intrinsic) {
        super(params, host, intrinsic);
    }

    @Override
    public boolean performTest(Map<AbilityKey, Object> runParams) {
        if (!matchesValidParam("ValidPlayer", runParams.get(AbilityKey.Player))) {
            return false;
        }
        return true;
    }

    @Override
    public void setTriggeringObjects(Map<AbilityKey, Object> objects, Map<AbilityKey, Object> runParams) {
        objects.put(AbilityKey.Cards, runParams.get(AbilityKey.Cards));

    }

    @Override
    public String getImportantStackObjects(SpellAbility sa) {
        // TODO Auto-generated method stub
        return "";
    }

}
