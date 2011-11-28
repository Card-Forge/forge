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
