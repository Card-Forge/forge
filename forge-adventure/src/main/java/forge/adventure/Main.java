package forge.adventure;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Clipboard;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.utils.Clipboard;
import forge.Forge;
import forge.FrameRate;
import forge.GuiMobile;
import forge.adventure.scene.SettingsScene;
import forge.adventure.util.Config;
import forge.assets.AssetsDownloader;
import forge.assets.FSkin;
import forge.assets.FSkinFont;
import forge.assets.ImageCache;
import forge.error.ExceptionHandler;
import forge.gui.FThreads;
import forge.gui.GuiBase;
import forge.interfaces.IDeviceAdapter;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;
import forge.screens.FScreen;
import forge.screens.SplashScreen;
import forge.sound.MusicPlaylist;
import forge.sound.SoundSystem;
import forge.util.BuildInfo;
import forge.util.CardTranslation;
import forge.util.FileUtil;
import forge.util.Localizer;
import io.sentry.Sentry;
import io.sentry.SentryClient;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Wrapper to start forge first (splash screen and resources loading)
 *
 */
class StartAdventure extends AdventureApplicationAdapter {
    private static final int continuousRenderingCount = 1; //initialize to 1 since continuous rendering is the default
    private static final Deque<FScreen> Dscreens = new ArrayDeque<>();
    private static final boolean isloadingaMatch = false;
    public static String extrawide = "default";
    public static float heigtModifier = 0.0f;
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
    public static Forge app;
    private static Clipboard clipboard;
    private static IDeviceAdapter deviceAdapter;
    private static FrameRate frameRate;
    private static FScreen currentScreen;
    private static SplashScreen splashScreen;
    private static Forge.KeyInputAdapter keyInputAdapter;
    private static boolean exited;
    private static boolean textureFiltering = false;
    private static boolean destroyThis = false;

    public StartAdventure() {

        super();
        Forge.isTabletDevice = true;
        Forge.isPortraitMode = false;
        Forge.hdbuttons = true;
        Forge.hdstart = true;

        String path= Files.exists(Paths.get("./res"))?"./":"../forge-gui/";

        app = (Forge) Forge.getApp(new Lwjgl3Clipboard(), new DesktopAdapter(""), path, true, false, 0, true, 0, "", "");

        clipboard = new Lwjgl3Clipboard();
        GuiBase.setUsingAppDirectory(false); //obb directory on android uses the package name as entrypoint
        GuiBase.setInterface(new GuiMobile(path));
        GuiBase.enablePropertyConfig(true);
        isPortraitMode = true;
        totalDeviceRAM = 0;
        GuiBase.setDeviceInfo("", "", 0, 0);

    }

    @Override
    public void render() {
        if (splashScreen != null) {
            Gdx.gl.glClearColor(1, 0, 1, 1);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT); // Clear the screen.
            getGraphics().begin(getCurrentWidth(), getCurrentHeight());
            splashScreen.setSize(getCurrentWidth(), getCurrentHeight());
            splashScreen.screenPos.setSize(getCurrentWidth(), getCurrentHeight());
            if (splashScreen.getRotate180()) {
                getGraphics().startRotateTransform(getCurrentWidth() / 2f, getCurrentHeight() / 2f, 180);
            }
            splashScreen.draw(getGraphics());
            if (splashScreen.getRotate180()) {
                getGraphics().endTransform();
            }

            getGraphics().end();
        } else {
            super.render();
        }
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        if (splashScreen != null)
            splashScreen.setSize(width, height);
    }

    @Override
    public void create() {
        //install our error handler
        ExceptionHandler.registerErrorHandling();
        splashScreen = new SplashScreen();
        frameRate = new FrameRate();
        /*
         Set CatchBackKey here and exit the app when you hit the
         back button while the textures,fonts,etc are still loading,
         to prevent rendering issue when you try to restart
         the app again (seems it doesnt dispose correctly...?!?)
         */
        Gdx.input.setCatchKey(Input.Keys.BACK, true);
        destroyThis = true; //Prevent back()
        ForgePreferences prefs = SettingsScene.Preference = new ForgePreferences();


        String skinName;
        if (FileUtil.doesFileExist(ForgeConstants.MAIN_PREFS_FILE)) {
            skinName = prefs.getPref(ForgePreferences.FPref.UI_SKIN);
        } else {
            skinName = "default"; //use default skin if preferences file doesn't exist yet
        }
        FSkin.loadLight(skinName, splashScreen,Config.instance().getFile("skin"));

        textureFiltering = prefs.getPrefBoolean(ForgePreferences.FPref.UI_LIBGDX_TEXTURE_FILTERING);
        showFPS = prefs.getPrefBoolean(ForgePreferences.FPref.UI_SHOW_FPS);
        altPlayerLayout = prefs.getPrefBoolean(ForgePreferences.FPref.UI_ALT_PLAYERINFOLAYOUT);
        altZoneTabs = prefs.getPrefBoolean(ForgePreferences.FPref.UI_ALT_PLAYERZONETABS);
        enableUIMask = prefs.getPref(ForgePreferences.FPref.UI_ENABLE_BORDER_MASKING);
        if (prefs.getPref(ForgePreferences.FPref.UI_ENABLE_BORDER_MASKING).equals("true")) //override old settings if not updated
            enableUIMask = "Full";
        else if (prefs.getPref(ForgePreferences.FPref.UI_ENABLE_BORDER_MASKING).equals("false"))
            enableUIMask = "Off";
        enablePreloadExtendedArt = prefs.getPrefBoolean(ForgePreferences.FPref.UI_ENABLE_PRELOAD_EXTENDED_ART);
        locale = prefs.getPref(ForgePreferences.FPref.UI_LANGUAGE);
        autoCache = prefs.getPrefBoolean(ForgePreferences.FPref.UI_AUTO_CACHE_SIZE);
        disposeTextures = prefs.getPrefBoolean(ForgePreferences.FPref.UI_ENABLE_DISPOSE_TEXTURES);
        CJK_Font = prefs.getPref(ForgePreferences.FPref.UI_CJK_FONT);

        if (autoCache) {
            //increase cacheSize for devices with RAM more than 5GB, default is 400. Some phones have more than 10GB RAM (Mi 10, OnePlus 8, S20, etc..)
            if (totalDeviceRAM > 5000) //devices with more than 10GB RAM will have 800 Cache size, 600 Cache size for morethan 5GB RAM
                cacheSize = totalDeviceRAM > 10000 ? 800 : 600;
        }
        //init cache
        ImageCache.initCache(cacheSize);
        final Localizer localizer = Localizer.getInstance();

        //load model on background thread (using progress bar to report progress)
        super.create();
        FThreads.invokeInBackgroundThread(new Runnable() {
            @Override
            public void run() {
                //see if app or assets need updating
                AssetsDownloader.checkForUpdates(splashScreen);
                if (exited) {
                    return;
                } //don't continue if user chose to exit or couldn't download required assets

                FModel.initialize(splashScreen.getProgressBar(), null);

                splashScreen.getProgressBar().setDescription(localizer.getMessage("lblLoadingFonts"));
                FSkinFont.preloadAll(locale);

                splashScreen.getProgressBar().setDescription(localizer.getMessage("lblLoadingCardTranslations"));
                CardTranslation.preloadTranslation(locale, ForgeConstants.LANG_DIR);

                splashScreen.getProgressBar().setDescription(localizer.getMessage("lblFinishingStartup"));

                //add reminder to preload
                if (enablePreloadExtendedArt) {
                    if (autoCache)
                        splashScreen.getProgressBar().setDescription(localizer.getMessage("lblPreloadExtendedArt") + "\nDetected RAM: " + totalDeviceRAM + "MB. Cache size: " + cacheSize);
                    else
                        splashScreen.getProgressBar().setDescription(localizer.getMessage("lblPreloadExtendedArt"));
                } else {
                    if (autoCache)
                        splashScreen.getProgressBar().setDescription(localizer.getMessage("lblFinishingStartup") + "\nDetected RAM: " + totalDeviceRAM + "MB. Cache size: " + cacheSize);
                    else
                        splashScreen.getProgressBar().setDescription(localizer.getMessage("lblFinishingStartup"));
                }

                Gdx.app.postRunnable(new Runnable() {
                    @Override
                    public void run() {

                        FSkin.loadFull(splashScreen);

                        SoundSystem.instance.setBackgroundMusic(MusicPlaylist.MENUS); //start background music
                        destroyThis = false; //Allow back()
                        Gdx.input.setCatchKey(Input.Keys.MENU, true);
                        //openHomeScreen(-1); //default for startup
                        splashScreen = null;


                        //adjust height modifier

                        //update landscape mode preference if it doesn't match what the app loaded as
                        if (!FModel.getPreferences().getPrefBoolean(ForgePreferences.FPref.UI_LANDSCAPE_MODE)) {
                            FModel.getPreferences().setPref(ForgePreferences.FPref.UI_LANDSCAPE_MODE, true);
                            FModel.getPreferences().save();
                        }

                        resLoaded();
                        if (!enablePreloadExtendedArt)
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
                        /*  call preloadExtendedArt here, if we put it above we will  *
                         *  get error: No OpenGL context found in the current thread. */

                    }
                });
            }
        });

    }

}
/**
 * Main entry point
 */
public class Main {

    public static void main(String[] args) {

        Sentry.init();
        SentryClient sentryClient = Sentry.getStoredClient();
        sentryClient.setRelease(BuildInfo.getVersionString());
        sentryClient.setEnvironment(System.getProperty("os.name"));
        sentryClient.addTag("Java Version", System.getProperty("java.version"));

        // HACK - temporary solution to "Comparison method violates it's general contract!" crash
        System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");

        //Turn off the Java 2D system's use of Direct3D to improve rendering speed (particularly when Full Screen)
        System.setProperty("sun.java2d.d3d", "false");


        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setResizable(false);
        StartAdventure start=new StartAdventure();

        if (Config.instance().getSettingData().fullScreen)
        {
            config.setFullscreenMode(Lwjgl3ApplicationConfiguration.getDisplayMode());
        } else {
            config.setWindowedMode(Config.instance().getSettingData().width, Config.instance().getSettingData().height);
        }

        config.setWindowIcon(Config.instance().getFilePath("forge-adventure.png"));

        new Lwjgl3Application(start, config);

    }
}
