package forge.util;

import forge.ImageKeys;
import forge.StaticData;
import forge.card.CardDb;
import forge.card.CardRules;
import forge.card.CardSplitType;
import forge.item.PaperCard;

public class ImageUtil {
    public static float getNearestHQSize(float baseSize, float actualSize) {
        //get nearest power of actualSize to baseSize so that the image renders good
        return (float)Math.round(actualSize) * (float)Math.pow(2, (double)Math.round(Math.log(baseSize / actualSize) / Math.log(2)));
    }

    public static PaperCard getPaperCardFromImageKey(String key) {
        if ( key == null ) {
            return null;
        }

        key = key.substring(2);
        PaperCard cp = StaticData.instance().getCommonCards().getCard(key);
        if (cp == null) {
            cp = StaticData.instance().getCustomCards().getCard(key);
        }
        if (cp == null) {
            cp = StaticData.instance().getVariantCards().getCard(key);
        }
        return cp;
    }

    public static String getImageRelativePath(PaperCard cp, boolean backFace, boolean includeSet, boolean isDownloadUrl) {
        final String nameToUse = cp == null ? null : getNameToUse(cp, backFace);
        if (nameToUse == null) {
            return null;
        }
        StringBuilder s = new StringBuilder();

        CardRules card = cp.getRules();
        String edition = cp.getEdition();
        s.append(toMWSFilename(nameToUse));

        final int cntPictures;
        final boolean hasManyPictures;
        final CardDb db =  !card.isVariant() ? StaticData.instance().getCommonCards() : StaticData.instance().getVariantCards();
        if (includeSet) {
            cntPictures = db.getPrintCount(card.getName(), edition);
            hasManyPictures = cntPictures > 1;
        } else {
            cntPictures = 1;

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

        String fname;
        if (isDownloadUrl) {
            s.append(".jpg");
            fname = s.toString().replaceAll("\\s", "%20");
        } else {
            fname = s.toString();
        }

        if (includeSet) {
            String editionAliased = isDownloadUrl ? StaticData.instance().getEditions().getCode2ByCode(edition) : ImageKeys.getSetFolder(edition);
            if (editionAliased == "") //FIXME: Custom Cards Workaround
                editionAliased = edition;
            return TextUtil.concatNoSpace(editionAliased, "/", fname);
        } else {
            return fname;
        }
    }

    public static boolean hasBackFacePicture(PaperCard cp) {
        CardSplitType cst = cp.getRules().getSplitType();
        return cst == CardSplitType.Transform || cst == CardSplitType.Flip || cst == CardSplitType.Meld || cst == CardSplitType.Modal;
    }

    public static String getNameToUse(PaperCard cp, boolean backFace) {
        final CardRules card = cp.getRules();
        if (backFace ) {
            if ( hasBackFacePicture(cp) )
                if (card.getOtherPart() != null) {
                    return card.getOtherPart().getName();
                } else if (!card.getMeldWith().isEmpty()) {
                    final CardDb db =  StaticData.instance().getCommonCards();
                    return db.getRules(card.getMeldWith()).getOtherPart().getName();
                } else {
                    return null;
                }
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

    public static String getScryfallDownloadUrl(PaperCard cp, boolean backFace, String setCode){
        return getScryfallDownloadUrl(cp, backFace, setCode, "en");
    }

    public static String getScryfallDownloadUrl(PaperCard cp, boolean backFace, String setCode, String langCode){
        String editionCode;
        if ((setCode != null) && (setCode.length() > 0))
            editionCode = setCode;
        else
            editionCode = cp.getEdition().toLowerCase();
        String cardCollectorNumber = cp.getCollectorNumber();
        // Hack to account for variations in Arabian Nights
        cardCollectorNumber = cardCollectorNumber.replace("+", "†");
        String faceParam = "";
        if (cp.getRules().getOtherPart() != null) {
            faceParam = (backFace ? "&face=back" : "&face=front");
        }
        return String.format("%s/%s/%s?format=image&version=normal%s", editionCode, cardCollectorNumber,
                langCode, faceParam);
    }

    public static String toMWSFilename(String in) {
        final StringBuilder out = new StringBuilder();
        char c;
        for (int i = 0; i < in.length(); i++) {
            c = in.charAt(i);
            if ((c == '"') || (c == '/') || (c == ':') || (c == '?')) {
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }
}