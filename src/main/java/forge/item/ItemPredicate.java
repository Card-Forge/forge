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
     * Checks that the inventory item is a Booster Pack.
     * 
     * @return the predicate
     */
    public static Predicate<InventoryItem> IsBoosterPack = new Predicate<InventoryItem>() {

        @Override
        public boolean apply(final InventoryItem card) {
            return card instanceof BoosterPack;
        }
    };

    /**
     * Checks that the inventory item is a Fat Pack.
     * 
     * @return the predicate
     */
    public static Predicate<InventoryItem> IsFatPack = new Predicate<InventoryItem>() {

        @Override
        public boolean apply(final InventoryItem card) {
            return card instanceof FatPack;
        }
    };

    /**
     * Checks that the inventory item is a Tournament Pack.
     * 
     * @return the predicate
     */
    public static Predicate<InventoryItem> IsTournamentPack = new Predicate<InventoryItem>() {

        @Override
        public boolean apply(final InventoryItem card) {
            return card instanceof TournamentPack && !((TournamentPack) card).isStarterDeck();
        }
    };

    /**
     * Checks that the inventory item is a Starter Deck.
     * 
     * @return the predicate
     */
    public static Predicate<InventoryItem> IsStarterDeck = new Predicate<InventoryItem>() {

        @Override
        public boolean apply(final InventoryItem card) {
            return card instanceof TournamentPack && ((TournamentPack) card).isStarterDeck();
        }
    };

    /**
     * Checks that the inventory item is a Prebuilt Deck.
     * 
     * @return the predicate
     */
    public static Predicate<InventoryItem> IsPrebuiltDeck = new Predicate<InventoryItem>() {

        @Override
        public boolean apply(final InventoryItem card) {
            return card instanceof PreconDeck;
        }
    };

    /**
     * The Class Presets.
     */
    public static class Presets {



        /** The Item IsPack. */
        @SuppressWarnings("unchecked")
        public static final Predicate<InventoryItem> IS_PACK = Predicates.or(IsBoosterPack, IsFatPack, IsTournamentPack);

        /** The Item IsDeck. */
        public static final Predicate<InventoryItem> IS_DECK = Predicates.or(IsStarterDeck, IsPrebuiltDeck);
    }
}
