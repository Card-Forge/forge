package forge.game.staticability;

import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.Optional;

import static forge.game.staticability.StaticAbilityMode.CantSearchLibrary;
import static forge.game.staticability.StaticAbilityMode.LimitSearchLibrary;


public class StaticAbilitySearchLibrary {

    /**
     * @return maximum number of cards which can be fetched from a library considering its size and search limit or null if there is no limit
     */
    public static Integer limitSearchLibraryConsideringSize(Player player) {
        Integer limit = limitSearchLibrary(player);
        if (limit != null) {
            return Math.min(player.getCardsIn(ZoneType.Library).size(), limit);
        } else {
            return null;
        }
    }

    /**
     * @return maximum number of cards which can be revealed from a library or null if there is no limit
     */
    public static Integer limitSearchLibrary(Player player) {
        return findStaticAbilityForValidPlayer(player, LimitSearchLibrary)
                .map(stAb -> Integer.valueOf(stAb.getParam("LimitNum")))
                .orElse(null);
    }

    public static boolean cantSearchLibrary(Player player, SpellAbility sa) {
        return findStaticAbilityForValidPlayer(player, CantSearchLibrary)
                .filter(stAb -> !stAb.getIgnoreEffectPlayers().contains(player))
                .filter(stAb -> !stAb.hasParam("ValidCause")
                        || (stAb.matchesValidParam("ValidCause", sa)
                            && player.equals(sa.getActivatingPlayer())))
                .isPresent();
    }

    private static Optional<StaticAbility> findStaticAbilityForValidPlayer(final Player player, final StaticAbilityMode mode) {
        return player.getGame()
                .getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)
                .stream()
                .flatMap(card -> card.getStaticAbilities().stream())
                .filter(stAb -> stAb.checkConditions(mode) && stAb.matchesValidParam("ValidPlayer", player))
                .findAny();
    }
}
