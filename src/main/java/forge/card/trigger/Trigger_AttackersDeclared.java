package forge.card.trigger;

import java.util.HashMap;
import java.util.Map;

import forge.Card;
import forge.CardList;
import forge.card.spellability.SpellAbility;

/**
 * TODO Write javadoc for this type.
 * 
 */
public class Trigger_AttackersDeclared extends Trigger {

    /**
     * Instantiates a new trigger_ attackers declared.
     * 
     * @param params
     *            the params
     * @param host
     *            the host
     * @param intrinsic
     *            the intrinsic
     */
    public Trigger_AttackersDeclared(final HashMap<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }

    /** {@inheritDoc} */
    @Override
    public final boolean performTest(final Map<String, Object> runParams2) {
        if (mapParams.containsKey("SingleAttacker")) {
            CardList attackers = (CardList) runParams2.get("Attackers");
            if (attackers.size() != 1) {
                return false;
            }
        }
        if (mapParams.containsKey("AttackingPlayer")) {
            if (!matchesValid(runParams2.get("AttackingPlayer"),
                    mapParams.get("AttackingPlayer").split(","), hostCard)) {
                return false;
            }
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public final Trigger getCopy() {
        Trigger copy = new Trigger_AttackersDeclared(this.mapParams, this.hostCard, this.isIntrinsic);

        if (overridingAbility != null) {
            copy.setOverridingAbility(overridingAbility);
        }
        copy.setName(name);
        copy.setID(ID);

        return copy;
    }

    /** {@inheritDoc} */
    @Override
    public final void setTriggeringObjects(final SpellAbility sa) {
        sa.setTriggeringObject("Attackers", runParams.get("Attackers"));
    }
}
