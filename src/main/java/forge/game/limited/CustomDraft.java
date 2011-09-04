package forge.game.limited;

import forge.AllZone;

/**
 * <p>CustomDraft class.</p>
 *
 * @author Forge
 * @version $Id$
 */
class CustomDraft {
    public String Name;
    public String Type;
    public String DeckFile;
    public Boolean IgnoreRarity;
    public int NumCards = 15;
    public int NumSpecials = 0;
    public int NumMythics = 1;
    public int NumRares = 1;
    public int NumUncommons = 3;
    public int NumCommons = 11;
    public int NumPacks = 3;
    public String LandSetCode = AllZone.getCardFactory().getCard("Plains", AllZone.getHumanPlayer()).getMostRecentSet();
}
