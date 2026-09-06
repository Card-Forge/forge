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

    public static FrameRate getInstance() {
        return instance == null ? instance = new FrameRate() : instance;
    }

    private FrameRate() {
        font = FSkinFont.get(10);
        lastTimeCounted = TimeUtils.millis();
        sinceChange = 0;
        frameRate = Gdx.graphics.getFramesPerSecond();
    }

    public void update(int loadedCardSize, float toAlloc) {
        allocT = (int) toAlloc;
        cardsLoaded = loadedCardSize;
        long delta = TimeUtils.timeSinceMillis(lastTimeCounted);
        lastTimeCounted = TimeUtils.millis();
        sinceChange += delta;
        if(sinceChange >= 1000) {
            sinceChange = 0;
            frameRate = Gdx.graphics.getFramesPerSecond();
        }
    }

    public void render(boolean showFPS) {
        if (font == null) // shouldn't be null
            return;
        if (showFPS) {
            Forge.getGraphics().getBatch().begin();
            font.draw(Forge.getGraphics().getBatch(), composeDisplay(), Color.WHITE, 5, Forge.getScreenHeight() - 5, Forge.getScreenWidth(), false, Align.left);
            Forge.getGraphics().getBatch().end();
        }
    }

    private String composeDisplay() {
        // TODO: make the display better..
        return (int)frameRate + " FPS | "
            + cardsLoaded + " cards re/loaded | "
            + allocT + " MB | "
            + maxClassicSpritesThisFrame + " Classic Sprites | "
            + maxAdventureSpritesThisFrame + " Adventure Sprites ";
    }

    public void sampleClassic() {
        int batchMax = Forge.getGraphics().getBatch().maxSpritesInBatch;
        if (batchMax > maxClassicSpritesThisFrame) {
            maxClassicSpritesThisFrame = batchMax;
        }
    }

    public void sampleAdventure(Batch batch) {
        int batchMax = ((SpriteBatch) batch).maxSpritesInBatch;
        if (batchMax > maxAdventureSpritesThisFrame) {
            maxAdventureSpritesThisFrame = batchMax;
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
    }

    public int getHistoricalMaxSprites(boolean isAdventure) {
        return isAdventure ? historicalAdventureMaxSprites : historicalClassicMaxSprites;
    }
}