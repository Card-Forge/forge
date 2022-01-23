package forge.game.staticability;

import forge.game.Game;
import forge.game.GameEntity;
import forge.game.card.Card;
import forge.game.keyword.Keyword;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class StaticAbilityIgnoreHexproofShroud {

    static String HEXPROOF_MODE = "IgnoreHexproof";
    static String SHROUD_MODE = "IgnoreShroud";

    static public boolean ignore(GameEntity entity, final SpellAbility spellAbility, Keyword keyword) {
        final Game game = entity.getGame();
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (keyword.equals(Keyword.HEXPROOF) && !stAb.getParam("Mode").equals(HEXPROOF_MODE)) {
                    continue;
                }
                if (keyword.equals(Keyword.SHROUD) && !stAb.getParam("Mode").equals(SHROUD_MODE)) {
                    continue;
                }
                if (stAb.isSuppressed() || !stAb.checkConditions()) {
                    continue;
                }
                if (commonAbility(stAb, entity, spellAbility)) {
                    return true;
                }
            }
        }
        return false;
    }

    static protected boolean commonAbility(StaticAbility stAb, GameEntity entity, final SpellAbility spellAbility) {
        final Player activator = spellAbility.getActivatingPlayer();

        if (!stAb.matchesValidParam("Activator", activator)) {
            return false;
        }

        if (!stAb.matchesValidParam("ValidEntity", entity)) {
            return false;
        }

        return true;
    }
}
