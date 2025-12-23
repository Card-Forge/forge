package forge.game.ability.effects;

import forge.game.ability.SpellAbilityEffect;
import forge.game.spellability.SpellAbility;
import forge.util.Lang;

public class LookAtEffect extends SpellAbilityEffect {

    @Override
    public void resolve(final SpellAbility sa) {
        sa.getHostCard().getGame().getAction().revealTo(getTargetCards(sa), sa.getActivatingPlayer());
    }

    @Override
    protected String getStackDescription(final SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        sb.append(sa.getActivatingPlayer());
        sb.append(" looks at ");
        sb.append(Lang.joinHomogenous(getTargetCards(sa)));
        sb.append('.');
        return sb.toString();
    }

}
