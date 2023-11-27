package forge.game.replacement;

import java.util.Map;

import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.Expressions;

/**
 * TODO: Write javadoc for this type.
 *
 */
public class ReplaceLifeReduced extends ReplacementEffect {

    /**
     * Instantiates a new replace life reduced.
     *
     * @param map  the map
     * @param host the host
     */
    public ReplaceLifeReduced(Map<String, String> map, Card host, boolean intrinsic) {
        super(map, host, intrinsic);
    }

    /* (non-Javadoc)
     * @see forge.card.replacement.ReplacementEffect#canReplace(java.util.HashMap)
     */
    @Override
    public boolean canReplace(Map<AbilityKey, Object> runParams) {
        int amount = (int)runParams.get(AbilityKey.Amount);
        Player affected = (Player) runParams.get(AbilityKey.Affected);
        if (amount <= 0) {
            return false;
        }

        if (!matchesValidParam("ValidPlayer", affected)) {
            return false;
        }

        if (hasParam("IsDamage")) {
            if (getParam("IsDamage").equals("True") != ((Boolean) runParams.get(AbilityKey.IsDamage))) {
                return false;
            }
        }

        if (hasParam("Result")) {
            final int n = affected.getLife() - amount;
            String comparator = getParam("Result");
            final String operator = comparator.substring(0, 2);
            final int operandValue = Integer.parseInt(comparator.substring(2));
            if (!Expressions.compare(n, operator, operandValue)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void setReplacingObjects(Map<AbilityKey, Object> runParams, SpellAbility sa) {
        sa.setReplacingObjectsFrom(runParams, AbilityKey.Amount);
        sa.setReplacingObject(AbilityKey.Player, runParams.get(AbilityKey.Affected));
    }
}
