package forge.item;

import java.util.ArrayList;
import java.util.Locale;

import forge.card.*;
import org.apache.commons.lang3.StringUtils;

import forge.ImageKeys;
import forge.util.MyRandom;

public class PaperToken implements InventoryItemFromSet, IPaperCard {
    private static final long serialVersionUID = 1L;
    private String name;
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

    public static String makeTokenFileName(final CardRules rules, CardEdition edition) {
        ArrayList<String> build = new ArrayList<>();

        String subtypes = StringUtils.join(rules.getType().getSubtypes(), " ");
        if (!rules.getName().equals(subtypes)) {
            return makeTokenFileName(rules.getName());
        }

        ColorSet colors = rules.getColor();

        if (colors.isColorless()) {
            build.add("C");
        } else {
            String color = "";
            if (colors.hasWhite()) color += "W";
            if (colors.hasBlue()) color += "U";
            if (colors.hasBlack()) color += "B";
            if (colors.hasRed()) color += "R";
            if (colors.hasGreen()) color += "G";

            build.add(color);
        }

        if (rules.getPower() != null && rules.getToughness() != null) {
            build.add(rules.getPower());
            build.add(rules.getToughness());
        }

        String cardTypes = "";
        if (rules.getType().isArtifact()) cardTypes += "A";
        if (rules.getType().isEnchantment()) cardTypes += "E";

        if (!cardTypes.isEmpty()) {
            build.add(cardTypes);
        }

        build.add(subtypes);

        // Are these keywords sorted?
        for (String keyword : rules.getMainPart().getKeywords()) {
            build.add(keyword);
        }

        if (edition != null) {
            build.add(edition.getCode());
        }

        return StringUtils.join(build, "_").replace('*', 'x').toLowerCase();
    }

    public PaperToken(final CardRules c, CardEdition edition0, String imageFileName) {
        this.cardRules = c;
        this.name = c.getName();
        this.edition = edition0;

        if (edition != null && edition.getTokens().containsKey(imageFileName)) {
            this.artIndex = edition.getTokens().get(imageFileName);
        }

        if (imageFileName == null) {
            // This shouldn't really happen. We can just use the normalized name again for the base image name
            this.imageFileName.add(makeTokenFileName(c, edition0));
        } else {
            String formatEdition = null == edition || CardEdition.UNKNOWN == edition ? "" : "_" + edition.getCode().toLowerCase();

            this.imageFileName.add(String.format("%s%s", imageFileName, formatEdition));
            for (int idx = 2; idx <= this.artIndex; idx++) {
                this.imageFileName.add(String.format("%s%d%s", imageFileName, idx, formatEdition));
            }
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
        return edition != null ? edition.getCode() : "???";
    }

    @Override
    public String getCollectorNumber() {
        return IPaperCard.NO_COLLECTOR_NUMBER;
    }

    @Override
    public String getFunctionalVariant() {
        //Tokens aren't differentiated by name, so they don't really need support for this.
        return IPaperCard.NO_FUNCTIONAL_VARIANT;
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
    public String getArtist() { /*TODO*/
        return "";
    }

    // Unfortunately this is a property of token, cannot move it outside of class
    public String getImageFilename() {
        return getImageFilename(1);
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
        if (hasBackFace()) {
            String edCode = edition != null ? "_" + edition.getCode().toLowerCase() : "";
            if (altState) {
                String name = ImageKeys.TOKEN_PREFIX + cardRules.getOtherPart().getName().toLowerCase().replace(" token", "");
                name.replace(" ", "_");
                return name + edCode;
            } else {
                String name = ImageKeys.TOKEN_PREFIX + cardRules.getMainPart().getName().toLowerCase().replace(" token", "");
                name.replace(" ", "_");
                return name + edCode;
            }
        }
        int idx = MyRandom.getRandom().nextInt(artIndex);
        return getImageKey(idx);
    }

    public String getImageKey(int artIndex) {
        return ImageKeys.TOKEN_PREFIX + imageFileName.get(artIndex).replace(" ", "_");
    }

    public boolean isRebalanced() {
        return false;
    }
}
