package forge.game.staticability;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.zone.ZoneType;

public class StaticAbilityCantPhase {

    static String MODE_CANT_PHASE_IN = "CantPhaseIn";
    static String MODE_CANT_PHASE_OUT = "CantPhaseOut";

    static public boolean cantPhaseIn(Card card) {
        return cantPhase(card, MODE_CANT_PHASE_IN);
    }

    static public boolean cantPhaseOut(Card card) {
        return cantPhase(card, MODE_CANT_PHASE_OUT);
    }

    static private boolean cantPhase(Card card, String mode) {
        final Game game = card.getGame();
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.checkConditions(mode)) {
                    continue;
                }
                if (applyCantPhase(stAb, card)) {
                    return true;
                }
            }
        }
        return false;
    }

    static private boolean applyCantPhase(StaticAbility stAb, Card card) {
        return stAb.matchesValidParam("ValidCard", card);
    }
}

