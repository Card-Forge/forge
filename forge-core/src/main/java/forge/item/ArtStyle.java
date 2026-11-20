package forge.item;

public enum ArtStyle {
    Normal("fullborder", "normal"),
    Crop("artcrop", "art_crop");

    public final String filename;
    public final String scryfall;
    ArtStyle(String filename, String scryfall) {
        this.filename = filename;
        this.scryfall = scryfall;
    }
}
