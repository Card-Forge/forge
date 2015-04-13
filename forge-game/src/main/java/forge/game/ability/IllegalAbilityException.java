package forge.game.ability;

import forge.game.spellability.SpellAbility;

public class IllegalAbilityException extends RuntimeException {
    private static final long serialVersionUID = -8638474348184716635L;

    public IllegalAbilityException(final SpellAbility sa) {
        this(sa.toString());
    }

    public IllegalAbilityException(final SpellAbility sa, final SpellAbilityEffect effect) {
        this(String.format("%s (effect %s)", sa, effect.getClass().getName()));
    }

    private IllegalAbilityException(final String message) {
        super(message);
    }

}
