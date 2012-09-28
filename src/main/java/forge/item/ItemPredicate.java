package forge.item;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;




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
    public static class PredicateBoosterPack implements Predicate<InventoryItem> {

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
    public static class PredicateFatPack implements Predicate<InventoryItem> {

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
    public static class PredicateTournamentPack implements Predicate<InventoryItem> {

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
    public static class PredicateStarterDeck implements Predicate<InventoryItem> {

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
    public static class PredicatePrebuiltDeck implements Predicate<InventoryItem> {

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
        @SuppressWarnings("unchecked")
        public static final Predicate<InventoryItem> IS_PACK = Predicates.or(IS_BOOSTER_PACK, IS_FAT_PACK, IS_TOURNAMENT_PACK);

        /** The Item IsStarterDeck. */
        public static final Predicate<InventoryItem> IS_STARTER_DECK = starterDeck();

        /** The Item IsPrebuiltDeck. */
        public static final Predicate<InventoryItem> IS_PREBUILT_DECK = prebuiltDeck();

        /** The Item IsDeck. */
        public static final Predicate<InventoryItem> IS_DECK = Predicates.or(IS_STARTER_DECK, IS_PREBUILT_DECK);
    }
}
