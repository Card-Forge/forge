package forge.card.ability.ai;

import forge.Singletons;
import forge.card.ability.SpellAiLogic;
import forge.card.spellability.SpellAbility;
import forge.game.GameState;
import forge.game.ai.ComputerUtil;
import forge.game.phase.PhaseType;
import forge.game.player.AIPlayer;

/** 
 * AbilityFactory for Creature Spells.
 *
 */
public class PermanentNoncreatureAi extends SpellAiLogic {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, forge.card.spellability.SpellAbility)
     */
    @Override
    protected boolean canPlayAI(AIPlayer aiPlayer, SpellAbility sa) {
        String logic = sa.getParam("AILogic");
        GameState game = Singletons.getModel().getGame();

        if ("DontCast".equals(logic)) {
            return false;
        } else if ("MoreCreatures".equals(logic)) {
            return (aiPlayer.getCreaturesInPlay().size() > aiPlayer.getOpponent().getCreaturesInPlay().size());
        }

        // Wait for Main2 if possible
        if (game.getPhaseHandler().is(PhaseType.MAIN1)
                && !ComputerUtil.castPermanentInMain1(aiPlayer, sa)) {
            return false;
        }

        // AI shouldn't be retricted all that much for Creatures for now
        return true;
    }
}
