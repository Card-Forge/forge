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

import com.google.common.base.Function;
import forge.StaticData;
import forge.item.PaperCard;

import java.util.*;

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
        return humanDeck;
    }

    /**
     * Gets the ai decks.
     *
     * @return the ai decks
     */
    public final List<Deck> getAiDecks() {
        return aiDecks;
    }

    /**
     * Sets the human deck.
     *
     * @param humanDeck0 the new human deck
     */
    public final void setHumanDeck(final Deck humanDeck0) {
        humanDeck = humanDeck0;
        if (humanDeck != null) {
            humanDeck.setDirectory(getDirectory());
        }
    }

    /**
     * Evaluate and 'rank' the ai decks.
     *
     * 
     */
    public final void rankAiDecks(Comparator<Deck> comparator) {
        if (aiDecks.size() < 2) {
            return;
        }
        Collections.sort(aiDecks, comparator);
    }
    
    @Override
    public String getItemType() {
        return "Group of decks";
    }        

    @Override
    protected void cloneFieldsTo(final DeckBase clone) {
        super.cloneFieldsTo(clone);

        DeckGroup myClone = (DeckGroup) clone;
        myClone.setHumanDeck((Deck) humanDeck.copyTo(getName())); //human deck name should always match DeckGroup name

        for (int i = 0; i < aiDecks.size(); i++) {
            Deck src = aiDecks.get(i);
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
        aiDeck.setDirectory(getDirectory());
        aiDecks.add(aiDeck);
    }

    /**
     * Adds the ai decks.
     *
     * @param computer the computer
     */
    public void addAiDecks(final Deck[] computer) {
        for (final Deck element : computer) {
            aiDecks.add(element);
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


    public static final Function<DeckGroup, Deck> FN_HUMAN_DECK = new Function<DeckGroup, Deck>() {
        @Override
        public Deck apply(DeckGroup arg1) {
            return arg1.humanDeck;
        }
    };

    @Override
    public boolean isEmpty() {
        return humanDeck == null || humanDeck.isEmpty();
    }

    @Override
    public String getImageKey(boolean altState) {
        return null;
    }


    @Override
    public void importDeck(Deck deck) {
        CardPool draftedCards = this.getHumanDeck().getAllCardsInASinglePool(false);

        this.getHumanDeck().putSection(DeckSection.Main, new CardPool());
        this.getHumanDeck().putSection(DeckSection.Sideboard, new CardPool());

        HashMap<String, Integer> countByName = getCountByName(deck);

        addFromDraftedCardPool(countByName, draftedCards);
        addBasicLands(deck, countByName, draftedCards);
    }

    private HashMap<String, Integer> getCountByName(Deck deck) {
        HashMap<String, Integer> result = new HashMap<String, Integer>();

        for (Map.Entry<PaperCard, Integer> entry: deck.getMain()) {
            PaperCard importedCard = entry.getKey();

            Integer previousCount = result.getOrDefault(importedCard.getName(), 0);
            int countToAdd = entry.getValue();

            result.put(importedCard.getName(), countToAdd + previousCount);
        }

        return result;
    }

    private void addFromDraftedCardPool(HashMap<String, Integer> countByName, CardPool availableCards) {
        for (Map.Entry<PaperCard, Integer> entry: availableCards) {

            PaperCard availableCard = entry.getKey();
            Integer availableCount = entry.getValue();
            int countToAdd = countByName.getOrDefault(availableCard.getName(), 0);

            if (availableCard.getRules().getType().isBasicLand()) {
                // basic lands are added regardless from drafted cards
                continue;
            }

            int countMain = Math.min(availableCount, countToAdd);

            if (countMain > 0) {
                this.getHumanDeck().getMain().add(availableCard, countMain);
                countByName.put(availableCard.getName(), countToAdd - countMain);
            }

            int countSideboard = availableCount - countMain;

            if (countSideboard > 0) {
                CardPool sideboard = this.getHumanDeck().getOrCreate(DeckSection.Sideboard);
                sideboard.add(availableCard, countSideboard);
            }
        }
    }

    private void addBasicLands(Deck deck, HashMap<String, Integer> countByName, CardPool availableCards) {
        HashMap<String, PaperCard> basicLandsByName = getBasicLandsByName(deck, countByName);

        Date dateWithAllCards = StaticData.instance().getEditions().getEarliestDateWithAllCards(availableCards);
        for (String cardName: countByName.keySet()) {

            PaperCard card = basicLandsByName.getOrDefault(cardName, null);

            if (card == null) {
                continue;
            }

            int countToAdd = countByName.get(cardName);

            card = StaticData.instance().getCardByEditionDate(card, dateWithAllCards);
            this.getHumanDeck().getMain().add(card.getName(), card.getEdition(), countToAdd);
        }
    }

    private HashMap<String, PaperCard> getBasicLandsByName(Deck deck, HashMap<String, Integer> countByName) {
        HashMap<String, PaperCard> result = new HashMap<String, PaperCard>();

        for (Map.Entry<PaperCard, Integer> entry: deck.getMain()) {
            PaperCard card = entry.getKey();

            if (!card.getRules().getType().isBasicLand()) {
                continue;
            }

            if (result.containsKey(card.getName())) {
                continue;
            }

            result.put(card.getName(), card);
        }

        return result;
    }
}
