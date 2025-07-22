package forge.ai.ability;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.MyRandom;

public class TapOrUntapAi extends TapAiBase {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected AiAbilityDecision canPlayAI(Player ai, SpellAbility sa) {
        final Card source = sa.getHostCard();

        boolean randomReturn = MyRandom.getRandom().nextFloat() <= Math.pow(.6667, sa.getActivationsThisTurn());

        if (!sa.usesTargeting()) {
            // assume we are looking to tap human's stuff
            // TODO - check for things with untap abilities, and don't tap those.

            boolean bFlag = false;
            for (final Card c : AbilityUtils.getDefinedCards(source, sa.getParam("Defined"), sa)) {
                bFlag |= c.isUntapped();
            }

            if (!bFlag) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
        } else {
            sa.resetTargets();
            if (!tapPrefTargeting(ai, source, sa, false)) {
                return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
            }
        }

        if (randomReturn) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        } else {
            return new AiAbilityDecision(0, AiPlayDecision.StopRunawayActivations);
        }
    }

}
