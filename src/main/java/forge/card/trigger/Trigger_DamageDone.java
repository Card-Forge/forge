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
public class Trigger_DamageDone extends Trigger {

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
    public Trigger_DamageDone(final HashMap<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }

    /** {@inheritDoc} */
    @Override
    public final boolean performTest(final java.util.Map<String, Object> runParams2) {
        Card src = (Card) runParams2.get("DamageSource");
        Object tgt = runParams2.get("DamageTarget");

        if (mapParams.containsKey("ValidSource")) {
            if (!src.isValid(mapParams.get("ValidSource").split(","), hostCard.getController(), hostCard)) {
                return false;
            }
        }

        if (mapParams.containsKey("ValidTarget")) {
            if (!matchesValid(tgt, mapParams.get("ValidTarget").split(","), hostCard)) {
                return false;
            }
        }

        if (mapParams.containsKey("CombatDamage")) {
            if (mapParams.get("CombatDamage").equals("True")) {
                if (!((Boolean) runParams2.get("IsCombatDamage"))) {
                    return false;
                }
            } else if (mapParams.get("CombatDamage").equals("False")) {
                if (((Boolean) runParams2.get("IsCombatDamage"))) {
                    return false;
                }
            }
        }

        if (mapParams.containsKey("DamageAmount")) {
            String fullParam = mapParams.get("DamageAmount");

            String operator = fullParam.substring(0, 2);
            int operand = Integer.parseInt(fullParam.substring(2));
            int actualAmount = (Integer) runParams2.get("DamageAmount");

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
        Trigger copy = new Trigger_DamageDone(mapParams, hostCard, isIntrinsic);
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
        sa.setTriggeringObject("Source", runParams.get("DamageSource"));
        sa.setTriggeringObject("Target", runParams.get("DamageTarget"));
        sa.setTriggeringObject("DamageAmount", runParams.get("DamageAmount"));
    }
}
