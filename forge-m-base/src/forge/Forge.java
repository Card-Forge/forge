package forge;

import java.util.ArrayList;
import java.util.Stack;

import com.badlogic.gdx.ApplicationListener;
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
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;

import forge.assets.FSkin;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.FImage;
import forge.screens.home.HomeScreen;
import forge.toolbox.FDisplayObject;

public class Forge implements ApplicationListener {
    private static Forge game;
    private static int screenWidth;
    private static int screenHeight;
    private static SpriteBatch batch;
    private static ShapeRenderer shapeRenderer;
    private static FScreen currentScreen;
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
        Gdx.input.setInputProcessor(new FGestureDetector());

        FSkin.loadLight("journeyman", true);
        FSkin.loadFull(true);
        openScreen(new HomeScreen());
    }

    public static void showMenu() {
    	if (currentScreen == null) { return; }
    	currentScreen.showMenu();
    }

    public static void back() {
        if (screens.size() < 2) { return; } //don't allow going back from initial screen
        screens.pop();
        setCurrentScreen(screens.lastElement());
    }

    public static void openScreen(FScreen screen0) {
        if (currentScreen == screen0) { return; }
        screens.push(screen0);
        setCurrentScreen(screen0);
        screen0.onOpen();
    }

    private static void setCurrentScreen(FScreen screen0) {
        currentScreen = screen0;
        currentScreen.setSize(screenWidth, screenHeight);
    }

    @Override
    public void render () {
        Gdx.gl.glClear(GL10.GL_COLOR_BUFFER_BIT); // Clear the screen.
        if (currentScreen != null) {
            batch.begin();
            Graphics g = new Graphics();
            currentScreen.draw(g);
            batch.end();
        }
    }

    @Override
    public void resize(int width, int height) {
        screenWidth = width;
        screenHeight = height;
        if (currentScreen != null) {
            currentScreen.setSize(width, height);
        }
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void dispose () {
        currentScreen = null;
        screens.clear();
        batch.dispose();
        shapeRenderer.dispose();
    }

    private static class FGestureDetector extends GestureDetector {
        private static final ArrayList<FDisplayObject> potentialListeners = new ArrayList<FDisplayObject>();

        @Override
        public boolean touchUp(float x, float y, int pointer, int button) {
            for (FDisplayObject listener : potentialListeners) {
                if (listener.touchUp(x, y)) {
                    break;
                }
            }
            return super.touchUp(x, y, pointer, button);
        }

        private FGestureDetector() {
            super(new GestureListener() {
                @Override
                public boolean touchDown(float x, float y, int pointer, int button) {
                    potentialListeners.clear();
                    if (currentScreen != null) { //base potential listeners on object containing touch down point
                        currentScreen.buildTouchListeners(x, y, potentialListeners);
                    }
                    for (FDisplayObject listener : potentialListeners) {
                        if (listener.touchDown(x, y)) {
                            return true;
                        }
                    }
                    return false;
                }

                @Override
                public boolean tap(float x, float y, int count, int button) {
                    for (FDisplayObject listener : potentialListeners) {
                        if (listener.tap(x, y, count)) {
                            return true;
                        }
                    }
                    return false;
                }

                @Override
                public boolean longPress(float x, float y) {
                    for (FDisplayObject listener : potentialListeners) {
                        if (listener.longPress(x, y)) {
                            return true;
                        }
                    }
                    return false;
                }

                @Override
                public boolean fling(float velocityX, float velocityY, int button) {
                    for (FDisplayObject listener : potentialListeners) {
                        if (listener.fling(velocityX, velocityY)) {
                            return true;
                        }
                    }
                    return false;
                }

                @Override
                public boolean pan(float x, float y, float deltaX, float deltaY) {
                    for (FDisplayObject listener : potentialListeners) {
                        if (listener.pan(x, y, deltaX, deltaY)) {
                            return true;
                        }
                    }
                    return false;
                }

                @Override
                public boolean panStop(float x, float y, int pointer, int button) {
                    for (FDisplayObject listener : potentialListeners) {
                        if (listener.panStop(x, y)) {
                            return true;
                        }
                    }
                    return false;
                }

                @Override
                public boolean zoom(float initialDistance, float distance) {
                    for (FDisplayObject listener : potentialListeners) {
                        if (listener.zoom(initialDistance, distance)) {
                            return true;
                        }
                    }
                    return false;
                }

                @Override
                public boolean pinch(Vector2 initialPointer1, Vector2 initialPointer2, Vector2 pointer1, Vector2 pointer2) {
                    for (FDisplayObject listener : potentialListeners) {
                        if (listener.pinch(initialPointer1, initialPointer2, pointer1, pointer2)) {
                            return true;
                        }
                    }
                    return false;
                }
            });
        }
    }

    public static class Graphics {
        private Rectangle bounds;
        private int failedClipCount;

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

        public void drawRect(FSkinColor skinColor, float x, float y, float w, float h) {
            drawRect(skinColor.getColor(), x, y, w, h);
        }
        public void drawRect(Color color, float x, float y, float w, float h) {
            batch.end(); //must pause batch while rendering shapes

            //adjust width/height/y so rectangle covers equivalent filled area
            w = Math.round(w - 1);
            h = Math.round(h - 1);
            y = Math.round(y + 1);

            shapeRenderer.begin(ShapeType.Line);
            shapeRenderer.setColor(color);

            //must user 4 line() calls rather than rect() since rect() leaves corner unfilled
            x = adjustX(x);
            float y2 = adjustY(y, h);
            float x2 = x + w;
            y = y2 + h;
            shapeRenderer.line(x, y, x, y2);
            shapeRenderer.line(x, y2, x2, y2);
            shapeRenderer.line(x2, y2, x2, y);
            shapeRenderer.line(x2 + 1, y, x, y); //+1 prevents corner not being filled

            shapeRenderer.end();
            batch.begin();
        }

        public void fillRect(FSkinColor skinColor, float x, float y, float w, float h) {
            fillRect(skinColor.getColor(), x, y, w, h);
        }
        public void fillRect(Color color, float x, float y, float w, float h) {
            batch.end(); //must pause batch while rendering shapes

            boolean needBlending = (color.a != 0);
            if (needBlending) { //enable blending so alpha colored shapes work properly
                Gdx.gl.glEnable(GL20.GL_BLEND);
            }

            shapeRenderer.begin(ShapeType.Filled);
            shapeRenderer.setColor(color);
            shapeRenderer.rect(adjustX(x), adjustY(y, h), w, h);
            shapeRenderer.end();

            if (needBlending) {
                Gdx.gl.glDisable(GL20.GL_BLEND);
            }

            batch.begin();
        }

        public void fillGradientRect(FSkinColor skinColor1, FSkinColor skinColor2, boolean vertical, float x, float y, float w, float h) {
            fillGradientRect(skinColor1.getColor(), skinColor2.getColor(), vertical, x, y, w, h);
        }
        public void fillGradientRect(FSkinColor skinColor1, Color color2, boolean vertical, float x, float y, float w, float h) {
            fillGradientRect(skinColor1.getColor(), color2, vertical, x, y, w, h);
        }
        public void fillGradientRect(Color color1, FSkinColor skinColor2, boolean vertical, float x, float y, float w, float h) {
            fillGradientRect(color1, skinColor2.getColor(), vertical, x, y, w, h);
        }
        public void fillGradientRect(Color color1, Color color2, boolean vertical, float x, float y, float w, float h) {
            batch.end(); //must pause batch while rendering shapes

            boolean needBlending = (color1.a != 0 || color2.a != 0);
            if (needBlending) { //enable blending so alpha colored shapes work properly
                Gdx.gl.glEnable(GL20.GL_BLEND);
            }

            Color topLeftColor = color1;
            Color topRightColor = vertical ? color1 : color2;
            Color bottomLeftColor = vertical ? color2 : color1;
            Color bottomRightColor = color2;

            shapeRenderer.begin(ShapeType.Filled);
            shapeRenderer.rect(adjustX(x), adjustY(y, h), w, h, bottomLeftColor, bottomRightColor, topRightColor, topLeftColor);
            shapeRenderer.end();

            if (needBlending) {
                Gdx.gl.glDisable(GL20.GL_BLEND);
            }

            batch.begin();
        }

        public void drawImage(FImage image, float x, float y, float w, float h) {
        	image.draw(this, x, y, w, h);
        }
        public void drawImage(TextureRegion image, float x, float y, float w, float h) {
            batch.draw(image, adjustX(x), adjustY(y, h), w, h);
        }

        public void drawText(String text, FSkinFont skinFont, FSkinColor skinColor, float x, float y, float w, float h, boolean wrap, HAlignment horzAlignment, boolean centerVertically) {
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
                font.drawWrapped(batch, text, adjustX(x), adjustY(y, h), w, horzAlignment);
            }
            else {
                float textHeight = font.getMultiLineBounds(text).height;
                if (h > textHeight && centerVertically) {
                    y += (h - textHeight) / 2;
                }
                else if (h == 0) {
                    h = textHeight;
                }
                font.drawMultiLine(batch, text, adjustX(x), adjustY(y, 0), w, horzAlignment);
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
