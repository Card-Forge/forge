package forge.game.ability;

import forge.game.spellability.SpellAbility;
import forge.util.TextUtil;

public class IllegalAbilityException extends RuntimeException {
    private static final long serialVersionUID = -8638474348184716635L;
    private boolean fromCustom = false;

    public IllegalAbilityException(final SpellAbility sa) {
        super(sa.toString());
    }

    public IllegalAbilityException(final SpellAbility sa, final SpellAbilityEffect effect) {
        super(TextUtil.concatWithSpace(sa.toString(), "(effect "+effect.getClass().getName()+")"));
    }

    public IllegalAbilityException(final String message, final Throwable t, final boolean isCustom) {
        super(message, t);
        fromCustom = isCustom;
    }

    public boolean isCustom() {
        return fromCustom;
    }

}
