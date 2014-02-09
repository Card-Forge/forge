package forge.item;

import forge.card.CardEdition;
import forge.card.CardRarity;
import forge.card.CardRules;

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

    public static String makeTokenFileName(String colors, int power, int toughness, String name) {
        return makeTokenFileName(colors, String.valueOf(power), String.valueOf(toughness), name);
    }
    
    public static String makeTokenFileName(String colors, String power, String toughness, String name) {
        StringBuilder fileName = new StringBuilder();
        fileName.append(colors).append('_').append(power).append('_').append(toughness).append('_').append(name);
        return makeTokenFileName(fileName.toString());
    }
    
    public PaperToken(final CardRules c, CardEdition edition0, final String imageFileName) {
        this.card = c;
        this.name = c.getName();
        this.edition = edition0;
        this.imageFileName = String.format("%s%s", null == edition || CardEdition.UNKNOWN == edition ? "" : edition.getCode(), imageFileName);
    }
    
    @Override public String getName() { return name; }

    @Override public String toString() { return name; }
    @Override public String getEdition() { return edition.getCode(); }
    @Override public int getArtIndex() { return 0; } // This might change however
    @Override public boolean isFoil() { return false; }
    @Override public CardRules getRules() { return card; }

    @Override public CardRarity getRarity() { return CardRarity.Common; } // They don't have rarity though!

    // Unfortunately this is a property of token, cannot move it outside of class
    public String getImageFilename() { return imageFileName; }

    @Override public String getItemType() { return "Token"; }

    @Override public boolean isToken() { return true; }
}
