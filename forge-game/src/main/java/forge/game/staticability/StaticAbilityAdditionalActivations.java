package forge.game.staticability;

import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class StaticAbilityAdditionalActivations {

    public static int getLimit(final Card card, final SpellAbility sa, Player activator) {
        return getLimit(card, sa, activator, 1);
    }

    public static int getLimit(final Card card, final SpellAbility sa, Player activator, int def) {
        int result = def;
        int additional = 0;
        for (final Card ca : card.getGame().getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.checkConditions(StaticAbilityMode.Activations)) {
                    continue;
                }

                if (!isValid(stAb, card, sa, activator)) {
                    continue;
                }
                if (stAb.hasParam("MinLimit")) {
                    int min = AbilityUtils.calculateAmount(stAb.getHostCard(), stAb.getParam("MinLimit"), stAb);
                    if (min == -1) {
                        return Integer.MAX_VALUE;
                    }
                    result = Math.max(result, min);
                }
                if (stAb.hasParam("Additional")) {
                    additional += AbilityUtils.calculateAmount(stAb.getHostCard(), stAb.getParam("Additional"), stAb);
                }

            }
        }
        return result + additional;
    }

    public static boolean isValid(final StaticAbility stAb, final Card card, final SpellAbility sa, final Player activator) {
        if (!stAb.matchesValidParam("ValidCard", card)) {
            return false;
        }
        if (!stAb.matchesValidParam("ValidSA", sa)) {
            return false;
        }
        if (!stAb.matchesValidParam("ValidPlayer", activator)) {
            return false;
        }
        return true;
    }
}
