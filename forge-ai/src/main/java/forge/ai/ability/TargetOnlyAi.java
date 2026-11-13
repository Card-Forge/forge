package forge.ai.ability;

import java.util.Map;

import forge.ai.AiAbilityDecision;
import forge.ai.PlaceholderTargetAi;
import forge.ai.SpellAbilityAi;
import forge.game.player.Player;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.SpellAbility;

/** The choice of a target for the effects that follow (see {@link PlaceholderTargetAi} for the AILogic values). */
public class TargetOnlyAi extends SpellAbilityAi {
    @Override
    protected AiAbilityDecision canPlay(final Player ai, final SpellAbility sa) {
        return PlaceholderTargetAi.decide(ai, sa, false);
    }

    @Override
    protected AiAbilityDecision doTriggerNoCost(final Player ai, final SpellAbility sa, final boolean mandatory) {
        return PlaceholderTargetAi.decide(ai, sa, mandatory);
    }

    @Override
    public AiAbilityDecision chkDrawback(final Player ai, final SpellAbility sa) {
        return PlaceholderTargetAi.decide(ai, sa, false);
    }

    @Override
    public boolean confirmAction(final Player player, final SpellAbility sa, final PlayerActionConfirmMode mode,
                                 final String message, final Map<String, Object> params) {
        return true;
    }
}
