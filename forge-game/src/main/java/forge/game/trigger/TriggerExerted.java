package forge.game.trigger;

import forge.game.card.Card;
import forge.game.spellability.SpellAbility;

import java.util.HashMap;
import java.util.Map;

public class TriggerExerted extends Trigger {
    /**
     * <p>
     * Constructor for Trigger.
     * </p>
     *
     * @param params    a {@link HashMap} object.
     * @param host      a {@link Card} object.
     * @param intrinsic
     */
    public TriggerExerted(Map<String, String> params, Card host, boolean intrinsic) {
        super(params, host, intrinsic);
    }

    @Override
    public boolean performTest(Map<String, Object> runParams2) {
        final Card exerter = (Card) runParams2.get("Card");
        if (this.mapParams.containsKey("ValidCard")) {
            if (!exerter.isValid(this.mapParams.get("ValidCard").split(","), this.getHostCard().getController(),
                    this.getHostCard(), null)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void setTriggeringObjects(SpellAbility sa) {
        sa.setTriggeringObject("Card", this.getRunParams().get("Card"));
        sa.setTriggeringObject("Player", this.getRunParams().get("Player"));
    }

    @Override
    public String getImportantStackObjects(SpellAbility sa) {
        StringBuilder sb = new StringBuilder();
        sb.append("Exerted: ").append(sa.getTriggeringObject("Card"));
        return sb.toString();
    }
}
