package forge.ai.ability;


import java.util.Map;

import forge.ai.SpecialCardAi;
import forge.ai.SpellAbilityAi;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.SpellAbility;

public class BranchAi extends SpellAbilityAi {
    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected boolean canPlayAI(Player aiPlayer, SpellAbility sa) {
        final String aiLogic = sa.getParamOrDefault("AILogic", "");
        if ("GrislySigil".equals(aiLogic)) {
            return SpecialCardAi.GrislySigil.consider(aiPlayer, sa);
        }

        // TODO: expand for other cases where the AI is needed to make a decision on a branch
        return true;
    }

    @Override
    public boolean confirmAction(Player player, SpellAbility sa, PlayerActionConfirmMode mode, String message, Map<String, Object> params) {
        return true;
    }
}
