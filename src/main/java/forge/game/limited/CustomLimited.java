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
package forge.game.limited;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import forge.card.CardRarity;
import forge.deck.Deck;
import forge.deck.DeckBase;
import forge.item.CardDb;
import forge.item.CardPrinted;
import forge.item.ItemPool;
import forge.item.ItemPoolView;
import forge.util.FileSection;
import forge.util.storage.IStorageView;

/**
 * <p>
 * CustomDraft class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class CustomLimited extends DeckBase {

    /**
     * TODO: Write javadoc for Constructor.
     *
     * @param name0 the name0
     */
    public CustomLimited(final String name0) {
        super(name0);
    }

    private static final long serialVersionUID = 7435640939026612173L;

    /** The Ignore rarity. */
    private boolean ignoreRarity;

    /** The Singleton. */
    private boolean singleton = false;

    /** The Num cards. */
    private int numCards = 15;

    private final Map<CardRarity, Integer> numRarity = new EnumMap<CardRarity, Integer>(CardRarity.class);

    /** The Num packs. */
    private int numPacks = 3;

    private transient ItemPoolView<CardPrinted> cardPool;

    /** The Land set code. */
    private String landSetCode = CardDb.instance().getCard("Plains", true).getEdition();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return this.getName();
    }

    /**
     * Parses the.
     *
     * @param dfData the df data
     * @param cubes the cubes
     * @return the custom limited
     */
    public static CustomLimited parse(final List<String> dfData, final IStorageView<Deck> cubes) {

        final FileSection data = FileSection.parse(dfData, ":");

        final CustomLimited cd = new CustomLimited(data.get("Name"));
        cd.setIgnoreRarity(data.getBoolean("IgnoreRarity"));
        cd.setSingleton(data.getBoolean("Singleton"));
        cd.setLandSetCode(data.get("LandSetCode"));
        cd.numCards = data.getInt("NumCards", 15);

        cd.numRarity.put(CardRarity.BasicLand, data.getInt("NumBasicLands", 1));
        cd.numRarity.put(CardRarity.Special, data.getInt("NumSpecials"));
        cd.numRarity.put(CardRarity.Rare, data.getInt("NumRares", 1));
        cd.numRarity.put(CardRarity.MythicRare, data.getInt("NumMythics"));
        cd.numRarity.put(CardRarity.Uncommon, data.getInt("NumUncommons", 3));
        cd.numRarity.put(CardRarity.Common, data.getInt("NumCommons", 10));

        cd.numPacks = data.getInt("NumPacks");

        final String deckName = data.get("DeckFile");
        final Deck deckCube = cubes.get(deckName);
        cd.cardPool = deckCube == null ? ItemPool.createFrom(
                        CardDb.instance().getUniqueCards(), CardPrinted.class)
                : deckCube.getMain();

        return cd;
    }

    /**
     * Gets the num cards.
     * 
     * @return the numCards
     */
    public int getNumCards() {
        return this.numCards;
    }

    /**
     * Sets the num cards.
     * 
     * @param numCardsIn
     *            the numCards to set
     */
    public void setNumCards(final int numCardsIn) {
        this.numCards = numCardsIn;
    }

    /**
     * Gets the num packs.
     * 
     * @return the numPacks
     */
    public int getNumPacks() {
        return this.numPacks;
    }

    /**
     * Sets the num packs.
     * 
     * @param numPacksIn
     *            the numPacks to set
     */
    public void setNumPacks(final int numPacksIn) {
        this.numPacks = numPacksIn;
    }

    /**
     * Gets the singleton.
     * 
     * @return the singleton
     */
    public boolean getSingleton() {
        return this.singleton;
    }

    /**
     * Sets the singleton.
     * 
     * @param singletonIn
     *            the singleton to set
     */
    public void setSingleton(final boolean singletonIn) {
        this.singleton = singletonIn;
    }

    /**
     * Gets the ignore rarity.
     * 
     * @return the ignoreRarity
     */
    public boolean getIgnoreRarity() {
        return this.ignoreRarity;
    }

    /**
     * Sets the ignore rarity.
     * 
     * @param ignoreRarityIn
     *            the ignoreRarity to set
     */
    public void setIgnoreRarity(final boolean ignoreRarityIn) {
        this.ignoreRarity = ignoreRarityIn;
    }

    /**
     * Gets the land set code.
     * 
     * @return the landSetCode
     */
    public String getLandSetCode() {
        return this.landSetCode;
    }

    /**
     * Sets the land set code.
     * 
     * @param landSetCodeIn
     *            the landSetCode to set
     */
    public void setLandSetCode(final String landSetCodeIn) {
        this.landSetCode = landSetCodeIn;
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.item.CardCollectionBase#getCardPool()
     */
    public ItemPoolView<CardPrinted> getCardPool() {
        return this.cardPool;
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.deck.DeckBase#getInstance(java.lang.String)
     */
    @Override
    protected DeckBase newInstance(final String name0) {
        return new CustomLimited(name0);
    }

    /**
     * TODO: Write javadoc for this method.
     *
     * @return the numbers by rarity
     */
    public Map<CardRarity, Integer> getNumbersByRarity() {
        return this.numRarity;
    }

}
