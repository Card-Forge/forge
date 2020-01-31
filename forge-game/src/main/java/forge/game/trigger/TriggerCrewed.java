package forge.game.trigger;

import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.spellability.SpellAbility;
import forge.util.Localizer;

import java.util.Map;

public class TriggerCrewed extends Trigger {

    public TriggerCrewed(final Map<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }

    @Override
    public boolean performTest(Map<AbilityKey, Object> runParams) {
        if (hasParam("ValidVehicle")) {
            if (!matchesValid(runParams.get(AbilityKey.Vehicle), getParam("ValidVehicle").split(","),
                    this.getHostCard())) {
                return false;
            }
        }
        if (hasParam("ValidCrew")) {
            if (runParams.get(AbilityKey.Crew) == null) {
                return false;
            }

            boolean passes = false;
            for (Object member : (CardCollection) runParams.get(AbilityKey.Crew)) {
                passes |= matchesValid(member, getParam("ValidCrew").split(","),
                        this.getHostCard());
            }
            if (!passes)
                return passes;
        }
        return true;
    }

    @Override
    public void setTriggeringObjects(SpellAbility sa, Map<AbilityKey, Object> runParams) {
        sa.setTriggeringObjectsFrom(runParams, AbilityKey.Vehicle, AbilityKey.Crew);
    }

    @Override
    public String getImportantStackObjects(SpellAbility sa) {
        StringBuilder sb = new StringBuilder();
        sb.append(Localizer.getInstance().getMessage("lblVehicle")).append(": ").append(sa.getTriggeringObject(AbilityKey.Vehicle));
        sb.append("  ");
        sb.append(Localizer.getInstance().getMessage("lblCrew")).append(": ").append(sa.getTriggeringObject(AbilityKey.Crew));
        return sb.toString();
    }
}
