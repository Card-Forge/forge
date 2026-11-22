package forge.game.staticability;

import forge.game.card.Card;
import forge.game.spellability.SpellAbility;

public class StaticAbilityManaRestriction {
    public static boolean manaRestriction(final SpellAbility sa, final Card source)  {
        if (!sa.isSpell()) {
            return false;
        }
        // only applies to Static Abilities of the HostCard
        for (final StaticAbility stAb : sa.getHostCard().getStaticAbilities()) {
            if (!stAb.checkConditions(StaticAbilityMode.ManaRestriction)) {
                continue;
            }

            if (!applySource(stAb, source)) {
                return true;
            }
        }
        return false;
    }

    private static boolean applySource(final StaticAbility stAb, final Card source) {
        if (!stAb.matchesValidParam("ValidSource", source)) {
            return false;
        }
        return true;
    }
}
