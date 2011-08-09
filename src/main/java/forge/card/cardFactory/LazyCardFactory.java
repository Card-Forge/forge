package forge.card.cardFactory;

import java.io.File;
import java.util.Iterator;
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

    /**
     * Construct an instance, pointing it to a specific cardsfolder.
     *
     * @param cardsfolder  a directory containing cardsfolder.zip or
     * subdirectories and txt files.
     */
    public LazyCardFactory(final File cardsfolder) {
        super(cardsfolder);

        getMap().clear();
        cardReader = new CardReader(cardsfolder, getMap());
    }

    /**
     * Getter for cardReader.
     * @return cardReader
     */
    public CardReader getCardReader() {
        return cardReader;
    }

    /**
     * Not implemented; do not call.
     *
     * @return never
     */
    @Override
    public Iterator<Card> iterator() {
        throw new NotImplementedError();
    }

    /**
     * Like AbstractCardFactory.getCard2, but loads the card into the map
     * first if it's not there.
     *
     * @param cardName  the name of the card to fetch
     * @param owner  the owner of the returned card
     *
     * @return a new Card instance with abilities and an owner
     */
    @Override
    protected Card getCard2(final String cardName, final Player owner) {
        final Map<String, Card> cardNamesToCards = getMap();
        Card result = null;
        boolean cardExists = false;

        if (!cardNamesToCards.containsKey(cardName)) {
            final String canonicalASCIIName = CardUtil.canonicalizeCardName(cardName);
            getCardReader().findCard(canonicalASCIIName);

            if (cardNamesToCards.containsKey(cardName)) {
                cardExists = true;
            }
        }

        if (cardExists) {
            result = super.getCard2(cardName, owner);
        }

        return result;
    }
}
