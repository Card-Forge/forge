package forge.game.player;

import java.util.Comparator;

import com.google.common.base.Predicate;

import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public final class PlayerPredicates {

    public static final Predicate<Player> isTargetableBy(final SpellAbility source) {
        return new Predicate<Player>() {
            @Override
            public boolean apply(final Player p) {
                return p.canBeTargetedBy(source);
            }
        };
    }

    public static final Comparator<Player> compareByZoneSize(final ZoneType zone) {
        return new Comparator<Player>() {
            @Override
            public int compare(Player arg0, Player arg1) {
                return Integer.compare(arg0.getCardsIn(zone).size(), arg1.getCardsIn(zone).size());
            }
        };
    }
}
