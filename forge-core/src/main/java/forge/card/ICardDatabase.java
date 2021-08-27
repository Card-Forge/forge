package forge.card;

import com.google.common.base.Predicate;
import forge.card.CardDb.CardArtPreference;
import forge.item.PaperCard;

import java.util.Collection;
import java.util.Date;
import java.util.List;

public interface ICardDatabase extends Iterable<PaperCard> {
    /**
     * Magic Cards Database.
     * --------------------
     * This interface defines the general API for Database Access and Cards' Lookup.
     *
     * Methods for single Card's lookup currently support three alternative strategies:
     * 1. [getCard]: Card search based on a single card's attributes
     *               (i.e. name, edition, art, collectorNumber)
     *
     * 2. [getCardFromSet]: Card Lookup from a single Expansion set.
     *                     Particularly useful in Deck Editors when a specific Set is specified.
     *
     * 3. [getCardFromEditions]: Card search considering a predefined `SetPreference` policy and/or a specified Date
     *                           when no expansion is specified for a card.
     *                           This method is particularly useful for Re-prints whenever no specific
     *                           Expansion is specified (e.g. in Deck Import) and a decision should be made
     *                           on which card to pick. This methods allows to adopt a SetPreference selection
     *                           policy to make this decision.
     *
     * The API also includes methods to fetch Collection of Card (i.e. PaperCard instances):
     * - all cards (no filter)
     * - all unique cards (by name)
     * - all prints of a single card
     * - all cards from a single Expansion Set
     * - all cards compliant with a filter condition (i.e. Predicate)
     *
     * Finally, various utility methods are supported:
     * - Get the foil version of a Card (if Any)
     * - Get the Order Number of a Card in an Expansion Set
     * - Get the number of Print/Arts for a card in a Set (useful for those exp. having multiple arts)
     * */

    /* SINGLE CARD RETRIEVAL METHODS
    *  ============================= */
    // 1. Card Lookup by attributes
    PaperCard getCard(String cardName);
    PaperCard getCard(String cardName, String edition);
    PaperCard getCard(String cardName, String edition, int artIndex);
    // [NEW Methods] Including the card CollectorNumber as criterion for DB lookup
    PaperCard getCard(String cardName, String edition, String collectorNumber);
    PaperCard getCard(String cardName, String edition, int artIndex, String collectorNumber);

    // 2. Card Lookup from a single Expansion Set
    PaperCard getCardFromSet(String cardName, CardEdition edition, boolean isFoil);  // NOT yet used, included for API symmetry
    PaperCard getCardFromSet(String cardName, CardEdition edition, String collectorNumber, boolean isFoil);
    PaperCard getCardFromSet(String cardName, CardEdition edition, int artIndex, boolean isFoil);
    PaperCard getCardFromSet(String cardName, CardEdition edition, int artIndex, String collectorNumber, boolean isFoil);

    // 3. Card lookup based on CardArtPreference Selection Policy
    PaperCard getCardFromEditions(String cardName, CardArtPreference artPreference);
    PaperCard getCardFromEditions(String cardName, CardArtPreference artPreference, int artIndex);

    // 4. Specialised Card Lookup on CardArtPreference Selection and Release Date
    PaperCard getCardFromEditionsReleasedBefore(String cardName, Date releaseDate);
    PaperCard getCardFromEditionsReleasedBefore(String cardName, int artIndex, Date releaseDate);
    PaperCard getCardFromEditionsReleasedBefore(String cardName, CardArtPreference artPreference, Date releaseDate);
    PaperCard getCardFromEditionsReleasedBefore(String cardName, CardArtPreference artPreference, int artIndex, Date releaseDate);

    PaperCard getCardFromEditionsReleasedAfter(String cardName, Date releaseDate);
    PaperCard getCardFromEditionsReleasedAfter(String cardName, int artIndex, Date releaseDate);
    PaperCard getCardFromEditionsReleasedAfter(String cardName, CardArtPreference artPreference, Date releaseDate);
    PaperCard getCardFromEditionsReleasedAfter(String cardName, CardArtPreference artPreference, int artIndex, Date releaseDate);



    /* CARDS COLLECTION RETRIEVAL METHODS
     * ================================== */
    Collection<PaperCard> getAllCards();
    Collection<PaperCard> getAllCards(String cardName);
    Collection<PaperCard> getAllCards(Predicate<PaperCard> predicate);
    Collection<PaperCard> getAllCards(String cardName,Predicate<PaperCard> predicate);
    Collection<PaperCard> getAllCards(CardEdition edition);
    Collection<PaperCard> getUniqueCards();

    /* UTILITY METHODS
     * =============== */
    int getMaxArtIndex(String cardName);
    int getArtCount(String cardName, String edition);
    // Utility Predicates
    Predicate<? super PaperCard> wasPrintedInSets(List<String> allowedSetCodes);
    Predicate<? super PaperCard> wasPrintedAtRarity(CardRarity rarity);
}