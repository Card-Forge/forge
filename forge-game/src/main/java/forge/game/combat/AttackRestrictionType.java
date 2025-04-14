package forge.game.combat;

import forge.card.MagicColor;
import forge.game.card.Card;
import forge.game.card.CardPredicates;

import java.util.function.Predicate;

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
                return CardPredicates.isColor((byte) (MagicColor.BLACK | MagicColor.GREEN))
                        // may explicitly not be black/green itself
                        .and(Predicate.not(attacker::equals));
            case NOT_ALONE:
                return x -> true;
            default:
        }
        return null;
    }
}
