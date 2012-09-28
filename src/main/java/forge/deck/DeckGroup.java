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
import java.util.TreeMap;
// import java.lang.Double;
import java.util.List;

import com.google.common.base.Function;


import forge.item.CardPrinted;
import forge.item.ItemPoolView;

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
    private List<Deck> aiDecks = new ArrayList<Deck>();

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

    /**
     * Evaluate and 'rank' the ai decks.
     *
     * 
     */
    public final void rankAiDecks() {
        if (this.aiDecks.size() < 2) {
            return;
        }

        // double [] draftValues = new double [this.aiDecks.size()];
        TreeMap<Double, Deck> draftData = new TreeMap<Double, Deck>();

        for (int i = 0; i < this.aiDecks.size(); i++) {
            // draftValues[i] = this.aiDecks.get(i).getDraftValue();
            draftData.put(new Double(this.aiDecks.get(i).getDraftValue()), this.aiDecks.get(i));
            // System.out.println("\nAI Deck " + i  + "(" + this.aiDecks.get(i) + ") has draft value:" + this.aiDecks.get(i).getDraftValue() + "\n\n");
        }

        List<Deck> sortedData = new ArrayList<Deck>(draftData.values());

        this.aiDecks = sortedData;

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

    public static final Function<DeckGroup, String> FN_NAME_SELECTOR = new Function<DeckGroup, String>() {
        @Override
        public String apply(DeckGroup arg1) {
            return arg1.getName();
        }
    };

}
