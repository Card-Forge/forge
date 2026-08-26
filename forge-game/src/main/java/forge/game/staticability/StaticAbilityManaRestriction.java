package forge.game.staticability;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class StaticAbilityManaRestriction {
    public static boolean manaRestriction(final SpellAbility sa, final Card source)  {
        final Game game = source.getGame();
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.checkConditions(StaticAbilityMode.ManaRestriction)) {
                    continue;
                }

                if (applyValid(stAb, sa) && !applySource(stAb, source)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean applyValid(final StaticAbility stAb, final SpellAbility sa) {
        if (!stAb.matchesValidParam("ValidSA", sa)) {
            return false;
        }
        return true;
    }

    private static boolean applySource(final StaticAbility stAb, final Card source) {
        if (!stAb.matchesValidParam("ValidSource", source)) {
            return false;
        }
        return true;
    }
}
