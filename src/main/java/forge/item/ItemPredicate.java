package forge.item;


import forge.util.closures.Predicate;

/**
 * Filtering conditions for miscellaneous InventoryItems.
 */
public abstract class ItemPredicate {

    // Static builder methods - they choose concrete implementation by
    // themselves
    /**
     * Booster Pack.
     * 
     * @return the predicate
     */
    public static Predicate<InventoryItem> boosterPack() {
        return new PredicateBoosterPack();
    }

    /**
     * Fat Pack.
     * 
     * @return the predicate
     */
    public static Predicate<InventoryItem> fatPack() {
        return new PredicateFatPack();
    }

    /**
     * Tournament Pack.
     * 
     * @return the predicate
     */
    public static Predicate<InventoryItem> tournamentPack() {
        return new PredicateTournamentPack();
    }

    /**
     * Starter Deck.
     * 
     * @return the predicate
     */
    public static Predicate<InventoryItem> starterDeck() {
        return new PredicateStarterDeck();
    }

    /**
     * Prebuilt Deck.
     * 
     * @return the predicate
     */
    public static Predicate<InventoryItem> prebuiltDeck() {
        return new PredicatePrebuiltDeck();
    }

    /**
     * Checks that the inventory item is a Booster Pack.
     * 
     * @return the predicate
     */
    public static class PredicateBoosterPack extends Predicate<InventoryItem> {

        @Override
        public boolean apply(final InventoryItem card) {
            return card.getType() == "Booster Pack";
        }
    }

    /**
     * Checks that the inventory item is a Fat Pack.
     * 
     * @return the predicate
     */
    public static class PredicateFatPack extends Predicate<InventoryItem> {

        @Override
        public boolean apply(final InventoryItem card) {
            return card.getType() == "Fat Pack";
        }
    }

    /**
     * Checks that the inventory item is a Tournament Pack.
     * 
     * @return the predicate
     */
    public static class PredicateTournamentPack extends Predicate<InventoryItem> {

        @Override
        public boolean apply(final InventoryItem card) {
            return card.getType() == "Tournament Pack";
        }
    }

    /**
     * Checks that the inventory item is a Starter Deck.
     * 
     * @return the predicate
     */
    public static class PredicateStarterDeck extends Predicate<InventoryItem> {

        @Override
        public boolean apply(final InventoryItem card) {
            return card.getType() == "Starter Deck";
        }
    }

    /**
     * Checks that the inventory item is a Prebuilt Deck.
     * 
     * @return the predicate
     */
    public static class PredicatePrebuiltDeck extends Predicate<InventoryItem> {

        @Override
        public boolean apply(final InventoryItem card) {
            return card.getType() == "Prebuilt Deck";
        }
    }

    /**
     * The Class Presets.
     */
    public static class Presets {

        /** The Item IsBoosterPack. */
        public static final Predicate<InventoryItem> IS_BOOSTER_PACK = boosterPack();

        /** The Item IsFatPack. */
        public static final Predicate<InventoryItem> IS_FAT_PACK = fatPack();

        /** The Item IsTournamentPack. */
        public static final Predicate<InventoryItem> IS_TOURNAMENT_PACK = tournamentPack();

        /** The Item IsPack. */
        public static final Predicate<InventoryItem> IS_PACK = Predicate.or(Predicate.or(IS_BOOSTER_PACK, IS_FAT_PACK), IS_TOURNAMENT_PACK);

        /** The Item IsStarterDeck. */
        public static final Predicate<InventoryItem> IS_STARTER_DECK = starterDeck();

        /** The Item IsPrebuiltDeck. */
        public static final Predicate<InventoryItem> IS_PREBUILT_DECK = prebuiltDeck();

        /** The Item IsDeck. */
        public static final Predicate<InventoryItem> IS_DECK = Predicate.or(IS_STARTER_DECK, IS_PREBUILT_DECK);
    }
}
