package forge.game.staticability;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class StaticAbilityCantGainLosePayLife {

    static String MODE_CANT_GAIN_LIFE = "CantGainLife";
    static String MODE_CANT_CHANGE_LIFE = "CantChangeLife";
    static String MODE_CANT_PAY_LIFE = "CantPayLife";

    public static boolean anyCantGainLife(final Player player) {
        final Game game = player.getGame();
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (stAb.isSuppressed() || !stAb.checkConditions()) {
                    continue;
                }
                if (!(stAb.getParam("Mode").equals(MODE_CANT_GAIN_LIFE) || stAb.getParam("Mode").equals(MODE_CANT_CHANGE_LIFE))) {
                    continue;
                }

                if (applyCommonAbility(stAb, player)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean anyCantLoseLife(final Player player)  {
        final Game game = player.getGame();
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (stAb.isSuppressed() || !stAb.checkConditions()) {
                    continue;
                }

                if (!stAb.getParam("Mode").equals(MODE_CANT_CHANGE_LIFE)) {
                    continue;
                }

                if (applyCommonAbility(stAb, player)) {
                    return true;
                }
            }
        }

        return false;
    }

    public static boolean anyCantPayLife(final Player player, final boolean effect, final SpellAbility cause)  {
        final Game game = player.getGame();
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (stAb.isSuppressed() || !stAb.checkConditions()) {
                    continue;
                }

                if (!(stAb.getParam("Mode").equals(MODE_CANT_PAY_LIFE) || stAb.getParam("Mode").equals(MODE_CANT_CHANGE_LIFE))) {
                    continue;
                }

                if (stAb.hasParam("ForCost")) {
                    if ("True".equalsIgnoreCase(stAb.getParam("ForCost")) == effect) {
                        continue;
                    }
                }

                if (!stAb.matchesValidParam("ValidCause", cause)) {
                    return false;
                }

                if (applyCommonAbility(stAb, player)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean applyCommonAbility(final StaticAbility stAb, final Player player) {
        if (!stAb.matchesValidParam("ValidPlayer", player)) {
            return false;
        }
        return true;
    }
}
