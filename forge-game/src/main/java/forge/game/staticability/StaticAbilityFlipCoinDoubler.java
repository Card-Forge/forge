package forge.game.staticability;

import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.util.collect.FCollectionView;

public class StaticAbilityFlipCoinDoubler {

    public static int getFlipMultiplier(final Player flipper) {
        return 1 << flipper.getGame()
                .getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)
                .stream()
                .map(Card::getStaticAbilities)
                .flatMap(FCollectionView::stream)
                .filter(stAb -> stAb.checkConditions(StaticAbilityMode.FlipCoinDoubler))
                .filter(stAb -> stAb.matchesValidParam("ValidPlayer", flipper))
                .count();
    }
}
