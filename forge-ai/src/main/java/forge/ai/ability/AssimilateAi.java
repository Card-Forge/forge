package forge.ai.ability;

import java.util.List;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtilCard;
import forge.ai.SpellAbilityAi;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

/**
 * The card ends up under our control either way, so any legal target is a gain; the only decision
 * is which one.
 */
public class AssimilateAi extends SpellAbilityAi {

    @Override
    protected AiAbilityDecision checkApiLogic(final Player ai, final SpellAbility sa) {
        if (!sa.usesTargeting()) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }
        return takeBest(ai, sa) ? new AiAbilityDecision(100, AiPlayDecision.WillPlay)
                : new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
    }

    @Override
    protected AiAbilityDecision doTriggerNoCost(final Player ai, final SpellAbility sa, final boolean mandatory) {
        if (!sa.usesTargeting()) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }
        if (takeBest(ai, sa)) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }
        // a mandatory trigger with no legal target simply does nothing
        return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
    }

    private boolean takeBest(final Player ai, final SpellAbility sa) {
        sa.resetTargets();
        final List<Card> options = new CardCollection(
                ai.getGame().getCardsIn(sa.getTargetRestrictions().getZone()));
        final CardCollection legal = new CardCollection();
        for (final Card c : options) {
            if (sa.canTarget(c)) {
                legal.add(c);
            }
        }
        if (legal.isEmpty()) {
            return false;
        }
        sa.getTargets().add(ComputerUtilCard.getBestCreatureAI(legal));
        return true;
    }
}
