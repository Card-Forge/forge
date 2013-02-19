package forge.card.ability.ai;

import forge.card.ability.SpellAbilityAi;
import forge.card.spellability.SpellAbility;
import forge.game.player.AIPlayer;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class TapOrUntapAllAi extends SpellAbilityAi {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, forge.card.spellability.SpellAbility)
     */
    @Override
    protected boolean canPlayAI(AIPlayer aiPlayer, SpellAbility sa) {
        // Only Turnabout currently uses this, it's hardcoded to always return false
        // Looks like Faces of the Past could also use this
        return false;
    }

}
