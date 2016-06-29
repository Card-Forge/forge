package forge;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.utils.Clipboard;

import forge.animation.ForgeAnimation;
import forge.assets.AssetsDownloader;
import forge.assets.FSkin;
import forge.assets.FSkinFont;
import forge.assets.ImageCache;
import forge.error.BugReporter;
import forge.error.ExceptionHandler;
import forge.interfaces.IDeviceAdapter;
import forge.model.FModel;
import forge.properties.ForgeConstants;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;
import forge.screens.FScreen;
import forge.screens.SplashScreen;
import forge.screens.home.HomeScreen;
import forge.screens.home.NewGameMenu;
import forge.screens.match.MatchController;
import forge.sound.MusicPlaylist;
import forge.sound.SoundSystem;
import forge.toolbox.FContainer;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FGestureAdapter;
import forge.toolbox.FOptionPane;
import forge.toolbox.FOverlay;
import forge.util.Callback;
import forge.util.FileUtil;
import forge.util.Utils;

public class Forge implements ApplicationListener {
    public static final String CURRENT_VERSION = "1.5.54.001";

    private static final ApplicationListener app = new Forge();
    private static Clipboard clipboard;
    private static IDeviceAdapter deviceAdapter;
    private static int screenWidth;
    private static int screenHeight;
    private static Graphics graphics;
    private static FScreen currentScreen;
    private static SplashScreen splashScreen;
    private static KeyInputAdapter keyInputAdapter;
    private static boolean exited;
    private static int continuousRenderingCount = 1; //initialize to 1 since continuous rendering is the default
    private static final Stack<FScreen> screens = new Stack<FScreen>();

    public static ApplicationListener getApp(Clipboard clipboard0, IDeviceAdapter deviceAdapter0, String assetDir0) {
        if (GuiBase.getInterface() == null) {
            clipboard = clipboard0;
            deviceAdapter = deviceAdapter0;
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

        graphics = new Graphics();
        splashScreen = new SplashScreen();
        Gdx.input.setInputProcessor(new MainInputProcessor());

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
                if (exited) { return; } //don't continue if user chose to exit or couldn't download required assets

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
        stopContinuousRendering(); //save power consumption by disabling continuous rendering once assets loaded

        FSkin.loadFull(splashScreen);

        SoundSystem.instance.setBackgroundMusic(MusicPlaylist.MENUS); //start background music

        Gdx.input.setCatchBackKey(true);
        Gdx.input.setCatchMenuKey(true);
        openScreen(HomeScreen.instance);
        splashScreen = null;

        boolean isLandscapeMode = isLandscapeMode();
        if (isLandscapeMode) { //open preferred new game screen by default if landscape mode
            NewGameMenu.getPreferredScreen().open();
        }

        //update landscape mode preference if it doesn't match what the app loaded as
        if (FModel.getPreferences().getPrefBoolean(FPref.UI_LANDSCAPE_MODE) != isLandscapeMode) {
            FModel.getPreferences().setPref(FPref.UI_LANDSCAPE_MODE, isLandscapeMode);
            FModel.getPreferences().save();
        }
    }

    public static Clipboard getClipboard() {
        return clipboard;
    }

    public static IDeviceAdapter getDeviceAdapter() {
        return deviceAdapter;
    }

    public static void startContinuousRendering() {
        if (++continuousRenderingCount == 1) {
            //only set continuous rendering to true if needed
            Gdx.graphics.setContinuousRendering(true);
        }
    }
    public static void stopContinuousRendering() {
        if (continuousRenderingCount > 0 && --continuousRenderingCount == 0) {
            //only set continuous rendering to false if all continuous rendering requests have been ended
            Gdx.graphics.setContinuousRendering(false);
        }
    }

    public static void showMenu() {
        if (currentScreen == null) { return; }
        endKeyInput(); //end key input before menu shown
        if (FOverlay.getTopOverlay() == null) { //don't show menu if overlay open
            currentScreen.showMenu();
        }
    }

    public static boolean onHomeScreen() {
        return screens.size() == 1;
    }

    public static void back() {
        if (screens.size() < 2) {
            exit(false); //prompt to exit if attempting to go back from home screen
            return;
        }
        currentScreen.onClose(new Callback<Boolean>() {
            @Override
            public void run(Boolean result) {
                if (result) {
                    screens.pop();
                    setCurrentScreen(screens.lastElement());
                }
            }
        });
    }

    //set screen that will be gone to on pressing Back before going to current Back screen
    public static void setBackScreen(final FScreen screen0, boolean replace) {
        screens.remove(screen0); //remove screen from previous position in navigation history
        int index = screens.size() - 1;
        if (index > 0) {
            screens.add(index, screen0);
            if (replace) { //remove previous back screen if replacing back screen
                screens.remove(index - 1);
            }
        }
    }

    public static void restart(boolean silent) {
        if (exited) { return; } //don't allow exiting multiple times

        Callback<Boolean> callback = new Callback<Boolean>() {
            @Override
            public void run(Boolean result) {
                if (result) {
                    exited = true;
                    deviceAdapter.restart();
                }
            }
        };
        if (silent) {
            callback.run(true);
        }
        else {
            FOptionPane.showConfirmDialog("Are you sure you wish to restart Forge?", "Restart Forge", "Restart", "Cancel", callback);
        }
    }

    public static void exit(boolean silent) {
        if (exited) { return; } //don't allow exiting multiple times

        Callback<Boolean> callback = new Callback<Boolean>() {
            @Override
            public void run(Boolean result) {
                if (result) {
                    exited = true;
                    deviceAdapter.exit();
                }
            }
        };
        if (silent) {
            callback.run(true);
        }
        else {
            FOptionPane.showConfirmDialog("Are you sure you wish to exit Forge?", "Exit Forge", "Exit", "Cancel", callback);
        }
    }

    public static void openScreen(final FScreen screen0) {
        openScreen(screen0, false);
    }
    public static void openScreen(final FScreen screen0, final boolean replaceBackScreen) {
        if (currentScreen == screen0) { return; }

        if (currentScreen == null) {
            screens.push(screen0);
            setCurrentScreen(screen0);
            return;
        }

        currentScreen.onSwitchAway(new Callback<Boolean>() {
            @Override
            public void run(Boolean result) {
                if (result) {
                    if (replaceBackScreen && !screens.isEmpty()) {
                        screens.pop();
                    }
                    if (screens.peek() != screen0) { //prevent screen being its own back screen
                        screens.push(screen0);
                    }
                    setCurrentScreen(screen0);
                }
            }
        });
    }

    public static boolean isLandscapeMode() {
        return screenWidth > screenHeight;
    }

    public static int getScreenWidth() {
        return screenWidth;
    }

    public static int getScreenHeight() {
        return screenHeight;
    }

    public static FScreen getCurrentScreen() {
        return currentScreen;
    }

    private static void setCurrentScreen(FScreen screen0) {
        try {
            endKeyInput(); //end key input before switching screens
            ForgeAnimation.endAll(); //end all active animations before switching screens

            currentScreen = screen0;
            currentScreen.setSize(screenWidth, screenHeight);
            currentScreen.onActivate();
        }
        catch (Exception ex) {
            graphics.end();
            BugReporter.reportException(ex);
        }
    }

    @Override
    public void render() {
        try {
            ImageCache.allowSingleLoad();
            ForgeAnimation.advanceAll();

            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT); // Clear the screen.

            FContainer screen = currentScreen;
            if (screen == null) {
                screen = splashScreen;
                if (screen == null) { 
                    return;
                }
            }

            graphics.begin(screenWidth, screenHeight);
            screen.screenPos.setSize(screenWidth, screenHeight);
            if (screen.getRotate180()) {
                graphics.startRotateTransform(screenWidth / 2, screenHeight / 2, 180);
            }
            screen.draw(graphics);
            if (screen.getRotate180()) {
                graphics.endTransform();
            }
            for (FOverlay overlay : FOverlay.getOverlays()) {
                if (overlay.isVisibleOnScreen(currentScreen)) {
                    overlay.screenPos.setSize(screenWidth, screenHeight);
                    overlay.setSize(screenWidth, screenHeight); //update overlay sizes as they're rendered
                    if (overlay.getRotate180()) {
                        graphics.startRotateTransform(screenWidth / 2, screenHeight / 2, 180);
                    }
                    overlay.draw(graphics);
                    if (overlay.getRotate180()) {
                        graphics.endTransform();
                    }
                }
            }
            graphics.end();
        }
        catch (Exception ex) {
            graphics.end();
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
            graphics.end();
            BugReporter.reportException(ex);
        }
    }

    @Override
    public void pause() {
        if (MatchController.getHostedMatch() != null) {
            MatchController.getHostedMatch().pause();
        }
    }

    @Override
    public void resume() {
        if (MatchController.getHostedMatch() != null) {
            MatchController.getHostedMatch().resume();
        }
    }

    @Override
    public void dispose() {
        if (currentScreen != null) {
            FOverlay.hideAll();
            currentScreen.onClose(null);
            currentScreen = null;
        }
        screens.clear();
        graphics.dispose();
        SoundSystem.instance.dispose();
        try {
            ExceptionHandler.unregisterErrorHandling();
        }
        catch (Exception e) {}
    }

    //log message to Forge.log file
    public static void log(Object message) {
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
        private static final List<FDisplayObject> potentialListeners = new ArrayList<FDisplayObject>();
        private static char lastKeyTyped;
        private static boolean keyTyped, shiftKeyDown;

        @Override
        public boolean keyDown(int keyCode) {
            if (keyCode == Keys.MENU) {
                showMenu();
                return true;
            }
            if (keyCode == Keys.SHIFT_LEFT || keyCode == Keys.SHIFT_RIGHT) {
                shiftKeyDown = true;
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
            if (keyCode == Keys.SHIFT_LEFT || keyCode == Keys.SHIFT_RIGHT) {
                shiftKeyDown = false;
            }
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

            //base potential listeners on object containing touch down point
            for (FOverlay overlay : FOverlay.getOverlaysTopDown()) {
                if (overlay.isVisibleOnScreen(currentScreen)) {
                    overlay.buildTouchListeners(x, y, potentialListeners);
                    if (overlay.preventInputBehindOverlay()) {
                        return;
                    }
                }
            }
            if (currentScreen != null) {
                currentScreen.buildTouchListeners(x, y, potentialListeners);
            }
        }

        @Override
        public boolean touchDown(int x, int y, int pointer, int button) {
            if (pointer == 0) { //don't change listeners when second finger goes down for zoom
                updatePotentialListeners(x, y);
                if (keyInputAdapter != null) {
                    if (!keyInputAdapter.allowTouchInput() || !potentialListeners.contains(keyInputAdapter.getOwner())) {
                        endKeyInput(); //end key input if needed
                    }
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
            if (shiftKeyDown && flick(x, y)) {
                return true; //give flick logic a chance to handle Shift+click
            }
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
        public boolean flick(float x, float y) {
            try {
                for (FDisplayObject listener : potentialListeners) {
                    if (listener.flick(listener.screenToLocalX(x), listener.screenToLocalY(y))) {
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
}
