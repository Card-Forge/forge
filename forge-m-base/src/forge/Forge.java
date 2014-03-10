package forge;

import java.util.ArrayList;
import java.util.Stack;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;

import forge.assets.FSkin;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.FImage;
import forge.model.FModel;
import forge.screens.FScreen;
import forge.screens.SplashScreen;
import forge.screens.home.HomeScreen;
import forge.toolbox.FContainer;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FGestureAdapter;

public class Forge implements ApplicationListener {
    private static Forge game;
    private static int screenWidth;
    private static int screenHeight;
    private static SpriteBatch batch;
    private static ShapeRenderer shapeRenderer;
    private static FScreen currentScreen;
    private static SplashScreen splashScreen;
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

        splashScreen = new SplashScreen();

        //load model on background thread (using progress bar to report progress)
        new Thread(new Runnable() {
            @Override
            public void run() {
                FModel.initialize(splashScreen.getProgressBar());

                splashScreen.getProgressBar().setDescription("Opening main window...");

                Gdx.app.postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        afterDbLoaded();
                    }
                });
            }
        }).start();
    }

    private void afterDbLoaded() {
        Gdx.graphics.setContinuousRendering(false); //save power consumption by disabling continuous rendering once assets loaded

        FSkin.loadFull(splashScreen);

        Gdx.input.setInputProcessor(new MainInputProcessor());
        openScreen(new HomeScreen());
        splashScreen = null;
    }

    public static void showMenu() {
        if (currentScreen == null) { return; }
        currentScreen.showMenu();
    }

    public static void back() {
        if (screens.size() < 2) { return; } //don't allow going back from initial screen
        if (!currentScreen.onClose(true)) {
            return;
        }
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
        currentScreen.onOpen();
    }

    @Override
    public void render () {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT); // Clear the screen.

        FContainer screen = currentScreen;
        if (screen == null) {
            screen = splashScreen;
            if (screen == null) { 
                return;
            }
        }

        batch.begin();
        Graphics g = new Graphics();
        screen.draw(g);
        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        screenWidth = width;
        screenHeight = height;
        if (currentScreen != null) {
            currentScreen.setSize(width, height);
        }
        else if (splashScreen != null) {
            splashScreen.setSize(width, height);
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
        if (currentScreen != null) {
            currentScreen.onClose(false);
            currentScreen = null;
        }
        screens.clear();
        batch.dispose();
        shapeRenderer.dispose();
    }

    private static class MainInputProcessor extends FGestureAdapter {
        private static final ArrayList<FDisplayObject> potentialListeners = new ArrayList<FDisplayObject>();

        @Override
        public boolean touchDown(int x, int y, int pointer, int button) {
            potentialListeners.clear();
            if (currentScreen != null) { //base potential listeners on object containing touch down point
                currentScreen.buildTouchListeners(x, y, potentialListeners);
            }
            return super.touchDown(x, y, pointer, button);
        }

        @Override
        public boolean press(float x, float y) {
            for (FDisplayObject listener : potentialListeners) {
                if (listener.press(listener.screenToLocalX(x), listener.screenToLocalY(y))) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean release(float x, float y) {
            for (FDisplayObject listener : potentialListeners) {
                if (listener.release(listener.screenToLocalX(x), listener.screenToLocalY(y))) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean longPress(float x, float y) {
            for (FDisplayObject listener : potentialListeners) {
                if (listener.longPress(listener.screenToLocalX(x), listener.screenToLocalY(y))) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean tap(float x, float y, int count) {
            for (FDisplayObject listener : potentialListeners) {
                if (listener.tap(listener.screenToLocalX(x), listener.screenToLocalY(y), count)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean fling(float velocityX, float velocityY) {
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
                if (listener.pan(listener.screenToLocalX(x), listener.screenToLocalY(y), deltaX, deltaY)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean panStop(float x, float y) {
            for (FDisplayObject listener : potentialListeners) {
                if (listener.panStop(listener.screenToLocalX(x), listener.screenToLocalY(y))) {
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
            if (!ScissorStack.pushScissors(new Rectangle(adjustX(x), adjustY(y, h), w, h))) {
                failedClipCount++; //tracked failed clips to prevent calling popScissors on endClip
            }
        }
        public void endClip() {
            if (failedClipCount == 0) {
                batch.flush(); //must flush batch to ensure stuffed rendered during clip respects that clip
                ScissorStack.popScissors();
            }
            else {
                failedClipCount--;
            }
        }

        public void draw(FDisplayObject displayObj) {
            if (displayObj.getWidth() <= 0 || displayObj.getHeight() <= 0) {
                return;
            }

            final Rectangle parentBounds = bounds;
            bounds = new Rectangle(parentBounds.x + displayObj.getLeft(), parentBounds.y + displayObj.getTop(), displayObj.getWidth(), displayObj.getHeight());
            displayObj.setScreenPosition(bounds.x, bounds.y);

            if (bounds.overlaps(parentBounds)) { //avoid drawing object if it's not within visible region
                displayObj.draw(this);
            }

            bounds = parentBounds;
        }

        public void drawLine(float thickness, FSkinColor skinColor, float x1, float y1, float x2, float y2) {
            drawLine(thickness, skinColor.getColor(), x1, y1, x2, y2);
        }
        public void drawLine(float thickness, Color color, float x1, float y1, float x2, float y2) {
            batch.end(); //must pause batch while rendering shapes

            if (thickness > 1) {
                Gdx.gl.glLineWidth(thickness);
            }
            if (color.a != 0) { //enable blending so alpha colored shapes work properly
                Gdx.gl.glEnable(GL20.GL_BLEND);
            }
            boolean needSmoothing = (x1 != x2 && y1 != y2);
            if (needSmoothing) {
                Gdx.gl.glEnable(GL10.GL_LINE_SMOOTH);
            }

            shapeRenderer.begin(ShapeType.Line);
            shapeRenderer.setColor(color);
            shapeRenderer.line(adjustX(x1), adjustY(y1, 0), adjustX(x2), adjustY(y2, 0));
            shapeRenderer.end();

            if (needSmoothing) {
                Gdx.gl.glDisable(GL10.GL_LINE_SMOOTH);
            }
            if (color.a != 0) {
                Gdx.gl.glDisable(GL20.GL_BLEND);
            }
            if (thickness > 1) {
                Gdx.gl.glLineWidth(1);
            }

            batch.begin();
        }

        public void drawRoundRect(float thickness, FSkinColor skinColor, float x, float y, float w, float h, float cornerRadius) {
            drawRoundRect(thickness, skinColor.getColor(), x, y, w, h, cornerRadius);
        }
        public void drawRoundRect(float thickness, Color color, float x, float y, float w, float h, float cornerRadius) {
            batch.end(); //must pause batch while rendering shapes

            if (thickness > 1) {
                Gdx.gl.glLineWidth(thickness);
            }
            if (color.a != 0) { //enable blending so alpha colored shapes work properly
                Gdx.gl.glEnable(GL20.GL_BLEND);
            }
            if (cornerRadius > 0) {
                Gdx.gl.glEnable(GL10.GL_LINE_SMOOTH);
            }

            //adjust width/height so rectangle covers equivalent filled area
            w = Math.round(w - 1);
            h = Math.round(h - 1);

            shapeRenderer.begin(ShapeType.Line);
            shapeRenderer.setColor(color);

            x = adjustX(x);
            float y2 = adjustY(y, h);
            float x2 = x + w;
            y = y2 + h;
            //TODO: draw arcs at corners
            shapeRenderer.line(x, y, x, y2);
            shapeRenderer.line(x, y2, x2 + 1, y2); //+1 prevents corner not being filled
            shapeRenderer.line(x2, y2, x2, y);
            shapeRenderer.line(x2 + 1, y, x, y); //+1 prevents corner not being filled

            shapeRenderer.end();

            if (cornerRadius > 0) {
                Gdx.gl.glDisable(GL10.GL_LINE_SMOOTH);
            }
            if (color.a != 0) {
                Gdx.gl.glDisable(GL20.GL_BLEND);
            }
            if (thickness > 1) {
                Gdx.gl.glLineWidth(1);
            }

            batch.begin();
        }

        public void drawRect(float thickness, FSkinColor skinColor, float x, float y, float w, float h) {
            drawRect(thickness, skinColor.getColor(), x, y, w, h);
        }
        public void drawRect(float thickness, Color color, float x, float y, float w, float h) {
            batch.end(); //must pause batch while rendering shapes

            if (thickness > 1) {
                Gdx.gl.glLineWidth(thickness);
            }
            if (color.a != 0) { //enable blending so alpha colored shapes work properly
                Gdx.gl.glEnable(GL20.GL_BLEND);
            }
            Gdx.gl.glEnable(GL10.GL_LINE_SMOOTH); //must be smooth to ensure edges aren't missed

            shapeRenderer.begin(ShapeType.Line);
            shapeRenderer.setColor(color);
            shapeRenderer.rect(adjustX(x), adjustY(y, h), w, h);
            shapeRenderer.end();

            Gdx.gl.glDisable(GL10.GL_LINE_SMOOTH);
            if (color.a != 0) {
                Gdx.gl.glDisable(GL20.GL_BLEND);
            }
            if (thickness > 1) {
                Gdx.gl.glLineWidth(1);
            }

            batch.begin();
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
            shapeRenderer.rect(adjustX(x), adjustY(y, h), w, h);
            shapeRenderer.end();

            if (color.a != 0) {
                Gdx.gl.glDisable(GL20.GL_BLEND);
            }

            batch.begin();
        }

        public void drawCircle(float thickness, FSkinColor skinColor, float x, float y, float radius) {
            drawCircle(thickness, skinColor.getColor(), x, y, radius);
        }
        public void drawCircle(float thickness, Color color, float x, float y, float radius) {
            batch.end(); //must pause batch while rendering shapes

            if (thickness > 1) {
                Gdx.gl.glLineWidth(thickness);
            }
            if (color.a != 0) { //enable blending so alpha colored shapes work properly
                Gdx.gl.glEnable(GL20.GL_BLEND);
            }
            Gdx.gl.glEnable(GL10.GL_LINE_SMOOTH);

            shapeRenderer.begin(ShapeType.Line);
            shapeRenderer.setColor(color);
            shapeRenderer.circle(adjustX(x), adjustY(y, 0), radius);
            shapeRenderer.end();

            Gdx.gl.glDisable(GL10.GL_LINE_SMOOTH);
            if (color.a != 0) {
                Gdx.gl.glDisable(GL20.GL_BLEND);
            }
            if (thickness > 1) {
                Gdx.gl.glLineWidth(1);
            }

            batch.begin();
        }

        public void fillCircle(FSkinColor skinColor, float x, float y, float radius) {
            fillCircle(skinColor.getColor(), x, y, radius);
        }
        public void fillCircle(Color color, float x, float y, float radius) {
            batch.end(); //must pause batch while rendering shapes

            if (color.a != 0) { //enable blending so alpha colored shapes work properly
                Gdx.gl.glEnable(GL20.GL_BLEND);
            }

            shapeRenderer.begin(ShapeType.Filled);
            shapeRenderer.setColor(color);
            shapeRenderer.circle(adjustX(x), adjustY(y, 0), radius); //TODO: Make smoother
            shapeRenderer.end();

            if (color.a != 0) {
                Gdx.gl.glDisable(GL20.GL_BLEND);
            }

            batch.begin();
        }

        public void fillTriangle(FSkinColor skinColor, float x1, float y1, float x2, float y2, float x3, float y3) {
            fillTriangle(skinColor.getColor(), x1, y1, x2, y2, x3, y3);
        }
        public void fillTriangle(Color color, float x1, float y1, float x2, float y2, float x3, float y3) {
            batch.end(); //must pause batch while rendering shapes

            if (color.a != 0) { //enable blending so alpha colored shapes work properly
                Gdx.gl.glEnable(GL20.GL_BLEND);
            }

            shapeRenderer.begin(ShapeType.Filled);
            shapeRenderer.setColor(color);
            shapeRenderer.triangle(adjustX(x1), adjustY(y1, 0), adjustX(x2), adjustY(y2, 0), adjustX(x3), adjustY(y3, 0));
            shapeRenderer.end();

            if (color.a != 0) {
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

        public void setImageTint(Color color) {
            batch.setColor(color);
        }
        public void clearImageTint() {
            batch.setColor(Color.WHITE);
        }

        public void drawImage(FImage image, float x, float y, float w, float h) {
            image.draw(this, x, y, w, h);
        }
        public void drawImage(Texture image, float x, float y, float w, float h) {
            batch.draw(image, adjustX(x), adjustY(y, h), w, h);
        }
        public void drawImage(TextureRegion image, float x, float y, float w, float h) {
            batch.draw(image, adjustX(x), adjustY(y, h), w, h);
        }

        public void drawRotatedImage(Texture image, float x, float y, float w, float h, float originX, float originY, float rotation) {
            batch.draw(image, adjustX(x), adjustY(y, h), originX - x, h - (originY - y), w, h, 1, 1, rotation, 0, 0, image.getWidth(), image.getHeight(), false, false);
        }

        public void drawText(String text, FSkinFont skinFont, FSkinColor skinColor, float x, float y, float w, float h, boolean wrap, HAlignment horzAlignment, boolean centerVertically) {
            drawText(text, skinFont.getFont(), skinColor.getColor(), x, y, w, h, wrap, horzAlignment, centerVertically);
        }
        public void drawText(String text, FSkinFont skinFont, Color color, float x, float y, float w, float h, boolean wrap, HAlignment horzAlignment, boolean centerVertically) {
            drawText(text, skinFont.getFont(), color, x, y, w, h, wrap, horzAlignment, centerVertically);
        }
        public void drawText(String text, BitmapFont font, Color color, float x, float y, float w, float h, boolean wrap, HAlignment horzAlignment, boolean centerVertically) {
            font.setColor(color);
            if (wrap) {
                float textHeight = font.getWrappedBounds(text, w).height;
                if (h > textHeight && centerVertically) {
                    y += (h - textHeight) / 2;
                }
                font.drawWrapped(batch, text, adjustX(x), adjustY(y, 0), w, horzAlignment);
            }
            else {
                float textHeight = font.getMultiLineBounds(text).height;
                if (h > textHeight && centerVertically) {
                    y += (h - textHeight) / 2;
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
