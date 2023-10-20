package forge.game.ability.effects;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
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
        if (!sa.usesTargeting() && !sa.hasParam("Defined")) {
            cards = game.getCardsIn(ZoneType.Battlefield);
        } else {
            cards = getTargetPlayers(sa).getCardsIn(ZoneType.Battlefield);
        }

        cards = AbilityUtils.filterListByType(cards, sa.getParam("ValidCards"), sa);

        Player tapper = activator;

        for (final Card tgtC : cards) {
            if (remTapped) {
                card.addRemembered(tgtC);
            }
            if (sa.hasParam("TapperController")) {
                tapper = tgtC.getController();
            }
            tgtC.tap(true, sa, tapper);
        }
    }

}
