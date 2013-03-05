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

import java.io.File;
import java.util.List;
import java.util.Map;

import com.google.common.base.Function;


import forge.ImageCache;
import forge.Singletons;
import forge.deck.Deck;
import forge.quest.SellRules;
import forge.util.FileSection;
import forge.util.FileUtil;

/**
 * TODO: Write javadoc for this type.
 * 
 */
public class PreconDeck implements InventoryItemFromSet {

    private final Deck deck;
    private final String imageFilename;
    private final String set;
    private final String description;

    private final SellRules recommendedDeals;

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
     * @see forge.item.InventoryItemFromSet#getImageFilename()
     */
    @Override
    public String getImageFilename() {
        return ImageCache.SEALED_PRODUCT + "precons/" + this.imageFilename;
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

    /**
     * Instantiates a new precon deck.
     * 
     * @param f
     *            the f
     */
    public PreconDeck(final File f) {
        final List<String> deckLines = FileUtil.readFile(f);
        final Map<String, List<String>> sections = FileSection.parseSections(deckLines);
        this.deck = Deck.fromSections(sections);

        String setProxy = "n/a";

        FileSection kv = FileSection.parse(sections.get("metadata"), "=");

        imageFilename = kv.get("Image");
        description = kv.get("Description");
        if (Singletons.getModel().getEditions().get(kv.get("set").toUpperCase()) != null) {
            setProxy = kv.get("set");
        }

        this.set = setProxy;
        this.recommendedDeals = new SellRules(sections.get("shop"));

    }

    /**
     * Gets the deck.
     * 
     * @return the deck
     */
    public final Deck getDeck() {
        return this.deck;
    }

    /**
     * Gets the recommended deals.
     * 
     * @return the recommended deals
     */
    public final SellRules getRecommendedDeals() {
        return this.recommendedDeals;
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
    @Override
    public final String getDescription() {
        return this.description;
    }

    public static final Function<PreconDeck, String> FN_NAME_SELECTOR = new Function<PreconDeck, String>() {
        @Override
        public String apply(PreconDeck arg1) {
            return arg1.getName();
        }
    };

}
