package forge.game.trigger;

import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;

import java.util.Map;

// Turn Begin isn't a "real" trigger, but is useful for Advanced Scripting Techniques
public class TriggerTurnBegin extends Trigger {
    public TriggerTurnBegin(final java.util.Map<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }

    @Override
    public final boolean performTest(final Map<AbilityKey, Object> runParams) {
        if (this.mapParams.containsKey("ValidPlayer")) {
            return matchesValid(runParams.get(AbilityKey.Player), this.mapParams.get("ValidPlayer").split(","),
                    this.getHostCard());
        }
        return true;
    }

    @Override
    public final void setTriggeringObjects(final SpellAbility sa) {
        sa.setTriggeringObjectsFrom(this, AbilityKey.Player);
    }

    @Override
    public String getImportantStackObjects(SpellAbility sa) {
        StringBuilder sb = new StringBuilder();
        sb.append("Player: ").append(sa.getTriggeringObject(AbilityKey.Player));
        return sb.toString();
    }
}
