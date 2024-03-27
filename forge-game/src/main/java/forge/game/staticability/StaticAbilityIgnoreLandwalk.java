package forge.game.staticability;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.keyword.KeywordInterface;
import forge.game.zone.ZoneType;

public class StaticAbilityIgnoreLandwalk {

    static String MODE = "IgnoreLandWalk";
    
    public static boolean ignoreLandWalk(Card attacker, Card blocker, KeywordInterface k) {
        final Game game = attacker.getGame();
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.checkConditions(MODE)) {
                    continue;
                }

                if (ignoreLandWalkAbility(stAb, attacker, blocker, k)) {
                    return true;
                }
            }
        }
        return false;

    }
    
    public static boolean ignoreLandWalkAbility(final StaticAbility stAb, Card attacker, Card blocker, KeywordInterface k) {
        if (!stAb.matchesValidParam("ValidAttacker", attacker)) {
            return false;
        }
        if (!stAb.matchesValidParam("ValidBlocker", blocker)) {
            return false;
        }
        if (!stAb.matchesValidParam("ValidKeyword", k.getOriginal())) {
            return false;
        }

        return true;
    }
}
