package forge.item;

import forge.ImageKeys;
import forge.card.CardEdition;
import forge.card.CardRarity;
import forge.card.CardRules;
import forge.card.ColorSet;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Locale;

public class PaperToken implements InventoryItemFromSet, IPaperCard {
    private String name;
    private CardEdition edition;
    private String imageFileName;
    private CardRules card;

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
        for(String keyword : rules.getMainPart().getKeywords()) {
            build.add(keyword);
        }

        build.add(edition.getCode());

        // Should future image file names be all lower case? Instead of Up case sets?
        return StringUtils.join(build, "_").toLowerCase();
    }

    public PaperToken(final CardRules c) { this(c, null, null); }
    public PaperToken(final CardRules c, final String fileName) { this(c, null, fileName); }
    public PaperToken(final CardRules c, CardEdition edition) { this(c, edition, null); }
    public PaperToken(final CardRules c, CardEdition edition0, String imageFileName) {
        this.card = c;
        this.name = c.getName();
        this.edition = edition0;

        if (imageFileName == null) {
            this.imageFileName = makeTokenFileName(c, edition0);
        } else {
            String formatEdition = null == edition || CardEdition.UNKNOWN == edition ? "" : edition.getCode();
            this.imageFileName = String.format("%s%s", formatEdition, imageFileName);
        }
    }
    
    @Override public String getName() { return name; }

    @Override public String toString() { return name; }
    @Override public String getEdition() { return edition.getCode(); }
    @Override public int getArtIndex() { return 0; } // This might change however
    @Override public boolean isFoil() { return false; }
    @Override public CardRules getRules() { return card; }

    @Override public CardRarity getRarity() { return CardRarity.None; }

    // Unfortunately this is a property of token, cannot move it outside of class
    public String getImageFilename() { return imageFileName; }

    @Override public String getItemType() { return "Token"; }

    @Override public boolean isToken() { return true; }

    @Override
    public String getImageKey(boolean altState) {
        return ImageKeys.TOKEN_PREFIX + imageFileName;
    }
}
