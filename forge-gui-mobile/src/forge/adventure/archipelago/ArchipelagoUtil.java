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

import java.util.Arrays;
import java.util.List;

public class ArchipelagoUtil {
    private static final FImage CARD_LOCKED_ICON = FSkinImage.LOCK;

    public static final String[] regionNames = {"waste", "white", "blue", "black", "red"};
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
