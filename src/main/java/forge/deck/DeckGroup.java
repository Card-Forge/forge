/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Nate
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
package forge.deck;

import java.util.ArrayList;
import java.util.List;


import forge.item.CardPrinted;
import forge.item.ItemPoolView;
import forge.util.closures.Lambda1;

/**
 * TODO: Write javadoc for this type.
 * 
 */
public class DeckGroup extends DeckBase {

    /**
     * Instantiates a new deck group.
     *
     * @param name0 the name0
     */
    public DeckGroup(final String name0) {
        super(name0);
    }

    private static final long serialVersionUID = -1628725522049635829L;
    private Deck humanDeck;
    private final List<Deck> aiDecks = new ArrayList<Deck>();

    /**
     * Gets the human deck.
     *
     * @return the human deck
     */
    public final Deck getHumanDeck() {
        return this.humanDeck;
    }

    /**
     * Gets the ai decks.
     *
     * @return the ai decks
     */
    public final List<Deck> getAiDecks() {
        return this.aiDecks;
    }

    /**
     * Sets the human deck.
     *
     * @param humanDeck the new human deck
     */
    public final void setHumanDeck(final Deck humanDeck) {
        this.humanDeck = humanDeck;
    }

    @Override
    protected void cloneFieldsTo(final DeckBase clone) {
        super.cloneFieldsTo(clone);

        DeckGroup myClone = (DeckGroup) clone;
        myClone.setHumanDeck((Deck) this.getHumanDeck().copyTo(this.getHumanDeck().getName()));

        for (int i = 0; i < this.getAiDecks().size(); i++) {
            Deck src = this.getAiDecks().get(i);
            myClone.addAiDeck((Deck) src.copyTo(src.getName()));
        }
    }

    /**
     * Adds the ai deck.
     *
     * @param aiDeck the ai deck
     */
    public final void addAiDeck(final Deck aiDeck) {
        if (aiDeck == null) {
            return;
        }
        this.aiDecks.add(aiDeck);
    }

    /* (non-Javadoc)
     * @see forge.deck.DeckBase#getCardPool()
     */
    @Override
    public ItemPoolView<CardPrinted> getCardPool() {
        return this.getHumanDeck().getMain();
    }

    /**
     * Adds the ai decks.
     *
     * @param computer the computer
     */
    public void addAiDecks(final Deck[] computer) {
        for (final Deck element : computer) {
            this.aiDecks.add(element);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.deck.DeckBase#newInstance(java.lang.String)
     */
    @Override
    protected DeckBase newInstance(final String name0) {
        return new DeckGroup(name0);
    }

    public static final Lambda1<String, DeckGroup> FN_NAME_SELECTOR = new Lambda1<String, DeckGroup>() {
        @Override
        public String apply(DeckGroup arg1) {
            return arg1.getName();
        }
    };

}
