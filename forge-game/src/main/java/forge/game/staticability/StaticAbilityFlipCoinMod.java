package forge.game.staticability;

import forge.game.player.Player;
import forge.game.zone.ZoneType;

import java.util.stream.Stream;

import static forge.game.staticability.StaticAbilityMode.FlipCoinDoubler;
import static forge.game.staticability.StaticAbilityMode.FlipCoinMod;

public class StaticAbilityFlipCoinMod {

    public static Boolean fixedResult(final Player flipper) {
        return filterStaticAbilities(flipper, FlipCoinMod)
                .map(stAb -> Boolean.valueOf(stAb.getParam("Result")))
                .findFirst()
                .orElse(null);
    }

    public static int getFlipMultiplier(final Player flipper) {
        return 1 << filterStaticAbilities(flipper, FlipCoinDoubler)
                .count();
    }

    private static Stream<StaticAbility> filterStaticAbilities(final Player flipper, final StaticAbilityMode mode) {
        return flipper.getGame()
                .getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)
                .stream()
                .flatMap(card -> card.getStaticAbilities().stream())
                .filter(stAb -> stAb.checkConditions(mode) && stAb.matchesValidParam("ValidPlayer", flipper));
    }

}
