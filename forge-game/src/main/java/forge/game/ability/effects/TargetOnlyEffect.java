package forge.game.ability.effects;

import forge.game.ability.SpellAbilityEffect;
import forge.game.spellability.SpellAbility;

/**
 * Does nothing but choose targets (and the bookkeeping of {@link BookkeepingEffect}, when the ability asks for it): "choose target opponent" (or "up to one target opponent") for the effects of the
 * same ability that follow and read the choice ({@code Defined$ Targeted}, {@code TargetedPlayer$...},
 * {@code NewController$ ParentTarget}). Scripts used {@code DB$ Pump | ValidTgts$ Opponent} for this, which is not a
 * pump: it made the AI judge the choice as one. The AI's logic for the choice is named by {@code AILogic$}.
 */
public class TargetOnlyEffect extends SpellAbilityEffect {
    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return sa.getParamOrDefault("StackDescription", "");
    }

    @Override
    public void resolve(final SpellAbility sa) {
        // the targets were chosen when the ability was put on the stack, and are read by what follows; only the
        // bookkeeping (remember, imprint, note) of the same ability is done here
        BookkeepingEffect.apply(sa);
    }
}
