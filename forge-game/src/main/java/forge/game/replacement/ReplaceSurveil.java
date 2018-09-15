package forge.game.replacement;

import forge.game.card.Card;
import forge.game.spellability.SpellAbility;

import java.util.Map;

/**
 * TODO: Write javadoc for this type.
 *
 */
public class ReplaceSurveil extends ReplacementEffect {

    /**
     *
     * ReplaceProduceMana.
     * @param mapParams &emsp; HashMap<String, String>
     * @param host &emsp; Card
     */
    public ReplaceSurveil(final Map<String, String> mapParams, final Card host, final boolean intrinsic) {
        super(mapParams, host, intrinsic);
    }

    /* (non-Javadoc)
     * @see forge.card.replacement.ReplacementEffect#canReplace(java.util.Map)
     */
    @Override
    public boolean canReplace(Map<String, Object> runParams) {
        if (!runParams.get("Event").equals("Surveil") || ((int) runParams.get("SurveilNum")) <= 0) {
            return false;
        }

        if (hasParam("ValidPlayer")) {
            if (!matchesValid(runParams.get("Affected"), getParam("ValidPlayer").split(","), getHostCard())) {
                return false;
            }
        }

        return true;
    }

    /* (non-Javadoc)
     * @see forge.card.replacement.ReplacementEffect#setReplacingObjects(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public void setReplacingObjects(Map<String, Object> runParams, SpellAbility sa) {
        sa.setReplacingObject("Player", runParams.get("Affected"));
        sa.setReplacingObject("SurveilNum", runParams.get("SurveilNum"));
    }

}
