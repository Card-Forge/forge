package forge.ai.ability;

import java.util.Map;

import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.SpellAbilityAi;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.SpellAbility;

/**
 * {@code DB$ Nothing}, {@code DB$ Bookkeeping} and {@code DB$ EndOfTurn}: a mode name, a step that carries a cost or a
 * condition for its sub-ability, a note of what to remember, a delayed sacrifice. They have nothing of
 * its own to judge: as a sub-ability it is fine (what follows decides), as a trigger it runs when it must, and it is
 * never started on its own.
 */
public class NothingAi extends SpellAbilityAi {
    @Override
    protected AiAbilityDecision checkApiLogic(final Player ai, final SpellAbility sa) {
        return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
    }

    @Override
    protected AiAbilityDecision doTriggerNoCost(final Player ai, final SpellAbility sa, final boolean mandatory) {
        return mandatory ? new AiAbilityDecision(100, AiPlayDecision.WillPlay)
                : new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
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
