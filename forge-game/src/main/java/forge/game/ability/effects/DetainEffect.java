package forge.game.ability.effects;

import forge.game.Game;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

public class DetainEffect extends SpellAbilityEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        return "Detain " + getTargetCards(sa) + " .";
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Player pl = sa.getActivatingPlayer();
        final Game game = pl.getGame();
        for (final Card c : getTargetCards(sa)) {
            c.detain(pl);
            game.getCleanup().addUntil(pl, () -> c.removeDetainedBy(pl));
        }
    }
}
