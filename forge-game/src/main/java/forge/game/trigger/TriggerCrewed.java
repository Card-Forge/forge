package forge.game.trigger;

import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.spellability.SpellAbility;

import java.util.Map;

public class TriggerCrewed extends Trigger {

    public TriggerCrewed(final java.util.Map<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }

    @Override
    public boolean performTest(Map<String, Object> runParams2) {
        if (this.mapParams.containsKey("ValidVehicle")) {
            if (!matchesValid(runParams2.get("Vehicle"), this.mapParams.get("ValidVehicle").split(","),
                    this.getHostCard())) {
                return false;
            }
        }
        if (this.mapParams.containsKey("ValidCrew")) {
            boolean passes = false;
            for (Object member : (CardCollection)runParams2.get("Crew")) {
                passes |= matchesValid(member, this.mapParams.get("ValidCrew").split(","),
                        this.getHostCard());
            }
            if (!passes)
                return passes;
        }
        return true;
    }

    @Override
    public void setTriggeringObjects(SpellAbility sa) {
        sa.setTriggeringObject("Vehicle", this.getRunParams().get("Vehicle"));
        sa.setTriggeringObject("Crew", this.getRunParams().get("Crew"));
    }

    @Override
    public String getImportantStackObjects(SpellAbility sa) {
        StringBuilder sb = new StringBuilder();
        sb.append("Vehicle: ").append(sa.getTriggeringObject("Vehicle"));
        sb.append("  ");
        sb.append("Crew: ").append(sa.getTriggeringObject("Crew"));
        return sb.toString();
    }
}
