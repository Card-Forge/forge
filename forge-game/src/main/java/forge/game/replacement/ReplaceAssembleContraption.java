package forge.game.replacement;

import forge.game.ability.AbilityKey;
import forge.game.card.Card;

import java.util.Map;

public class ReplaceAssembleContraption extends ReplacementEffect {

    public ReplaceAssembleContraption(Map<String, String> map, Card host, boolean intrinsic) {
        super(map, host, intrinsic);
    }

    @Override
    public boolean canReplace(Map<AbilityKey, Object> runParams) {
        if (!matchesValidParam("ValidPlayer", runParams.get(AbilityKey.Player))) {
            return false;
        }
        if (!matchesValidParam("ValidCause", runParams.get(AbilityKey.Cause))) {
            return false;
        }

        return true;
    }
}
