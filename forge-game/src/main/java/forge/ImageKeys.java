package forge;

import forge.card.CardDb;
import forge.item.*;

public class ImageKeys {
    public static final String CARD_PREFIX           = "c:";
    public static final String TOKEN_PREFIX          = "t:";
    public static final String ICON_PREFIX           = "i:";
    public static final String BOOSTER_PREFIX        = "b:";
    public static final String FATPACK_PREFIX        = "f:";
    public static final String BOOSTERBOX_PREFIX     = "x:";
    public static final String PRECON_PREFIX         = "p:";
    public static final String TOURNAMENTPACK_PREFIX = "o:";

    public static final String MORPH_IMAGE          = "morph";
    public static final String HIDDEN_CARD          = TOKEN_PREFIX + MORPH_IMAGE;
    
    public static final String BACKFACE_POSTFIX  = "$alt";

    public static String getImageKey(PaperCard pc, boolean altState) {
        return ImageKeys.CARD_PREFIX + pc.getName() + CardDb.NameSetSeparator + pc.getEdition() + CardDb.NameSetSeparator + pc.getArtIndex() + (altState ? BACKFACE_POSTFIX : "");
    }

    // Inventory items don't have to know how a certain client should draw them. 
    // That's why this method is not encapsulated and overloaded in the InventoryItem descendants
    public static String getImageKey(InventoryItem ii, boolean altState) {
        if (ii instanceof PaperCard) {
            return getImageKey((PaperCard)ii, altState);
        }
        if (ii instanceof TournamentPack) {
            return ImageKeys.TOURNAMENTPACK_PREFIX + ((TournamentPack)ii).getEdition();
        }
        if (ii instanceof BoosterPack) {
            BoosterPack bp = (BoosterPack)ii;
            int cntPics = StaticData.instance().getEditions().get(bp.getEdition()).getCntBoosterPictures();
            String suffix = (1 >= cntPics) ? "" : ("_" + bp.getArtIndex());
            return ImageKeys.BOOSTER_PREFIX + bp.getEdition() + suffix;
        }
        if (ii instanceof FatPack) {
            return ImageKeys.FATPACK_PREFIX + ((FatPack)ii).getEdition();
        }
        if (ii instanceof BoosterBox) {
            return ImageKeys.BOOSTERBOX_PREFIX + ((BoosterBox)ii).getEdition();
        }
        if (ii instanceof PreconDeck) {
            return ImageKeys.PRECON_PREFIX + ((PreconDeck)ii).getImageFilename();
        }
        if (ii instanceof PaperToken) {
            return ImageKeys.TOKEN_PREFIX + ((PaperToken)ii).getImageFilename();
        }
        return null;
    }

    public static String getTokenKey(String tokenName) {
        return ImageKeys.TOKEN_PREFIX + tokenName;
    }    
}
