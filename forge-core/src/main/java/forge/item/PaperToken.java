package forge.item;

import forge.ImageKeys;
import forge.card.*;
import forge.util.MyRandom;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Locale;

public class PaperToken implements InventoryItemFromSet, IPaperCard {
    private static final long serialVersionUID = 1L;
    private String name;
    private String collectorNumber;
    private String artist;
    private transient CardEdition edition;
    private ArrayList<String> imageFileName = new ArrayList<>();
    private transient CardRules cardRules;
    private int artIndex = 1;

    // takes a string of the form "<colors> <power> <toughness> <name>" such as: "B 0 0 Germ"
    public static String makeTokenFileName(String in) {
        StringBuffer out = new StringBuffer();
        char c;
        for (int i = 0; i < in.length(); i++) {
            c = in.charAt(i);
            if ((c == ' ') || (c == '-') || (c == '_')) {
                out.append('_');
            } else if (Character.isLetterOrDigit(c)) {
                out.append(c);
            }
        }
        return out.toString().toLowerCase(Locale.ENGLISH);
    }

    public static String makeTokenFileName(String colors, String power, String toughness, String types) {
        return makeTokenFileName(null, colors, power, toughness, types);
    }

    public static String makeTokenFileName(String name, String colors, String power, String toughness, String types) {
        ArrayList<String> build = new ArrayList<>();
        if (name != null) {
            build.add(name);
        }

        build.add(colors);

        if (power != null && toughness != null) {
            build.add(power);
            build.add(toughness);
        }
        build.add(types);

        String fileName = StringUtils.join(build, "_");
        return makeTokenFileName(fileName);
    }

    public PaperToken(final CardRules c, CardEdition edition0, String imageFileName, String collectorNumber, String artist) {
        this.cardRules = c;
        this.name = c.getName();
        this.edition = edition0;
        this.collectorNumber = collectorNumber;
        this.artist = artist;

        if (collectorNumber != null && !collectorNumber.isEmpty() && edition != null && edition.getTokens().containsKey(imageFileName)) {
            int idx = 0;
            // count the one with the same collectorNumber
            for (CardEdition.EditionEntry t : edition.getTokens().get(imageFileName)) {
                ++idx;
                if (!t.collectorNumber().equals(collectorNumber)) {
                    continue;
                }
                // TODO make better image file names when collector number is known
                // for the right index, we need to count the ones with wrong collector number too
                this.imageFileName.add(String.format("%s|%s|%s|%d", imageFileName, edition.getCode(), collectorNumber, idx));
            }
            this.artIndex = this.imageFileName.size();
        } else if (null == edition || CardEdition.UNKNOWN == edition) {
            this.imageFileName.add(imageFileName);
        } else {
            // Fallback if CollectorNumber is not used
            this.imageFileName.add(String.format("%s|%s", imageFileName, edition.getCode()));
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public String getEdition() {
        return edition != null ? edition.getCode() : CardEdition.UNKNOWN_CODE;
    }

    @Override
    public String getCollectorNumber() {
        if (collectorNumber.isEmpty())
            return IPaperCard.NO_COLLECTOR_NUMBER;
        return collectorNumber;
    }

    @Override
    public String getFunctionalVariant() {
        //Tokens aren't differentiated by name, so they don't really need support for this.
        return IPaperCard.NO_FUNCTIONAL_VARIANT;
    }

    @Override
    public ColorSet getMarkedColors() {
        return null;
    }

    @Override
    public int getArtIndex() {
        return artIndex;
    }

    @Override
    public boolean isFoil() {
        return false;
    }

    @Override
    public CardRules getRules() {
        return cardRules;
    }

    @Override
    public CardRarity getRarity() {
        return CardRarity.Token;
    }

    @Override
    public String getArtist() {
        return artist;
    }

    public String getImageFilename(int idx) {
        return imageFileName.get(idx - 1);
    }

    @Override
    public String getItemType() {
        return "Token";
    }

    @Override
    public boolean hasBackFace() {
        if (this.cardRules == null)
            return false;
        CardSplitType cst = this.cardRules.getSplitType();
        //expand this on future for other tokens that has other backsides besides transform..
        return cst == CardSplitType.Transform;
    }

    @Override
    public ICardFace getMainFace() {
        return this.getRules().getMainPart();
    }

    @Override
    public ICardFace getOtherFace() {
        return this.getRules().getOtherPart();
    }

    @Override
    public boolean isToken() {
        return true;
    }

    // IPaperCard
    @Override
    public String getCardImageKey() {
        return this.getImageKey(false);
    }

    @Override
    public String getCardAltImageKey() {
        return getImageKey(true);
    }

    @Override
    public String getCardWSpecImageKey() {
        return getImageKey(false);
    }

    @Override
    public String getCardUSpecImageKey() {
        return getImageKey(false);
    }

    @Override
    public String getCardBSpecImageKey() {
        return getImageKey(false);
    }

    @Override
    public String getCardRSpecImageKey() {
        return getImageKey(false);
    }

    @Override
    public String getCardGSpecImageKey() {
        return getImageKey(false);
    }

    // InventoryItem
    @Override
    public String getImageKey(boolean altState) {
        String suffix = "";
        if (hasBackFace() && altState) {
            if (collectorNumber != null && !collectorNumber.isEmpty() && edition != null) {
                String name = cardRules.getOtherPart().getName().toLowerCase().replace(" token", "").replace(" ", "_");
                return ImageKeys.getTokenKey(String.format("%s|%s|%s%s", name, edition.getCode(), collectorNumber, ImageKeys.BACKFACE_POSTFIX));
            } else {
                suffix = ImageKeys.BACKFACE_POSTFIX;
            }
        }
        int idx = MyRandom.getRandom().nextInt(artIndex);
        return getImageKey(idx) + suffix;
    }

    public String getImageKey(int artIndex) {
        return ImageKeys.getTokenKey(imageFileName.get(artIndex).replace(" ", "_"));
    }

    public boolean isRebalanced() {
        return false;
    }
}
