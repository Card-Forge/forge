package forge;

import forge.item.BoosterPack;
import forge.item.FatPack;
import forge.item.InventoryItem;
import forge.item.PaperCard;
import forge.item.PaperToken;
import forge.item.PreconDeck;
import forge.item.TournamentPack;

public class ImageKeys {
    public static final String CARD_PREFIX           = "c:";
    public static final String TOKEN_PREFIX          = "t:";
    public static final String ICON_PREFIX           = "i:";
    public static final String BOOSTER_PREFIX        = "b:";
    public static final String FATPACK_PREFIX        = "f:";
    public static final String PRECON_PREFIX         = "p:";
    public static final String TOURNAMENTPACK_PREFIX = "o:";
    
    public static final String MORPH_IMAGE          = "morph";
    
    public static final String BACKFACE_POSTFIX  = "$alt";
    
    // Inventory items don't have to know how a certain client should draw them. 
    // That's why this method is not encapsulated and overloaded in the InventoryItem descendants
    public static String getImageKey(InventoryItem ii, boolean altState) {
        if ( ii instanceof PaperCard ) {
            PaperCard cp =  (PaperCard)ii;
            return ImageKeys.CARD_PREFIX + cp.getName() + "|" + cp.getEdition() + "|" + cp.getArtIndex() + (altState ? BACKFACE_POSTFIX : "");
        }
        if ( ii instanceof TournamentPack )
            return ImageKeys.TOURNAMENTPACK_PREFIX + ((TournamentPack)ii).getEdition();
        if ( ii instanceof BoosterPack ) {
            BoosterPack bp = (BoosterPack)ii;
            int cntPics = StaticData.instance().getEditions().get(bp.getEdition()).getCntBoosterPictures();
            String suffix = (1 >= cntPics) ? "" : ("_" + bp.getArtIndex());
            return ImageKeys.BOOSTER_PREFIX + bp.getEdition() + suffix;
        }
        if ( ii instanceof FatPack )
            return ImageKeys.FATPACK_PREFIX + ((FatPack)ii).getEdition();
        if ( ii instanceof PreconDeck )
            return ImageKeys.PRECON_PREFIX + ((PreconDeck)ii).getImageFilename();
        if ( ii instanceof PaperToken ) 
            return ImageKeys.TOKEN_PREFIX + ((PaperToken)ii).getImageFilename();
        return null;
    }

    public static String getTokenKey(String tokenName) {
        return ImageKeys.TOKEN_PREFIX + tokenName;
    }    
}
