package forge.card.cardFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.slightlymagic.braids.util.NotImplementedError;
import forge.Card;
import forge.CardReader;
import forge.CardUtil;
import forge.Player;

/**
 * Like PreloadingCardFactory, but loads cards one at a time, instead of all at
 * once; only used for unit testing at this time.
 * 
 * Iteration has been disabled for this class.
 */
public class LazyCardFactory extends AbstractCardFactory {

    private final CardReader cardReader;
    private final List<String> cardsFailedToLoad = new ArrayList<String>();

    /**
     * Construct an instance, pointing it to a specific cardsfolder.
     * 
     * @param cardsfolder
     *            a directory containing cardsfolder.zip or subdirectories and
     *            txt files.
     */
    public LazyCardFactory(final File cardsfolder) {
        super(cardsfolder);

        this.getMap().clear();
        this.cardReader = new CardReader(cardsfolder, this.getMap());
    }

    /**
     * Getter for cardReader.
     * 
     * @return cardReader
     */
    public final CardReader getCardReader() {
        return this.cardReader;
    }

    /**
     * Not implemented; do not call.
     * 
     * @return never
     */
    @Override
    public final Iterator<Card> iterator() {
        throw new NotImplementedError();
    }

    /**
     * Like AbstractCardFactory.getCard2, but loads the card into the map first
     * if it's not there.
     * 
     * @param cardName
     *            the name of the card to fetch
     * @param owner
     *            the owner of the returned card
     * 
     * @return a new Card instance with abilities and an owner
     */
    @Override
    protected final Card getCard2(final String cardName, final Player owner) {
        final Map<String, Card> cardNamesToCards = this.getMap();
        Card result = null;
        boolean wasLoaded = cardNamesToCards.containsKey(cardName);

        if (!wasLoaded) {

            if (this.cardsFailedToLoad.contains(cardName)) {
                return null; // no more System.err, exceptions of other drama -
                             // just return null.
            }

            final String canonicalASCIIName = CardUtil.canonicalizeCardName(cardName);
            final Card cardRequested = this.getCardReader().findCard(canonicalASCIIName);
            if (null != cardRequested) {
                cardNamesToCards.put(cardName, cardRequested);
                wasLoaded = true;
            } else {
                this.cardsFailedToLoad.add(cardName);
                System.err.println(String.format("LazyCF: Tried to read from disk card '%s' but not found it!",
                        cardName));
                return null;
            }
        }

        // Factory should return us a copy, ready for changes.
        if (wasLoaded) {
            result = super.getCard2(cardName, owner);
        }

        return result;
    }
}
