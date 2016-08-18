package forge.card;

import com.google.common.base.Predicate;

import forge.card.mana.ManaCost;


public final class CardFacePredicates {

    private static class PredicateCoreType implements Predicate<ICardFace> {
        private final CardType.CoreType operand;
        private final boolean shouldBeEqual;

        @Override
        public boolean apply(final ICardFace face) {
            if (null == face) {
                return false;
            }
            return this.shouldBeEqual == face.getType().hasType(this.operand);
        }

        public PredicateCoreType(final CardType.CoreType type, final boolean wantEqual) {
            this.operand = type;
            this.shouldBeEqual = wantEqual;
        }
    }

    private static class PredicateSuperType implements Predicate<ICardFace> {
        private final CardType.Supertype operand;
        private final boolean shouldBeEqual;

        @Override
        public boolean apply(final ICardFace face) {
            return this.shouldBeEqual == face.getType().hasSupertype(this.operand);
        }

        public PredicateSuperType(final CardType.Supertype type, final boolean wantEqual) {
            this.operand = type;
            this.shouldBeEqual = wantEqual;
        }
    }

    /**
     * Core type.
     *
     * @param isEqual
     *            the is equal
     * @param type
     *            the type
     * @return the predicate
     */
    public static Predicate<ICardFace> coreType(final boolean isEqual, final CardType.CoreType type) {
        return new PredicateCoreType(type, isEqual);
    }

    /**
     * Super type.
     *
     * @param isEqual
     *            the is equal
     * @param type
     *            the type
     * @return the predicate
     */
    public static Predicate<ICardFace> superType(final boolean isEqual, final CardType.Supertype type) {
        return new PredicateSuperType(type, isEqual);
    }

    public static Predicate<ICardFace> cmc(final int value) {
        return new Predicate<ICardFace>() {
            @Override
            public boolean apply(ICardFace input) {
                ManaCost cost = input.getManaCost();
                return cost != null && cost.getCMC() == value;
            }
        };
    }

    public static class Presets {
        /** The Constant isBasicLand. */
        public static final Predicate<ICardFace> IS_BASIC_LAND = new Predicate<ICardFace>() {
            @Override
            public boolean apply(final ICardFace subject) {
                return subject.getType().isBasicLand();
            }
        };

        /** The Constant isNonBasicLand. */
        public static final Predicate<ICardFace> IS_NONBASIC_LAND = new Predicate<ICardFace>() {
            @Override
            public boolean apply(final ICardFace subject) {
                return subject.getType().isLand() && !subject.getType().isBasicLand();
            }
        };

        /** The Constant isCreature. */
        public static final Predicate<ICardFace> IS_CREATURE = CardFacePredicates
                .coreType(true, CardType.CoreType.Creature);

        public static final Predicate<ICardFace> IS_LEGENDARY = CardFacePredicates
                .superType(true, CardType.Supertype.Legendary);
        
        public static final Predicate<ICardFace> IS_NON_LAND = CardFacePredicates
        		.coreType(false, CardType.CoreType.Land);
    }
}
