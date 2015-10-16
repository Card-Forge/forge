package forge.game.trigger;

import forge.game.card.Card;
import forge.game.spellability.SpellAbility;

// Turn Begin isn't a "real" trigger, but is useful for Advanced Scripting Techniques
public class TriggerTurnBegin extends Trigger {
    public TriggerTurnBegin(final java.util.Map<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }

    @Override
    public final boolean performTest(final java.util.Map<String, Object> runParams2) {
        if (this.mapParams.containsKey("ValidPlayer")) {
            if (!matchesValid(runParams2.get("Player"), this.mapParams.get("ValidPlayer").split(","),
                    this.getHostCard())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public final void setTriggeringObjects(final SpellAbility sa) {
        sa.setTriggeringObject("Player", this.getRunParams().get("Player"));
    }

    @Override
    public String getImportantStackObjects(SpellAbility sa) {
        StringBuilder sb = new StringBuilder();
        sb.append("Player: ").append(sa.getTriggeringObject("Player"));
        return sb.toString();
    }
}
