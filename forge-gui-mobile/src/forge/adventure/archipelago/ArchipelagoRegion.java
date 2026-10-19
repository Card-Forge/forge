package forge.adventure.archipelago;

import java.util.Arrays;
import java.util.List;

/**
 * The world regions that have their own Archipelago checks, one per biome.
 * The game itself only knows regions by their biome name (BiomeData.name, e.g. "white"), so convert those with
 * {@link #fromBiomeName} or {@link #fromShopName} before using them in fork code.
 * The order matters: a region's ordinal is its index in the APWorld's location ids (see the location bases in
 * ArchipelagoUtil), so never reorder these.
 */
public enum ArchipelagoRegion {
    WASTE("waste", "Wastes", ArchipelagoColors.RegionWastes, null, null),
    WHITE("white", "White", ArchipelagoColors.RegionWhite, "white rune", "WhiteRune"),
    BLUE("blue", "Blue", ArchipelagoColors.RegionBlue, "blue rune", "BlueRune"),
    BLACK("black", "Black", ArchipelagoColors.RegionBlack, "black rune", "BlackRune"),
    RED("red", "Red", ArchipelagoColors.RegionRed, "red rune", "RedRune"),
    GREEN("green", "Green", ArchipelagoColors.RegionGreen, "green rune", "GreenRune");

    public final String biomeName;      // The game's name for this biome, also how the region is stored in the save
    public final String label;          // Display name
    public final String textColor;      // TypingLabel color token for the region name
    public final String runeItemName;   // Teleport rune item that unlocks this region, null for the Wastes (never locked)
    public final String runeSpriteName; // Item sprite of that rune, null for the Wastes

    // The regions that start locked and are unlocked by their rune.
    public static final List<ArchipelagoRegion> LOCKABLE =
            Arrays.stream(values()).filter(region -> region.runeItemName != null).toList();

    ArchipelagoRegion(String biomeName, String label, String textColor, String runeItemName, String runeSpriteName) {
        this.biomeName = biomeName;
        this.label = label;
        this.textColor = textColor;
        this.runeItemName = runeItemName;
        this.runeSpriteName = runeSpriteName;
    }

    // The region of a biome the game reports (case-insensitive), or null if that biome isn't an Archipelago region.
    public static ArchipelagoRegion fromBiomeName(String biomeName) {
        for (ArchipelagoRegion region : values()) {
            if (region.biomeName.equalsIgnoreCase(biomeName)) return region;
        }
        return null;
    }

    // The region of a shop from its map name, e.g. "white equipment"; shops without a color in their name are in the Wastes.
    public static ArchipelagoRegion fromShopName(String shopName) {
        String lowerShopName = shopName.toLowerCase();
        for (ArchipelagoRegion region : LOCKABLE) {
            if (lowerShopName.contains(region.biomeName)) return region;
        }
        return WASTE;
    }

    // The region a rune item unlocks (case-insensitive), or null if the item isn't a region rune.
    public static ArchipelagoRegion fromRuneItemName(String itemName) {
        for (ArchipelagoRegion region : LOCKABLE) {
            if (region.runeItemName.equalsIgnoreCase(itemName)) return region;
        }
        return null;
    }
}
