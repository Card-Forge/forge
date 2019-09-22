package forge.game.trigger;

import java.util.Map;

import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;

public class TriggerRevealed extends Trigger {

    public TriggerRevealed(Map<String, String> params, Card host, boolean intrinsic) {
        super(params, host, intrinsic);
    }

    @Override
    public boolean performTest(Map<AbilityKey, Object> runParams) {
        if (hasParam("ValidCard")) {
            final Card moved = (Card) runParams.get(AbilityKey.Card);
            if (!moved.isValid(this.mapParams.get("ValidCard").split(","), this.getHostCard().getController(),
                    this.getHostCard(), null)) {
                return false;
            }
        }
        if (hasParam("Miracle")) {
            if (!matchesValid(runParams.get(AbilityKey.Card), getParam("ValidCard").split(","), getHostCard())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void setTriggeringObjects(SpellAbility sa) {
        sa.setTriggeringObjectsFrom(this, AbilityKey.Card);
    }

    @Override
    public String getImportantStackObjects(SpellAbility sa) {
        StringBuilder sb = new StringBuilder();
        sb.append("Revealed: ").append(sa.getTriggeringObject(AbilityKey.Card));
        return sb.toString();
    }

}
