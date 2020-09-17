package forge.game.trigger;

import java.util.Map;

import com.google.common.collect.Iterables;

import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.card.CardPredicates;
import forge.game.spellability.SpellAbility;
import forge.util.Localizer;

public class TriggerAttackerBlockedOnce extends Trigger {

    public TriggerAttackerBlockedOnce(Map<String, String> params, Card host, boolean intrinsic) {
        super(params, host, intrinsic);
    }

    /** {@inheritDoc}
     * @param runParams*/
    @Override
    public final boolean performTest(final Map<AbilityKey, Object> runParams) {
        if (hasParam("ValidCard")) {
            @SuppressWarnings("unchecked")
            final Iterable<Card> srcs = (Iterable<Card>) runParams.get(AbilityKey.Attackers);
            if (!Iterables.any(srcs,
                    CardPredicates.restriction(getParam("ValidCard").split(","),
                        getHostCard().getController(), getHostCard(), null))) {
                return false;
            }
        }

        return true;
    }

    /** {@inheritDoc} */
    @Override
    public final void setTriggeringObjects(final SpellAbility sa, Map<AbilityKey, Object> runParams) {
        sa.setTriggeringObjectsFrom(runParams, AbilityKey.Attackers);
    }

    @Override
    public String getImportantStackObjects(SpellAbility sa) {
        StringBuilder sb = new StringBuilder();
        sb.append(Localizer.getInstance().getMessage("lblAttackers")).append(": ").append(sa.getTriggeringObject(AbilityKey.Attackers));
        return sb.toString();
    }
}
