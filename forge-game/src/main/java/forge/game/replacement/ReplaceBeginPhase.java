package forge.game.replacement;

import java.util.Map;

import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.player.Player;
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
        Player affected = (Player) runParams.get(AbilityKey.Affected);
        if (!matchesValidParam("ValidPlayer", affected)) {
            return false;
        }
        if (hasParam("Phase")) {
            final String phase = getParam("Phase");
            final String currentPhase = (String) runParams.get(AbilityKey.Phase);
            if (phase.equals("Combat") && currentPhase.equals("BeginCombat")) {
                return true;
            }
            if (phase.equals("Main") && (currentPhase.equals("Main1") || currentPhase.equals("Main2"))) {
                return true;
            }
            if (!phase.equals(currentPhase)) {
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
