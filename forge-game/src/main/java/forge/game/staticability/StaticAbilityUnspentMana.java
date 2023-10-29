package forge.game.staticability;

import java.util.Collection;
import java.util.Set;

import com.google.common.collect.Sets;

import forge.card.MagicColor;
import forge.card.mana.ManaAtom;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

public class StaticAbilityUnspentMana {

    static String MODE = "UnspentMana";

    public static Collection<Byte> getManaToKeep(final Player player) {
        final Game game = player.getGame();
        Set<Byte> result = Sets.newHashSet();
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.checkConditions(MODE)) {
                    continue;
                }
                applyUnspentManaAbility(stAb, player, result);
            }
        }
        return result;
    }

    private static void applyUnspentManaAbility(final StaticAbility stAb, final Player player, Set<Byte> result) {
        if (!stAb.matchesValidParam("ValidPlayer", player)) {
            return;
        }
        if (!stAb.hasParam("ManaType")) {
            for (byte b : ManaAtom.MANATYPES) {
                result.add(b);
            }
        } else {
            result.add(MagicColor.fromName(stAb.getParam("ManaType")));
        }
    }
}
