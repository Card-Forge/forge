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
    //private String type;

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
                //cd.type = value;
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
     * @param numCardsIn the numCards to set
     */
    public void setNumCards(int numCardsIn) {
        this.numCards = numCardsIn;
    }

    /**
     * @return the numPacks
     */
    public int getNumPacks() {
        return numPacks;
    }

    /**
     * @param numPacksIn the numPacks to set
     */
    public void setNumPacks(int numPacksIn) {
        this.numPacks = numPacksIn;
    }

    /**
     * @return the numSpecials
     */
    public int getNumSpecials() {
        return numSpecials;
    }

    /**
     * @param numSpecialsIn the numSpecials to set
     */
    public void setNumSpecials(int numSpecialsIn) {
        this.numSpecials = numSpecialsIn;
    }

    /**
     * @return the singleton
     */
    public Boolean getSingleton() {
        return singleton;
    }

    /**
     * @param singletonIn the singleton to set
     */
    public void setSingleton(Boolean singletonIn) {
        this.singleton = singletonIn;
    }

    /**
     * @return the ignoreRarity
     */
    public Boolean getIgnoreRarity() {
        return ignoreRarity;
    }

    /**
     * @param ignoreRarityIn the ignoreRarity to set
     */
    public void setIgnoreRarity(Boolean ignoreRarityIn) {
        this.ignoreRarity = ignoreRarityIn;
    }

    /**
     * @return the numUncommons
     */
    public int getNumUncommons() {
        return numUncommons;
    }

    /**
     * @param numUncommonsIn the numUncommons to set
     */
    public void setNumUncommons(int numUncommonsIn) {
        this.numUncommons = numUncommonsIn;
    }

    /**
     * @return the numCommons
     */
    public int getNumCommons() {
        return numCommons;
    }

    /**
     * @param numCommonsIn the numCommons to set
     */
    public void setNumCommons(int numCommonsIn) {
        this.numCommons = numCommonsIn;
    }

    /**
     * @return the numRares
     */
    public int getNumRares() {
        return numRares;
    }

    /**
     * @param numRaresIn the numRares to set
     */
    public void setNumRares(int numRaresIn) {
        this.numRares = numRaresIn;
    }

    /**
     * @return the numMythics
     */
    public int getNumMythics() {
        return numMythics;
    }

    /**
     * @param numMythicsIn the numMythics to set
     */
    public void setNumMythics(int numMythicsIn) {
        this.numMythics = numMythicsIn;
    }

    /**
     * @return the deckFile
     */
    public String getDeckFile() {
        return deckFile;
    }

    /**
     * @param deckFileIn the deckFile to set
     */
    public void setDeckFile(String deckFileIn) {
        this.deckFile = deckFileIn;
    }

    /**
     * @return the landSetCode
     */
    public String getLandSetCode() {
        return landSetCode;
    }

    /**
     * @param landSetCodeIn the landSetCode to set
     */
    public void setLandSetCode(String landSetCodeIn) {
        this.landSetCode = landSetCodeIn;
    }

    /**
     * @return the numDoubleFaced
     */
    public int getNumDoubleFaced() {
        return numDoubleFaced;
    }

    /**
     * @param numDoubleFacedIn the numDoubleFaced to set
     */
    public void setNumDoubleFaced(int numDoubleFacedIn) {
        this.numDoubleFaced = numDoubleFacedIn;
    }

    /**
     * @return the name
     */
    private String getName() {
        return name;
    }

    /**
     * @param nameIn the name to set
     */
    private void setName(String nameIn) {
        this.name = nameIn;
    }
}
