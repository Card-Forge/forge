package forge.item;

import java.util.function.Predicate;

/**
 * Filtering conditions for miscellaneous InventoryItems.
 */
public abstract class ItemPredicate {

    // Static builder methods - they choose concrete implementation by themselves

    public static final Predicate<Object> IsBoosterPack = BoosterPack.class::isInstance;
    /**
     * Checks that the inventory item is a Prebuilt Deck.
     */
    public static final Predicate<Object> IsPrebuiltDeck = PreconDeck.class::isInstance;
    public static final Predicate<Object> IsFatPack = FatPack.class::isInstance;

    /**
     * Checks that the inventory item is a Tournament Pack.
     */
    public static final Predicate<Object> IsTournamentPack = card -> card instanceof TournamentPack && !((TournamentPack) card).isStarterDeck();

    /**
     * Checks that the inventory item is a Starter Deck.
     */
    public static final Predicate<Object> IsStarterDeck = card -> card instanceof TournamentPack && ((TournamentPack) card).isStarterDeck();

    public static final Predicate<Object> IS_PACK_OR_DECK = IsBoosterPack.or(IsFatPack).or(IsTournamentPack).or(IsStarterDeck).or(IsPrebuiltDeck);
}
