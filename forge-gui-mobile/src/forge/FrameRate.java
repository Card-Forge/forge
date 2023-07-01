package forge;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.TimeUtils;

/**
 * A nicer class for showing framerate that doesn't spam the console
 * like Logger.log()
 *
 * @author William Hartman
 */

public class FrameRate implements Disposable{
    long lastTimeCounted;
    int cardsLoaded = 0;
    int allocT = 0;
    private float sinceChange;
    private float frameRate;
    private BitmapFont font;
    private SpriteBatch batch;
    private OrthographicCamera cam;

    public FrameRate() {
        lastTimeCounted = TimeUtils.millis();
        sinceChange = 0;
        frameRate = Gdx.graphics.getFramesPerSecond();
        font = new BitmapFont();
        batch = new SpriteBatch();
        cam = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    public void resize(int screenWidth, int screenHeight) {
        cam = new OrthographicCamera(screenWidth, screenHeight);
        cam.translate(screenWidth / 2, screenHeight / 2);
        cam.update();
        batch.setProjectionMatrix(cam.combined);
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

    public void render() {
        batch.begin();
        font.draw(batch, (int)frameRate + " FPS | " + cardsLoaded + " cards re/loaded | " + allocT + " MB", 3, Gdx.graphics.getHeight() - 3);
        batch.end();
    }

    public void dispose() {
        font.dispose();
        batch.dispose();
    }

}