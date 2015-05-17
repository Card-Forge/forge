package forge.util;

import org.apache.commons.lang3.StringUtils;

import forge.ImageKeys;
import forge.StaticData;
import forge.card.CardDb;
import forge.card.CardRules;
import forge.card.CardSplitType;
import forge.item.PaperCard;

public class ImageUtil {
    public static float getNearestHQSize(final float baseSize, final float actualSize) {
        //get nearest power of actualSize to baseSize so that the image renders good
        return Math.round(actualSize) * (float)Math.pow(2, Math.round(Math.log(baseSize / actualSize) / Math.log(2)));
    }

    public static PaperCard getPaperCardFromImageKey(final String key) {
        if ( key == null ) {
            return null;
        }

        PaperCard cp = StaticData.instance().getCommonCards().getCard(key);
        if (cp == null) {
            cp = StaticData.instance().getVariantCards().getCard(key);
        }
        return cp;
    }

    public static String getImageRelativePath(final PaperCard cp, final boolean backFace, final boolean includeSet, final boolean isDownloadUrl) {
        final String nameToUse = cp == null ? null : getNameToUse(cp, backFace);
        if (nameToUse == null) {
            return null;
        }
        final StringBuilder s = new StringBuilder();

        final CardRules card = cp.getRules();
        final String edition = cp.getEdition();
        s.append(toMWSFilename(nameToUse));

        final int cntPictures;
        final boolean hasManyPictures;
        final CardDb db =  !card.isVariant() ? StaticData.instance().getCommonCards() : StaticData.instance().getVariantCards();
        if (includeSet) {
            cntPictures = db.getPrintCount(card.getName(), edition);
            hasManyPictures = cntPictures > 1;
        } else {
            // without set number of pictures equals number of urls provided in Svar:Picture
            final String urls = card.getPictureUrl(backFace);
            cntPictures = StringUtils.countMatches(urls, "\\") + 1;

            // raise the art index limit to the maximum of the sets this card was printed in
            final int maxCntPictures = db.getMaxPrintCount(card.getName());
            hasManyPictures = maxCntPictures > 1;
        }

        int artIdx = cp.getArtIndex() - 1;
        if (hasManyPictures) {
            if ( cntPictures <= artIdx ) {
                artIdx = cntPictures == 0 ? 0 : artIdx % cntPictures;
            }
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
            final String editionAliased = isDownloadUrl ? StaticData.instance().getEditions().getCode2ByCode(edition) : ImageKeys.getSetFolder(edition);
            return String.format("%s/%s", editionAliased, fname);
        } else {
            return fname;
        }
    }

    public static boolean hasBackFacePicture(final PaperCard cp) {
        final CardSplitType cst = cp.getRules().getSplitType();
        return cst == CardSplitType.Transform || cst == CardSplitType.Flip;
    }

    public static String getNameToUse(final PaperCard cp, final boolean backFace) {
        final CardRules card = cp.getRules();
        final String nameToUse;
        if (backFace) {
            if (hasBackFacePicture(cp)) {
                nameToUse = card.getOtherPart().getName();
            } else {
                return null;
            }
        } else if (CardSplitType.Split == cp.getRules().getSplitType()) {
            nameToUse = card.getMainPart().getName() + card.getOtherPart().getName();
        } else {
            nameToUse = cp.getName();
        }

        return formatName(nameToUse);
    }

    private static String formatName(final String name) {
        return StringUtils.replaceChars(name, "áàâéèêúùûíìîóòô", "aaaeeeuuuiiiooo");
    }

    public static String getImageKey(final PaperCard cp, final boolean backFace, final boolean includeSet) {
        return getImageRelativePath(cp, backFace, includeSet, false);
    }

    public static String getDownloadUrl(final PaperCard cp, final boolean backFace) {
        return getImageRelativePath(cp, backFace, true, true);
    }

    public static String toMWSFilename(final String in) {
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
