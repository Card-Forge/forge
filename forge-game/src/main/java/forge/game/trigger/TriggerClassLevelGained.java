package forge.game.trigger;

import java.util.Map;

import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;

public class TriggerClassLevelGained extends Trigger {

    public TriggerClassLevelGained(final Map<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }

    @Override
    public final boolean performTest(final Map<AbilityKey, Object> runParams) {
        if (!matchesValidParam("ValidCard", runParams.get(AbilityKey.Card))) {
            return false;
        }

        if (hasParam("ClassLevel") && runParams.containsKey(AbilityKey.ClassLevel)) {
            final int levelCondition = Integer.parseInt(getParam("ClassLevel"));
            final int level = (Integer) runParams.get(AbilityKey.ClassLevel);

            if (levelCondition != level) {
                return false;
            }
        }

        return true;
    }

    @Override
    public final void setTriggeringObjects(final SpellAbility sa, Map<AbilityKey, Object> runParams) {
        sa.setTriggeringObjectsFrom(runParams, AbilityKey.ClassLevel);
    }

    public String getImportantStackObjects(SpellAbility sa) {
        Integer level = (Integer)sa.getTriggeringObject(AbilityKey.ClassLevel);
        StringBuilder sb = new StringBuilder("Class Level: ");
        sb.append(level);
        return sb.toString();
    }
}
