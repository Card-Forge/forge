package forge.ai.ability;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.MyRandom;

public class RevealHandAi extends RevealAiBase {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected boolean checkApiLogic(final Player ai, final SpellAbility sa) {
        final boolean bFlag = revealHandTargetAI(ai, sa, false);

        if (!bFlag) {
            return false;
        }

        boolean randomReturn = MyRandom.getRandom().nextFloat() <= Math.pow(.667, sa.getActivationsThisTurn() + 1);

        if (playReusable(ai, sa)) {
            randomReturn = true;
        }

        return randomReturn;
    }

    @Override
    protected AiAbilityDecision doTriggerAINoCost(Player ai, SpellAbility sa, boolean mandatory) {
        if (revealHandTargetAI(ai, sa, mandatory)) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        } else {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
    }
}
