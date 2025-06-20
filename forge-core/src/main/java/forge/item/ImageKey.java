package forge.item;

import java.io.Serializable;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Lists;

import forge.ImageKeys;
import forge.StaticData;
import forge.card.CardEdition;
import forge.card.CardStateName;

public record ImageKey(String setCode, String name, String collectorNumber, String artistName, CardStateName state, ImageType type, boolean custom) implements Serializable
{

    public List<String> getFilename(ArtStyle art) {
        List<String> result = Lists.newArrayList();
        String cn = getCollectorNumberByState();
        if (type == ImageType.Token) {
            if (ImageKeys.HIDDEN_CARD.equals(name)) {
                // hidden only exist as png
                result.add("hidden.png");
                return result;
            }
            // TODO Token doesn't use fullborder or artcrop ArtStyle yet
            if (!StringUtils.isEmpty(setCode) && !setCode.equals(CardEdition.UNKNOWN_CODE)) {
                if (!StringUtils.isEmpty(cn) && !cn.equals(IPaperCard.NO_COLLECTOR_NUMBER)) {
                    result.add(setCode + "/" + cn + "_" + name);
                }
                result.add(setCode + "/" + name);
            }
            result.add(name);
        } else if (type == ImageType.Card) {
            if (!StringUtils.isEmpty(setCode) && !setCode.equals(CardEdition.UNKNOWN_CODE)) {
                if (!StringUtils.isEmpty(cn) && !cn.equals(IPaperCard.NO_COLLECTOR_NUMBER)) {
                    result.add(setCode + "/" + cn + "_" + name + "." + art.filename);
                }
                result.add(setCode + "/" + name + "." + art.filename);
            }
            result.add(name + "." + art.filename);
        }
        return result;
    }

    public Pair<String, String> getDownloadUrl(ArtStyle art) {
        if (custom) {
            return null;
        }
        if (type == ImageType.Token && ImageKeys.HIDDEN_CARD.equals(name)) {
            return Pair.of("hidden.png", "https://cards.scryfall.io/back.png");
        }
        if (StringUtils.isEmpty(collectorNumber) || collectorNumber.equals(IPaperCard.NO_COLLECTOR_NUMBER)) {
            return null;
        }
        // Scryfall only for Cards or Tokens
        if (type != ImageType.Card && type != ImageType.Token) {
            return null;
        }
        if (StringUtils.isEmpty(setCode) || setCode.equals(CardEdition.UNKNOWN_CODE)) {
            return null;
        }
        CardEdition edition = StaticData.instance().getCardEdition(setCode);
        if (edition == null || edition.getType() == CardEdition.Type.CUSTOM_SET) return null;
        // differ token code
        String setCode = type == ImageType.Card ? edition.getScryfallCode() : edition.getTokensCode();
        String cn = getCollectorNumberByState();
        String langCode = edition.getCardsLangCode();
        String faceParam = "";
        switch(state) {
        case Meld:
        case Modal:
        case Secondary:
        case Transformed:
            faceParam = "&face=back";
            break;
        default:
            faceParam = "&face=front";
            break;
        }

        String ext = art.scryfall == "png" ? ".png" : ".jpg";
        String filepath = setCode + "/" + cn + "_" + name + ext;
        // TODO make scryfall art_crop of split cards separate
        String url = ImageKeys.URL_PIC_SCRYFALL_DOWNLOAD + String.format(
            "%s/%s/%s?format=image&version=%s%s", setCode, cn, langCode, art.scryfall, faceParam
        );
        return Pair.of(filepath, url);
    }
    protected String getCollectorNumberByState() {
        String scryCN = collectorNumber;
        switch(state) {
        case SpecializeB:
            scryCN += "b";
            break;
        case SpecializeG:
            scryCN += "g";
            break;
        case SpecializeR:
            scryCN += "r";
            break;
        case SpecializeU:
            scryCN += "u";
            break;
        case SpecializeW:
            scryCN += "w";
            break;
        default:
            break;
        }
        return scryCN;
    }
}
