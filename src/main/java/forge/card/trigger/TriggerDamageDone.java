package forge.card.trigger;

import java.util.HashMap;

import forge.AllZoneUtil;
import forge.Card;
import forge.card.spellability.SpellAbility;

/**
 * <p>
 * Trigger_DamageDone class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class TriggerDamageDone extends Trigger {

    /**
     * <p>
     * Constructor for Trigger_DamageDone.
     * </p>
     * 
     * @param params
     *            a {@link java.util.HashMap} object.
     * @param host
     *            a {@link forge.Card} object.
     * @param intrinsic
     *            the intrinsic
     */
    public TriggerDamageDone(final HashMap<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }

    /** {@inheritDoc} */
    @Override
    public final boolean performTest(final java.util.Map<String, Object> runParams2) {
        final Card src = (Card) runParams2.get("DamageSource");
        final Object tgt = runParams2.get("DamageTarget");

        if (this.getMapParams().containsKey("ValidSource")) {
            if (!src.isValid(this.getMapParams().get("ValidSource").split(","), this.getHostCard().getController(),
                    this.getHostCard())) {
                return false;
            }
        }

        if (this.getMapParams().containsKey("ValidTarget")) {
            if (!this.matchesValid(tgt, this.getMapParams().get("ValidTarget").split(","), this.getHostCard())) {
                return false;
            }
        }

        if (this.getMapParams().containsKey("CombatDamage")) {
            if (this.getMapParams().get("CombatDamage").equals("True")) {
                if (!((Boolean) runParams2.get("IsCombatDamage"))) {
                    return false;
                }
            } else if (this.getMapParams().get("CombatDamage").equals("False")) {
                if (((Boolean) runParams2.get("IsCombatDamage"))) {
                    return false;
                }
            }
        }

        if (this.getMapParams().containsKey("DamageAmount")) {
            final String fullParam = this.getMapParams().get("DamageAmount");

            final String operator = fullParam.substring(0, 2);
            final int operand = Integer.parseInt(fullParam.substring(2));
            final int actualAmount = (Integer) runParams2.get("DamageAmount");

            if (!AllZoneUtil.compare(actualAmount, operator, operand)) {
                return false;
            }

            System.out.print("DamageDone Amount Operator: ");
            System.out.println(operator);
            System.out.print("DamageDone Amount Operand: ");
            System.out.println(operand);
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override
    public final Trigger getCopy() {
        final Trigger copy = new TriggerDamageDone(this.getMapParams(), this.getHostCard(), this.isIntrinsic());
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
        sa.setTriggeringObject("Source", this.getRunParams().get("DamageSource"));
        sa.setTriggeringObject("Target", this.getRunParams().get("DamageTarget"));
        sa.setTriggeringObject("DamageAmount", this.getRunParams().get("DamageAmount"));
    }
}
