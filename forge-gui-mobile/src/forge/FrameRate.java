package forge;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.TimeUtils;
import forge.assets.FSkinFont;

/**
 * A nicer class for showing framerate that doesn't spam the console
 * like Logger.log()
 *
 * @author William Hartman
 */

public class FrameRate {
    long lastTimeCounted;
    int cardsLoaded = 0;
    int allocT = 0;
    private float sinceChange;
    private float frameRate;
    private final FSkinFont font;
    private static FrameRate instance;
    private int maxClassicSpritesThisFrame = 0;
    private int historicalClassicMaxSprites = 0;
    private int maxAdventureSpritesThisFrame = 0;
    private int historicalAdventureMaxSprites = 0;
    private final StringBuilder displayBuilder;
    private String cachedDisplayString;
    private final Color hudColor;
    private int lastAllocT = 0;
    private float gcFlashTimer = 0f;
    public static volatile int hideFPSCountdown = 0;
    public static FrameRate getInstance() {
        return instance == null ? instance = new FrameRate() : instance;
    }

    private FrameRate() {
        float size = Forge.isLandscapeMode() ? Forge.getScreenWidth() / 64 : Forge.getScreenHeight() / 64;
        font = FSkinFont.forHeight(size);
        lastTimeCounted = TimeUtils.millis();
        sinceChange = 0;
        frameRate = Gdx.graphics.getFramesPerSecond();

        this.displayBuilder = new StringBuilder(128);
        this.cachedDisplayString = "";
        this.hudColor = new Color(Color.WHITE);
    }

    public void update(int loadedCardSize, float toAlloc) {
        allocT = (int) toAlloc;
        cardsLoaded = loadedCardSize;

        if (allocT < lastAllocT - 2) {
            gcFlashTimer = 0.5f;
        }
        lastAllocT = allocT;

        long delta = TimeUtils.timeSinceMillis(lastTimeCounted);
        lastTimeCounted = TimeUtils.millis();
        sinceChange += delta;

        if (sinceChange >= 1000) {
            sinceChange = 0;
            frameRate = Gdx.graphics.getFramesPerSecond();
            composeDisplay();
        }
    }

    public void render(boolean showFPS) {
        if (hideFPSCountdown > 0) {
            hideFPSCountdown--;
            return;
        }

        if (!showFPS || font == null)
            return;

        if (gcFlashTimer > 0) {
            gcFlashTimer -= Gdx.graphics.getDeltaTime();
            hudColor.set(Color.ORANGE);
        } else if (frameRate >= 55f) {
            hudColor.set(Color.GREEN);
        } else if (frameRate >= 30f) {
            hudColor.set(Color.YELLOW);
        } else {
            hudColor.set(Color.RED);
        }

        Forge.getGraphics().setProjectionMatrix(Forge.camera.combined);
        Forge.getGraphics().getBatch().begin();

        font.draw(Forge.getGraphics().getBatch(), cachedDisplayString, hudColor, 5, Forge.getScreenHeight() - 5, Forge.getScreenWidth(), true, Align.left);

        Forge.getGraphics().getBatch().end();
    }

    private void composeDisplay() {
        displayBuilder.setLength(0);

        displayBuilder.append((int) frameRate).append(" FPS | ")
            .append(cardsLoaded).append(" cards re/loaded | ")
            .append(allocT).append(" MB");

        if (gcFlashTimer > 0) {
            displayBuilder.append(" [GC]");
        }

        displayBuilder.append(" | ")
            .append(maxClassicSpritesThisFrame).append(" Classic Sprites | ")
            .append(maxAdventureSpritesThisFrame).append(" Adventure Sprites ");

        cachedDisplayString = displayBuilder.toString();
    }

    public void sampleClassic(boolean showFPS) {
        if (!showFPS)
            return;
        int batchMax = Forge.getGraphics().getBatch().maxSpritesInBatch;
        if (batchMax > maxClassicSpritesThisFrame) {
            maxClassicSpritesThisFrame = batchMax;
            composeDisplay();
        }
    }

    public void sampleAdventure(Batch batch, boolean showFPS) {
        if (!showFPS)
            return;
        int batchMax = ((SpriteBatch) batch).maxSpritesInBatch;
        if (batchMax > maxAdventureSpritesThisFrame) {
            maxAdventureSpritesThisFrame = batchMax;
            composeDisplay();
        }
    }

    public void updateHistoricalPeak(boolean update) {
        if (!update)
            return;
        if (maxAdventureSpritesThisFrame > historicalAdventureMaxSprites) {
            historicalAdventureMaxSprites = maxAdventureSpritesThisFrame;
        }
        maxAdventureSpritesThisFrame = 0;

        if (maxClassicSpritesThisFrame > historicalClassicMaxSprites) {
            historicalClassicMaxSprites = maxClassicSpritesThisFrame;
        }
        maxClassicSpritesThisFrame = 0;
        composeDisplay();
    }

    public int getHistoricalMaxSprites(boolean isAdventure) {
        return isAdventure ? historicalAdventureMaxSprites : historicalClassicMaxSprites;
    }
}
