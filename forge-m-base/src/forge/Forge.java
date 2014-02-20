package forge;

import java.util.Stack;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;

import forge.assets.FSkin;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.screens.home.HomeScreen;
import forge.toolbox.FDisplayObject;

public class Forge extends Game {
    private static Forge game;
    private static int screenWidth;
    private static int screenHeight;
    private static SpriteBatch batch;
    private static ShapeRenderer shapeRenderer;
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
        shapeRenderer = new ShapeRenderer();
        Gdx.graphics.setContinuousRendering(false); //save power consumption by disabling continuous rendering

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
        screenWidth = width;
        screenHeight = height;
    }

    @Override
    public void dispose () {
        super.dispose();
        batch.dispose();
        shapeRenderer.dispose();
    }

    public static class Graphics {
        private Rectangle bounds;
        private int failedClipCount;

        public static void drawScreen(FScreen screen) {
            Gdx.gl.glEnable(GL10.GL_SCISSOR_TEST);
            Graphics g = new Graphics();
            g.draw(screen);
            Gdx.gl.glDisable(GL10.GL_SCISSOR_TEST);
        }

        private Graphics() {
            bounds = new Rectangle(0, 0, screenWidth, screenHeight);
        }

        public void startClip() {
            startClip(0, 0, bounds.width, bounds.height);
        }
        public void startClip(float x, float y, float w, float h) {
            batch.flush(); //must flush batch to prevent other things not rendering
            if (!ScissorStack.pushScissors(new Rectangle(adjustX(0), adjustY(0, h), w, h))) {
                failedClipCount++; //tracked failed clips to prevent calling popScissors on endClip
            }
        }
        public void endClip() {
            if (failedClipCount == 0) {
                ScissorStack.popScissors();
            }
            else {
                failedClipCount--;
            }
        }

        public void draw(FDisplayObject displayObj) {
            final Rectangle parentBounds = bounds;
            bounds = new Rectangle(parentBounds.x + displayObj.getLeft(), parentBounds.y + displayObj.getTop(), displayObj.getWidth(), displayObj.getHeight());

            if (bounds.overlaps(parentBounds)) { //avoid drawing object if it's not within visible region
                displayObj.draw(this);
            }

            bounds = parentBounds;
        }

        public void fillRect(FSkinColor skinColor, float x, float y, float w, float h) {
            fillRect(skinColor.getColor(), x, y, w, h);
        }
        public void fillRect(Color color, float x, float y, float w, float h) {
            batch.end(); //must pause batch while rendering shapes

            if (color.a != 0) { //enable blending so alpha colored shapes work properly
                Gdx.gl.glEnable(GL20.GL_BLEND);
            }
            shapeRenderer.begin(ShapeType.Filled);
            shapeRenderer.setColor(color);
            shapeRenderer.rect(x, y, w, h);
            shapeRenderer.end();

            batch.begin();
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

        public void drawText(String text, FSkinFont skinFont, FSkinColor skinColor, float x, float y, float w, float h, boolean wrap, boolean centerHorizontally, boolean centerVertically) {
            BitmapFont font = skinFont.getFont();
            font.setColor(skinColor.getColor());
            if (wrap) {
                float textHeight = font.getWrappedBounds(text, w).height;
                if (h > textHeight && centerVertically) {
                    y += (h - textHeight) / 2;
                }
                else if (h == 0) {
                    h = textHeight;
                }
                font.drawWrapped(batch, text, adjustX(x), adjustY(y, h), w, centerHorizontally ? HAlignment.CENTER : HAlignment.LEFT);
            }
            else {
                float textHeight = font.getMultiLineBounds(text).height;
                if (h > textHeight && centerVertically) {
                    y += (h - textHeight) / 2;
                }
                else if (h == 0) {
                    h = textHeight;
                }
                font.drawMultiLine(batch, text, adjustX(x), adjustY(y, 0), w, centerHorizontally ? HAlignment.CENTER : HAlignment.LEFT);
            }
        }

        private float adjustX(float x) {
            return x + bounds.x;
        }

        private float adjustY(float y, float height) {
            return screenHeight - y - bounds.y - height; //flip y-axis
        }
    }
}
