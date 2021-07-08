package forge.game.trigger;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;
import forge.util.Expressions;
import forge.util.Localizer;

public class TriggerRolledDie extends Trigger {

    public TriggerRolledDie(final Map<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }

    /** {@inheritDoc}
     * @param runParams
     */
    @Override
    public final boolean performTest(final Map<AbilityKey, Object> runParams) {
        if (hasParam("ValidPlayer")) {
            if (!matchesValid(runParams.get(AbilityKey.Player), getParam("ValidPlayer").split(","),
                    this.getHostCard())) {
                return false;
            }
        }
        if (hasParam("ValidResult")) {
            String[] params = getParam("ValidResult").split(",");
            final int result = (int) runParams.get(AbilityKey.Result);
            for (String param : params) {
                if (StringUtils.isNumeric(param)) {
                    if (param.equals("" + result)) return true;
                } else {
                    final String comp = param.substring(0, 2);
                    final int rightSide = Integer.parseInt(param.substring(2));
                    if (Expressions.compare(result, comp, rightSide)) return true;
                }
            }
            return false;
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public final void setTriggeringObjects(final SpellAbility sa, Map<AbilityKey, Object> runParams) {
        sa.setTriggeringObjectsFrom(runParams, AbilityKey.Result, AbilityKey.Player);
    }

    @Override
    public String getImportantStackObjects(SpellAbility sa) {
        return Localizer.getInstance().getMessage("lblPlayer") + ": " + sa.getTriggeringObject(AbilityKey.Player) + ", " +
                Localizer.getInstance().getMessage("lblResultIs", sa.getTriggeringObject(AbilityKey.Result));
    }
}
