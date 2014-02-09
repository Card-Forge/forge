package forge.game.ability.effects;

import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

import java.util.List;

public class OwnershipGainEffect extends SpellAbilityEffect {

    @Override
    public void resolve(SpellAbility sa) {
        final List<Card> cards = getTargetCards(sa);
        final List<Player> controllers = getDefinedPlayersOrTargeted(sa, "DefinedPlayer");

        final Player newOwner = controllers.isEmpty() ? sa.getActivatingPlayer() : controllers.get(0);

        for (Card card : cards) {
            newOwner.changeOwnership(card);
        }
    }
}
