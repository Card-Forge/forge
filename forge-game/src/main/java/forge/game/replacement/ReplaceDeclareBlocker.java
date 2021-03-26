package forge.game.replacement;

import java.util.Map;

import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;

public class ReplaceDeclareBlocker extends ReplacementEffect {

    public ReplaceDeclareBlocker(final Map<String, String> mapParams, final Card host, final boolean intrinsic) {
        super(mapParams, host, intrinsic);
    }

    @Override
    public boolean canReplace(Map<AbilityKey, Object> runParams) {
        if (!matchesValidParam("ValidPlayer", runParams.get(AbilityKey.Affected))) {
            return false;
        }
        return true;
    }

    @Override
    public void setReplacingObjects(Map<AbilityKey, Object> runParams, SpellAbility sa) {
        sa.setReplacingObject(AbilityKey.DefendingPlayer, runParams.get(AbilityKey.Affected));
        // Here the Player is the one who would declare blockers (may be changed by some Card's effect)
        sa.setReplacingObject(AbilityKey.Player, runParams.get(AbilityKey.Player));
    }
}
