package forge;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import com.badlogic.gdx.Application;
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
import forge.error.ExceptionHandler;
import forge.gui.FThreads;
import forge.gui.GuiBase;
import forge.gui.error.BugReporter;
import forge.interfaces.IDeviceAdapter;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
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
import forge.util.CardTranslation;
import forge.util.FileUtil;
import forge.util.Localizer;
import forge.util.Utils;

public class Forge implements ApplicationListener {
    public static final String CURRENT_VERSION = "1.6.44.001";

    private static final ApplicationListener app = new Forge();
    private static Clipboard clipboard;
    private static IDeviceAdapter deviceAdapter;
    private static int screenWidth;
    private static int screenHeight;
    private static Graphics graphics;
    private static FrameRate frameRate;
    private static FScreen currentScreen;
    private static SplashScreen splashScreen;
    private static KeyInputAdapter keyInputAdapter;
    private static boolean exited;
    private static int continuousRenderingCount = 1; //initialize to 1 since continuous rendering is the default
    private static final Deque<FScreen> Dscreens = new ArrayDeque<>();
    private static boolean textureFiltering = false;
    private static boolean destroyThis = false;
    public static String extrawide = "default";
    public static float heigtModifier = 0.0f;
    private static boolean isloadingaMatch = false;
    public static boolean showFPS = false;
    public static boolean altPlayerLayout = false;
    public static boolean altZoneTabs = false;
    public static String enableUIMask = "Crop";
    public static boolean enablePreloadExtendedArt = false;
    public static boolean isTabletDevice = false;
    public static String locale = "en-US";
    public static boolean hdbuttons = false;
    public static boolean hdstart = false;
    public static boolean isPortraitMode = false;
    public static boolean gameInProgress = false;
    public static boolean disposeTextures = false;
    public static int cacheSize = 400;
    public static int totalDeviceRAM = 0;
    public static int androidVersion = 0;
    public static boolean autoCache = false;
    public static int lastButtonIndex = 0;
    public static String CJK_Font = "";

    public static ApplicationListener getApp(Clipboard clipboard0, IDeviceAdapter deviceAdapter0, String assetDir0, boolean value, boolean androidOrientation, int totalRAM, boolean isTablet, int AndroidAPI, String AndroidRelease, String deviceName) {
        if (GuiBase.getInterface() == null) {
            clipboard = clipboard0;
            deviceAdapter = deviceAdapter0;
            GuiBase.setUsingAppDirectory(assetDir0.contains("forge.app")); //obb directory on android uses the package name as entrypoint
            GuiBase.setInterface(new GuiMobile(assetDir0));
            GuiBase.enablePropertyConfig(value);
            isPortraitMode = androidOrientation;
            totalDeviceRAM = totalRAM;
            isTabletDevice = isTablet;
            androidVersion = AndroidAPI;
        }
        GuiBase.setDeviceInfo(deviceName, AndroidRelease, AndroidAPI, totalRAM);
        return app;
    }

    private Forge() {
    }

    @Override
    public void create() {
        //install our error handler
        ExceptionHandler.registerErrorHandling();

        GuiBase.setIsAndroid(Gdx.app.getType() == Application.ApplicationType.Android);

        graphics = new Graphics();
        splashScreen = new SplashScreen();
        frameRate = new FrameRate();
        Gdx.input.setInputProcessor(new MainInputProcessor());
        /*
         Set CatchBackKey here and exit the app when you hit the
         back button while the textures,fonts,etc are still loading,
         to prevent rendering issue when you try to restart
         the app again (seems it doesnt dispose correctly...?!?)
         */
        Gdx.input.setCatchKey(Keys.BACK, true);
        destroyThis = true; //Prevent back()
        ForgePreferences prefs = new ForgePreferences();

        String skinName;
        if (FileUtil.doesFileExist(ForgeConstants.MAIN_PREFS_FILE)) {
            skinName = prefs.getPref(FPref.UI_SKIN);
        }
        else {
            skinName = "default"; //use default skin if preferences file doesn't exist yet
        }
        FSkin.loadLight(skinName, splashScreen);

        textureFiltering = prefs.getPrefBoolean(FPref.UI_LIBGDX_TEXTURE_FILTERING);
        showFPS = prefs.getPrefBoolean(FPref.UI_SHOW_FPS);
        altPlayerLayout = prefs.getPrefBoolean(FPref.UI_ALT_PLAYERINFOLAYOUT);
        altZoneTabs = prefs.getPrefBoolean(FPref.UI_ALT_PLAYERZONETABS);
        enableUIMask = prefs.getPref(FPref.UI_ENABLE_BORDER_MASKING);
        if (prefs.getPref(FPref.UI_ENABLE_BORDER_MASKING).equals("true")) //override old settings if not updated
            enableUIMask = "Full";
        else if (prefs.getPref(FPref.UI_ENABLE_BORDER_MASKING).equals("false"))
            enableUIMask = "Off";
        enablePreloadExtendedArt = prefs.getPrefBoolean(FPref.UI_ENABLE_PRELOAD_EXTENDED_ART);
        locale = prefs.getPref(FPref.UI_LANGUAGE);
        autoCache = prefs.getPrefBoolean(FPref.UI_AUTO_CACHE_SIZE);
        disposeTextures = prefs.getPrefBoolean(FPref.UI_ENABLE_DISPOSE_TEXTURES);
        CJK_Font = prefs.getPref(FPref.UI_CJK_FONT);

        if (autoCache) {
            //increase cacheSize for devices with RAM more than 5GB, default is 400. Some phones have more than 10GB RAM (Mi 10, OnePlus 8, S20, etc..)
            if (totalDeviceRAM>5000) //devices with more than 10GB RAM will have 800 Cache size, 600 Cache size for morethan 5GB RAM
                cacheSize = totalDeviceRAM>10000 ? 800: 600;
        }
        //init cache
        ImageCache.initCache(cacheSize);
        final Localizer localizer = Localizer.getInstance();

        //load model on background thread (using progress bar to report progress)
        FThreads.invokeInBackgroundThread(new Runnable() {
            @Override
            public void run() {
                //see if app or assets need updating
                AssetsDownloader.checkForUpdates(splashScreen);
                if (exited) { return; } //don't continue if user chose to exit or couldn't download required assets

                FModel.initialize(splashScreen.getProgressBar(), null);

                splashScreen.getProgressBar().setDescription(localizer.getMessage("lblLoadingFonts"));
                FSkinFont.preloadAll(locale);

                splashScreen.getProgressBar().setDescription(localizer.getMessage("lblLoadingCardTranslations"));
                CardTranslation.preloadTranslation(locale, ForgeConstants.LANG_DIR);

                splashScreen.getProgressBar().setDescription(localizer.getMessage("lblFinishingStartup"));

                //add reminder to preload
                if (enablePreloadExtendedArt) {
                    if(autoCache)
                        splashScreen.getProgressBar().setDescription(localizer.getMessage("lblPreloadExtendedArt")+"\nDetected RAM: " +totalDeviceRAM+"MB. Cache size: "+cacheSize);
                    else
                        splashScreen.getProgressBar().setDescription(localizer.getMessage("lblPreloadExtendedArt"));
                } else {
                    if(autoCache)
                        splashScreen.getProgressBar().setDescription(localizer.getMessage("lblFinishingStartup")+"\nDetected RAM: " +totalDeviceRAM+"MB. Cache size: "+cacheSize);
                    else
                        splashScreen.getProgressBar().setDescription(localizer.getMessage("lblFinishingStartup"));
                }

                Gdx.app.postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        afterDbLoaded();
                        /*  call preloadExtendedArt here, if we put it above we will  *
                         *  get error: No OpenGL context found in the current thread. */
                        preloadExtendedArt();
                    }
                });
            }
        });
    }

    private void preloadExtendedArt() {
        if (!enablePreloadExtendedArt)
            return;
        List<String> borderlessCardlistkeys = FileUtil.readFile(ForgeConstants.BORDERLESS_CARD_LIST_FILE);
        if(borderlessCardlistkeys.isEmpty())
            return;
        List<String> filteredkeys = new ArrayList<>();
        for (String cardname : borderlessCardlistkeys){
            File image = new File(ForgeConstants.CACHE_CARD_PICS_DIR+ForgeConstants.PATH_SEPARATOR+cardname+".jpg");
            if (image.exists())
                filteredkeys.add(cardname);
        }
        if (!filteredkeys.isEmpty())
            ImageCache.preloadCache(filteredkeys);
    }

    public static void openHomeScreen(int index) {
        openScreen(HomeScreen.instance);
        HomeScreen.instance.openMenu(index);
    }

    private void afterDbLoaded() {
        stopContinuousRendering(); //save power consumption by disabling continuous rendering once assets loaded

        FSkin.loadFull(splashScreen);

        SoundSystem.instance.setBackgroundMusic(MusicPlaylist.MENUS); //start background music
        destroyThis = false; //Allow back()
        Gdx.input.setCatchKey(Keys.MENU, true);
        openHomeScreen(-1); //default for startup
        splashScreen = null;

        boolean isLandscapeMode = isLandscapeMode();
        if (isLandscapeMode) { //open preferred new game screen by default if landscape mode
            NewGameMenu.getPreferredScreen().open();
        }

        //adjust height modifier
        adjustHeightModifier(getScreenWidth(), getScreenHeight());

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

    public static void setHeightModifier(float height) {
        heigtModifier = height;
    }

    public static float getHeightModifier() {
        return heigtModifier;
    }

    public static void adjustHeightModifier(float DisplayW, float DisplayH) {
        if(isLandscapeMode())
        {//TODO: Fullscreen support for Display without screen controls
            float aspectratio = DisplayW / DisplayH;
            if(aspectratio > 1.82f) {/* extra wide */
                setHeightModifier(200.0f);
                extrawide = "extrawide";
            }
            else if(aspectratio > 1.7f) {/* wide */
                setHeightModifier(100.0f);
                extrawide = "wide";
            }
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
        return Dscreens.size() == 1;
    }

    public static void back() {
        if(destroyThis && isLandscapeMode())
            return;
        if (Dscreens.size() < 2 || (currentScreen == HomeScreen.instance && Forge.isPortraitMode)) {
            exit(false); //prompt to exit if attempting to go back from home screen
            return;
        }
        currentScreen.onClose(new Callback<Boolean>() {
            @Override
            public void run(Boolean result) {
                if (result) {
                    Dscreens.pollFirst();
                    setCurrentScreen(Dscreens.peekFirst());
                }
            }
        });
    }

    //set screen that will be gone to on pressing Back before going to current Back screen
    public static void setBackScreen(final FScreen screen0, boolean replace) {
        Dscreens.remove(screen0); //remove screen from previous position in navigation history
        int index = Dscreens.size() - 1;
        if (index > 0) {
            Dscreens.addLast(screen0);
            if (replace) { //remove previous back screen if replacing back screen
                Dscreens.removeFirst();
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

        final Localizer localizer = Localizer.getInstance();

        if (silent) {
            callback.run(true);
        }
        else {
            FOptionPane.showConfirmDialog(
                    localizer.getMessage("lblAreYouSureYouWishRestartForge"), localizer.getMessage("lblRestartForge"),
                    localizer.getMessage("lblRestart"), localizer.getMessage("lblCancel"), callback);
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
        
        final Localizer localizer = Localizer.getInstance();

        if (silent) {
            callback.run(true);
        }
        else {
            FOptionPane.showConfirmDialog(
                localizer.getMessage("lblAreYouSureYouWishExitForge"), localizer.getMessage("lblExitForge"),
                localizer.getMessage("lblExit"), localizer.getMessage("lblCancel"), callback);
        }
    }

    public static void openScreen(final FScreen screen0) {
        openScreen(screen0, false);
    }
    public static void openScreen(final FScreen screen0, final boolean replaceBackScreen) {
        if (currentScreen == screen0) { return; }

        if (currentScreen == null) {
            Dscreens.addFirst(screen0);
            setCurrentScreen(screen0);
            return;
        }

        currentScreen.onSwitchAway(new Callback<Boolean>() {
            @Override
            public void run(Boolean result) {
                if (result) {
                    if (replaceBackScreen && !Dscreens.isEmpty()) {
                        Dscreens.removeFirst();
                    }
                    if (Dscreens.peekFirst() != screen0) { //prevent screen being its own back screen
                        Dscreens.addFirst(screen0);
                    }
                    setCurrentScreen(screen0);
                }
            }
        });
    }

    public static boolean isTextureFilteringEnabled() {
        return textureFiltering; 
    }

    public static boolean isLandscapeMode() {
        if(GuiBase.isAndroid())
            return !isPortraitMode;
        return screenWidth > screenHeight;
    }

    public static boolean isLoadingaMatch() {
        return isloadingaMatch;
    }

    public static void setLoadingaMatch(boolean value) {
        isloadingaMatch = value;
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
        String toNewScreen = screen0 != null ? screen0.toString() : "";
        String previousScreen = currentScreen != null ? currentScreen.toString() : "";

        gameInProgress = toNewScreen.toLowerCase().contains("match") || previousScreen.toLowerCase().contains("match");
        boolean dispose = toNewScreen.toLowerCase().contains("homescreen") && disposeTextures;
        try {
            endKeyInput(); //end key input before switching screens
            ForgeAnimation.endAll(); //end all active animations before switching screens

            currentScreen = screen0;
            currentScreen.setSize(screenWidth, screenHeight);
            currentScreen.onActivate();
            /*keep Dscreens growing
            if (Dscreens.size() > 3) {
                for(int x = Dscreens.size(); x > 3; x--) {
                    Dscreens.removeLast();
                }
            }*/
            /* for checking only
            if (!Dscreens.isEmpty()) {
                int x = 0;
                for(FScreen fScreen : Dscreens) {
                    System.out.println("Screen ["+x+"]: "+fScreen.toString());
                    x++;
                }
                System.out.println("---------------");
            }*/
        }
        catch (Exception ex) {
            graphics.end();
            //check if sentry is enabled, if not it will call the gui interface but here we end the graphics so we only send it via sentry..
            if (BugReporter.isSentryEnabled())
                BugReporter.reportException(ex);
        } finally {
            if(dispose)
                ImageCache.disposeTexture();
        }
    }

    @Override
    public void render() {
        if (showFPS)
            frameRate.update();

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
            //check if sentry is enabled, if not it will call the gui interface but here we end the graphics so we only send it via sentry..
            if (BugReporter.isSentryEnabled())
                BugReporter.reportException(ex);
        }
        if (showFPS)
            frameRate.render();
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
            //check if sentry is enabled, if not it will call the gui interface but here we end the graphics so we only send it via sentry..
            if (BugReporter.isSentryEnabled())
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
        Dscreens.clear();
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
        private static final List<FDisplayObject> potentialListeners = new ArrayList<>();
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

            // Cursor keys emulate swipe gestures
            // First we touch the screen and later swipe (fling) in the direction of the key pressed
            if (keyCode == Keys.LEFT) {
                touchDown(0,0,0,0);
                return fling(1000,0);
            }
            if (keyCode == Keys.RIGHT) {
                touchDown(0,0,0,0);
                return fling(-1000,0);
            }
            if (keyCode == Keys.UP) {
                touchDown(0,0,0,0);
                return fling(0,-1000);
            }
            if (keyCode == Keys.DOWN) {
                touchDown(0,0,0,0);
                return fling(0,1000);
            }
            if(keyCode == Keys.BACK){
                if (destroyThis)
                    deviceAdapter.exit();
                else if(onHomeScreen() && isLandscapeMode())
                    back();
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
        public boolean scrolled(float amountX, float amountY) {
            updatePotentialListeners(mouseMovedX, mouseMovedY);

            if (KeyInputAdapter.isCtrlKeyDown()) { //zoom in or out based on amount
                return zoom(mouseMovedX, mouseMovedY, -Utils.AVG_FINGER_WIDTH * amountY);
            }

            boolean handled;
            if (KeyInputAdapter.isShiftKeyDown()) {
                handled = pan(mouseMovedX, mouseMovedY, -Utils.AVG_FINGER_WIDTH * amountX, 0, false);
            }
            else {
                handled = pan(mouseMovedX, mouseMovedY, 0, -Utils.AVG_FINGER_HEIGHT * amountY, true);
            }
            if (panStop(mouseMovedX, mouseMovedY)) {
                handled = true;
            }
            return handled;
        }
    }
}
