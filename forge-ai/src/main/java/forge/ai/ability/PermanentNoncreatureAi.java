package forge.ai.ability;

import forge.ai.ComputerUtil;
import forge.ai.SpellAbilityAi;
import forge.game.Game;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

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
        Game game = aiPlayer.getGame();
        final PhaseHandler ph = game.getPhaseHandler();

        if ("DontCast".equals(logic)) {
            return false;
        }

        // Wait for Main2 if possible
        if (ph.is(PhaseType.MAIN1) && ph.isPlayerTurn(aiPlayer)
                && !ComputerUtil.castPermanentInMain1(aiPlayer, sa)) {
            return false;
        }

        // AI shouldn't be retricted all that much for Creatures for now
        return true;
    }
}
