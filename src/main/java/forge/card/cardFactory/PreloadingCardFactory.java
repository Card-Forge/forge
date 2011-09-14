package forge.card.cardFactory;


import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import forge.AllZone;
import forge.Card;
import forge.CardReader;
import forge.card.CardRules;
import forge.error.ErrorViewer;
import forge.item.CardDb;
import forge.properties.ForgeProps;

/**
 * <p>CardFactory class.</p>
 *
 * TODO The map field contains Card instances that have not gone through
 * getCard2, and thus lack abilities.  However, when a new
 * Card is requested via getCard, it is this map's values that serve as
 * the templates for the values it returns.  This class has another field,
 * allCards, which is another copy of the card database.  These cards have
 * abilities attached to them, and are owned by the human player by
 * default.  <b>It would be better memory-wise if we had only one or the
 * other.</b>  We may experiment in the future with using allCard-type
 * values for the map instead of the less complete ones that exist there
 * today.
 *
 * @author Forge
 * @version $Id$
 */
public class PreloadingCardFactory extends AbstractCardFactory {
    /**
     * <p>Constructor for CardFactory.</p>
     *
     * @param filename a {@link java.lang.String} object.
     */
    public PreloadingCardFactory(final String filename) {
        this(new File(filename));
    }

    /**
     * <p>Constructor for CardFactory.</p>
     *
     * @param file a {@link java.io.File} object.
     */
    public PreloadingCardFactory(final File file) {
        super(file);

        try {
            readCards(file);

            // initialize CardList allCards
            Iterator<String> it = getMap().keySet().iterator();
            Card c;
            while (it.hasNext()) {
                c = getCard(it.next().toString(), AllZone.getHumanPlayer());
                getAllCards().add(c);
            }
        } catch (Exception ex) {
            ErrorViewer.showError(ex);
        }
    } // constructor


    /**
     * <p>readCards.</p>
     *
     * @param file a {@link java.io.File} object.
     */
    protected final void readCards(final File file) {
        getMap().clear();

        List<CardRules> listCardRules = new ArrayList<CardRules>();
        CardReader read = new CardReader(ForgeProps.getFile(CARDSFOLDER), getMap(), listCardRules);

        // this fills in our map of card names to Card instances.
        read.run();
        CardDb.setup(listCardRules.iterator());

    } // readCard()

} //end class PreloadingCardFactory
