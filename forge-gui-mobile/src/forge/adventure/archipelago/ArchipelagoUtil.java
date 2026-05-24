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

public class ArchipelagoUtil {
    private static final FImage CARD_LOCKED_ICON = FSkinImage.LOCK;

    public static void drawLockedCardOverlay(
            Batch batch, float x, float y, float w, float h) {

        // Todo: Darkening the card doesn't actually work here yet, please fix.
        // Darken card
//        batch.setColor(0.5f, 0.5f, 0.5f, 0.50f);
//        batch.draw(
//                FSkinImage.BLANK.getTextureRegion(),
//                x, y, w, h
//        );
//        batch.setColor(Color.WHITE);

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
}
