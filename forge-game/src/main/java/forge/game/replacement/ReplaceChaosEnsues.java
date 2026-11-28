package forge.game.replacement;

import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;

import java.util.Map;

public class ReplaceChaosEnsues extends ReplacementEffect {

    public ReplaceChaosEnsues(final Map<String, String> params, final Card host, final boolean intrinsic) {
        super(params, host, intrinsic);
    }

    @Override
    public boolean canReplace(Map<AbilityKey, Object> runParams) {
        return matchesValidParam("ValidPlayer", runParams.get(AbilityKey.Affected))
                && matchesValidParam("ValidCause", runParams.get(AbilityKey.Cause));
    }

    @Override
    public void setReplacingObjects(Map<AbilityKey, Object> runParams, SpellAbility sa) {
        sa.setReplacingObject(AbilityKey.Cause, runParams.get(AbilityKey.Cause));
    }
}
