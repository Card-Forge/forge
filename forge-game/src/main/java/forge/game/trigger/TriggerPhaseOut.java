package forge.game.trigger;

import forge.game.card.Card;
import forge.game.spellability.SpellAbility;

import java.util.Map;

public class TriggerPhaseOut extends Trigger {

    public TriggerPhaseOut(final Map<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }

    /** {@inheritDoc} */
    @Override
    public final boolean performTest(final java.util.Map<String, Object> runParams2) {
        final Card phaser = (Card) runParams2.get("Card");

        if (this.mapParams.containsKey("ValidCard")) {
            if (this.mapParams.get("ValidCard").equals("Card.Self")) {
                // Since Phased out cards aren't visible in .isValid, use a special check here.
                // NOTE: All Phase Out Triggers should use ValidCard$ Card.Self
                if (phaser != this.getHostCard()) {
                    return false;
                }
            } else if (!phaser.isValid(this.mapParams.get("ValidCard").split(","), this.getHostCard().getController(),
                    this.getHostCard(), null)) {
                return false;
            }
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override
    public final void setTriggeringObjects(final SpellAbility sa) {
        sa.setTriggeringObject("Card", this.getRunParams().get("Card"));
    }

    @Override
    public String getImportantStackObjects(SpellAbility sa) {
        StringBuilder sb = new StringBuilder();
        sb.append("Phased Out: ").append(sa.getTriggeringObject("Card"));
        return sb.toString();
    }
}
