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

import java.util.List;

import forge.AllZone;

/**
 * <p>
 * CustomDraft class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
class CustomLimited {

    /** The Name. */
    private String name;

    /** The Type. */
    // private String type;

    /** The Deck file. */
    private String deckFile;

    /** The Ignore rarity. */
    private Boolean ignoreRarity;

    /** The Singleton. */
    private Boolean singleton = false;

    /** The Num cards. */
    private int numCards = 15;

    /** The Num specials. */
    private int numSpecials = 0;

    /** The Num mythics. */
    private int numMythics = 1;

    /** The Num rares. */
    private int numRares = 1;

    /** The Num uncommons. */
    private int numUncommons = 3;

    /** The Num commons. */
    private int numCommons = 11;

    /** The Num double faced. */
    private int numDoubleFaced = 0;

    /** The Num packs. */
    private int numPacks = 3;

    /** The Land set code. */
    private String landSetCode = AllZone.getCardFactory().getCard("Plains", AllZone.getHumanPlayer())
            .getMostRecentSet();

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
     * @param dfData
     *            the df data
     * @return the custom limited
     */
    public static CustomLimited parse(final List<String> dfData) {
        final CustomLimited cd = new CustomLimited();

        for (final String dd : dfData) {
            final String[] v = dd.split(":", 2);
            final String key = v[0];
            final String value = v.length > 1 ? v[1].trim() : "";

            if (key.equalsIgnoreCase("Name")) {
                cd.setName(value);
            }
            if (key.equalsIgnoreCase("Type")) {
                // cd.type = value;
            }
            if (key.equalsIgnoreCase("DeckFile")) {
                cd.setDeckFile(value);
            }
            if (key.equalsIgnoreCase("IgnoreRarity")) {
                cd.setIgnoreRarity(value.equals("True"));
            }
            if (key.equalsIgnoreCase("Singleton")) {
                cd.setSingleton(value.equals("True"));
            }
            if (key.equalsIgnoreCase("LandSetCode")) {
                cd.setLandSetCode(value);
            }

            if (key.equalsIgnoreCase("NumCards")) {
                cd.setNumCards(Integer.parseInt(value));
            }
            if (key.equalsIgnoreCase("NumDoubleFaced")) {
                cd.setNumDoubleFaced(Integer.parseInt(value));
            }
            if (key.equalsIgnoreCase("NumSpecials")) {
                cd.setNumSpecials(Integer.parseInt(value));
            }
            if (key.equalsIgnoreCase("NumMythics")) {
                cd.setNumMythics(Integer.parseInt(value));
            }
            if (key.equalsIgnoreCase("NumRares")) {
                cd.setNumRares(Integer.parseInt(value));
            }
            if (key.equalsIgnoreCase("NumUncommons")) {
                cd.setNumUncommons(Integer.parseInt(value));
            }
            if (key.equalsIgnoreCase("NumCommons")) {
                cd.setNumCommons(Integer.parseInt(value));
            }
            if (key.equalsIgnoreCase("NumPacks")) {
                cd.setNumPacks(Integer.parseInt(value));
            }
        }
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
     * Gets the num specials.
     * 
     * @return the numSpecials
     */
    public int getNumSpecials() {
        return this.numSpecials;
    }

    /**
     * Sets the num specials.
     * 
     * @param numSpecialsIn
     *            the numSpecials to set
     */
    public void setNumSpecials(final int numSpecialsIn) {
        this.numSpecials = numSpecialsIn;
    }

    /**
     * Gets the singleton.
     * 
     * @return the singleton
     */
    public Boolean getSingleton() {
        return this.singleton;
    }

    /**
     * Sets the singleton.
     * 
     * @param singletonIn
     *            the singleton to set
     */
    public void setSingleton(final Boolean singletonIn) {
        this.singleton = singletonIn;
    }

    /**
     * Gets the ignore rarity.
     * 
     * @return the ignoreRarity
     */
    public Boolean getIgnoreRarity() {
        return this.ignoreRarity;
    }

    /**
     * Sets the ignore rarity.
     * 
     * @param ignoreRarityIn
     *            the ignoreRarity to set
     */
    public void setIgnoreRarity(final Boolean ignoreRarityIn) {
        this.ignoreRarity = ignoreRarityIn;
    }

    /**
     * Gets the num uncommons.
     * 
     * @return the numUncommons
     */
    public int getNumUncommons() {
        return this.numUncommons;
    }

    /**
     * Sets the num uncommons.
     * 
     * @param numUncommonsIn
     *            the numUncommons to set
     */
    public void setNumUncommons(final int numUncommonsIn) {
        this.numUncommons = numUncommonsIn;
    }

    /**
     * Gets the num commons.
     * 
     * @return the numCommons
     */
    public int getNumCommons() {
        return this.numCommons;
    }

    /**
     * Sets the num commons.
     * 
     * @param numCommonsIn
     *            the numCommons to set
     */
    public void setNumCommons(final int numCommonsIn) {
        this.numCommons = numCommonsIn;
    }

    /**
     * Gets the num rares.
     * 
     * @return the numRares
     */
    public int getNumRares() {
        return this.numRares;
    }

    /**
     * Sets the num rares.
     * 
     * @param numRaresIn
     *            the numRares to set
     */
    public void setNumRares(final int numRaresIn) {
        this.numRares = numRaresIn;
    }

    /**
     * Gets the num mythics.
     * 
     * @return the numMythics
     */
    public int getNumMythics() {
        return this.numMythics;
    }

    /**
     * Sets the num mythics.
     * 
     * @param numMythicsIn
     *            the numMythics to set
     */
    public void setNumMythics(final int numMythicsIn) {
        this.numMythics = numMythicsIn;
    }

    /**
     * Gets the deck file.
     * 
     * @return the deckFile
     */
    public String getDeckFile() {
        return this.deckFile;
    }

    /**
     * Sets the deck file.
     * 
     * @param deckFileIn
     *            the deckFile to set
     */
    public void setDeckFile(final String deckFileIn) {
        this.deckFile = deckFileIn;
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

    /**
     * Gets the num double faced.
     * 
     * @return the numDoubleFaced
     */
    public int getNumDoubleFaced() {
        return this.numDoubleFaced;
    }

    /**
     * Sets the num double faced.
     * 
     * @param numDoubleFacedIn
     *            the numDoubleFaced to set
     */
    public void setNumDoubleFaced(final int numDoubleFacedIn) {
        this.numDoubleFaced = numDoubleFacedIn;
    }

    /**
     * @return the name
     */
    private String getName() {
        return this.name;
    }

    /**
     * @param nameIn
     *            the name to set
     */
    private void setName(final String nameIn) {
        this.name = nameIn;
    }
}
