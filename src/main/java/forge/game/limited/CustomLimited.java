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
    public String Name;

    /** The Type. */
    public String Type;

    /** The Deck file. */
    public String DeckFile;

    /** The Ignore rarity. */
    public Boolean IgnoreRarity;

    /** The Singleton. */
    public Boolean Singleton = false;

    /** The Num cards. */
    public int NumCards = 15;

    /** The Num specials. */
    public int NumSpecials = 0;

    /** The Num mythics. */
    public int NumMythics = 1;

    /** The Num rares. */
    public int NumRares = 1;

    /** The Num uncommons. */
    public int NumUncommons = 3;

    /** The Num commons. */
    public int NumCommons = 11;

    /** The Num double faced. */
    public int NumDoubleFaced = 0;

    /** The Num packs. */
    public int NumPacks = 3;

    /** The Land set code. */
    public String LandSetCode = AllZone.getCardFactory().getCard("Plains", AllZone.getHumanPlayer()).getMostRecentSet();

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return this.Name;
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
                cd.Name = value;
            }
            if (key.equalsIgnoreCase("Type")) {
                cd.Type = value;
            }
            if (key.equalsIgnoreCase("DeckFile")) {
                cd.DeckFile = value;
            }
            if (key.equalsIgnoreCase("IgnoreRarity")) {
                cd.IgnoreRarity = value.equals("True");
            }
            if (key.equalsIgnoreCase("Singleton")) {
                cd.Singleton = value.equals("True");
            }
            if (key.equalsIgnoreCase("LandSetCode")) {
                cd.LandSetCode = value;
            }

            if (key.equalsIgnoreCase("NumCards")) {
                cd.NumCards = Integer.parseInt(value);
            }
            if (key.equalsIgnoreCase("NumDoubleFaced")) {
                cd.NumDoubleFaced = Integer.parseInt(value);
            }
            if (key.equalsIgnoreCase("NumSpecials")) {
                cd.NumSpecials = Integer.parseInt(value);
            }
            if (key.equalsIgnoreCase("NumMythics")) {
                cd.NumMythics = Integer.parseInt(value);
            }
            if (key.equalsIgnoreCase("NumRares")) {
                cd.NumRares = Integer.parseInt(value);
            }
            if (key.equalsIgnoreCase("NumUncommons")) {
                cd.NumUncommons = Integer.parseInt(value);
            }
            if (key.equalsIgnoreCase("NumCommons")) {
                cd.NumCommons = Integer.parseInt(value);
            }
            if (key.equalsIgnoreCase("NumPacks")) {
                cd.NumPacks = Integer.parseInt(value);
            }
        }
        return cd;
    }
}
