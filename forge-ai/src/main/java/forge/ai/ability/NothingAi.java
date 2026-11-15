package forge.ai.ability;

import java.util.Map;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.SpellAbilityAi;
import forge.game.card.Card;
import forge.game.cost.Cost;
import forge.game.cost.CostCollectEvidence;
import forge.game.cost.CostForage;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;

/**
 * {@code DB$ Nothing}, {@code DB$ Bookkeeping} and {@code DB$ EndOfTurn}: a mode name, a step that carries a cost or a
 * condition for its sub-ability, a note of what to remember, a delayed sacrifice. They have nothing of
 * its own to judge: as a sub-ability it is fine (what follows decides); as a trigger the AI may decline it is taken
 * when the cost and what follows suit the AI; it is never started on its own.
 */
public class NothingAi extends SpellAbilityAi {
    @Override
    protected AiAbilityDecision checkApiLogic(final Player ai, final SpellAbility sa) {
        return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
    }

    /**
     * A trigger the AI may decline ("you may forage"): the step itself has nothing to judge, so it is the cost and what
     * follows. The costs the AI refuses (life, discard, sacrifice, counters) and the sub-abilities that do not suit it
     * are checked by {@code doTriggerNoCostWithSubs} after this. What is left to decide here is a cost that pays for
     * something elsewhere on the card: forage or collect evidence for a "whenever you forage / collect evidence"
     * trigger of the same card (Corpseberry Cultivator): worth it when the card has that trigger.
     */
    @Override
    protected AiAbilityDecision doTriggerNoCost(final Player ai, final SpellAbility sa, final boolean mandatory) {
        if (mandatory || "Always".equals(sa.getParam("AILogic")) || !paysForSomethingElsewhere(sa)
                || hasReward(sa)) {
            return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
        }
        return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
    }

    /** The cost is a forage or a collect evidence: its payoff is not in this ability's own sub-abilities. */
    private static boolean paysForSomethingElsewhere(final SpellAbility sa) {
        final Cost cost = sa.getPayCosts();
        return cost != null && sa.getSubAbility() == null
                && (cost.hasSpecificCostType(CostForage.class) || cost.hasSpecificCostType(CostCollectEvidence.class));
    }

    /** The host has the trigger that pays for the forage or the evidence. */
    static boolean hasReward(final SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Cost cost = sa.getPayCosts();
        if (host == null || cost == null) {
            return false;
        }
        for (final Trigger t : host.getTriggers()) {
            final String mode = t.getParam("Mode");
            if ("Forage".equals(mode) && cost.hasSpecificCostType(CostForage.class)
                    || "CollectEvidence".equals(mode) && cost.hasSpecificCostType(CostCollectEvidence.class)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public AiAbilityDecision chkDrawback(final Player ai, final SpellAbility sa) {
        return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
    }

    @Override
    public boolean confirmAction(final Player player, final SpellAbility sa, final PlayerActionConfirmMode mode,
            final String message, final Map<String, Object> params) {
        return true;
    }
}
