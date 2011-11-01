package forge.card.trigger;

import java.util.HashMap;

import forge.Card;
import forge.card.spellability.SpellAbility;

/**
 * <p>
 * Trigger_ChangesZone class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class Trigger_ChangesZone extends Trigger {

    /**
     * <p>
     * Constructor for Trigger_ChangesZone.
     * </p>
     * 
     * @param params
     *            a {@link java.util.HashMap} object.
     * @param host
     *            a {@link forge.Card} object.
     * @param intrinsic
     *            the intrinsic
     */
    public Trigger_ChangesZone(final HashMap<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }

    /** {@inheritDoc} */
    @Override
    public final boolean performTest(final java.util.Map<String, Object> runParams2) {
        if (this.getMapParams().containsKey("Origin")) {
            if (!this.getMapParams().get("Origin").equals("Any")) {
                if (this.getMapParams().get("Origin") == null) {
                    return false;
                }
                if (!this.getMapParams().get("Origin").equals(runParams2.get("Origin"))) {
                    return false;
                }
            }
        }

        if (this.getMapParams().containsKey("Destination")) {
            if (!this.getMapParams().get("Destination").equals("Any")) {
                if (!this.getMapParams().get("Destination").equals(runParams2.get("Destination"))) {
                    return false;
                }
            }
        }

        if (this.getMapParams().containsKey("ValidCard")) {
            final Card moved = (Card) runParams2.get("Card");
            if (!moved.isValid(this.getMapParams().get("ValidCard").split(","), this.getHostCard().getController(),
                    this.getHostCard())) {
                return false;
            }
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override
    public final Trigger getCopy() {
        final Trigger copy = new Trigger_ChangesZone(this.getMapParams(), this.getHostCard(), this.isIntrinsic());
        if (this.getOverridingAbility() != null) {
            copy.setOverridingAbility(this.getOverridingAbility());
        }
        copy.setName(this.getName());
        copy.setID(this.getId());

        return copy;
    }

    /** {@inheritDoc} */
    @Override
    public final void setTriggeringObjects(final SpellAbility sa) {
        sa.setTriggeringObject("Card", this.getRunParams().get("Card"));
    }
}
