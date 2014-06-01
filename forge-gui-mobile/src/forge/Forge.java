package forge;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont.TextBounds;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.badlogic.gdx.utils.Clipboard;

import forge.assets.AssetsDownloader;
import forge.assets.FSkin;
import forge.assets.FSkinColor;
import forge.assets.FSkinFont;
import forge.assets.FImage;
import forge.error.BugReporter;
import forge.error.ExceptionHandler;
import forge.model.FModel;
import forge.properties.ForgeConstants;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;
import forge.screens.FScreen;
import forge.screens.SplashScreen;
import forge.screens.home.HomeScreen;
import forge.screens.match.FControl;
import forge.toolbox.FContainer;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FGestureAdapter;
import forge.toolbox.FOptionPane;
import forge.toolbox.FOverlay;
import forge.util.Callback;
import forge.util.FileUtil;
import forge.util.Utils;

public class Forge implements ApplicationListener {
    public static final String CURRENT_VERSION = "1.5.19.007";

    private static final ApplicationListener app = new Forge();
    private static Clipboard clipboard;
    private static int screenWidth;
    private static int screenHeight;
    private static SpriteBatch batch;
    private static ShapeRenderer shapeRenderer;
    private static FScreen currentScreen;
    private static SplashScreen splashScreen;
    private static KeyInputAdapter keyInputAdapter;
    private static final Stack<FScreen> screens = new Stack<FScreen>();

    public static ApplicationListener getApp(Clipboard clipboard0, String assetDir0) {
        if (GuiBase.getInterface() == null) {
            clipboard = clipboard0;
            GuiBase.setInterface(new GuiMobile(assetDir0));
        }
        return app;
    }

    private Forge() {
    }

    @Override
    public void create() {
        //install our error handler
        ExceptionHandler.registerErrorHandling();

        Texture.setEnforcePotImages(false); //ensure image dimensions don't have to be powers of 2

        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();

        splashScreen = new SplashScreen();

        String skinName;
        if (FileUtil.doesFileExist(ForgeConstants.MAIN_PREFS_FILE)) {
            skinName = new ForgePreferences().getPref(FPref.UI_SKIN);
        }
        else {
            skinName = "default"; //use default skin if preferences file doesn't exist yet
        }
        FSkin.loadLight(skinName, splashScreen);

        //load model on background thread (using progress bar to report progress)
        FThreads.invokeInBackgroundThread(new Runnable() {
            @Override
            public void run() {
                //see if app or assets need updating
                AssetsDownloader.checkForUpdates(splashScreen);

                FModel.initialize(splashScreen.getProgressBar());

                splashScreen.getProgressBar().setDescription("Loading fonts...");
                FSkinFont.preloadAll();

                splashScreen.getProgressBar().setDescription("Finishing startup...");

                Gdx.app.postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        afterDbLoaded();
                    }
                });
            }
        });
    }

    private void afterDbLoaded() {
        Gdx.graphics.setContinuousRendering(false); //save power consumption by disabling continuous rendering once assets loaded

        FSkin.loadFull(splashScreen);

        Gdx.input.setInputProcessor(new MainInputProcessor());
        Gdx.input.setCatchBackKey(true);
        Gdx.input.setCatchMenuKey(true);
        openScreen(new HomeScreen());
        splashScreen = null;
    }

    public static Clipboard getClipboard() {
        return clipboard;
    }

    public static void showMenu() {
        if (currentScreen == null) { return; }
        endKeyInput(); //end key input before menu shown
        if (FOverlay.getTopOverlay() == null) { //don't show menu if overlay open
            currentScreen.showMenu();
        }
    }

    public static void back() {
        if (screens.size() < 2) {
            exit(); //prompt to exit if attempting to go back from home screen
            return;
        }
        if (!currentScreen.onClose(true)) {
            return;
        }
        screens.pop();
        setCurrentScreen(screens.lastElement());
    }

    public static void exit() {
        FOptionPane.showConfirmDialog("Are you sure you wish to exit Forge?", "Exit Forge", "Exit", "Cancel", new Callback<Boolean>() {
            @Override
            public void run(Boolean result) {
                if (result) {
                    Gdx.app.exit();
                }
            }
        });
    }

    public static void openScreen(FScreen screen0) {
        if (currentScreen == screen0) { return; }
        if (currentScreen != null && !currentScreen.onSwitchAway()) {
            return;
        }
        screens.push(screen0);
        setCurrentScreen(screen0);
    }

    public static FScreen getCurrentScreen() {
        return currentScreen;
    }

    private static void setCurrentScreen(FScreen screen0) {
        try {
            endKeyInput(); //end key input before switching screens
            Animation.endAll(); //end all active animations before switching screens
    
            currentScreen = screen0;
            currentScreen.setSize(screenWidth, screenHeight);
            currentScreen.onActivate();
        }
        catch (Exception ex) {
            batch.end();
            BugReporter.reportException(ex);
        }
    }

    @Override
    public void render() {
        try {
            Animation.advanceAll();
    
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
            for (FOverlay overlay : FOverlay.getOverlays()) {
                overlay.setSize(screenWidth, screenHeight); //update overlay sizes as they're rendered
                overlay.draw(g);
            }
            batch.end();
        }
        catch (Exception ex) {
            batch.end();
            BugReporter.reportException(ex);
        }
    }

    @Override
    public void resize(int width, int height) {
        try {
            screenWidth = width;
            screenHeight = height;
            if (currentScreen != null) {
                currentScreen.setSize(width, height);
            }
            else if (splashScreen != null) {
                splashScreen.setSize(width, height);
            }
        }
        catch (Exception ex) {
            batch.end();
            BugReporter.reportException(ex);
        }
    }

    @Override
    public void pause() {
        FControl.pause();
    }

    @Override
    public void resume() {
    }

    @Override
    public void dispose () {
        if (currentScreen != null) {
            FOverlay overlay = FOverlay.getTopOverlay();
            while (overlay != null) {
                overlay.hide();
                overlay = FOverlay.getTopOverlay();
            }
            currentScreen.onClose(false);
            currentScreen = null;
        }
        screens.clear();
        batch.dispose();
        shapeRenderer.dispose();
        System.exit(0);
    }

    //log message to Forge.log file
    public static void log(String message) {
        System.out.println(message);
    }

    public static void startKeyInput(KeyInputAdapter adapter) {
        if (keyInputAdapter == adapter) { return; }
        if (keyInputAdapter != null) {
            keyInputAdapter.onInputEnd(); //make sure previous adapter is ended
        }
        keyInputAdapter = adapter;
        Gdx.input.setOnscreenKeyboardVisible(true);
    }

    public static boolean endKeyInput() {
        if (keyInputAdapter == null) { return false; }
        keyInputAdapter.onInputEnd();
        keyInputAdapter = null;
        MainInputProcessor.keyTyped = false;
        MainInputProcessor.lastKeyTyped = '\0';
        Gdx.input.setOnscreenKeyboardVisible(false);
        return true;
    }

    public static abstract class KeyInputAdapter {
        public abstract FDisplayObject getOwner();
        public abstract boolean allowTouchInput();
        public abstract boolean keyTyped(char ch);
        public abstract boolean keyDown(int keyCode);
        public abstract void onInputEnd();

        //also allow handling of keyUp but don't require it
        public boolean keyUp(int keyCode) { return false; }

        public static boolean isCtrlKeyDown() {
            return Gdx.input.isKeyPressed(Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Keys.CONTROL_RIGHT);
        }
        public static boolean isShiftKeyDown() {
            return Gdx.input.isKeyPressed(Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Keys.SHIFT_RIGHT);
        }
        public static boolean isAltKeyDown() {
            return Gdx.input.isKeyPressed(Keys.ALT_LEFT) || Gdx.input.isKeyPressed(Keys.ALT_RIGHT);
        }
        public static boolean isModifierKey(int keyCode) {
            switch (keyCode) {
            case Keys.CONTROL_LEFT:
            case Keys.CONTROL_RIGHT:
            case Keys.SHIFT_LEFT:
            case Keys.SHIFT_RIGHT:
            case Keys.ALT_LEFT:
            case Keys.ALT_RIGHT:
                return true;
            }
            return false;
        }
    }

    private static class MainInputProcessor extends FGestureAdapter {
        private static final ArrayList<FDisplayObject> potentialListeners = new ArrayList<FDisplayObject>();
        private static char lastKeyTyped;
        private static boolean keyTyped;

        @Override
        public boolean keyDown(int keyCode) {
            if (keyCode == Keys.MENU) {
                showMenu();
                return true;
            }
            if (keyInputAdapter == null) {
                if (KeyInputAdapter.isModifierKey(keyCode)) {
                    return false; //don't process modifiers keys for unknown adapter
                }
                //if no active key input adapter, give current screen or overlay a chance to handle key
                FContainer container = FOverlay.getTopOverlay();
                if (container == null) {
                    container = currentScreen;
                    if (container == null) {
                        return false;
                    }
                }
                return container.keyDown(keyCode);
            }
            return keyInputAdapter.keyDown(keyCode);
        }

        @Override
        public boolean keyUp(int keyCode) {
            keyTyped = false; //reset on keyUp
            if (keyInputAdapter != null) {
                return keyInputAdapter.keyUp(keyCode);
            }
            return false;
        }

        @Override
        public boolean keyTyped(char ch) {
            if (keyInputAdapter != null) {
                if (ch >= ' ' && ch <= '~') { //only process this event if character is printable
                    //prevent firing this event more than once for the same character on the same key down, otherwise it fires too often
                    if (lastKeyTyped != ch || !keyTyped) {
                        keyTyped = true;
                        lastKeyTyped = ch;
                        return keyInputAdapter.keyTyped(ch);
                    }
                }
            }
            return false;
        }

        private void updatePotentialListeners(int x, int y) {
            potentialListeners.clear();
            if (currentScreen != null) { //base potential listeners on object containing touch down point
                FOverlay overlay = FOverlay.getTopOverlay();
                if (overlay != null) { //let top overlay handle gestures if any is open
                    overlay.buildTouchListeners(x, y, potentialListeners);
                }
                else {
                    currentScreen.buildTouchListeners(x, y, potentialListeners);
                }
            }
        }

        @Override
        public boolean touchDown(int x, int y, int pointer, int button) {
            updatePotentialListeners(x, y);
            if (keyInputAdapter != null) {
                if (!keyInputAdapter.allowTouchInput() || !potentialListeners.contains(keyInputAdapter.getOwner())) {
                    endKeyInput(); //end key input and suppress touch event if needed
                    potentialListeners.clear();
                }
            }
            return super.touchDown(x, y, pointer, button);
        }

        @Override
        public boolean press(float x, float y) {
            try {
                for (FDisplayObject listener : potentialListeners) {
                    if (listener.press(listener.screenToLocalX(x), listener.screenToLocalY(y))) {
                        return true;
                    }
                }
                return false;
            }
            catch (Exception ex) {
                BugReporter.reportException(ex);
                return true;
            }
        }

        @Override
        public boolean release(float x, float y) {
            try {
                for (FDisplayObject listener : potentialListeners) {
                    if (listener.release(listener.screenToLocalX(x), listener.screenToLocalY(y))) {
                        return true;
                    }
                }
                return false;
            }
            catch (Exception ex) {
                BugReporter.reportException(ex);
                return true;
            }
        }

        @Override
        public boolean longPress(float x, float y) {
            try {
                for (FDisplayObject listener : potentialListeners) {
                    if (listener.longPress(listener.screenToLocalX(x), listener.screenToLocalY(y))) {
                        return true;
                    }
                }
                return false;
            }
            catch (Exception ex) {
                BugReporter.reportException(ex);
                return true;
            }
        }

        @Override
        public boolean tap(float x, float y, int count) {
            try {
                for (FDisplayObject listener : potentialListeners) {
                    if (listener.tap(listener.screenToLocalX(x), listener.screenToLocalY(y), count)) {
                        return true;
                    }
                }
                return false;
            }
            catch (Exception ex) {
                BugReporter.reportException(ex);
                return true;
            }
        }

        @Override
        public boolean fling(float velocityX, float velocityY) {
            try {
                for (FDisplayObject listener : potentialListeners) {
                    if (listener.fling(velocityX, velocityY)) {
                        return true;
                    }
                }
                return false;
            }
            catch (Exception ex) {
                BugReporter.reportException(ex);
                return true;
            }
        }

        @Override
        public boolean pan(float x, float y, float deltaX, float deltaY, boolean moreVertical) {
            try {
                for (FDisplayObject listener : potentialListeners) {
                    if (listener.pan(listener.screenToLocalX(x), listener.screenToLocalY(y), deltaX, deltaY, moreVertical)) {
                        return true;
                    }
                }
                return false;
            }
            catch (Exception ex) {
                BugReporter.reportException(ex);
                return true;
            }
        }

        @Override
        public boolean panStop(float x, float y) {
            try {
                for (FDisplayObject listener : potentialListeners) {
                    if (listener.panStop(listener.screenToLocalX(x), listener.screenToLocalY(y))) {
                        return true;
                    }
                }
                return false;
            }
            catch (Exception ex) {
                BugReporter.reportException(ex);
                return true;
            }
        }

        @Override
        public boolean zoom(float x, float y, float amount) {
            try {
                for (FDisplayObject listener : potentialListeners) {
                    if (listener.zoom(listener.screenToLocalX(x), listener.screenToLocalY(y), amount)) {
                        return true;
                    }
                }
                return false;
            }
            catch (Exception ex) {
                BugReporter.reportException(ex);
                return true;
            }
        }

        //mouseMoved and scrolled events for desktop version
        private int mouseMovedX, mouseMovedY;

        @Override
        public boolean mouseMoved(int x, int y) {
            mouseMovedX = x;
            mouseMovedY = y;
            return true;
        }

        @Override
        public boolean scrolled(int amount) {
            updatePotentialListeners(mouseMovedX, mouseMovedY);

            if (KeyInputAdapter.isCtrlKeyDown()) { //zoom in or out based on amount
                return zoom(mouseMovedX, mouseMovedY, -Utils.AVG_FINGER_WIDTH * amount);
            }

            boolean handled;
            if (KeyInputAdapter.isShiftKeyDown()) {
                handled = pan(mouseMovedX, mouseMovedY, -Utils.AVG_FINGER_WIDTH * amount, 0, false);
            }
            else {
                handled = pan(mouseMovedX, mouseMovedY, 0, -Utils.AVG_FINGER_HEIGHT * amount, true);
            }
            if (panStop(mouseMovedX, mouseMovedY)) {
                handled = true;
            }
            return handled;
        }
    }

    public static abstract class Animation {
        private static final List<Animation> activeAnimations = new ArrayList<Animation>();

        public void start() {
            if (activeAnimations.contains(this)) { return; } //prevent starting the same animation multiple times

            activeAnimations.add(this);
            if (activeAnimations.size() == 1) { //if first animation being started, ensure continuous rendering turned on
                Gdx.graphics.setContinuousRendering(true);
            }
        }

        private static void advanceAll() {
            if (activeAnimations.isEmpty()) { return; }

            float dt = Gdx.graphics.getDeltaTime();
            for (int i = 0; i < activeAnimations.size(); i++) {
                if (!activeAnimations.get(i).advance(dt)) {
                    activeAnimations.remove(i);
                    i--;
                }
            }

            if (activeAnimations.isEmpty()) { //when all animations have ended, turn continuous rendering back off
                Gdx.graphics.setContinuousRendering(false);
            }
        }

        private static void endAll() {
            if (activeAnimations.isEmpty()) { return; }

            activeAnimations.clear();
            Gdx.graphics.setContinuousRendering(false);
        }

        //return true if animation should continue, false to stop the animation
        protected abstract boolean advance(float dt);
    }

    public static class Graphics {
        private Rectangle bounds;
        private Rectangle visibleBounds;
        private int failedClipCount;
        private float alphaComposite = 1;

        private Graphics() {
            bounds = new Rectangle(0, 0, screenWidth, screenHeight);
            visibleBounds = new Rectangle(bounds);
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

            Rectangle intersection = Utils.getIntersection(bounds, visibleBounds);
            if (intersection != null) { //avoid drawing object if it's not within visible region
                final Rectangle backup = visibleBounds;
                visibleBounds = intersection;

                displayObj.draw(this);

                visibleBounds = backup;
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
            if (alphaComposite < 1) {
                color = FSkinColor.alphaColor(color, color.a * alphaComposite);
            }
            boolean needSmoothing = (x1 != x2 && y1 != y2);
            if (color.a < 1 || needSmoothing) { //enable blending so alpha colored shapes work properly
                Gdx.gl.glEnable(GL20.GL_BLEND);
            }
            if (needSmoothing) {
                Gdx.gl.glEnable(GL10.GL_LINE_SMOOTH);
            }

            startShape(ShapeType.Line);
            shapeRenderer.setColor(color);
            shapeRenderer.line(adjustX(x1), adjustY(y1, 0), adjustX(x2), adjustY(y2, 0));
            endShape();

            if (needSmoothing) {
                Gdx.gl.glDisable(GL10.GL_LINE_SMOOTH);
            }
            if (color.a < 1 || needSmoothing) {
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
            if (alphaComposite < 1) {
                color = FSkinColor.alphaColor(color, color.a * alphaComposite);
            }
            if (color.a < 1 || cornerRadius > 0) { //enable blending so alpha colored shapes work properly
                Gdx.gl.glEnable(GL20.GL_BLEND);
            }
            if (cornerRadius > 0) {
                Gdx.gl.glEnable(GL10.GL_LINE_SMOOTH);
            }

            //adjust width/height so rectangle covers equivalent filled area
            w = Math.round(w - 1);
            h = Math.round(h - 1);

            startShape(ShapeType.Line);
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

            endShape();

            if (cornerRadius > 0) {
                Gdx.gl.glDisable(GL10.GL_LINE_SMOOTH);
            }
            if (color.a < 1 || cornerRadius > 0) {
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
            if (alphaComposite < 1) {
                color = FSkinColor.alphaColor(color, color.a * alphaComposite);
            }
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glEnable(GL10.GL_LINE_SMOOTH); //must be smooth to ensure edges aren't missed

            startShape(ShapeType.Line);
            shapeRenderer.setColor(color);
            shapeRenderer.rect(adjustX(x), adjustY(y, h), w, h);
            endShape();

            Gdx.gl.glDisable(GL10.GL_LINE_SMOOTH);
            Gdx.gl.glDisable(GL20.GL_BLEND);
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

            if (alphaComposite < 1) {
                color = FSkinColor.alphaColor(color, color.a * alphaComposite);
            }
            if (color.a < 1) { //enable blending so alpha colored shapes work properly
                Gdx.gl.glEnable(GL20.GL_BLEND);
            }

            startShape(ShapeType.Filled);
            shapeRenderer.setColor(color);
            shapeRenderer.rect(adjustX(x), adjustY(y, h), w, h);
            endShape();

            if (color.a < 1) {
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
            if (alphaComposite < 1) {
                color = FSkinColor.alphaColor(color, color.a * alphaComposite);
            }
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glEnable(GL10.GL_LINE_SMOOTH);

            startShape(ShapeType.Line);
            shapeRenderer.setColor(color);
            shapeRenderer.circle(adjustX(x), adjustY(y, 0), radius);
            endShape();

            Gdx.gl.glDisable(GL10.GL_LINE_SMOOTH);
            Gdx.gl.glDisable(GL20.GL_BLEND);
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

            if (alphaComposite < 1) {
                color = FSkinColor.alphaColor(color, color.a * alphaComposite);
            }
            if (color.a < 1) { //enable blending so alpha colored shapes work properly
                Gdx.gl.glEnable(GL20.GL_BLEND);
            }

            startShape(ShapeType.Filled);
            shapeRenderer.setColor(color);
            shapeRenderer.circle(adjustX(x), adjustY(y, 0), radius); //TODO: Make smoother
            endShape();

            if (color.a < 1) {
                Gdx.gl.glDisable(GL20.GL_BLEND);
            }

            batch.begin();
        }

        public void fillTriangle(FSkinColor skinColor, float x1, float y1, float x2, float y2, float x3, float y3) {
            fillTriangle(skinColor.getColor(), x1, y1, x2, y2, x3, y3);
        }
        public void fillTriangle(Color color, float x1, float y1, float x2, float y2, float x3, float y3) {
            batch.end(); //must pause batch while rendering shapes

            if (alphaComposite < 1) {
                color = FSkinColor.alphaColor(color, color.a * alphaComposite);
            }
            if (color.a < 1) { //enable blending so alpha colored shapes work properly
                Gdx.gl.glEnable(GL20.GL_BLEND);
            }

            startShape(ShapeType.Filled);
            shapeRenderer.setColor(color);
            shapeRenderer.triangle(adjustX(x1), adjustY(y1, 0), adjustX(x2), adjustY(y2, 0), adjustX(x3), adjustY(y3, 0));
            endShape();

            if (color.a < 1) {
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

            if (alphaComposite < 1) {
                color1 = FSkinColor.alphaColor(color1, color1.a * alphaComposite);
                color2 = FSkinColor.alphaColor(color2, color2.a * alphaComposite);
            }
            boolean needBlending = (color1.a < 1 || color2.a < 1);
            if (needBlending) { //enable blending so alpha colored shapes work properly
                Gdx.gl.glEnable(GL20.GL_BLEND);
            }

            Color topLeftColor = color1;
            Color topRightColor = vertical ? color1 : color2;
            Color bottomLeftColor = vertical ? color2 : color1;
            Color bottomRightColor = color2;

            startShape(ShapeType.Filled);
            shapeRenderer.rect(adjustX(x), adjustY(y, h), w, h, bottomLeftColor, bottomRightColor, topRightColor, topLeftColor);
            endShape();

            if (needBlending) {
                Gdx.gl.glDisable(GL20.GL_BLEND);
            }

            batch.begin();
        }

        private void startShape(ShapeType shapeType) {
            if (isTransformed) {
                //must copy matrix before starting shape if transformed
                shapeRenderer.setTransformMatrix(batch.getTransformMatrix());
            }
            shapeRenderer.begin(shapeType);
        }

        private void endShape() {
            shapeRenderer.end();
        }

        public void setAlphaComposite(float alphaComposite0) {
            alphaComposite = alphaComposite0;
            batch.setColor(new Color(1, 1, 1, alphaComposite));
        }
        public void resetAlphaComposite() {
            alphaComposite = 1;
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

        public void drawRepeatingImage(Texture image, float x, float y, float w, float h) {
            startClip(x, y, w, h);

            int tilesW = (int)(w / image.getWidth()) + 1;
            int tilesH = (int)(h / image.getHeight()) + 1;  
            batch.draw(image, adjustX(x), adjustY(y, h),
                    image.getWidth() * tilesW, 
                    image.getHeight() * tilesH, 
                    0, tilesH, tilesW, 0);

            endClip();
        }

        private boolean isTransformed;

        public void setRotateTransform(float originX, float originY, float rotation) {
            batch.end();
            float dx = adjustX(originX);
            float dy = adjustY(originY, 0);
            batch.getTransformMatrix().translate(dx, dy, 0);
            batch.getTransformMatrix().rotate(Vector3.Z, rotation);
            batch.getTransformMatrix().translate(-dx, -dy, 0);
            batch.begin();
            isTransformed = true;
        }

        public void clearTransform() {
            batch.end();
            batch.getTransformMatrix().idt();
            shapeRenderer.setTransformMatrix(batch.getTransformMatrix());
            batch.begin();
            isTransformed = false;
        }

        public void drawRotatedImage(Texture image, float x, float y, float w, float h, float originX, float originY, float rotation) {
            batch.draw(image, adjustX(x), adjustY(y, h), originX - x, h - (originY - y), w, h, 1, 1, rotation, 0, 0, image.getWidth(), image.getHeight(), false, false);
        }

        public void drawText(String text, FSkinFont font, FSkinColor skinColor, float x, float y, float w, float h, boolean wrap, HAlignment horzAlignment, boolean centerVertically) {
            drawText(text, font, skinColor.getColor(), x, y, w, h, wrap, horzAlignment, centerVertically);
        }
        public void drawText(String text, FSkinFont font, Color color, float x, float y, float w, float h, boolean wrap, HAlignment horzAlignment, boolean centerVertically) {
            if (alphaComposite < 1) {
                color = FSkinColor.alphaColor(color, color.a * alphaComposite);
            }
            if (color.a < 1) { //enable blending so alpha colored shapes work properly
                Gdx.gl.glEnable(GL20.GL_BLEND);
            }

            TextBounds textBounds;
            if (wrap) {
                textBounds = font.getWrappedBounds(text, w);
            }
            else {
                textBounds = font.getMultiLineBounds(text);
            }
            
            boolean needClip = false;

            while (textBounds.width > w || textBounds.height > h) {
                if (font.canShrink()) { //shrink font to fit if possible
                    font = font.shrink();
                    if (wrap) {
                        textBounds = font.getWrappedBounds(text, w);
                    }
                    else {
                        textBounds = font.getMultiLineBounds(text);
                    }
                }
                else {
                    needClip = true;
                    break;
                }
            }

            if (needClip) { //prevent text flowing outside region if couldn't shrink it to fit
                startClip(x, y, w, h);
            }

            float textHeight = textBounds.height;
            if (h > textHeight && centerVertically) {
                y += (h - textHeight) / 2;
            }

            font.draw(batch, text, color, adjustX(x), adjustY(y, 0), w, wrap, horzAlignment);

            if (needClip) {
                endClip();
            }

            if (color.a < 1) {
                Gdx.gl.glDisable(GL20.GL_BLEND);
            }
        }

        //use nifty trick with multiple text renders to draw outlined text
        public void drawOutlinedText(String text, FSkinFont skinFont, Color textColor, Color outlineColor, float x, float y, float w, float h, boolean wrap, HAlignment horzAlignment, boolean centerVertically) {
            drawText(text, skinFont, outlineColor, x - 1, y, w, h, wrap, horzAlignment, centerVertically);
            drawText(text, skinFont, outlineColor, x, y - 1, w, h, wrap, horzAlignment, centerVertically);
            drawText(text, skinFont, outlineColor, x - 1, y - 1, w, h, wrap, horzAlignment, centerVertically);
            drawText(text, skinFont, outlineColor, x + 1, y, w, h, wrap, horzAlignment, centerVertically);
            drawText(text, skinFont, outlineColor, x, y + 1, w, h, wrap, horzAlignment, centerVertically);
            drawText(text, skinFont, outlineColor, x + 1, y + 1, w, h, wrap, horzAlignment, centerVertically);
            drawText(text, skinFont, textColor, x, y, w, h, wrap, horzAlignment, centerVertically);
        }

        private float adjustX(float x) {
            return x + bounds.x;
        }

        private float adjustY(float y, float height) {
            return screenHeight - y - bounds.y - height; //flip y-axis
        }
    }
}
