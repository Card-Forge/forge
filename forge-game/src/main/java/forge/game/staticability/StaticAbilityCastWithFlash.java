package forge.game.staticability;

import com.google.common.collect.Iterables;

import forge.game.Game;
import forge.game.GameObjectPredicates;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

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
                if (applyWithFlashNeedsTargeting(stAb, sa, card, activator)) {
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

    public static boolean applyWithFlashNeedsTargeting(final StaticAbility stAb, final SpellAbility sa, final Card card, final Player activator) {
        if (!commonParts(stAb, sa, card, activator)) {
            return false;
        }

        return stAb.hasParam("Targeting");
    }

    public static boolean applyWithFlashAbility(final StaticAbility stAb, final SpellAbility sa, final Card card, final Player activator) {
        final Card hostCard = stAb.getHostCard();

        if (!commonParts(stAb, sa, card, activator)) {
            return false;
        }

        if (stAb.hasParam("Targeting")) {
            if (!sa.usesTargeting()) {
                return false;
            }

            String[] valids = stAb.getParam("Targeting").split(",");
            if (!Iterables.any(sa.getTargets(), GameObjectPredicates.restriction(valids, hostCard.getController(), hostCard, stAb))) {
                return false;
            }
        }

        return true;
    }
}
