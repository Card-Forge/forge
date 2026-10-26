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
import java.util.Set;

public class ArchipelagoUtil {
    private static final FImage CARD_LOCKED_ICON = FSkinImage.LOCK;
    public static final int TOTAL_AMOUNT_OF_SET_UNLOCK_CHECKS = 100;
    // Location id of a check that doesn't exist in the APWorld (Emrakul only counts towards the goal).
    public static final long NO_LOCATION = -1L;
    public static final Set<String> COLOR_SET_NAMES =
            Collections.unmodifiableSet(new LinkedHashSet<>(List.of("white", "blue", "black", "red", "green")));

    /**
     * Static definition of a castle boss or miniboss that gives an Archipelago check: who it is, where it lives and
     * which check it sends. It holds no progress or state; whether a save has defeated it is stored per save, see
     * {@link ArchipelagoData#isBossOrMinibossDefeated(Boss)}.
     */
    public static final class Boss {
        public final String name;          // Display name
        public final String enemyName;     // Enemy data name for their portrait; differs when a map or nameOverride renames the enemy
        public final ArchipelagoRegion region;
        public final boolean castleBoss;
        public final ArchipelagoCheckTypes check;
        public final long locationId;     // Archipelago location id sent for this boss, or NO_LOCATION (Emrakul only counts towards the goal)
        private final List<String> recordedNames; // Lower-case names a defeat can be recorded under (EnemySprite.getName())

        private Boss(String name, String enemyName, ArchipelagoRegion region, boolean castleBoss, ArchipelagoCheckTypes check, long locationId, String... aliases) {
            this.name = name;
            this.enemyName = enemyName;
            this.region = region;
            this.castleBoss = castleBoss;
            this.check = check;
            this.locationId = locationId;
            List<String> names = new ArrayList<>();
            names.add(name.toLowerCase());
            for (String alias : aliases) names.add(alias.toLowerCase());
            this.recordedNames = Collections.unmodifiableList(names);
        }

        /**
         * True if a defeat recorded under this name belongs to this boss (case-insensitive). DuelScene records defeats
         * under {@code EnemySprite.getName()}, which can be an alias, e.g. "Fungus of Slimefoot" for Slimefoot.
         */
        public boolean matches(String recordedName) {
            return recordedName != null && recordedNames.contains(recordedName.toLowerCase());
        }
    }

    private static Boss castleBoss(String name, ArchipelagoRegion region, ArchipelagoCheckTypes check, long locationId) {
        return new Boss(name, name, region, true, check, locationId);
    }

    private static Boss miniBoss(String name, String enemyName, ArchipelagoRegion region, ArchipelagoCheckTypes check, long locationId, String... aliases) {
        return new Boss(name, enemyName, region, false, check, locationId, aliases);
    }

    // All checked bosses, grouped by region in ArchipelagoRegion order; the castle boss comes first in each region.
    public static final List<Boss> BOSSES = List.of(
            castleBoss("Emrakul", ArchipelagoRegion.WASTE, ArchipelagoCheckTypes.BOSS_COLORLESS_DEFEATED, NO_LOCATION),
            miniBoss("The Mother Slime", "Ooze Boss", ArchipelagoRegion.WASTE, ArchipelagoCheckTypes.SLIME_MOTHER_DEFEATED, 100L),
            miniBoss("Slobad", "Slobad", ArchipelagoRegion.WASTE, ArchipelagoCheckTypes.SLOBAD_DEFEATED, 101L),
            miniBoss("Xira", "Xira", ArchipelagoRegion.WASTE, ArchipelagoCheckTypes.XIRA_DEFEATED, 102L),
            castleBoss("Akroma", ArchipelagoRegion.WHITE, ArchipelagoCheckTypes.BOSS_WHITE_DEFEATED, 1L),
            miniBoss("Nahiri", "Nahiri", ArchipelagoRegion.WHITE, ArchipelagoCheckTypes.NAHIRI_DEFEATED, 200L),
            miniBoss("Valyx Feaster of Torment", "Valyx Feaster of Torment", ArchipelagoRegion.WHITE, ArchipelagoCheckTypes.VALYX_DEFEATED, 201L),
            castleBoss("Lorthos", ArchipelagoRegion.BLUE, ArchipelagoCheckTypes.BOSS_BLUE_DEFEATED, 2L),
            miniBoss("Jace", "Jace", ArchipelagoRegion.BLUE, ArchipelagoCheckTypes.JACE_DEFEATED, 300L),
            miniBoss("Kiora", "Kiora", ArchipelagoRegion.BLUE, ArchipelagoCheckTypes.KIORA_DEFEATED, 301L),
            miniBoss("Myr Superion", "Myr Superion", ArchipelagoRegion.BLUE, ArchipelagoCheckTypes.MYR_SUPERION_DEFEATED, 302L),
            miniBoss("Sliver Queen", "Sliver Queen", ArchipelagoRegion.BLUE, ArchipelagoCheckTypes.SLIVER_QUEEN_DEFEATED, 303L),
            miniBoss("Teferi", "Teferi", ArchipelagoRegion.BLUE, ArchipelagoCheckTypes.TEFERI_DEFEATED, 304L),
            castleBoss("Griselbrand", ArchipelagoRegion.BLACK, ArchipelagoCheckTypes.BOSS_BLACK_DEFEATED, 3L),
            miniBoss("Grolnok", "Grolnok", ArchipelagoRegion.BLACK, ArchipelagoCheckTypes.GROLNOK_DEFEATED, 400L),
            miniBoss("Guardian Angel", "Guardian Angel", ArchipelagoRegion.BLACK, ArchipelagoCheckTypes.GUARDIAN_ANGEL_DEFEATED, 401L),
            miniBoss("Liliana", "Liliana", ArchipelagoRegion.BLACK, ArchipelagoCheckTypes.LILIANA_DEFEATED, 402L),
            // Slimefoot's enemy has nameOverride "Fungus of Slimefoot", which is the name recorded on defeat
            miniBoss("Slimefoot", "Slimefoot", ArchipelagoRegion.BLACK, ArchipelagoCheckTypes.SLIMEFOOT_DEFEATED, 403L, "Fungus of Slimefoot"),
            miniBoss("Sorin", "Sorin", ArchipelagoRegion.BLACK, ArchipelagoCheckTypes.SORIN_DEFEATED, 404L),
            castleBoss("Lathliss", ArchipelagoRegion.RED, ArchipelagoCheckTypes.BOSS_RED_DEFEATED, 4L),
            miniBoss("Chandra", "Chandra", ArchipelagoRegion.RED, ArchipelagoCheckTypes.CHANDRA_DEFEATED, 500L),
            miniBoss("Tibalt's Torturer", "Torturer", ArchipelagoRegion.RED, ArchipelagoCheckTypes.TIBALTS_TORTURER_DEFEATED, 501L),
            miniBoss("Tibalt", "Tibalt", ArchipelagoRegion.RED, ArchipelagoCheckTypes.TIBALT_DEFEATED, 502L),
            miniBoss("Zedruu's Cook", "The Cheff", ArchipelagoRegion.RED, ArchipelagoCheckTypes.ZEDRUUS_COOK_DEFEATED, 503L),
            miniBoss("Conjurer", "Conjurer", ArchipelagoRegion.RED, ArchipelagoCheckTypes.CONJURER_DEFEATED, 504L),
            miniBoss("Zedruu", "Zedruu", ArchipelagoRegion.RED, ArchipelagoCheckTypes.ZEDRUU_DEFEATED, 505L),
            castleBoss("Ghalta", ArchipelagoRegion.GREEN, ArchipelagoCheckTypes.BOSS_GREEN_DEFEATED, 5L),
            miniBoss("Garruk", "Garruk", ArchipelagoRegion.GREEN, ArchipelagoCheckTypes.GARRUK_DEFEATED, 600L),
            miniBoss("The Hydra of Shandalaar", "The Hydra of Shandalaar", ArchipelagoRegion.GREEN, ArchipelagoCheckTypes.HYDRA_OF_SHANDALAAR_DEFEATED, 601L),
            miniBoss("Scarecrow Captain", "Scarecrow Captain", ArchipelagoRegion.GREEN, ArchipelagoCheckTypes.SCARECROW_CAPTAIN_DEFEATED, 602L));

    /**
     * Looks up which boss a defeated enemy's name belongs to (the name DuelScene passes to addBossDefeated/addMiniBossDefeated).
     * Returns null if that enemy doesn't give a check.
     */
    public static Boss findBoss(String recordedName) {
        for (Boss boss : BOSSES) {
            if (boss.matches(recordedName)) return boss;
        }
        return null;
    }

    // The bosses of one region, castle boss first, in BOSSES order.
    public static List<Boss> getBossesInRegion(ArchipelagoRegion region) {
        List<Boss> bosses = new ArrayList<>();
        for (Boss boss : BOSSES) {
            if (boss.region == region) bosses.add(boss);
        }
        return bosses;
    }

    // The boss that sends the given check, or null if the check isn't a boss check.
    public static Boss findBossByCheck(ArchipelagoCheckTypes check) {
        for (Boss boss : BOSSES) {
            if (boss.check == check) return boss;
        }
        return null;
    }

    /// --- Archipelago location ids (must match the APWorld's locations.py) ---
    // Repeatable checks per region: the n-th check (n starts at 1) is base + n, with base = offset + 10000 * the region's
    // ordinal in ArchipelagoRegion (Wastes 0 ... Green 5), e.g. the 2nd white battle check is 19999 + 2.
    private static final long BATTLE_LOCATION_OFFSET = 9999L;
    private static final long INN_EVENT_LOCATION_OFFSET = 10999L;
    private static final long TOWN_QUEST_LOCATION_OFFSET = 11999L;
    // Card collection checks: the n-th check for a rarity is base + n.
    public static final long COMMON_CARD_LOCATION_BASE = 4999L;
    public static final long UNCOMMON_CARD_LOCATION_BASE = 5099L;
    public static final long RARE_CARD_LOCATION_BASE = 5199L;
    public static final long MYTHIC_RARE_CARD_LOCATION_BASE = 5299L;

    public static long getBattleLocationBase(ArchipelagoRegion region) {
        return BATTLE_LOCATION_OFFSET + 10000L * region.ordinal();
    }

    public static long getInnEventLocationBase(ArchipelagoRegion region) {
        return INN_EVENT_LOCATION_OFFSET + 10000L * region.ordinal();
    }

    public static long getTownQuestLocationBase(ArchipelagoRegion region) {
        return TOWN_QUEST_LOCATION_OFFSET + 10000L * region.ordinal();
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

    // The rune sprite of the region the game calls biomeName, or null if that biome has no rune.
    private static TextureRegion getRegionRune(String biomeName) {
        ArchipelagoRegion region = ArchipelagoRegion.fromBiomeName(biomeName);
        return region != null && region.runeSpriteName != null ? Config.instance().getItemSprite(region.runeSpriteName) : null;
    }

    public static void updateLastTraversedRegionOnTeleport(String command) {
        String loweredCommand = command.toLowerCase();
        ArchipelagoData apData = ArchipelagoData.getInstance();
        if (loweredCommand.contains("spawn")) { apData.setLastTraversedRegion(ArchipelagoRegion.WASTE); }
        if (loweredCommand.contains("plains")) { apData.setLastTraversedRegion(ArchipelagoRegion.WHITE); }
        if (loweredCommand.contains("island")) { apData.setLastTraversedRegion(ArchipelagoRegion.BLUE); }
        if (loweredCommand.contains("swamp")) { apData.setLastTraversedRegion(ArchipelagoRegion.BLACK); }
        if (loweredCommand.contains("mountain")) { apData.setLastTraversedRegion(ArchipelagoRegion.RED); }
        if (loweredCommand.contains("forest")) { apData.setLastTraversedRegion(ArchipelagoRegion.GREEN); }
    }

    public static void drawLockedRegionOverhead(Batch batch, String biomeName, float centerX, float baseY, float alpha) {
        TextureRegion rune = getRegionRune(biomeName);
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
        if (block == null || data.getArchipelagoMode() == ArchipelagoMode.disabled) {
            return "";
        }
        List<CardEdition> sets = block.getSets();
        if (sets.isEmpty()) {
            return "";
        }
        for (CardEdition set : sets) {
            if (data.isSetUnlocked(set.getCode())) {
                return "";
            }
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
