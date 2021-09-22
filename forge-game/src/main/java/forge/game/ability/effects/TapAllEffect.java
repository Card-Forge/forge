package forge.game.ability.effects;

import java.util.List;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.player.Player;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class TapAllEffect extends SpellAbilityEffect {
    @Override
    protected String getStackDescription(SpellAbility sa) {
        if (sa instanceof AbilitySub) {
            return "Tap all valid cards.";
        } else {
            return sa.getParam("SpellDescription");
        }
    }

    @Override
    public void resolve(SpellAbility sa) {
        final Player activator = sa.getActivatingPlayer();
        final Game game = activator.getGame();
        final Card card = sa.getHostCard();
        final boolean remTapped = sa.hasParam("RememberTapped");
        if (remTapped) {
            card.clearRemembered();
        }

        CardCollectionView cards;

        final List<Player> tgtPlayers = getTargetPlayers(sa);

        if (!sa.usesTargeting() && !sa.hasParam("Defined")) {
            cards = game.getCardsIn(ZoneType.Battlefield);
        } else {
            CardCollection cards2 = new CardCollection();
            for (final Player p : tgtPlayers) {
                cards2.addAll(p.getCardsIn(ZoneType.Battlefield));
            }
            cards = cards2;
        }

        cards = AbilityUtils.filterListByType(cards, sa.getParam("ValidCards"), sa);

        for (final Card c : cards) {
            if (remTapped) {
                card.addRemembered(c);
            }
            c.tap(true);
        }
    }

}
