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
package forge.item;

import com.google.common.base.Function;
import forge.StaticData;
import forge.deck.Deck;
import forge.deck.io.DeckSerializer;
import forge.deck.io.DeckStorage;
import forge.util.FileSection;
import forge.util.FileUtil;
import forge.util.storage.StorageReaderFolder;

import java.io.File;
import java.io.FilenameFilter;
import java.util.List;
import java.util.Map;

/**
 * TODO: Write javadoc for this type.
 * 
 */
public class PreconDeck implements InventoryItemFromSet {
    private final Deck deck;
    private final String set;
    private final String description;
    private String imageFilename;
    
    // private final SellRules recommendedDeals;

    /*
     * (non-Javadoc)
     * 
     * @see forge.item.InventoryItemFromSet#getName()
     */
    @Override
    public String getName() {
        return this.deck.getName();
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.item.InventoryItem#getType()
     */
    @Override
    public String getItemType() {
        return "Prebuilt Deck";
    }

    @Override
    public String toString() {
        return this.deck.toString();
    }

    public PreconDeck(final Deck d, String set, String description) {
        deck = d;
        this.set = set;
        this.description = description;
    }
    
    public final Deck getDeck() {
        return this.deck;
    }

    /**
     * Gets the recommended deals.
     * 
     * @return the recommended deals
     */
//    public final SellRules getRecommendedDeals() {
//        return this.recommendedDeals;
//    }

    public final String getImageFilename() {
        return imageFilename;
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.item.InventoryItemFromSet#getSet()
     */
    @Override
    public String getEdition() {
        return this.set;
    }

    /**
     * Gets the description.
     *
     * @return the description
     */
    public final String getDescription() {
        return this.description;
    }

    public static final Function<PreconDeck, String> FN_NAME_SELECTOR = new Function<PreconDeck, String>() {
        @Override
        public String apply(PreconDeck arg1) {
            return arg1.getName();
        }
    };

    public static class Reader extends StorageReaderFolder<PreconDeck> {
        public Reader(final File deckDir0) {
            super(deckDir0, PreconDeck.FN_NAME_SELECTOR);
        }

        @Override
        protected PreconDeck read(final File file) {
            return getPreconDeckFromSections(FileSection.parseSections(FileUtil.readFile(file)));
        }

        // To be able to read "shops" section in overloads
        protected PreconDeck getPreconDeckFromSections(final Map<String, List<String>> sections) {
            FileSection kv = FileSection.parse(sections.get("metadata"), "=");
            String imageFilename = kv.get("Image");
            String description = kv.get("Description");
            String deckEdition = kv.get("set");
            String set = deckEdition == null || StaticData.instance().getEditions().get(deckEdition.toUpperCase()) == null ? "n/a" : deckEdition;
            PreconDeck result = new PreconDeck(DeckSerializer.fromSections(sections), set, description);
            result.imageFilename = imageFilename;
            return result;
        }

        @Override
        protected FilenameFilter getFileFilter() {
            return DeckStorage.DCK_FILE_FILTER;
        }
    }
    
    public static final Function<PreconDeck, Deck> FN_GET_DECK = new Function<PreconDeck, Deck>() {
        @Override
        public Deck apply(PreconDeck arg1) {
            return arg1.getDeck();
        }
    };      
    
}
