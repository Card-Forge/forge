package forge.item;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

/**
 * Filtering conditions for miscellaneous InventoryItems.
 */
public abstract class ItemPredicate {

    // Static builder methods - they choose concrete implementation by themselves

    public static final Predicate<Object> IsBoosterPack = Predicates.instanceOf(BoosterPack.class);
    public static final Predicate<Object> IsPrebuiltDeck = Predicates.instanceOf(PreconDeck.class);
    public static final Predicate<Object> IsFatPack = Predicates.instanceOf(FatPack.class);

    /**
     * Checks that the inventory item is a Tournament Pack.
     * 
     * @return the predicate
     */
    public static final Predicate<InventoryItem> IsTournamentPack = card -> card instanceof TournamentPack && !((TournamentPack) card).isStarterDeck();

    /**
     * Checks that the inventory item is a Starter Deck.
     * 
     * @return the predicate
     */
    public static final Predicate<InventoryItem> IsStarterDeck = card -> card instanceof TournamentPack && ((TournamentPack) card).isStarterDeck();

    /**
     * Checks that the inventory item is a Prebuilt Deck.
     * 
     * @return the predicate
     */

    /**
     * The Class Presets.
     */
    public static class Presets {
        /** The Item IsPack. */
        public static final Predicate<InventoryItem> IS_PACK_OR_DECK = Predicates.or(IsBoosterPack, IsFatPack, IsTournamentPack, IsStarterDeck, IsPrebuiltDeck);
    }
}
