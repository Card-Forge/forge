package forge.game.staticability;

import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.mana.ManaConversionMatrix;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class StaticAbilityManaConvert {

    static String MODE = "ManaConvert";

    public static boolean manaConvert(ManaConversionMatrix matrix, Player p, Card card, SpellAbility sa) {
        final Game game = p.getGame();
        boolean changed = false;
        for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                if (!stAb.checkConditions(MODE)) {
                    continue;
                }
                if (checkManaConvert(stAb, p, card, sa)) {
                    AbilityUtils.applyManaColorConversion(matrix, stAb.getParam("ManaConversion"));
                    changed = true;
                }
            }
        }
        return changed;
    }

    public static boolean checkManaConvert(StaticAbility stAb, Player p, Card card, SpellAbility sa) {
        if (!stAb.matchesValidParam("ValidPlayer", p)) {
            return false;
        }
        if (!stAb.matchesValidParam("ValidCard", card)) {
            return false;
        }
        if (!stAb.matchesValidParam("ValidSA", sa)) {
            return false;
        }
        return true;
    }
}
