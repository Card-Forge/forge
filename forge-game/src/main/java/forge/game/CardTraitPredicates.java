package forge.game;

import forge.game.card.Card;
import forge.game.keyword.Keyword;

import java.util.function.Predicate;

public class CardTraitPredicates {

    public static final Predicate<CardTraitBase> isHostCard(final Card host) {
        return sa -> host.equals(sa.getHostCard());
    }

    public static final Predicate<CardTraitBase> isKeyword(final Keyword kw) {
        return sa -> sa.isKeyword(kw);
    }

    public static final Predicate<CardTraitBase> hasParam(final String name) {
        return sa -> sa.hasParam(name);
    }

    public static final Predicate<CardTraitBase> hasParam(final String name, final String val) {
        return sa -> {
            if (!sa.hasParam(name)) {
                return false;
            }
            return val.equals(sa.getParam(name));
        };
    }
}
