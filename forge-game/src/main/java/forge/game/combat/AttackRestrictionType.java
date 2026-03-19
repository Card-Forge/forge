package forge.game.combat;

import forge.game.card.Card;
import forge.game.card.CardPredicates;

import java.util.function.Predicate;

public enum AttackRestrictionType {

    ONLY_ALONE,
    NEED_GREATER_POWER,
    NOT_ALONE,
    NEED_TWO_OTHERS,
    NEVER;

    public Predicate<Card> getPredicate(final Card attacker) {
        switch (this) {
            case NEED_GREATER_POWER:
                return CardPredicates.hasGreaterPowerThan(attacker.getNetPower());
            case NOT_ALONE:
                return x -> true;
            default:
        }
        return null;
    }
}
