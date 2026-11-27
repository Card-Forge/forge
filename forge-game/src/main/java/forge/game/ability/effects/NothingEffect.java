package forge.game.ability.effects;

import forge.game.ability.SpellAbilityEffect;
import forge.game.spellability.SpellAbility;

/**
 * A step that does nothing itself: the name of a mode a card offers ("Khans", "Dragons"), or a step that only exists
 * to carry a cost or a condition ({@code UnlessCost$}, {@code Cost$ Forage}) with the real effect in its
 * {@code SubAbility$}. Scripts spelled these "Pump", which made the AI and the engine treat them as pumps.
 */
public class NothingEffect extends SpellAbilityEffect {
    @Override
    protected String getStackDescription(final SpellAbility sa) {
        return sa.getParamOrDefault("StackDescription", "");
    }

    @Override
    public void resolve(final SpellAbility sa) {
        // nothing to do
    }
}
