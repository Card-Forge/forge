package forge.util;

import forge.ImageKeys;
import forge.StaticData;
import forge.card.CardDb;
import forge.card.CardRules;
import forge.card.CardSplitType;
import forge.card.CardStateName;
import forge.item.IPaperCard;
import forge.item.PaperCard;
import org.apache.commons.lang3.StringUtils;

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

        if (key.endsWith(ImageKeys.BACKFACE_POSTFIX)) {
            key = key.substring(0, key.length() - ImageKeys.BACKFACE_POSTFIX.length());
        }

        if (key.isEmpty())
            return null;

        String[] tempdata = key.split("\\|");
        PaperCard cp = StaticData.instance().fetchCard(tempdata[0], tempdata[1], tempdata[2]);

        if (cp == null)
            System.err.println("Can't find PaperCard from key: " + key);
        // return cp regardless if it's null
        return cp;
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

    public static String getImageRelativePath(String name, String set, String collectorNumber, boolean artChop) {
        StringBuilder sb = new StringBuilder();

        sb.append(set).append("/");
        if (!collectorNumber.isEmpty() && !collectorNumber.equals(IPaperCard.NO_COLLECTOR_NUMBER)) {
            sb.append(collectorNumber).append("_");
        }
        sb.append(StringUtils.stripAccents(name));

        sb.append(artChop ? ".artcrop" : ".fullborder");
        sb.append(".jpg");
        return sb.toString();
    }


    public static String getImageRelativePath(PaperCard cp, String face, boolean includeSet, boolean isDownloadUrl) {
        final String nameToUse = cp == null ? null : getNameToUse(cp, face);
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
            if (editionAliased == "") //FIXME: Custom Cards Workaround
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
            return card.getImageName(CardStateName.SpecializeW);
        } else if (face.equals("blue")) {
            return card.getImageName(CardStateName.SpecializeU);
        } else if (face.equals("black")) {
            return card.getImageName(CardStateName.SpecializeB);
        } else if (face.equals("red")) {
            return card.getImageName(CardStateName.SpecializeR);
        } else if (face.equals("green")) {
            return card.getImageName(CardStateName.SpecializeG);
        } else if (CardSplitType.Split == cp.getRules().getSplitType()) {
            return card.getMainPart().getName() + card.getOtherPart().getName();
        } else if (!IPaperCard.NO_FUNCTIONAL_VARIANT.equals(cp.getFunctionalVariant())) {
            return cp.getName() + " " + cp.getFunctionalVariant();
        }
        return cp.getName();
    }

    public static String getNameToUse(PaperCard cp, CardStateName face) {
        if (!IPaperCard.NO_FUNCTIONAL_VARIANT.equals(cp.getFunctionalVariant())) {
            return cp.getFunctionalVariant();
        }
        final CardRules card = cp.getRules();
        return card.getImageName(face);
    }

    public static String getImageKey(PaperCard cp, String face, boolean includeSet) {
        return getImageRelativePath(cp, face, includeSet, false);
    }

    public static String getImageKey(PaperCard cp, CardStateName face) {
        String name = getNameToUse(cp, face);
        String number = cp.getCollectorNumber();
        String suffix = "";
        switch (face) {
        case SpecializeB:
            number += "b";
            break;
        case SpecializeG:
            number += "g";
            break;
        case SpecializeR:
            number += "r";
            break;
        case SpecializeU:
            number += "u";
            break;
        case SpecializeW:
            number += "w";
            break;
        case Meld:
        case Modal:
        case Secondary:
        case Transformed:
            suffix = ImageKeys.BACKFACE_POSTFIX;
            break;
        case Flipped:
            break; // add info to rotate the image?
        default:
            break;
        };
        return ImageKeys.CARD_PREFIX + name + CardDb.NameSetSeparator + cp.getEdition()
            + CardDb.NameSetSeparator + number + CardDb.NameSetSeparator + cp.getArtIndex() + suffix;
    }

    public static String getDownloadUrl(PaperCard cp, String face) {
        return getImageRelativePath(cp, face, true, true);
    }

    public static String getScryfallDownloadUrl(String collectorNumber, String setCode, String langCode, String faceParam, boolean useArtCrop){
        return getScryfallDownloadUrl(collectorNumber, setCode, langCode, faceParam, useArtCrop, false);
    }

    public static String getScryfallDownloadUrl(String collectorNumber, String setCode, String langCode, String faceParam, boolean useArtCrop, boolean hyphenateAlchemy){
        // Hack to account for variations in Arabian Nights
        collectorNumber = collectorNumber.replace("+", "†");
        // override old planechase sets from their modified id since scryfall move the planechase cards outside their original setcode
        if (collectorNumber.startsWith("OHOP")) {
            setCode = "ohop";
            collectorNumber = collectorNumber.substring("OHOP".length());
        } else if (collectorNumber.startsWith("OPCA")) {
            setCode = "opca";
            collectorNumber = collectorNumber.substring("OPCA".length());
        } else if (collectorNumber.startsWith("OPC2")) {
            setCode = "opc2";
            collectorNumber = collectorNumber.substring("OPC2".length());
        } else if (hyphenateAlchemy) {
            if (!collectorNumber.startsWith("A")) {
                return null;
            }

            collectorNumber = collectorNumber.replace("A", "A-");
        }
        String versionParam = useArtCrop ? "art_crop" : "normal";
        if (!faceParam.isEmpty()) {
            faceParam = (faceParam.equals("back") ? "&face=back" : "&face=front");
        }
        return String.format("%s/%s/%s?format=image&version=%s%s", setCode, collectorNumber,
                langCode, versionParam, faceParam);
    }

    public static String getScryfallTokenDownloadUrl(String collectorNumber, String setCode, String langCode, String faceParam) {
        String versionParam = "normal";
        if (!faceParam.isEmpty()) {
            faceParam = (faceParam.equals("back") ? "&face=back" : "&face=front");
        }
        return String.format("%s/%s/%s?format=image&version=%s%s", setCode, collectorNumber,
                langCode, versionParam, faceParam);
    }

    public static String toMWSFilename(String in) {
        in = StringUtils.stripAccents(in);
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