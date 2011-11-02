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
    private String type;

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
    private String landSetCode = AllZone.getCardFactory().getCard("Plains", AllZone.getHumanPlayer()).getMostRecentSet();

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
                cd.type = value;
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
     * @return the numCards
     */
    public int getNumCards() {
        return numCards;
    }

    /**
     * @param numCards the numCards to set
     */
    public void setNumCards(int numCards) {
        this.numCards = numCards; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the numPacks
     */
    public int getNumPacks() {
        return numPacks;
    }

    /**
     * @param numPacks the numPacks to set
     */
    public void setNumPacks(int numPacks) {
        this.numPacks = numPacks; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the numSpecials
     */
    public int getNumSpecials() {
        return numSpecials;
    }

    /**
     * @param numSpecials the numSpecials to set
     */
    public void setNumSpecials(int numSpecials) {
        this.numSpecials = numSpecials; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the singleton
     */
    public Boolean getSingleton() {
        return singleton;
    }

    /**
     * @param singleton the singleton to set
     */
    public void setSingleton(Boolean singleton) {
        this.singleton = singleton; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the ignoreRarity
     */
    public Boolean getIgnoreRarity() {
        return ignoreRarity;
    }

    /**
     * @param ignoreRarity the ignoreRarity to set
     */
    public void setIgnoreRarity(Boolean ignoreRarity) {
        this.ignoreRarity = ignoreRarity; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the numUncommons
     */
    public int getNumUncommons() {
        return numUncommons;
    }

    /**
     * @param numUncommons the numUncommons to set
     */
    public void setNumUncommons(int numUncommons) {
        this.numUncommons = numUncommons; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the numCommons
     */
    public int getNumCommons() {
        return numCommons;
    }

    /**
     * @param numCommons the numCommons to set
     */
    public void setNumCommons(int numCommons) {
        this.numCommons = numCommons; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the numRares
     */
    public int getNumRares() {
        return numRares;
    }

    /**
     * @param numRares the numRares to set
     */
    public void setNumRares(int numRares) {
        this.numRares = numRares; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the numMythics
     */
    public int getNumMythics() {
        return numMythics;
    }

    /**
     * @param numMythics the numMythics to set
     */
    public void setNumMythics(int numMythics) {
        this.numMythics = numMythics; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the deckFile
     */
    public String getDeckFile() {
        return deckFile;
    }

    /**
     * @param deckFile the deckFile to set
     */
    public void setDeckFile(String deckFile) {
        this.deckFile = deckFile; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the landSetCode
     */
    public String getLandSetCode() {
        return landSetCode;
    }

    /**
     * @param landSetCode the landSetCode to set
     */
    public void setLandSetCode(String landSetCode) {
        this.landSetCode = landSetCode; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the numDoubleFaced
     */
    public int getNumDoubleFaced() {
        return numDoubleFaced;
    }

    /**
     * @param numDoubleFaced the numDoubleFaced to set
     */
    public void setNumDoubleFaced(int numDoubleFaced) {
        this.numDoubleFaced = numDoubleFaced; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the name
     */
    private String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    private void setName(String name1) {
        this.name = name; // TODO: Add 0 to parameter's name.
    }
}
