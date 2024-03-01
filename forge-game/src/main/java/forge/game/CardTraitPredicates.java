package forge.game;

import com.google.common.base.Predicate;

import forge.game.card.Card;
import forge.game.keyword.Keyword;

public class CardTraitPredicates {

    public static final Predicate<CardTraitBase> isHostCard(final Card host) {
        return new Predicate<CardTraitBase>() {
            @Override
            public boolean apply(final CardTraitBase sa) {
                return host.equals(sa.getHostCard());
            }
        };
    }

    public static final Predicate<CardTraitBase> isKeyword(final Keyword kw) {
        return new Predicate<CardTraitBase>() {
            @Override
            public boolean apply(final CardTraitBase sa) {
                return sa.isKeyword(kw);
            }
        };
    }

    public static final Predicate<CardTraitBase> hasParam(final String name) {
        return new Predicate<CardTraitBase>() {
            @Override
            public boolean apply(final CardTraitBase sa) {
                return sa.hasParam(name);
            }
        };
    }

    public static final Predicate<CardTraitBase> hasParam(final String name, final String val) {
        return new Predicate<CardTraitBase>() {
            @Override
            public boolean apply(final CardTraitBase sa) {
                if (!sa.hasParam(name)) {
                    return false;
                }
                return val.equals(sa.getParam(name));
            }
        };
    }
}
