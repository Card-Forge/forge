package forge.card.trigger;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import forge.Card;
import forge.CardList;
import forge.card.spellability.SpellAbility;

/**
 * <p>
 * Trigger_Attacks class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class TriggerAttacks extends Trigger {

    /**
     * <p>
     * Constructor for Trigger_Attacks.
     * </p>
     * 
     * @param params
     *            a {@link java.util.HashMap} object.
     * @param host
     *            a {@link forge.Card} object.
     * @param intrinsic
     *            the intrinsic
     */
    public TriggerAttacks(final HashMap<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }

    /** {@inheritDoc} */
    @Override
    public final boolean performTest(final Map<String, Object> runParams2) {
        if (this.getMapParams().containsKey("ValidCard")) {
            if (!this.matchesValid(runParams2.get("Attacker"), this.getMapParams().get("ValidCard").split(","),
                    this.getHostCard())) {
                return false;
            }
        }

        if (this.getMapParams().containsKey("Attacked")) {
            if (this.getMapParams().get("Attacked").equals("Player")
                    && StringUtils.isNumeric(runParams2.get("Attacked").toString())
                    && (Integer.parseInt(runParams2.get("Attacked").toString()) > 0)) {
                return false;
            }
        }

        if (this.getMapParams().containsKey("Alone")) {
            final CardList otherAttackers = (CardList) runParams2.get("OtherAttackers");
            if (otherAttackers == null) {
                return false;
            }
            if (this.getMapParams().get("Alone").equals("True")) {
                if (otherAttackers.size() != 0) {
                    return false;
                }
            } else {
                if (otherAttackers.size() == 0) {
                    return false;
                }
            }
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override
    public final Trigger getCopy() {
        final Trigger copy = new TriggerAttacks(this.getMapParams(), this.getHostCard(), this.isIntrinsic());
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
        sa.setTriggeringObject("Attacker", this.getRunParams().get("Attacker"));
    }
}
