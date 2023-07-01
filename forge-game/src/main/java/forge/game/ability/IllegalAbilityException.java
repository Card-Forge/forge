package forge.game.ability;

import forge.game.spellability.SpellAbility;
import forge.util.TextUtil;

public class IllegalAbilityException extends RuntimeException {
    private static final long serialVersionUID = -8638474348184716635L;

    public IllegalAbilityException(final SpellAbility sa) {
        this(sa.toString());
    }

    public IllegalAbilityException(final SpellAbility sa, final SpellAbilityEffect effect) {
        this(TextUtil.concatWithSpace(sa.toString(), "(effect "+effect.getClass().getName()+")"));
    }

    private IllegalAbilityException(final String message) {
        super(message);
    }

}
