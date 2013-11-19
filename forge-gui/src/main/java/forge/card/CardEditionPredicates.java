package forge.card;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import forge.Singletons;
import forge.game.GameFormat;
import forge.util.Aggregates;

/**
 * The Class Predicates.
 */
public abstract class CardEditionPredicates {

    /** The Constant canMakeBooster. */
    public static final Predicate<CardEdition> CAN_MAKE_BOOSTER = new CanMakeBooster();

    private static class CanMakeBooster implements Predicate<CardEdition> {
        @Override
        public boolean apply(final CardEdition subject) {
            return subject.hasBoosterTemplate();
        }
    }


    public final static CardEdition getRandomSetWithAllBasicLands(Iterable<CardEdition> allEditions) {
        return Aggregates.random(Iterables.filter(allEditions, CardEditionPredicates.hasBasicLands));
    }
    
    public static final Predicate<CardEdition> HAS_TOURNAMENT_PACK = new CanMakeStarter();
    private static class CanMakeStarter implements Predicate<CardEdition> {
        @Override
        public boolean apply(final CardEdition subject) {
            return Singletons.getMagicDb().getTournamentPacks().contains(subject.getCode());
        }
    }

    public static final Predicate<CardEdition> HAS_FAT_PACK = new CanMakeFatPack();
    private static class CanMakeFatPack implements Predicate<CardEdition> {
        @Override
        public boolean apply(final CardEdition subject) {
            return Singletons.getMagicDb().getFatPacks().contains(subject.getCode());
        }
    }

    /**
     * Checks if is legal in format.
     *
     * @param format the format
     * @return the predicate
     */
    public static final Predicate<CardEdition> isLegalInFormat(final GameFormat format) {
        return new LegalInFormat(format);
    }

    private static class LegalInFormat implements Predicate<CardEdition> {
        private final GameFormat format;

        public LegalInFormat(final GameFormat fmt) {
            this.format = fmt;
        }

        @Override
        public boolean apply(final CardEdition subject) {
            return this.format.isSetLegal(subject.getCode());
        }
    }

    public static final Predicate<CardEdition> hasBasicLands = new Predicate<CardEdition>() {
        @Override
        public boolean apply(CardEdition ed) {
            for(String landName : MagicColor.Constant.BASIC_LANDS) {
                if (null == Singletons.getMagicDb().getCommonCards().tryGetCard(landName, ed.getCode(), 0))
                    return false;
            }
            return true;
        };
    };

}