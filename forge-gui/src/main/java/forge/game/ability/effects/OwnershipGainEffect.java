package forge.game.ability.effects;

import java.util.Arrays;
import java.util.List;

import forge.Command;
import forge.card.mana.ManaCost;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.Ability;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class OwnershipGainEffect extends SpellAbilityEffect {

    @Override
    public void resolve(SpellAbility sa) {
        Card source = sa.getSourceCard();

        final Game game = source.getGame();

        final List<Card> cards = getTargetCards(sa);
        final List<Player> controllers = getDefinedPlayersOrTargeted(sa, "DefinedPlayer");

        final Player newOwner = controllers.isEmpty() ? sa.getActivatingPlayer() : controllers.get(0);

        for (Card card : cards) {
            newOwner.changeOwnership(card);
        }
    }

}
