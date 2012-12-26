package forge.card.abilityfactory.ai;

import forge.card.abilityfactory.SpellAiLogic;
import forge.card.spellability.SpellAbility;
import forge.game.player.Player;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class AddPhaseAi extends SpellAiLogic {

    @Override
    protected boolean canPlayAI(Player aiPlayer, SpellAbility sa) {
        return false;
    }

}
