package forge.game.replacement;

import forge.game.card.Card;
import forge.game.card.CardFactoryUtil;
import forge.game.spellability.SpellAbility;
import forge.util.Expressions;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

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
    public boolean canReplace(Map<String, Object> runParams) {
        if (!runParams.get("Event").equals("ProduceMana")) {
            return false;
        }
        //Check for tapping
        if (!mapParams.containsKey("NoTapCheck")) {
            final SpellAbility manaAbility = (SpellAbility) runParams.get("AbilityMana");
            if (manaAbility == null || manaAbility.getRootAbility().getPayCosts() == null || !manaAbility.getRootAbility().getPayCosts().hasTapCost()) {
                return false;
            }
        }

        if (hasParam("ManaAmount")) {
            String full = getParam("ManaAmount");
            String operator = full.substring(0, 2);
            String operand = full.substring(2);
            int intoperand = 0;
            try {
                intoperand = Integer.parseInt(operand);
            } catch (NumberFormatException e) {
                intoperand = CardFactoryUtil.xCount(getHostCard(), getHostCard().getSVar(operand));
            }
            int manaAmount = StringUtils.countMatches((String) runParams.get("Mana"), " ") + 1;
            if (!Expressions.compare(manaAmount, operator, intoperand)) {
                return false;
            }
        }

        if (this.getMapParams().containsKey("ValidCard")) {
            if (!matchesValid(runParams.get("Affected"), this.getMapParams().get("ValidCard").split(","), this.getHostCard())) {
                return false;
            }
        }

        return true;
    }


}
