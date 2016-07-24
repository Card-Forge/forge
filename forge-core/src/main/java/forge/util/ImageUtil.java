package forge.util;

import forge.ImageKeys;
import forge.StaticData;
import forge.card.CardDb;
import forge.card.CardRules;
import forge.card.CardSplitType;
import forge.item.PaperCard;

import java.io.File;

import org.apache.commons.lang3.StringUtils;

public class ImageUtil {
    public static float getNearestHQSize(float baseSize, float actualSize) {
        //get nearest power of actualSize to baseSize so that the image renders good
        return (float)Math.round(actualSize) * (float)Math.pow(2, (double)Math.round(Math.log((double)(baseSize / actualSize)) / Math.log(2)));
    }

    public static PaperCard getPaperCardFromImageKey(String key) {
        if ( key == null ) {
            return null;
        }

        key = key.substring(2);
        PaperCard cp = StaticData.instance().getCommonCards().getCard(key);
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

        String fname;
        if (isDownloadUrl) {
            s.append(".jpg");
            fname = s.toString().replaceAll("\\s", "%20");
        } else {
            fname = s.toString();
        }

        if (includeSet) {
            String editionAliased = isDownloadUrl ? StaticData.instance().getEditions().getCode2ByCode(edition) : ImageKeys.getSetFolder(edition);
            return String.format("%s/%s", editionAliased, fname);
        } else {
            return fname;
        }
    }

    public static boolean hasBackFacePicture(PaperCard cp) {
        CardSplitType cst = cp.getRules().getSplitType();
        return cst == CardSplitType.Transform || cst == CardSplitType.Flip || cst == CardSplitType.Meld;
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
    
    public static String[] getDownloadUrlAndDestination(String cacheCardPicsDir, PaperCard c, boolean backFace) {
        final CardRules cardRules = c.getRules();
        final String urls = cardRules.getPictureUrl(backFace);
        if (StringUtils.isEmpty(urls)) {
            return null;
        }

        String filename = ImageUtil.getImageKey(c, backFace, false);
        final File destFile = new File(cacheCardPicsDir, filename + ".jpg");
        if (destFile.exists()) {
            return null;
        }

        filename = destFile.getAbsolutePath();

        final String urlToDownload;
        int urlIndex = 0;
        int allUrlsLen = 1;
        if (!urls.contains("\\")) {
            urlToDownload = urls;
        } else {
            final String[] allUrls = urls.split("\\\\");
            allUrlsLen = allUrls.length;
            urlIndex = (c.getArtIndex()-1) % allUrlsLen;
            urlToDownload = allUrls[urlIndex];
        }
        // System.out.println(c.getName() + "|" + c.getEdition() + " - " + c.getArtIndex() + " -> " + urlIndex + "/" + allUrlsLen + " === " + filename + " <<< " + urlToDownload);

        return new String[] { urlToDownload, filename };
    }

    public static String toMWSFilename(String in) {
        final StringBuilder out = new StringBuilder();
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
