package forge.card;

import forge.card.mana.ManaCost;

import java.util.function.Predicate;


public final class CardFacePredicates {

    private static class PredicateCoreType implements Predicate<ICardFace> {
        private final CardType.CoreType operand;
        private final boolean shouldBeEqual;

        @Override
        public boolean test(final ICardFace face) {
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
        public boolean test(final ICardFace face) {
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
        return input -> {
            ManaCost cost = input.getManaCost();
            return cost != null && cost.getCMC() == value;
        };
    }

    static class ValidPredicate implements Predicate<ICardFace> {
        private String valid;

        public ValidPredicate(final String valid) {
            this.valid = valid;
        }

        @Override
        public boolean test(ICardFace input) {
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
                    } else if (m.contains("cmcEQ")) {
                        int i = Integer.parseInt(m.substring(5));
                        if (!hasCMC(input, i)) return false;
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

        static protected boolean hasCMC(ICardFace input, final int value) {
            ManaCost cost = input.getManaCost();
            return cost != null && cost.getCMC() == value;
        }

    }

    public static Predicate<ICardFace> valid(final String val) {
        return new ValidPredicate(val);
    }

    public static final Predicate<ICardFace> IS_BASIC_LAND = subject -> subject.getType().isBasicLand();
    public static final Predicate<ICardFace> IS_NONBASIC_LAND = subject -> subject.getType().isLand() && !subject.getType().isBasicLand();
    public static final Predicate<ICardFace> IS_CREATURE = CardFacePredicates.coreType(true, CardType.CoreType.Creature);
    public static final Predicate<ICardFace> IS_LEGENDARY = CardFacePredicates.superType(true, CardType.Supertype.Legendary);
    public static final Predicate<ICardFace> IS_NON_LAND = CardFacePredicates.coreType(false, CardType.CoreType.Land);
}
