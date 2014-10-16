package forge.assets;

import org.apache.commons.lang3.StringUtils;

import forge.StaticData;
import forge.card.CardDb;
import forge.card.CardRules;
import forge.card.CardSplitType;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.properties.ForgeConstants;
import forge.properties.ForgePreferences.FPref;
import forge.util.Base64Coder;

public class ImageUtil {
    public static float getNearestHQSize(float baseSize, float actualSize) {
        //get nearest power of actualSize to baseSize so that the image renders good
        return (float)Math.round(actualSize) * (float)Math.pow(2, (double)Math.round(Math.log((double)(baseSize / actualSize)) / Math.log(2)));
    }

    public static PaperCard getPaperCardFromImageKey(String key) {
        if ( key == null ) {
            return null;
        }

        PaperCard cp = StaticData.instance().getCommonCards().getCard(key);
        if (cp == null) {
            cp = StaticData.instance().getVariantCards().getCard(key);
        }
        return cp;
    }

    public static String getImageRelativePath(PaperCard cp, boolean backFace, boolean includeSet, boolean isDownloadUrl) {
        final String nameToUse = cp == null ? null : getNameToUse(cp, backFace);
        if ( null == nameToUse )
            return null;
        
        StringBuilder s = new StringBuilder();
        
        CardRules card = cp.getRules();
        String edition = cp.getEdition();
        s.append(toMWSFilename(nameToUse));
        
        final int cntPictures;
        final boolean hasManyPictures;
        final CardDb db =  !card.isVariant() ? FModel.getMagicDb().getCommonCards() : FModel.getMagicDb().getVariantCards();
        if (includeSet) {
            cntPictures = db.getPrintCount(card.getName(), edition); 
            hasManyPictures = cntPictures > 1;
        } else {
            // without set number of pictures equals number of urls provided in Svar:Picture
            String urls = card.getPictureUrl(backFace);
            cntPictures = StringUtils.countMatches(urls, "\\") + 1;

            // raise the art index limit to the maximum of the sets this card was printed in
            int maxCntPictures = db.getMaxPrintCount(card.getName());
            hasManyPictures = maxCntPictures > 1;
        }
        
        int artIdx = cp.getArtIndex() - 1;
        if (hasManyPictures) {
            if ( cntPictures <= artIdx ) // prevent overflow
                artIdx = cntPictures == 0 ? 0 : artIdx % cntPictures;
            s.append(artIdx + 1);
        }
        
        // for whatever reason, MWS-named plane cards don't have the ".full" infix
        if (!card.getType().isPlane() && !card.getType().isPhenomenon()) {
            s.append(".full");
        }
        
        final String fname;
        if (isDownloadUrl) {
            s.append(".jpg");
            fname = Base64Coder.encodeString(s.toString(), true);
        } else {
            fname = s.toString();
        }
        
        if (includeSet) {
            String editionAliased = isDownloadUrl ? FModel.getMagicDb().getEditions().getCode2ByCode(edition) : getSetFolder(edition);
            return String.format("%s/%s", editionAliased, fname);
        } else {
            return fname;
        }
    }

    public static boolean mayEnlarge() {
        return FModel.getPreferences().getPrefBoolean(FPref.UI_SCALE_LARGER);        
    }

    public static boolean hasBackFacePicture(PaperCard cp) {
        CardSplitType cst = cp.getRules().getSplitType();
        return cst == CardSplitType.Transform || cst == CardSplitType.Flip; 
    }
    
    public static String getSetFolder(String edition) {
        return  !ForgeConstants.CACHE_CARD_PICS_SUBDIR.containsKey(edition)
                ? FModel.getMagicDb().getEditions().getCode2ByCode(edition) // by default 2-letter codes from MWS are used
                : ForgeConstants.CACHE_CARD_PICS_SUBDIR.get(edition); // may use custom paths though
    }

    public static String getNameToUse(PaperCard cp, boolean backFace) {
        final CardRules card = cp.getRules();
        if (backFace ) {
            if ( hasBackFacePicture(cp) ) 
                return card.getOtherPart().getName();
            else 
                return null;
        } else if(CardSplitType.Split == cp.getRules().getSplitType()) {
            return card.getMainPart().getName() + card.getOtherPart().getName();
        } else {
            return cp.getName();
        }
    }
    
    public static String getImageKey(PaperCard cp, boolean backFace, boolean includeSet) {
        return getImageRelativePath(cp, backFace, includeSet, false);
    }

    public static String getDownloadUrl(PaperCard cp, boolean backFace) {
        return getImageRelativePath(cp, backFace, true, true);
    }    
    
    public static String toMWSFilename(String in) {
        final StringBuffer out = new StringBuffer();
        char c;
        for (int i = 0; i < in.length(); i++) {
            c = in.charAt(i);
            if ((c == '"') || (c == '/') || (c == ':') || (c == '?')) {
                out.append("");
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}
