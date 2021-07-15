package forge.game.replacement;

import java.util.Map;

import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.card.TokenCreateTable;
import forge.game.spellability.SpellAbility;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class ReplaceToken extends ReplacementEffect {

    /**
     * 
     * ReplaceProduceMana.
     * @param mapParams &emsp; HashMap<String, String>
     * @param host &emsp; Card
     */
    public ReplaceToken(final Map<String, String> mapParams, final Card host, final boolean intrinsic) {
        super(mapParams, host, intrinsic);
    }

    /* (non-Javadoc)
     * @see forge.card.replacement.ReplacementEffect#canReplace(java.util.Map)
     */
    @Override
    public boolean canReplace(Map<AbilityKey, Object> runParams) {
        /*
        if (((int) runParams.get(AbilityKey.TokenNum)) <= 0) {
            return false;
        }
        //*/

        if (hasParam("EffectOnly")) {
            final Boolean effectOnly = (Boolean) runParams.get(AbilityKey.EffectOnly);
            if (!effectOnly) {
                return false;
            }
        }

        if (!matchesValidParam("ValidPlayer", runParams.get(AbilityKey.Affected))) {
            return false;
        }

        /*/
        if (!matchesValidParam("ValidToken", runParams.get(AbilityKey.Token))) {
            return false;
        }
        //*/

        if (filterAmount((TokenCreateTable) runParams.get(AbilityKey.Token)) <= 0) {
            return false;
        }

        return true;
    }

    /* (non-Javadoc)
     * @see forge.card.replacement.ReplacementEffect#setReplacingObjects(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public void setReplacingObjects(Map<AbilityKey, Object> runParams, SpellAbility sa) {
        sa.setReplacingObject(AbilityKey.TokenNum, filterAmount((TokenCreateTable) runParams.get(AbilityKey.Token)));
        sa.setReplacingObject(AbilityKey.Token, runParams.get(AbilityKey.Token));
        sa.setReplacingObject(AbilityKey.Player, runParams.get(AbilityKey.Affected));
    }

    
    public int filterAmount(final TokenCreateTable table) {
        return table.getFilterAmount(getParamOrDefault("ValidPlayer", null), getParamOrDefault("ValidToken", null), this);
    }
}
