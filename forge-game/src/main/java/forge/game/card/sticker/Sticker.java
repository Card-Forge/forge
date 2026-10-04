package forge.game.card.sticker;

import java.util.Map;

import forge.game.card.Card;

/**
 * One sticker on a sticker sheet, identified by sheet and slot (CR 123.3a).
 */
public class Sticker {
    // the Un-cards that count vowels count Y
    private static final String VOWELS = "AEIOUY";

    private final Card sheet;
    private final String slot;
    private final StickerKind kind;
    private final int tickets;
    private final String word;
    private final String text;
    private final String abilitySVar;
    private final String keywords;
    private final String triggers;
    private final String statics;
    private final int power;
    private final int toughness;

    public static Sticker parse(Card sheet, String slot, Map<String, String> params) {
        StickerKind kind = StickerKind.smartValueOf(params.get("Kind"));
        if (kind == null) {
            return null;
        }
        return new Sticker(sheet, slot, kind, params);
    }

    private Sticker(Card sheet, String slot, StickerKind kind, Map<String, String> params) {
        this.sheet = sheet;
        this.slot = slot;
        this.kind = kind;
        this.tickets = intParam(params, "Tickets", 0);
        this.word = params.get("Word");
        this.text = params.get("Text");
        this.abilitySVar = params.get("Ability");
        this.keywords = params.get("Keywords");
        this.triggers = params.get("Triggers");
        this.statics = params.get("Statics");
        this.power = intParam(params, "Power", 0);
        this.toughness = intParam(params, "Toughness", 0);
    }

    private static int intParam(Map<String, String> params, String key, int fallback) {
        String value = params.get(key);
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public Card getSheet() {
        return sheet;
    }

    public String getSlot() {
        return slot;
    }

    public StickerKind getKind() {
        return kind;
    }

    public int getTickets() {
        return tickets;
    }

    public String getWord() {
        return word;
    }

    public String getLetters() {
        return word == null ? "" : word.replaceAll("[^A-Za-z]", "");
    }

    public int getUniqueVowelCount() {
        return countUniqueVowels(getLetters());
    }

    public static int countUniqueVowels(String name) {
        String letters = name.toUpperCase();
        int unique = 0;
        for (char v : VOWELS.toCharArray()) {
            if (letters.indexOf(v) >= 0) {
                unique++;
            }
        }
        return unique;
    }

    public String getText() {
        return text;
    }

    public String getAbilitySVar() {
        return abilitySVar;
    }

    public String getKeywords() {
        return keywords;
    }

    public String getTriggers() {
        return triggers;
    }

    public String getStatics() {
        return statics;
    }

    public int getPower() {
        return power;
    }

    public int getToughness() {
        return toughness;
    }

    public boolean isImplemented() {
        return kind != StickerKind.ABILITY
                || abilitySVar != null || keywords != null || triggers != null || statics != null;
    }

    public String getDescription() {
        return switch (kind) {
            case NAME -> word;
            case ART -> "art sticker " + slot.replaceAll("[^0-9]", "");
            case ABILITY -> text != null ? text : "(ability)";
            case PT -> power + "/" + toughness;
        };
    }

    @Override
    public String toString() {
        return sheet.getName() + " " + slot + ": " + getDescription();
    }
}
