/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.card.cardfactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.SwingUtilities;

import forge.AllZone;
import forge.Card;
import forge.CardReader;
import forge.card.CardRules;
import forge.error.ErrorViewer;
import forge.gui.GuiUtils;
import forge.gui.toolbox.FProgressBar;
import forge.item.CardDb;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;
import forge.view.SplashFrame;

/**
 * <p>
 * CardFactory class.
 * </p>
 * 
 * TODO The map field contains Card instances that have not gone through
 * getCard2, and thus lack abilities. However, when a new Card is requested via
 * getCard, it is this map's values that serve as the templates for the values
 * it returns. This class has another field, allCards, which is another copy of
 * the card database. These cards have abilities attached to them, and are owned
 * by the human player by default. <b>It would be better memory-wise if we had
 * only one or the other.</b> We may experiment in the future with using
 * allCard-type values for the map instead of the less complete ones that exist
 * there today.
 * 
 * @author Forge
 * @version $Id$
 */
public class PreloadingCardFactory extends AbstractCardFactory {

    protected final List<Card> allCards = new ArrayList<Card>();
    /**
     * <p>
     * Constructor for CardFactory.
     * </p>
     * 
     * @param filename
     *            a {@link java.lang.String} object.
     */
    public PreloadingCardFactory(final String filename) {
        this(new File(filename));
    }

    /**
     * <p>
     * Constructor for CardFactory.
     * </p>
     * 
     * @param file
     *            a {@link java.io.File} object.
     */
    public PreloadingCardFactory(final File file) {
        super(file);
        GuiUtils.checkEDT("PreloadingCardFactory$constructor", false);

        try {
            this.readCards(file);

            final FProgressBar barProgress = SplashFrame.PROGRESS_BAR;
            if (barProgress != null) {
                SwingUtilities.invokeAndWait(new Runnable() {
                    @Override
                    public void run() {
                        barProgress.reset();
                        barProgress.setDescription("Creating card objects: ");
                    }
                });
            }

            // initialize CardList allCards
            final Iterator<String> it = this.getMap().keySet().iterator();
            if (barProgress != null) { barProgress.setMaximum(this.getMap().size()); }
            Card c;
            while (it.hasNext()) {
                c = this.getCard(it.next().toString(), AllZone.getHumanPlayer());
                this.getAllCards().add(c);
                if (barProgress != null) { barProgress.increment(); }
            }
        } catch (final Exception ex) {
            ErrorViewer.showError(ex);
        }
    } // constructor

    /**
     * <p>
     * readCards.
     * </p>
     * 
     * @param file
     *            a {@link java.io.File} object.
     */
    protected final void readCards(final File file) {
        this.getMap().clear();

        final List<CardRules> listCardRules = new ArrayList<CardRules>();
        final CardReader read = new CardReader(ForgeProps.getFile(NewConstants.CARDSFOLDER), this.getMap(),
                listCardRules);

        // this fills in our map of card names to Card instances.
        read.run();
        CardDb.setup(listCardRules.iterator());

    } // readCard()

    /* (non-Javadoc)
     * @see forge.card.cardfactory.AbstractCardFactory#getAllCards()
     */
    @Override
    protected List<Card> getAllCards() {
        return allCards;
    }

} // end class PreloadingCardFactory
