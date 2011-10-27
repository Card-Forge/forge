package forge.game.limited;

import java.util.List;

import forge.AllZone;

/**
 * <p>CustomDraft class.</p>
 *
 * @author Forge
 * @version $Id$
 */
class CustomLimited {
    public String Name;
    public String Type;
    public String DeckFile;
    public Boolean IgnoreRarity;
    public Boolean Singleton = false;
    public int NumCards = 15;
    public int NumSpecials = 0;
    public int NumMythics = 1;
    public int NumRares = 1;
    public int NumUncommons = 3;
    public int NumCommons = 11;
    public int NumDoubleFaced = 0;
    public int NumPacks = 3;
    public String LandSetCode = AllZone.getCardFactory().getCard("Plains", AllZone.getHumanPlayer()).getMostRecentSet();
    
    @Override public String toString() { return Name; }
    
    public static CustomLimited parse(List<String> dfData)
    {
        CustomLimited cd = new CustomLimited();

        for (String dd : dfData) {
            String[] v = dd.split(":", 2);
            String key = v[0];
            String value = v.length > 1 ? v[1].trim() : "";
            
            if (key.equalsIgnoreCase("Name")) { cd.Name = value; }
            if (key.equalsIgnoreCase("Type")) { cd.Type = value; }
            if (key.equalsIgnoreCase("DeckFile")) { cd.DeckFile = value; }
            if (key.equalsIgnoreCase("IgnoreRarity")) { cd.IgnoreRarity = value.equals("True"); }
            if (key.equalsIgnoreCase("Singleton")) { cd.Singleton = value.equals("True"); }
            if (key.equalsIgnoreCase("LandSetCode")) { cd.LandSetCode = value; }

            if (key.equalsIgnoreCase("NumCards")) { cd.NumCards = Integer.parseInt(value); }
            if (key.equalsIgnoreCase("NumDoubleFaced")) { cd.NumDoubleFaced = Integer.parseInt(value); }
            if (key.equalsIgnoreCase("NumSpecials")) { cd.NumSpecials = Integer.parseInt(value); }
            if (key.equalsIgnoreCase("NumMythics")) { cd.NumMythics = Integer.parseInt(value); }
            if (key.equalsIgnoreCase("NumRares")) { cd.NumRares = Integer.parseInt(value); }
            if (key.equalsIgnoreCase("NumUncommons")) { cd.NumUncommons = Integer.parseInt(value); }
            if (key.equalsIgnoreCase("NumCommons")) { cd.NumCommons = Integer.parseInt(value); }
            if (key.equalsIgnoreCase("NumPacks")) { cd.NumPacks = Integer.parseInt(value); }
        }
        return cd;
    }
}
