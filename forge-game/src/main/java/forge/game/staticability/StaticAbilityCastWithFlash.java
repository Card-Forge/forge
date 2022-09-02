package forge.game.staticability;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Expressions;

public class StaticAbilityCastWithFlash {

    static String MODE = "CastWithFlash";

    public static boolean anyWithFlashNeedsTargeting(final SpellAbility sa, final Card card, final Player activator) {
        final Game game = activator.getGame();
        final CardCollection allp = new CardCollection(game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES));
        allp.add(card);
        for (final Card ca : allp) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.getParam("Mode").equals(MODE) || stAb.isSuppressed() || !stAb.checkConditions()) {
                    continue;
                }
                if (applyWithFlashNeedsInfo(stAb, sa, card, activator)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean anyWithFlash(final SpellAbility sa, final Card card, final Player activator) {
        final Game game = activator.getGame();
        final CardCollection allp = new CardCollection(game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES));
        allp.add(card);
        for (final Card ca : allp) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.getParam("Mode").equals(MODE) || stAb.isSuppressed() || !stAb.checkConditions()) {
                    continue;
                }
                if (applyWithFlashAbility(stAb, sa, card, activator)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean commonParts(final StaticAbility stAb, final SpellAbility sa, final Card card, final Player activator) {
        if (!stAb.matchesValidParam("ValidCard", card)) {
            return false;
        }

        if (!stAb.matchesValidParam("ValidSA", sa)) {
            return false;
        }

        if (!stAb.matchesValidParam("Caster", activator)) {
            return false;
        }
        return true;
    }

    public static boolean applyWithFlashNeedsInfo(final StaticAbility stAb, final SpellAbility sa, final Card card, final Player activator) {
        if (!commonParts(stAb, sa, card, activator)) {
            return false;
        }

        return stAb.hasParam("Targeting") || stAb.hasParam("XCondition");
    }

    public static boolean applyWithFlashAbility(final StaticAbility stAb, final SpellAbility sa, final Card card, final Player activator) {
        if (!commonParts(stAb, sa, card, activator)) {
            return false;
        }

        if (stAb.hasParam("Targeting")) {
            if (!sa.usesTargeting()) {
                return false;
            }

            if (!stAb.matchesValidParam("Targeting", sa.getTargets())) {
                return false;
            }
        }

        if (stAb.hasParam("XCondition")) {
            final String value = stAb.getParam("XCondition");
            String comparator = value.substring(0, 2);
            int y = AbilityUtils.calculateAmount(sa.getHostCard(), value.substring(2), sa);
            if (!Expressions.compare(sa.getXManaCostPaid(), comparator, y)) {
                return false;
            }

        }

        return true;
    }
}
