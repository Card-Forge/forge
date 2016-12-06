package forge.game.trigger;

import java.util.Map;

import forge.game.card.Card;
import forge.game.spellability.SpellAbility;

public class TriggerRevealed extends Trigger {

    public TriggerRevealed(Map<String, String> params, Card host, boolean intrinsic) {
        super(params, host, intrinsic);
    }

    @Override
    public boolean performTest(Map<String, Object> runParams2) {
        if (this.mapParams.containsKey("ValidCard")) {
            final Card moved = (Card) runParams2.get("Card");
            if (!moved.isValid(this.mapParams.get("ValidCard").split(","), this.getHostCard().getController(),
                    this.getHostCard(), null)) {
                return false;
            }
        }
        if (this.mapParams.containsKey("Miracle")) {
            Boolean madness = (Boolean) runParams2.get("Miracle");
            if (this.mapParams.get("Miracle").equals("True") ^ madness) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void setTriggeringObjects(SpellAbility sa) {
        sa.setTriggeringObject("Card", this.getRunParams().get("Card"));
    }

    @Override
    public String getImportantStackObjects(SpellAbility sa) {
        StringBuilder sb = new StringBuilder();
        sb.append("Revealed: ").append(sa.getTriggeringObject("Card"));
        return sb.toString();
    }

}
