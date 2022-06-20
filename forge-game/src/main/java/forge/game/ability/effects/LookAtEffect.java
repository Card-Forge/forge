package forge.game.ability.effects;

import forge.game.Game;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.util.Lang;

public class LookAtEffect extends SpellAbilityEffect {

    @Override
    public void resolve(final SpellAbility sa) {
        final Card host = sa.getHostCard();
        final Game game = host.getGame();
        final Player activator = sa.getActivatingPlayer();

        game.getAction().revealTo(getTargetCards(sa), activator);
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
