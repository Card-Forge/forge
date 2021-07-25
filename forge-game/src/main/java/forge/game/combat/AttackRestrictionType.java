package forge.game.combat;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

import forge.card.MagicColor;
import forge.game.card.Card;
import forge.game.card.CardPredicates;

public enum AttackRestrictionType {

    ONLY_ALONE,
    NEED_GREATER_POWER,
    NEED_BLACK_OR_GREEN,
    NOT_ALONE,
    NEED_TWO_OTHERS,
    NEVER;

    public Predicate<Card> getPredicate(final Card attacker) {
        switch (this) {
        case NEED_GREATER_POWER:
            return CardPredicates.hasGreaterPowerThan(attacker.getNetPower());
        case NEED_BLACK_OR_GREEN:
            return Predicates.and(
                    CardPredicates.isColor((byte) (MagicColor.BLACK | MagicColor.GREEN)),
                    // may explicitly not be black/green itself
                    Predicates.not(Predicates.equalTo(attacker)));
        case NOT_ALONE:
            return Predicates.alwaysTrue();
        default:
        }
        return null;
    }
}
