package forge;

import java.util.Stack;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import forge.assets.FSkin;
import forge.assets.FSkinImage;
import forge.screens.home.HomeScreen;

public class Forge extends Game {
    private static Forge game;
    private static int screenHeight;
    private static SpriteBatch batch;
    private static final Stack<FScreen> screens = new Stack<FScreen>();

    public Forge() {
        if (game != null) {
            throw new RuntimeException("Cannot initialize Forge more than once");
        }
        game = this;
    }

    @Override
    public void create() {
        batch = new SpriteBatch();
        //Gdx.graphics.setContinuousRendering(false); //save power consumption by disabling continuous rendering

        FSkin.loadLight("journeyman", true);
        FSkin.loadFull(true);
        openScreen(new HomeScreen());
    }

    public static void back() {
        if (screens.size() < 2) { return; } //don't allow going back from initial screen
        screens.pop().dispose();
        game.setScreen(screens.lastElement());
    }

    public static void openScreen(FScreen screen0) {
        if (game.getScreen() == screen0) { return; }
        screens.push(screen0);
        game.setScreen(screen0);
    }

    @Override
    public void render () {
        Gdx.gl.glClear(GL10.GL_COLOR_BUFFER_BIT); // Clear the screen.
        batch.begin();
        super.render();
        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        screenHeight = height;
    }

    @Override
    public void dispose () {
        super.dispose();
        batch.dispose();
    }

    public static class Graphics {
        private final float offsetX, offsetY;

        public Graphics() {
            offsetX = 0;
            offsetY = 0;
        }

        public Graphics(Graphics g, float x, float y) {
            offsetX = g.offsetX + x;
            offsetY = g.offsetY + y;
        }

        public void drawImage(FSkinImage image, float x, float y) {
            drawImage(FSkin.getImages().get(image), x, y);
        }
        public void drawImage(TextureRegion image, float x, float y) {
            batch.draw(image, adjustX(x), adjustY(y, image.getRegionHeight()));
        }
        public void drawImage(FSkinImage image, float x, float y, float w, float h) {
            drawImage(FSkin.getImages().get(image), x, y, w, h);
        }
        public void drawImage(TextureRegion image, float x, float y, float w, float h) {
            batch.draw(image, adjustX(x), adjustY(y, h), w, h);
        }

        private float adjustX(float x) {
            return x + offsetX;
        }

        private float adjustY(float y, float height) {
            return screenHeight - y - offsetY - height; //flip y-axis
        }
    }
}
