package forge.game.replacement;

import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;

import java.util.Map;

public class ReplaceLoseLife extends ReplacementEffect {

    /**
     * Instantiates a new replace lose life.
     *
     * @param map the map
     * @param host the host
     */
    public ReplaceLoseLife(Map<String, String> map, Card host, boolean intrinsic) {
        super(map, host, intrinsic);
    }


    @Override
    public boolean canReplace(Map<AbilityKey, Object> runParams) {
        if (((int)runParams.get(AbilityKey.LifeAmount)) <= 0) {
            return false;
        }
        if (!matchesValidParam("ValidPlayer", runParams.get(AbilityKey.Affected))) {
            return false;
        }
        return true;
    }

    @Override
    public void setReplacingObjects(Map<AbilityKey, Object> runParams, SpellAbility sa) {
        sa.setReplacingObject(AbilityKey.LifeAmount, runParams.get(AbilityKey.LifeAmount));
    }
}
