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

    static class ValidPredicate implements Predicate<ICardFace> {
        private String valid;

        public ValidPredicate(final String valid) {
            this.valid = valid;
        }

        @Override
        public boolean apply(ICardFace input) {
            String[] k = valid.split("\\.", 2);

            if ("Card".equals(k[0])) {
                // okay
            } else if ("Permanent".equals(k[0])) {
                if (input.getType().isInstant() || input.getType().isSorcery()) {
                    return false;
                }
            } else if (!input.getType().hasStringType(k[0])) {
                return false;
            }
            if (k.length > 1) {
                for (final String m : k[1].split("\\+")) {
                    if (m.contains("ManaCost")) {
                        String manaCost = m.substring(8);
                        if (!hasManaCost(input, manaCost)) {
                            return false;
                        }
                    } else if (!hasProperty(input, m)) {
                        return false;
                    }
                }
            }

            return true;
        }

        static protected boolean hasProperty(ICardFace input, final String v) {
            if (v.startsWith("non")) {
                return !hasProperty(input, v.substring(3));
            } else return input.getType().hasStringType(v);
        }

        static protected boolean hasManaCost(ICardFace input, final String mC) {
            return mC.equals(input.getManaCost().getShortString());
        }

    }

    public static Predicate<ICardFace> valid(final String val) {
        return new ValidPredicate(val);
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
