package forge.game.card.sticker;

/**
 * CR 123.1
 */
public enum StickerKind {
    NAME,
    ART,
    ABILITY,
    PT;

    public static StickerKind smartValueOf(String value) {
        for (StickerKind k : values()) {
            if (k.name().equalsIgnoreCase(value)) {
                return k;
            }
        }
        return null;
    }
}
