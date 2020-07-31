package forge.game.trigger;

import forge.game.PlanarDice;
import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;
import forge.util.Localizer;

import java.util.Map;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class TriggerPlanarDice extends Trigger {

    /**
     * <p>
     * Constructor for Trigger_RollPlanarDice.
     * </p>
     * 
     * @param params
     *            a {@link java.util.HashMap} object.
     * @param host
     *            a {@link forge.game.card.Card} object.
     * @param intrinsic
     *            the intrinsic
     */
    public TriggerPlanarDice(final Map<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }

    /* (non-Javadoc)
     * @see forge.card.trigger.Trigger#performTest(java.util.Map)
     */
    @Override
    public boolean performTest(Map<AbilityKey, Object> runParams) {
        if (hasParam("ValidPlayer")) {
            if (!matchesValid(runParams.get(AbilityKey.Player), getParam("ValidPlayer").split(","),
                    this.getHostCard())) {
                return false;
            }
        }

        if (hasParam("Result")) {
            PlanarDice cond = PlanarDice.smartValueOf(getParam("Result"));
            if (cond != runParams.get(AbilityKey.Result)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void setTriggeringObjects(SpellAbility sa, Map<AbilityKey, Object> runParams) {
        sa.setTriggeringObjectsFrom(runParams, AbilityKey.Player);
    }

    @Override
    public String getImportantStackObjects(SpellAbility sa) {
        StringBuilder sb = new StringBuilder();
        sb.append(Localizer.getInstance().getMessage("lblRoller")).append(": ").append(sa.getTriggeringObject(AbilityKey.Player));
        return sb.toString();
    }
}
