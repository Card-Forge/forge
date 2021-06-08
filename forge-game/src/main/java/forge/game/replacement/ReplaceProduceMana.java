package forge.game.replacement;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;
import forge.util.Expressions;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class ReplaceProduceMana extends ReplacementEffect {

    /**
     * 
     * ReplaceProduceMana.
     * @param mapParams &emsp; HashMap<String, String>
     * @param host &emsp; Card
     */
    public ReplaceProduceMana(final Map<String, String> mapParams, final Card host, final boolean intrinsic) {
        super(mapParams, host, intrinsic);
    }

    /* (non-Javadoc)
     * @see forge.card.replacement.ReplacementEffect#canReplace(java.util.HashMap)
     */
    @Override
    public boolean canReplace(Map<AbilityKey, Object> runParams) {
        if (!matchesValidParam("ValidCard", runParams.get(AbilityKey.Affected))) {
            return false;
        }
        if (!matchesValidParam("ValidPlayer", runParams.get(AbilityKey.Player))) {
            return false;
        }
        if (!matchesValidParam("ValidActivator", runParams.get(AbilityKey.Activator))) {
            return false;
        }
        if (!matchesValidParam("ValidAbility", runParams.get(AbilityKey.AbilityMana))) {
            return false;
        }

        if (hasParam("ManaAmount")) {
            String full = getParam("ManaAmount");
            String operator = full.substring(0, 2);
            String operand = full.substring(2);

            int intoperand = AbilityUtils.calculateAmount(getHostCard(), operand, this);

            int manaAmount = StringUtils.countMatches((String) runParams.get(AbilityKey.Mana), " ") + 1;
            if (!Expressions.compare(manaAmount, operator, intoperand)) {
                return false;
            }
        }

        return true;
    }

    public void setReplacingObjects(Map<AbilityKey, Object> runParams, SpellAbility sa) {
        sa.setReplacingObject(AbilityKey.Mana, runParams.get(AbilityKey.Mana));
    }
}
