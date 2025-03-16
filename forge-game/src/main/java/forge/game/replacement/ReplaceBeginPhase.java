package forge.game.replacement;

import java.util.Map;

import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;

public class ReplaceBeginPhase extends ReplacementEffect {

    public ReplaceBeginPhase(final Map<String, String> mapParams, final Card host, final boolean intrinsic) {
        super(mapParams, host, intrinsic);
        // set default layer to control
        if (!mapParams.containsKey("Layer")) {
            this.setLayer(ReplacementLayer.Control);
        }
    }

    @Override
    public boolean canReplace(Map<AbilityKey, Object> runParams) {
        return true;
    }

    @Override
    public void setReplacingObjects(Map<AbilityKey, Object> runParams, SpellAbility sa) {
        sa.setReplacingObject(AbilityKey.Player, runParams.get(AbilityKey.Affected));
    }
}
