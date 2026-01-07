package forge.util;

import forge.ImageKeys;
import forge.StaticData;
import forge.card.CardDb;
import forge.card.CardEdition;
import forge.card.CardRules;
import forge.card.CardSplitType;
import forge.item.IPaperCard;
import forge.item.PaperCard;
import java.util.regex.Pattern;
import forge.item.PaperToken;
import forge.token.TokenDb;
import org.apache.commons.lang3.StringUtils;

import java.net.URLEncoder;

public class ImageUtil {
    public static float getNearestHQSize(float baseSize, float actualSize) {
        //get nearest power of actualSize to baseSize so that the image renders good
        return (float)Math.round(actualSize) * (float)Math.pow(2, (double)Math.round(Math.log(baseSize / actualSize) / Math.log(2)));
    }

    public static PaperCard getPaperCardFromImageKey(final String imageKey) {
        String key;
        if (imageKey == null || imageKey.length() < 2) {
            return null;
        }
        if (imageKey.startsWith(ImageKeys.CARD_PREFIX))
            key = imageKey.substring(ImageKeys.CARD_PREFIX.length());
        else
            return null;
        if (key.isEmpty())
            return null;

        CardDb db = StaticData.instance().getCommonCards();
        PaperCard cp = null;
        //db shouldn't be null
        if (db != null) {
            cp = db.getCard(key);
            if (cp == null) {
                db = StaticData.instance().getVariantCards();
                if (db != null)
                    cp = db.getCard(key);
            }
        }
        if (cp == null)
            System.err.println("Can't find PaperCard from key: " + key);
        // return cp regardless if it's null
        return cp;
    }

    public static PaperToken getPaperTokenFromImageKey(final String imageKey) {
        String key;
        if (imageKey == null ||
            !imageKey.startsWith(ImageKeys.TOKEN_PREFIX)) {
            return null;
        }

        key = imageKey.substring(ImageKeys.TOKEN_PREFIX.length());
            
        if (key.isEmpty()) {
            return null;
        }

        TokenDb db = StaticData.instance().getAllTokens();
        if (db == null) {
            return null;
        }
        
        String[] split = key.split("\\|");
        if (!db.containsRule(split[0])) {
            return null;
        }
        
        PaperToken pt = switch (split.length) {
            case 1 -> db.getToken(split[0]);
            case 2, 3 -> db.getToken(split[0], split[1]);
            default -> db.getToken(split[0], split[1], Integer.parseInt(split[3]));
        };

        if (pt == null) {
            System.err.println("Can't find PaperToken from key: " + key);
        }
            
        return pt;
    }

    public static String transformKey(String imageKey) {
        String key;
        String edition= imageKey.substring(0, imageKey.indexOf("/"));
        String artIndex = imageKey.substring(imageKey.indexOf("/")+1, imageKey.indexOf(".")).replaceAll("[^0-9]", "");
        String name = artIndex.isEmpty() ? imageKey.substring(imageKey.indexOf("/")+1, imageKey.indexOf(".")) : imageKey.substring(imageKey.indexOf("/")+1, imageKey.indexOf(artIndex));
        key = name + "|" + edition;
        if (!artIndex.isEmpty())
            key += "|" + artIndex;
        return key;
    }

    public static String getImageRelativePath(PaperCard cp, String face, boolean includeSet, boolean isDownloadUrl) {
        final String nameToUse = cp == null ? null : getNameToUse(cp, face);
        if (nameToUse == null) {
            return null;
        }
        StringBuilder s = new StringBuilder();

        CardRules card = cp.getRules();
        String edition = cp.getEdition().equals(CardEdition.UNKNOWN_CODE)
                ? CardEdition.UNKNOWN_SET_NAME
                : cp.getEdition();
        s.append(toMWSFilename(nameToUse));

        final int cntPictures;
        final boolean hasManyPictures;
        final CardDb db =  !card.isVariant() ? StaticData.instance().getCommonCards() : StaticData.instance().getVariantCards();
        if (includeSet) {
            cntPictures = db.getArtCount(card.getName(), edition, cp.getFunctionalVariant());
            hasManyPictures = cntPictures > 1;
        } else {
            cntPictures = 1;
            // raise the art index limit to the maximum of the sets this card was printed in
            int maxCntPictures = db.getMaxArtIndex(card.getName());
            hasManyPictures = maxCntPictures > 1;
        }

        int artIdx = cp.getArtIndex() - 1;
        if (hasManyPictures) {
            if (cntPictures <= artIdx) // prevent overflow
                artIdx = artIdx % cntPictures;
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
            if (editionAliased.isEmpty()) //FIXME: Custom Cards Workaround
                editionAliased = edition;
            return TextUtil.concatNoSpace(editionAliased, "/", fname);
        } else {
            return fname;
        }
    }

    public static String getNameToUse(PaperCard cp, String face) {
        final CardRules card = cp.getRules();
        if (face.equals("back")) {
            if (cp.hasBackFace())
                if (card.getOtherPart() != null) {
                    return card.getOtherPart().getName();
                } else if (!card.getMeldWith().isEmpty()) {
                    final CardDb db = StaticData.instance().getCommonCards();
                    return db.getRules(card.getMeldWith()).getOtherPart().getName();
                } else {
                    return null;
                }
            else
                return null;
        } else if (face.equals("white")) {
            if (card.getWSpecialize() != null) {
                return card.getWSpecialize().getName();
            }
        } else if (face.equals("blue")) {
            if (card.getUSpecialize() != null) {
                return card.getUSpecialize().getName();
            }
        } else if (face.equals("black")) {
            if (card.getBSpecialize() != null) {
                return card.getBSpecialize().getName();
            }
        } else if (face.equals("red")) {
            if (card.getRSpecialize() != null) {
                return card.getRSpecialize().getName();
            }
        } else if (face.equals("green")) {
            if (card.getGSpecialize() != null) {
                return card.getGSpecialize().getName();
            }
        } else if (CardSplitType.Split == cp.getRules().getSplitType()) {
            return card.getMainPart().getName() + card.getOtherPart().getName();
        } else if (cp.hasFlavorName()) {
            return cp.getDisplayName();
        } else if (!IPaperCard.NO_FUNCTIONAL_VARIANT.equals(cp.getFunctionalVariant())) {
            return cp.getName() + " " + cp.getFunctionalVariant();
        }
        return cp.getName();
    }

    public static String getImageKey(PaperCard cp, String face, boolean includeSet) {
        return getImageRelativePath(cp, face, includeSet, false);
    }

    public static String getDownloadUrl(PaperCard cp, String face) {
        return getImageRelativePath(cp, face, true, true);
    }


    public static String getScryfallDownloadUrl(PaperCard cp, String face, String setCode, String langCode, boolean useArtCrop){
        final Pattern funnyCardCollectorNumberPattern = Pattern.compile("^F\\d+");
        String editionCode;
        if (setCode != null && !setCode.isEmpty())
            editionCode = setCode;
        else
            editionCode = cp.getEdition().toLowerCase();
        String cardCollectorNumber = cp.getCollectorNumber();
        // override old planechase sets from their modified id since scryfall move the planechase cards outside their original setcode
        if (cardCollectorNumber.startsWith("OHOP")) {
            editionCode = "ohop";
            cardCollectorNumber = cardCollectorNumber.substring("OHOP".length());
        } else if (cardCollectorNumber.startsWith("OPCA")) {
            editionCode = "opca";
            cardCollectorNumber = cardCollectorNumber.substring("OPCA".length());
        } else if (cardCollectorNumber.startsWith("OPC2")) {
            editionCode = "opc2";
            cardCollectorNumber = cardCollectorNumber.substring("OPC2".length());
        }
        
        if (funnyCardCollectorNumberPattern.matcher(cardCollectorNumber).matches()) {
            cardCollectorNumber = cardCollectorNumber.substring(1);
        }

        String versionParam = useArtCrop ? "art_crop" : "normal";
        String faceParam = "";

        if (cp.getRules().getSplitType() == CardSplitType.Meld) {
            if (face.equals("back")) {
                PaperCard meldBasePc = cp.getMeldBaseCard();
                cardCollectorNumber = meldBasePc.getCollectorNumber();
                String collectorNumberSuffix = "";

                if (cardCollectorNumber.endsWith("a")) {
                    cardCollectorNumber = cardCollectorNumber.substring(0, cardCollectorNumber.length() - 1);
                } else if (cardCollectorNumber.endsWith("as")) {
                    cardCollectorNumber = cardCollectorNumber.substring(0, cardCollectorNumber.length() - 2);
                    collectorNumberSuffix = "s";
                } else if (cardCollectorNumber.endsWith("ap")) {
                    cardCollectorNumber = cardCollectorNumber.substring(0, cardCollectorNumber.length() - 2);
                    collectorNumberSuffix = "p";
                } else if (cp.getCollectorNumber().endsWith("a")) {
                    // SIR
                    cardCollectorNumber = cp.getCollectorNumber().substring(0, cp.getCollectorNumber().length() - 1);
                }

                cardCollectorNumber += "b" + collectorNumberSuffix;
            }

            faceParam = "&face=front";
        } else if (cp.getRules().getOtherPart() != null) {
            faceParam = (face.equals("back") && cp.getRules().getSplitType() != CardSplitType.Flip
                    ? "&face=back"
                    : "&face=front");
        }

        if (cardCollectorNumber.endsWith("☇")) {
            faceParam = "&face=back";
            cardCollectorNumber = cardCollectorNumber.substring(0, cardCollectorNumber.length() - 1);
        }

        return String.format("%s/%s/%s?format=image&version=%s%s", editionCode, encodeUtf8(cardCollectorNumber),
                langCode, versionParam, faceParam);
    }

    public static String getScryfallTokenDownloadUrl(String collectorNumber, String setCode, String langCode, String faceParam) {
        String versionParam = "normal";
        if (!faceParam.isEmpty()) {
            faceParam = (faceParam.equals("back") ? "&face=back" : "&face=front");
        }
        if (collectorNumber.endsWith("☇")) {
            faceParam = "&face=back";
            collectorNumber = collectorNumber.substring(0, collectorNumber.length() - 1);
        }
        return String.format("%s/%s/%s?format=image&version=%s%s", setCode, encodeUtf8(collectorNumber),
                langCode, versionParam, faceParam);
    }

    private static String encodeUtf8(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (Exception e) {
            // Unlikely, for the possibility that "UTF-8" is not supported.
            System.err.println("UTF-8 encoding not supported on this device.");
            return s;
        }
    }

    public static String toMWSFilename(String in) {
        in = StringUtils.stripAccents(in);
        final StringBuilder out = new StringBuilder();
        char c;
        for (int i = 0; i < in.length(); i++) {
            c = in.charAt(i);
            if ((c != '"') && (c != '/') && (c != ':') && (c != '?')) {
                out.append(c);
            }
        }
        return out.toString();
    }
}
