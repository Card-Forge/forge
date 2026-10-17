package forge.adventure.archipelago;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import forge.Graphics;
import forge.adventure.data.RewardData;
import forge.adventure.util.Config;
import forge.adventure.util.Reward;
import forge.assets.FImage;
import forge.assets.FSkinImage;
import forge.card.CardEdition;
import forge.model.CardBlock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class ArchipelagoUtil {
    private static final FImage CARD_LOCKED_ICON = FSkinImage.LOCK;
    public static final int TOTAL_AMOUNT_OF_SET_UNLOCK_CHECKS = 100;
    public static final String WASTE_REGION = "waste";
    public static final Set<String> COLOR_SET_NAMES =
            Collections.unmodifiableSet(new LinkedHashSet<>(List.of("white", "blue", "black", "red", "green")));

    public static final String[] REGION_NAMES =
            Stream.concat(
                    Stream.of(WASTE_REGION),
                    COLOR_SET_NAMES.stream().map(String::toLowerCase)
            ).toArray(String[]::new);

    private static final Map<String, String> REGION_LABELS = Map.of(
            WASTE_REGION, "Wastes", "white", "White", "blue", "Blue", "black", "Black", "red", "Red", "green", "Green");
    private static final Map<String, String> REGION_TEXT_COLORS = Map.of(
            WASTE_REGION, ArchipelagoColors.RegionWastes, "white", ArchipelagoColors.RegionWhite,
            "blue", ArchipelagoColors.RegionBlue, "black", ArchipelagoColors.RegionBlack,
            "red", ArchipelagoColors.RegionRed, "green", ArchipelagoColors.RegionGreen);

    public static String getRegionLabel(String region) {
        return REGION_LABELS.getOrDefault(region, region);
    }

    public static String getRegionTextColor(String region) {
        return REGION_TEXT_COLORS.getOrDefault(region, "");
    }

    /** A castle boss or miniboss that gives an Archipelago check. */
    public static final class Boss {
        public final String name;          // display name
        public final String enemyName;     // enemy data name (portrait); differs when a map or nameOverride renames the enemy
        public final String region;        // one of REGION_NAMES
        public final boolean castleBoss;
        public final ArchipelagoCheckTypes check;
        private final List<String> recordedNames; // lower-case names a defeat can be recorded under (EnemySprite.getName())

        private Boss(String name, String enemyName, String region, boolean castleBoss, ArchipelagoCheckTypes check, String... aliases) {
            this.name = name;
            this.enemyName = enemyName;
            this.region = region;
            this.castleBoss = castleBoss;
            this.check = check;
            List<String> names = new ArrayList<>();
            names.add(name.toLowerCase());
            for (String alias : aliases) names.add(alias.toLowerCase());
            this.recordedNames = Collections.unmodifiableList(names);
        }

        public boolean isDefeated(Set<String> defeatedNames) {
            for (String recorded : recordedNames) {
                if (defeatedNames.contains(recorded)) return true;
            }
            return false;
        }
    }

    private static Boss castleBoss(String name, String region, ArchipelagoCheckTypes check) {
        return new Boss(name, name, region, true, check);
    }

    private static Boss miniBoss(String name, String enemyName, String region, ArchipelagoCheckTypes check, String... aliases) {
        return new Boss(name, enemyName, region, false, check, aliases);
    }

    // All checked bosses, grouped by region in REGION_NAMES order; the castle boss comes first in each region.
    public static final List<Boss> BOSSES = List.of(
            castleBoss("Emrakul", WASTE_REGION, ArchipelagoCheckTypes.BOSS_COLORLESS_DEFEATED),
            miniBoss("The Mother Slime", "Ooze Boss", WASTE_REGION, ArchipelagoCheckTypes.SLIME_MOTHER_DEFEATED),
            miniBoss("Slobad", "Slobad", WASTE_REGION, ArchipelagoCheckTypes.SLOBAD_DEFEATED),
            miniBoss("Xira", "Xira", WASTE_REGION, ArchipelagoCheckTypes.XIRA_DEFEATED),
            castleBoss("Akroma", "white", ArchipelagoCheckTypes.BOSS_WHITE_DEFEATED),
            miniBoss("Nahiri", "Nahiri", "white", ArchipelagoCheckTypes.NAHIRI_DEFEATED),
            miniBoss("Valyx Feaster of Torment", "Valyx Feaster of Torment", "white", ArchipelagoCheckTypes.VALYX_DEFEATED),
            castleBoss("Lorthos", "blue", ArchipelagoCheckTypes.BOSS_BLUE_DEFEATED),
            miniBoss("Jace", "Jace", "blue", ArchipelagoCheckTypes.JACE_DEFEATED),
            miniBoss("Kiora", "Kiora", "blue", ArchipelagoCheckTypes.KIORA_DEFEATED),
            miniBoss("Myr Superion", "Myr Superion", "blue", ArchipelagoCheckTypes.MYR_SUPERION_DEFEATED),
            miniBoss("Sliver Queen", "Sliver Queen", "blue", ArchipelagoCheckTypes.SLIVER_QUEEN_DEFEATED),
            miniBoss("Teferi", "Teferi", "blue", ArchipelagoCheckTypes.TEFERI_DEFEATED),
            castleBoss("Griselbrand", "black", ArchipelagoCheckTypes.BOSS_BLACK_DEFEATED),
            miniBoss("Grolnok", "Grolnok", "black", ArchipelagoCheckTypes.GROLNOK_DEFEATED),
            miniBoss("Guardian Angel", "Guardian Angel", "black", ArchipelagoCheckTypes.GUARDIAN_ANGEL_DEFEATED),
            miniBoss("Liliana", "Liliana", "black", ArchipelagoCheckTypes.LILIANA_DEFEATED),
            // Slimefoot's enemy has nameOverride "Fungus of Slimefoot", which is the name recorded on defeat
            miniBoss("Slimefoot", "Slimefoot", "black", ArchipelagoCheckTypes.SLIMEFOOT_DEFEATED, "Fungus of Slimefoot"),
            miniBoss("Sorin", "Sorin", "black", ArchipelagoCheckTypes.SORIN_DEFEATED),
            castleBoss("Lathliss", "red", ArchipelagoCheckTypes.BOSS_RED_DEFEATED),
            miniBoss("Chandra", "Chandra", "red", ArchipelagoCheckTypes.CHANDRA_DEFEATED),
            miniBoss("Tibalt's Torturer", "Torturer", "red", ArchipelagoCheckTypes.TIBALTS_TORTURER_DEFEATED),
            miniBoss("Tibalt", "Tibalt", "red", ArchipelagoCheckTypes.TIBALT_DEFEATED),
            miniBoss("Zedruu's Cook", "The Cheff", "red", ArchipelagoCheckTypes.ZEDRUUS_COOK_DEFEATED),
            miniBoss("Conjurer", "Conjurer", "red", ArchipelagoCheckTypes.CONJURER_DEFEATED),
            miniBoss("Zedruu", "Zedruu", "red", ArchipelagoCheckTypes.ZEDRUU_DEFEATED),
            castleBoss("Ghalta", "green", ArchipelagoCheckTypes.BOSS_GREEN_DEFEATED),
            miniBoss("Garruk", "Garruk", "green", ArchipelagoCheckTypes.GARRUK_DEFEATED),
            miniBoss("The Hydra of Shandalaar", "The Hydra of Shandalaar", "green", ArchipelagoCheckTypes.HYDRA_OF_SHANDALAAR_DEFEATED),
            miniBoss("Scarecrow Captain", "Scarecrow Captain", "green", ArchipelagoCheckTypes.SCARECROW_CAPTAIN_DEFEATED));

    // Returns a boss a defeat was recorded under, or null if it doesn't give a check.
    public static Boss findBoss(String recordedName) {
        if (recordedName == null) return null;
        Set<String> name = Set.of(recordedName.toLowerCase());
        for (Boss boss : BOSSES) {
            if (boss.isDefeated(name)) return boss;
        }
        return null;
    }

    public static List<Boss> getBossesInRegion(String region) {
        List<Boss> bosses = new ArrayList<>();
        for (Boss boss : BOSSES) {
            if (boss.region.equals(region)) bosses.add(boss);
        }
        return bosses;
    }
    public static void drawLockedCardOverlay(
            Batch batch, float x, float y, float w, float h) {
        // Draw lock icon
        // Normalize bounds (works for flipped cards too)
        float drawX = Math.min(x, x + w);
        float drawY = Math.min(y, y + h);
        float drawW = Math.abs(w);
        float drawH = Math.abs(h);

        // Lock size
        float lockSize = drawW * 0.25f;

        // Centered horizontally, near visual top
        float lockX = drawX + (drawW - lockSize) / 2f;
        float lockY = drawY + drawH - lockSize - (drawH * 0.05f);

        // Draw lock
        TextureRegion lockIcon = FSkinImage.LOCK.getTextureRegion();
        batch.draw(lockIcon, lockX, lockY, lockSize, lockSize);
    }

    public static void drawLockedCardOverlay(Graphics g, float x, float y, float w, float h) {
        float lockSize = w * 0.25f;
        float lockX = x + (w - lockSize) / 2f;
        float lockY = y + h * 0.05f;
        CARD_LOCKED_ICON.draw(g, lockX, lockY, lockSize, lockSize);

        // Draw the card darker than the rest to show it's not unlocked.
        float oldalpha = g.getfloatAlphaComposite();
        g.setAlphaComposite(0.25f);
        g.fillRect(Color.BLACK, x, y, w, h);
        g.setAlphaComposite(oldalpha);
    }

    private static TextureRegion getRegionRune(String regionName) {
        return switch (regionName.toLowerCase()) {
            case "white" -> Config.instance().getItemSprite("WhiteRune");
            case "blue" -> Config.instance().getItemSprite("BlueRune");
            case "black" -> Config.instance().getItemSprite("BlackRune");
            case "red" -> Config.instance().getItemSprite("RedRune");
            case "green" -> Config.instance().getItemSprite("GreenRune");
            default -> null;
        };
    }

    public static void updateLastTraversedRegionOnTeleport(String command) {
        String loweredCommand = command.toLowerCase();
        ArchipelagoData apData = ArchipelagoData.getInstance();
        if (loweredCommand.contains("spawn")) { apData.setLastTraversedRegion("waste"); }
        if (loweredCommand.contains("plains")) { apData.setLastTraversedRegion("white"); }
        if (loweredCommand.contains("island")) { apData.setLastTraversedRegion("blue"); }
        if (loweredCommand.contains("swamp")) { apData.setLastTraversedRegion("black"); }
        if (loweredCommand.contains("mountain")) { apData.setLastTraversedRegion("red"); }
        if (loweredCommand.contains("forest")) { apData.setLastTraversedRegion("green"); }
    }

    public static void drawLockedRegionOverhead(Batch batch, String regionName, float centerX, float baseY, float alpha) {
        TextureRegion rune = getRegionRune(regionName);
        if (rune == null)
            return;

        float size = 24f; // world-space size; tweak if needed
        float x = centerX - size / 2f;
        float y = baseY + 16f; // slightly above head

        Color old = new Color(batch.getColor());

        // Draw rune
        batch.setColor(1f, 1f, 1f, alpha);
        batch.draw(rune, x, y, size, size);

        // Draw red cross overlay
        TextureRegion cross = FSkinImage.WARNING.getTextureRegion();
        batch.draw(cross, x, y, size / 2, size / 2);

        batch.setColor(old);
    }

    public static RewardData generateRewardData(String type, int count, String itemName) {
        RewardData data = new RewardData();
        data.type = type;
        data.count = count;
        data.itemName = itemName;
        return  data;
    }

    public static Reward generateReward(String type, int count, String itemName) {
        RewardData data = generateRewardData(type, count, itemName);
        Array<Reward> replacedAPReward = data.generate(false, true);
        if (replacedAPReward != null &&  !replacedAPReward.isEmpty()) {
            return replacedAPReward.get(0);
        }
        return null;
    }

    // Marker shown after an inn event's block when every set in that block is still locked, "" otherwise.
    // returnColor restores the label's text color after the red marker (e.g. "[BLACK]" for event descriptions).
    public static String lockedBlockMarker(CardBlock block, String returnColor) {
        ArchipelagoData data = ArchipelagoData.getInstance();
        if (block == null || data.getArchipelagoMode() == ArchipelagoMode.disabled) return "";
        List<CardEdition> sets = block.getSets();
        if (sets.isEmpty()) return "";
        for (CardEdition set : sets) {
            if (data.isSetUnlocked(set.getCode())) return "";
        }
        return " [RED](Locked)" + returnColor;
    }

    /// We depend on these POI names to have these exact names.
    /// These are used as identifiers to make sure that when generating a world in networked archipelago they are placed close enough to the center to always be well accessible within their region.
    /// This is to prevent logic errors where a region unlock might be locked behind a certain miniboss dungeon, but that dungeon happened to generate in a random bubble within another region.
    public static List<String> getArchipelagoPois() {
        return Arrays.asList("slime cave", "slobads factory", "xiras hive", "emrakul", "quest_aportaltonowhere", "quest_digsite", "quest_primaljungle", "quest_banditcave",
                                "nahiri encampment", "unhallowedabbey",
                                "kiora island", "teferi hideout", "jacehold", "skep", "quest_libraryofvarsil",
                                "slimefoots lair", "temple of liliana", "grolnoks bog", "vampirecastle3",
                                "temple of chandra", "tibalts fortress", "zedruu city", "quest_shardmines",
                                "garruk forest", "scarecrow farm", "quest_frostbittencavern", "grove7");
    }
}
