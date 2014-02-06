package forge.game.trigger;

import forge.game.card.Card;
import forge.game.spellability.SpellAbility;

import java.util.Map;

public class TriggerPhaseIn extends Trigger {

    public TriggerPhaseIn(final Map<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }

    /** {@inheritDoc} */
    @Override
    public final boolean performTest(final java.util.Map<String, Object> runParams2) {
        final Card phaser = (Card) runParams2.get("Card");

        if (this.mapParams.containsKey("ValidCard")) {
            if (!phaser.isValid(this.mapParams.get("ValidCard").split(","), this.getHostCard().getController(),
                    this.getHostCard())) {
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
}
