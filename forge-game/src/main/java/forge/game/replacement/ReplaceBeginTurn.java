package forge.game.replacement;

import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;

import java.util.Map;

public class ReplaceBeginTurn extends ReplacementEffect {

    public ReplaceBeginTurn(final Map<String, String> mapParams, final Card host, final boolean intrinsic) {
        super(mapParams, host, intrinsic);
    }

    @Override
    public boolean canReplace(Map<AbilityKey, Object> runParams) {
        if (hasParam("ValidPlayer")) {
            if (!matchesValid(runParams.get(AbilityKey.Affected), getParam("ValidPlayer").split(","), getHostCard())) {
                return false;
            }
        }
        if (hasParam("ExtraTurn")) {
            if (!(boolean) runParams.get(AbilityKey.ExtraTurn)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void setReplacingObjects(Map<AbilityKey, Object> runParams, SpellAbility sa) {
        sa.setReplacingObject(AbilityKey.Player, runParams.get(AbilityKey.Affected));
    }
}
