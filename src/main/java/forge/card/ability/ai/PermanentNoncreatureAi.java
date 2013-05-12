package forge.card.ability.ai;

import forge.card.ability.SpellAbilityAi;
import forge.card.spellability.SpellAbility;
import forge.game.ai.ComputerUtil;
import forge.game.phase.PhaseType;
import forge.game.player.Player;

/** 
 * AbilityFactory for Creature Spells.
 *
 */
public class PermanentNoncreatureAi extends SpellAbilityAi {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, forge.card.spellability.SpellAbility)
     */
    @Override
    protected boolean canPlayAI(Player aiPlayer, SpellAbility sa) {
        String logic = sa.getParam("AILogic");

        if ("DontCast".equals(logic)) {
            return false;
        } else if ("MoreCreatures".equals(logic)) {
            return (aiPlayer.getCreaturesInPlay().size() > aiPlayer.getOpponent().getCreaturesInPlay().size());
        }

        // Wait for Main2 if possible
        if (aiPlayer.getGame().getPhaseHandler().is(PhaseType.MAIN1)
                && !ComputerUtil.castPermanentInMain1(aiPlayer, sa)) {
            return false;
        }

        // AI shouldn't be retricted all that much for Creatures for now
        return true;
    }
}
