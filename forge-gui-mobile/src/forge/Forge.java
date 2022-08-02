package forge;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Clipboard;
import com.badlogic.gdx.utils.ScreenUtils;
import forge.adventure.scene.ForgeScene;
import forge.adventure.scene.GameScene;
import forge.adventure.scene.Scene;
import forge.adventure.scene.SceneType;
import forge.adventure.stage.MapStage;
import forge.adventure.util.Config;
import forge.animation.ForgeAnimation;
import forge.assets.Assets;
import forge.assets.AssetsDownloader;
import forge.assets.FSkin;
import forge.assets.FSkinFont;
import forge.assets.ImageCache;
import forge.error.ExceptionHandler;
import forge.gamemodes.limited.BoosterDraft;
import forge.gui.FThreads;
import forge.gui.GuiBase;
import forge.gui.error.BugReporter;
import forge.interfaces.IDeviceAdapter;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.screens.ClosingScreen;
import forge.screens.FScreen;
import forge.screens.SplashScreen;
import forge.screens.TransitionScreen;
import forge.screens.home.HomeScreen;
import forge.screens.home.NewGameMenu;
import forge.screens.match.MatchController;
import forge.screens.match.MatchScreen;
import forge.sound.MusicPlaylist;
import forge.sound.SoundSystem;
import forge.toolbox.*;
import forge.util.*;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class Forge implements ApplicationListener {
    public static final String CURRENT_VERSION = "1.6.53.001";

    private static ApplicationListener app = null;
    static Scene currentScene = null;
    static Array<Scene> lastScene = new Array<>();
    private static float animationTimeout;
    static Batch animationBatch;
    static TextureRegion lastScreenTexture;
    private static boolean sceneWasSwapped = false;
    private static Clipboard clipboard;
    private static IDeviceAdapter deviceAdapter;
    private static int screenWidth;
    private static int screenHeight;
    private static Graphics graphics;
    private static FrameRate frameRate;
    private static FScreen currentScreen;
    protected static SplashScreen splashScreen;
    protected static ClosingScreen closingScreen;
    protected static TransitionScreen transitionScreen;
    public static KeyInputAdapter keyInputAdapter;
    private static boolean exited;
    public boolean needsUpdate = false;
    public static boolean safeToClose = true;
    public static boolean magnify = false;
    public static boolean magnifyToggle = true;
    public static boolean magnifyShowDetails = false;
    public static String cursorName = "";
    private static int continuousRenderingCount = 1; //initialize to 1 since continuous rendering is the default
    private static final Deque<FScreen> Dscreens = new ArrayDeque<>();
    private static boolean textureFiltering = false;
    private static boolean destroyThis = false;
    public static String extrawide = "default";
    public static float heigtModifier = 0.0f;
    private static boolean isloadingaMatch = false;
    public static boolean autoAIDeckSelection = false;
    public static boolean showFPS = false;
    public static boolean allowCardBG = false;
    public static boolean altPlayerLayout = false;
    public static boolean altZoneTabs = false;
    public static boolean animatedCardTapUntap = false;
    public static String enableUIMask = "Crop";
    public static String selector = "Default";
    public static boolean enablePreloadExtendedArt = false;
    public static boolean isTabletDevice = false;
    public static String locale = "en-US";
    public Assets assets;
    public static boolean hdbuttons = false;
    public static boolean hdstart = false;
    public static boolean isPortraitMode = false;
    public static boolean gameInProgress = false;
    public static boolean disposeTextures = false;
    public static boolean isMobileAdventureMode = false;
    public static int cacheSize = 300;
    public static int totalDeviceRAM = 0;
    public static int androidVersion = 0;
    public static boolean autoCache = false;
    public static int lastButtonIndex = 0;
    public static String CJK_Font = "";
    public static int hoveredCount = 0;
    public static boolean afterDBloaded = false;
    public static int mouseButtonID = 0;
    public static InputProcessor inputProcessor;
    private static Cursor cursor0, cursor1, cursor2, cursorA0, cursorA1, cursorA2;
    public static boolean forcedEnglishonCJKMissing = false;
    public static boolean adventureLoaded = false;
    public static boolean createNewAdventureMap = false;
    private static Localizer localizer;

    public static ApplicationListener getApp(Clipboard clipboard0, IDeviceAdapter deviceAdapter0, String assetDir0, boolean value, boolean androidOrientation, int totalRAM, boolean isTablet, int AndroidAPI, String AndroidRelease, String deviceName) {
        app = new Forge();
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

    public static Localizer getLocalizer() {
        if (localizer == null)
            localizer = Localizer.getInstance();
        return localizer;
    }
    @Override
    public void create() {
        //install our error handler
        ExceptionHandler.registerErrorHandling();

        GuiBase.setIsAndroid(Gdx.app.getType() == Application.ApplicationType.Android);

        if (!GuiBase.isAndroid() || (androidVersion > 28 && totalDeviceRAM > 7000)) {
            allowCardBG = true;
        } else {
            // don't allow to read and process
            ForgeConstants.SPRITE_CARDBG_FILE = "";
        }
        assets = new Assets();
        graphics = new Graphics();
        splashScreen = new SplashScreen();
        frameRate = new FrameRate();
        animationBatch = new SpriteBatch();
        inputProcessor = new MainInputProcessor();

        Gdx.input.setInputProcessor(inputProcessor);
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
        } else {
            skinName = "default"; //use default skin if preferences file doesn't exist yet
        }
        FSkin.loadLight(skinName, splashScreen);

        textureFiltering = prefs.getPrefBoolean(FPref.UI_LIBGDX_TEXTURE_FILTERING);
        showFPS = prefs.getPrefBoolean(FPref.UI_SHOW_FPS);
        autoAIDeckSelection = prefs.getPrefBoolean(FPref.UI_AUTO_AIDECK_SELECTION);
        altPlayerLayout = prefs.getPrefBoolean(FPref.UI_ALT_PLAYERINFOLAYOUT);
        altZoneTabs = prefs.getPrefBoolean(FPref.UI_ALT_PLAYERZONETABS);
        animatedCardTapUntap = prefs.getPrefBoolean(FPref.UI_ANIMATED_CARD_TAPUNTAP);
        selector = prefs.getPref(FPref.UI_SELECTOR_MODE);
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
            //increase cacheSize for devices with RAM more than 5GB, default is 300. Some phones have more than 10GB RAM (Mi 10, OnePlus 8, S20, etc..)
            if (totalDeviceRAM > 5000) //devices with more than 10GB RAM will have 600 Cache size, 400 Cache size for morethan 5GB RAM
                cacheSize = totalDeviceRAM > 10000 ? 600 : 400;
        }
        //init cache
        ImageCache.initCache(cacheSize);

        //load model on background thread (using progress bar to report progress)
        FThreads.invokeInBackgroundThread(() -> {
            //see if app or assets need updating
            AssetsDownloader.checkForUpdates(splashScreen);
            if (exited) {
                return;
            } //don't continue if user chose to exit or couldn't download required assets

            safeToClose = false;
            ImageKeys.setIsLibGDXPort(GuiBase.getInterface().isLibgdxPort());
            FModel.initialize(splashScreen.getProgressBar(), null);

            splashScreen.getProgressBar().setDescription(getLocalizer().getMessage("lblLoadingFonts"));
            FSkinFont.preloadAll(locale);

            splashScreen.getProgressBar().setDescription(getLocalizer().getMessage("lblLoadingCardTranslations"));
            CardTranslation.preloadTranslation(locale, ForgeConstants.LANG_DIR);

            splashScreen.getProgressBar().setDescription(getLocalizer().getMessage("lblFinishingStartup"));

            //add reminder to preload
            if (enablePreloadExtendedArt) {
                if (autoCache)
                    splashScreen.getProgressBar().setDescription(getLocalizer().getMessage("lblPreloadExtendedArt") + "\nDetected RAM: " + totalDeviceRAM + "MB. Cache size: " + cacheSize);
                else
                    splashScreen.getProgressBar().setDescription(getLocalizer().getMessage("lblPreloadExtendedArt"));
            } else {
                if (autoCache)
                    splashScreen.getProgressBar().setDescription(getLocalizer().getMessage("lblFinishingStartup") + "\nDetected RAM: " + totalDeviceRAM + "MB. Cache size: " + cacheSize);
                else
                    splashScreen.getProgressBar().setDescription(getLocalizer().getMessage("lblFinishingStartup"));
            }

            Gdx.app.postRunnable(() -> {
                afterDbLoaded();
                /*  call preloadExtendedArt here, if we put it above we will  *
                 *  get error: No OpenGL context found in the current thread. */
                preloadExtendedArt();
            });
        });
    }

    public static InputProcessor getInputProcessor() {
        return inputProcessor;
    }

    public static Graphics getGraphics() {
        return graphics;
    }

    public static Scene getCurrentScene() {
        return currentScene;
    }

    private void preloadExtendedArt() {
        if (!enablePreloadExtendedArt || !enableUIMask.equals("Full"))
            return;
        List<String> borderlessCardlistkeys = FileUtil.readFile(ForgeConstants.BORDERLESS_CARD_LIST_FILE);
        if (borderlessCardlistkeys.isEmpty())
            return;
        List<String> filteredkeys = new ArrayList<>();
        for (String cardname : borderlessCardlistkeys) {
            File image = new File(ForgeConstants.CACHE_CARD_PICS_DIR + ForgeConstants.PATH_SEPARATOR + cardname + ".jpg");
            if (image.exists())
                filteredkeys.add(cardname);
        }
        if (!filteredkeys.isEmpty())
            ImageCache.preloadCache(filteredkeys);
    }

    private void preloadBoosterDrafts() {
        //preloading of custom drafts
        BoosterDraft.initializeCustomDrafts();
    }

    public static void openHomeScreen(int index, FScreen lastMatch) {
        openScreen(HomeScreen.instance);
        HomeScreen.instance.openMenu(index);
        if (lastMatch != null) {
            try {
                Dscreens.remove(lastMatch);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        //check
        /*for (FScreen fScreen : Dscreens)
            System.out.println(fScreen.toString());*/
    }

    public static void openHomeDefault() {
        //default to English only if CJK is missing
        getLocalizer().setEnglish(forcedEnglishonCJKMissing);
        GuiBase.setIsAdventureMode(false);
        openHomeScreen(-1, null); //default for startup
        isMobileAdventureMode = false;
        if (isLandscapeMode()) { //open preferred new game screen by default if landscape mode
            NewGameMenu.getPreferredScreen().open();
        }
        stopContinuousRendering(); //save power consumption by disabling continuous rendering once assets loaded
    }

    public static void openAdventure() {
        //default to english since it doesn't have CJK fonts, it will be updated on Forgescene enter/exit
        getLocalizer().setEnglish(forcedEnglishonCJKMissing);
        //continuous rendering is needed for adventure mode
        startContinuousRendering();
        GuiBase.setIsAdventureMode(true);
        isMobileAdventureMode = true;
        if (GuiBase.isAndroid()) //force it for adventure mode
            altZoneTabs = true;
        //pixl cursor for adventure
        setCursor(null, "0");
        try {
            if(!adventureLoaded)
            {
                for (SceneType sceneType : SceneType.values()) {
                    sceneType.instance.resLoaded();
                }
                adventureLoaded=true;
            }
            switchScene(SceneType.StartScene.instance);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    protected void afterDbLoaded() {
        destroyThis = false; //Allow back()
        Gdx.input.setCatchKey(Keys.MENU, true);

        afterDBloaded = true;
        //adjust height modifier
        adjustHeightModifier(getScreenWidth(), getScreenHeight());

        //update landscape mode preference if it doesn't match what the app loaded as
        if (FModel.getPreferences().getPrefBoolean(FPref.UI_LANDSCAPE_MODE) != isLandscapeMode()) {
            FModel.getPreferences().setPref(FPref.UI_LANDSCAPE_MODE, isLandscapeMode());
            FModel.getPreferences().save();
        }

        FThreads.invokeInBackgroundThread(() -> FThreads.invokeInEdtLater(() -> {
            //load skin full
            FSkin.loadFull(splashScreen);
            FThreads.invokeInBackgroundThread(() -> {
                //load Drafts
                preloadBoosterDrafts();
                FThreads.invokeInEdtLater(() -> {
                    //selection transition
                    setTransitionScreen(new TransitionScreen(() -> {
                        if (selector.equals("Classic")) {
                            openHomeDefault();
                            clearSplashScreen();
                        } else if (selector.equals("Adventure")) {
                            openAdventure();
                            clearSplashScreen();
                        } else if (splashScreen != null) {
                            splashScreen.setShowModeSelector(true);
                        } else {//default mode in case splashscreen is null at some point as seen on resume..
                            openHomeDefault();
                            clearSplashScreen();
                        }
                        //start background music
                        SoundSystem.instance.setBackgroundMusic(MusicPlaylist.MENUS);
                        safeToClose = true;
                        clearTransitionScreen();
                    }, Forge.takeScreenshot(), false, false, true, false));
                });
            });
        }));
    }

    public static void setCursor(TextureRegion textureRegion, String name) {
        if (GuiBase.isAndroid())
            return;
        if (isMobileAdventureMode) {
            if (cursorA0 != null && name == "0") {
                setGdxCursor(cursorA0);
                return;
            } else if (cursorA1 != null && name == "1") {
                setGdxCursor(cursorA1);
                return;
            } else if (cursorA2 != null && name == "2") {
                setGdxCursor(cursorA2);
                return;
            }

            String path = "skin/cursor" + name + ".png";
            Pixmap pm = new Pixmap(Config.instance().getFile(path));

            if (name == "0") {
                cursorA0 = Gdx.graphics.newCursor(pm, 0, 0);
                setGdxCursor(cursorA0);
            } else if (name == "1") {
                cursorA1 = Gdx.graphics.newCursor(pm, 0, 0);
                setGdxCursor(cursorA1);
            } else {
                cursorA2 = Gdx.graphics.newCursor(pm, 0, 0);
                setGdxCursor(cursorA2);
            }

            pm.dispose();
            return;
        }
        if (!FModel.getPreferences().getPrefBoolean(ForgePreferences.FPref.UI_ENABLE_MAGNIFIER) && name != "0")
            return; //don't change if it's disabled
        if (currentScreen != null && !currentScreen.toString().toLowerCase().contains("match") && name != "0")
            return; // cursor indicator should be during matches
        if (textureRegion == null) {
            return;
        }
        if (cursor0 != null && name == "0") {
            setGdxCursor(cursor0);
            return;
        } else if (cursor1 != null && name == "1") {
            setGdxCursor(cursor1);
            return;
        } else if (cursor2 != null && name == "2") {
            setGdxCursor(cursor2);
            return;
        }
        TextureData textureData = textureRegion.getTexture().getTextureData();
        if (!textureData.isPrepared()) {
            textureData.prepare();
        }
        Pixmap pm = new Pixmap(
                textureRegion.getRegionWidth(),
                textureRegion.getRegionHeight(),
                textureData.getFormat()
        );
        pm.drawPixmap(
                textureData.consumePixmap(), // The other Pixmap
                0, // The target x-coordinate (top left corner)
                0, // The target y-coordinate (top left corner)
                textureRegion.getRegionX(), // The source x-coordinate (top left corner)
                textureRegion.getRegionY(), // The source y-coordinate (top left corner)
                textureRegion.getRegionWidth(), // The width of the area from the other Pixmap in pixels
                textureRegion.getRegionHeight() // The height of the area from the other Pixmap in pixels
        );
        if (name == "0") {
            cursor0 = Gdx.graphics.newCursor(pm, 0, 0);
            setGdxCursor(cursor0);
        } else if (name == "1") {
            cursor1 = Gdx.graphics.newCursor(pm, 0, 0);
            setGdxCursor(cursor1);
        } else {
            cursor2 = Gdx.graphics.newCursor(pm, 0, 0);
            setGdxCursor(cursor2);
        }
        cursorName = name;
        pm.dispose();
    }

    static void setGdxCursor(Cursor c) {
        Gdx.graphics.setCursor(c);
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
        if (isLandscapeMode()) {//TODO: Fullscreen support for Display without screen controls
            float aspectratio = DisplayW / DisplayH;
            if (aspectratio > 1.82f) {/* extra wide */
                setHeightModifier(200.0f);
                extrawide = "extrawide";
            } else if (aspectratio > 1.7f) {/* wide */
                setHeightModifier(100.0f);
                extrawide = "wide";
            }
        }
    }

    public static void setForcedEnglishonCJKMissing() {
        if (!forcedEnglishonCJKMissing) {
            forcedEnglishonCJKMissing = true;
            getLocalizer().setEnglish(forcedEnglishonCJKMissing);
            System.err.println("Forge switches to English due to an error generating CJK Fonts. Language: "+locale);
        }
    }
    public static void showMenu() {
        if (isMobileAdventureMode)
            return;
        if (currentScreen == null)
            return;
        endKeyInput(); //end key input before menu shown
        if (FOverlay.getTopOverlay() == null) { //don't show menu if overlay open
            currentScreen.showMenu();
        }
    }

    public static boolean onHomeScreen() {
        return Dscreens.size() == 1;
    }

    public static void back() {
        back(false);
    }

    public static void back(boolean clearlastMatch) {
        if (isMobileAdventureMode) {
            return;
        }
        FScreen lastMatch = currentScreen;
        if (destroyThis && isLandscapeMode())
            return;
        if (Dscreens.size() < 2 || (currentScreen == HomeScreen.instance && isPortraitMode)) {
            exit(false); //prompt to exit if attempting to go back from home screen
            return;
        }
        currentScreen.onClose(new Callback<Boolean>() {
            @Override
            public void run(Boolean result) {
                if (result) {
                    Dscreens.pollFirst();
                    setCurrentScreen(Dscreens.peekFirst());
                    if (clearlastMatch) {
                        try {
                            Dscreens.remove(lastMatch);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        //check
                        /*for (FScreen fScreen : Dscreens)
                            System.out.println(fScreen.toString());*/
                    }
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
        if (exited) {
            return;
        } //don't allow exiting multiple times

        Callback<Boolean> callback = new Callback<Boolean>() {
            @Override
            public void run(Boolean result) {
                if (result) {
                    exited = true;
                    exitAnimation(true);
                }
            }
        };


        if (silent) {
            callback.run(true);
        } else {
            FOptionPane.showConfirmDialog(
                    getLocalizer().getMessage("lblAreYouSureYouWishRestartForge"), getLocalizer().getMessage("lblRestartForge"),
                    getLocalizer().getMessage("lblRestart"), getLocalizer().getMessage("lblCancel"), callback);
        }
    }

    public static void exit(boolean silent) {
        if (exited) {
            return;
        } //don't allow exiting multiple times

        final List<String> options = new ArrayList<>();
        options.add(getLocalizer().getMessage("lblExit"));
        options.add(getLocalizer().getMessage("lblAdventure"));
        options.add(getLocalizer().getMessage("lblCancel"));

        Callback<Integer> callback = new Callback<Integer>() {
            @Override
            public void run(Integer result) {
                if (result == 0) {
                    exited = true;
                    exitAnimation(false);
                } else if (result == 1) {
                    switchToAdventure();
                }
            }
        };

        if (silent) {
            callback.run(0);
        } else {
            FOptionPane.showOptionDialog(getLocalizer().getMessage("lblAreYouSureYouWishExitForge"), "",
                    FOptionPane.QUESTION_ICON, options, 0, callback);
        }
    }

    public static void openScreen(final FScreen screen0) {
        openScreen(screen0, false);
    }

    public static void openScreen(final FScreen screen0, final boolean replaceBackScreen) {
        if (currentScreen == screen0) {
            return;
        }

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
                    if (screen0 instanceof MatchScreen) {
                        //set cursor for classic mode
                        if (!isMobileAdventureMode) {
                            if (magnifyToggle) {
                                setCursor(FSkin.getCursor().get(1), "1");
                            } else {
                                setCursor(FSkin.getCursor().get(2), "2");
                            }
                        }
                    }
                }
            }
        });
    }

    public static boolean isTextureFilteringEnabled() {
        return textureFiltering;
    }

    public static boolean isLandscapeMode() {
        if (GuiBase.isAndroid())
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

    public static void clearCurrentScreen() {
        currentScreen = null;
    }

    public static void switchToClassic() {
        setTransitionScreen(new TransitionScreen(() -> {
            ImageCache.disposeTextures();
            isMobileAdventureMode = false;
            GuiBase.setIsAdventureMode(false);
            setCursor(FSkin.getCursor().get(0), "0");
            altZoneTabs = FModel.getPreferences().getPrefBoolean(FPref.UI_ALT_PLAYERZONETABS);
            Gdx.input.setInputProcessor(getInputProcessor());
            clearTransitionScreen();
            openHomeDefault();
            exited = false;
        }, Forge.takeScreenshot(), false, false));
    }

    public static void switchToAdventure() {
        setTransitionScreen(new TransitionScreen(() -> {
            ImageCache.disposeTextures();
            clearCurrentScreen();
            clearTransitionScreen();
            openAdventure();
            exited = false;
        }, null, false, true));
    }

    public static void setTransitionScreen(TransitionScreen screen) {
        transitionScreen = screen;
    }

    public static void clearTransitionScreen() {
        transitionScreen = null;
    }

    public static void clearSplashScreen() {
        splashScreen = null;
    }
    public static TextureRegion takeScreenshot() {
        TextureRegion screenShot = ScreenUtils.getFrameBufferTexture();
        return screenShot;
    }

    private static void setCurrentScreen(FScreen screen0) {
        String toNewScreen = screen0 != null ? screen0.toString() : "";
        String previousScreen = currentScreen != null ? currentScreen.toString() : "";
        //update gameInProgress for preload decks
        gameInProgress = toNewScreen.toLowerCase().contains("match") || previousScreen.toLowerCase().contains("match");
        //dispose card textures handled by assetmanager
        boolean dispose = toNewScreen.toLowerCase().contains("homescreen") && disposeTextures;
        try {
            endKeyInput(); //end key input before switching screens
            ForgeAnimation.endAll(); //end all active animations before switching screens
            currentScreen = screen0;
            currentScreen.setSize(screenWidth, screenHeight);
            currentScreen.onActivate();
        } catch (Exception ex) {
            graphics.end();
            //check if sentry is enabled, if not it will call the gui interface but here we end the graphics so we only send it via sentry..
            if (BugReporter.isSentryEnabled())
                BugReporter.reportException(ex);
        } finally {
            if (dispose)
                ImageCache.disposeTextures();
        }
    }

    @Override
    public void render() {
        if (showFPS)
            frameRate.update(ImageCache.counter, Forge.getAssets().manager().getMemoryInMegabytes());

        try {
            ImageCache.allowSingleLoad();
            ForgeAnimation.advanceAll();

            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT); // Clear the screen.

            FContainer screen = currentScreen;

            if (closingScreen != null) {
                screen = closingScreen;
            } else if (transitionScreen != null) {
                screen = transitionScreen;
            } else if (screen == null) {
                screen = splashScreen;
                if (screen == null) {
                    if (isMobileAdventureMode) {
                        try {
                            float delta = Gdx.graphics.getDeltaTime();
                            float transitionTime = 0.12f;
                            if (sceneWasSwapped) {
                                sceneWasSwapped = false;
                                animationTimeout = transitionTime;
                                Gdx.gl.glClearColor(0, 0, 0, 1);
                                Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
                                return;
                            }
                            if (animationTimeout >= 0) {
                                Gdx.gl.glClearColor(0, 0, 0, 1);
                                Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
                                animationBatch.begin();
                                animationTimeout -= delta;
                                animationBatch.setColor(1, 1, 1, 1);
                                animationBatch.draw(lastScreenTexture, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
                                animationBatch.setColor(1, 1, 1, 1 - (1 / transitionTime) * animationTimeout);
                                animationBatch.draw(getAssets().fallback_skins().get(1), 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
                                animationBatch.draw(getAssets().fallback_skins().get(1), 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
                                animationBatch.end();
                                if (animationTimeout < 0) {
                                    currentScene.render();
                                    storeScreen();
                                    Gdx.gl.glClearColor(0, 0, 0, 1);
                                    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
                                } else {
                                    return;
                                }
                            }
                            if (animationTimeout >= -transitionTime) {
                                Gdx.gl.glClearColor(0, 0, 0, 1);
                                Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
                                animationBatch.begin();
                                animationTimeout -= delta;
                                animationBatch.setColor(1, 1, 1, 1);
                                animationBatch.draw(lastScreenTexture, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
                                animationBatch.setColor(1, 1, 1, (1 / transitionTime) * (animationTimeout + transitionTime));
                                animationBatch.draw(getAssets().fallback_skins().get(1), 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
                                animationBatch.draw(getAssets().fallback_skins().get(1), 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
                                animationBatch.end();
                                return;
                            }
                            currentScene.render();
                            currentScene.act(delta);
                        } catch (IllegalStateException | NullPointerException ie) {
                            //silence this..
                        }
                    }
                    if (showFPS)
                        frameRate.render();
                    return;
                }
            }

            graphics.begin(screenWidth, screenHeight);
            screen.screenPos.setSize(screenWidth, screenHeight);
            if (screen.getRotate180()) {
                graphics.startRotateTransform(screenWidth / 2f, screenHeight / 2f, 180);
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
                        graphics.startRotateTransform(screenWidth / 2f, screenHeight / 2f, 180);
                    }
                    overlay.draw(graphics);
                    if (overlay.getRotate180()) {
                        graphics.endTransform();
                    }
                }
            }
            //update here
            if (needsUpdate) {
                if (getAssets().manager().update())
                    needsUpdate = false;
            }
            graphics.end();
        } catch (Exception ex) {
            graphics.end();
            //check if sentry is enabled, if not it will call the gui interface but here we end the graphics so we only send it via sentry..
            if (BugReporter.isSentryEnabled())
                BugReporter.reportException(ex);
            else
                ex.printStackTrace();
        }
        if (showFPS)
            frameRate.render();
    }

    public static void delayedSwitchBack() {
        FThreads.invokeInBackgroundThread(() -> FThreads.invokeInEdtLater(() -> {
            clearTransitionScreen();
            clearCurrentScreen();
            switchToLast();
        }));
    }

    @Override
    public void resize(int width, int height) {
        try {
            screenWidth = width;
            screenHeight = height;
            if (currentScreen != null) {
                currentScreen.setSize(width, height);
            } else if (splashScreen != null) {
                splashScreen.setSize(width, height);
            }
        } catch (Exception ex) {
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
        try {
            Texture.setAssetManager(getAssets().manager());
            needsUpdate = true;
        } catch (Exception e) {
            //the application context must have been recreated from its last state.
            //it could be triggered by the low memory on heap on android.
            needsUpdate = false;
            e.printStackTrace();
        }
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
        assets.dispose();
        Dscreens.clear();
        graphics.dispose();
        SoundSystem.instance.dispose();
        try {
            ExceptionHandler.unregisterErrorHandling();
        } catch (Exception e) {
        }
    }
    /** Retrieve assets.
     * @param other if set to true returns otherAssets otherwise returns cardAssets
     */
    public static Assets getAssets() {
        return ((Forge)Gdx.app.getApplicationListener()).assets;
    }
    public static boolean switchScene(Scene newScene) {
        if (currentScene != null) {
            if (!currentScene.leave())
                return false;
            lastScene.add(currentScene);
        }
        storeScreen();
        sceneWasSwapped = true;
        if (newScene instanceof GameScene)
            MapStage.getInstance().clearIsInMap();
        currentScene = newScene;
        currentScene.enter();
        return true;
    }

    protected static void storeScreen() {
        if (!(currentScene instanceof ForgeScene)) {
            if (lastScreenTexture != null)
                lastScreenTexture.getTexture().dispose();
            lastScreenTexture = Forge.takeScreenshot();
        }


    }

    public static Scene switchToLast() {

        if (lastScene.size != 0) {
            storeScreen();
            currentScene = lastScene.get(lastScene.size - 1);
            currentScene.enter();
            sceneWasSwapped = true;
            lastScene.removeIndex(lastScene.size - 1);
            return currentScene;
        }
        return null;
    }

    //log message to Forge.log file
    public static void log(Object message) {
        System.out.println(message);
    }

    public static void startKeyInput(KeyInputAdapter adapter) {
        if (keyInputAdapter == adapter) {
            return;
        }
        if (keyInputAdapter != null) {
            keyInputAdapter.onInputEnd(); //make sure previous adapter is ended
        }
        keyInputAdapter = adapter;
        Gdx.input.setOnscreenKeyboardVisible(true);
    }

    public static boolean endKeyInput() {
        if (keyInputAdapter == null) {
            return false;
        }
        keyInputAdapter.onInputEnd();
        keyInputAdapter = null;
        MainInputProcessor.keyTyped = false;
        MainInputProcessor.lastKeyTyped = '\0';
        Gdx.input.setOnscreenKeyboardVisible(false);
        return true;
    }

    public static void exitAnimation(boolean restart) {
        if (transitionScreen != null)
            return; //finish transition incase exit is touched
        if (closingScreen == null) {
            closingScreen = new ClosingScreen(restart);
        }
    }

    public static abstract class KeyInputAdapter {
        public abstract FDisplayObject getOwner();

        public abstract boolean allowTouchInput();

        public abstract boolean keyTyped(char ch);

        public abstract boolean keyDown(int keyCode);

        public abstract void onInputEnd();

        //also allow handling of keyUp but don't require it
        public boolean keyUp(int keyCode) {
            return false;
        }

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
                touchDown(0, 0, 0, 0);
                return fling(1000, 0);
            }
            if (keyCode == Keys.RIGHT) {
                touchDown(0, 0, 0, 0);
                return fling(-1000, 0);
            }
            if (keyCode == Keys.UP) {
                touchDown(0, 0, 0, 0);
                return fling(0, -1000);
            }
            if (keyCode == Keys.DOWN) {
                touchDown(0, 0, 0, 0);
                return fling(0, 1000);
            }
            if (keyCode == Keys.BACK) {
                if ((destroyThis && !isMobileAdventureMode) || (splashScreen != null && splashScreen.isShowModeSelector()))
                    exitAnimation(false);
                else if (onHomeScreen() && isLandscapeMode())
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
            if (splashScreen != null) {
                splashScreen.buildTouchListeners(x, y, potentialListeners);
            }
        }

        @Override
        public boolean touchDown(int x, int y, int pointer, int button) {
            if (transitionScreen != null)
                return false;
            if (pointer == 0) { //don't change listeners when second finger goes down for zoom
                updatePotentialListeners(x, y);
                if (keyInputAdapter != null) {
                    if (!keyInputAdapter.allowTouchInput() || !potentialListeners.contains(keyInputAdapter.getOwner())) {
                        endKeyInput(); //end key input if needed
                    }
                }
            }
            mouseButtonID = button;
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
            } catch (Exception ex) {
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
            } catch (Exception ex) {
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
            } catch (Exception ex) {
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
            } catch (Exception ex) {
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
            } catch (Exception ex) {
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
            } catch (Exception ex) {
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
            } catch (Exception ex) {
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
            } catch (Exception ex) {
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
            } catch (Exception ex) {
                BugReporter.reportException(ex);
                return true;
            }
        }

        //mouseMoved and scrolled events for desktop version
        private int mouseMovedX, mouseMovedY;

        @Override
        public boolean mouseMoved(int screenX, int screenY) {
            magnify = true;
            mouseMovedX = screenX;
            mouseMovedY = screenY;
            //todo: mouse listener for android?
            if (GuiBase.isAndroid())
                return true;
            hoveredCount = 0;
            //reset
            try {
                for (FDisplayObject listener : potentialListeners) {
                    listener.setHovered(false);
                }
            } catch (Exception ex) {
                BugReporter.reportException(ex);
            }
            updatePotentialListeners(screenX, screenY);
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
            } else {
                handled = pan(mouseMovedX, mouseMovedY, 0, -Utils.AVG_FINGER_HEIGHT * amountY, true);
            }
            if (panStop(mouseMovedX, mouseMovedY)) {
                handled = true;
            }
            return handled;
        }
    }
}
