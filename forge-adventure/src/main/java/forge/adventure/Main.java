package forge.adventure;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Clipboard;
import com.badlogic.gdx.utils.Clipboard;
import forge.Forge;
import forge.FrameRate;
import forge.Graphics;
import forge.GuiMobile;
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
import forge.util.CardTranslation;
import forge.util.FileUtil;
import forge.util.Localizer;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

class StartAdvanture extends AdventureApplicationAdapter
{
        private static Clipboard clipboard;
        private static IDeviceAdapter deviceAdapter;
        private static int screenWidth;
        private static int screenHeight;
        private static Graphics graphics;
        private static FrameRate frameRate;
        private static FScreen currentScreen;
        private static SplashScreen splashScreen;
        private static Forge.KeyInputAdapter keyInputAdapter;
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
        public StartAdvanture(String plane) {

                super(plane);
                clipboard = new Lwjgl3Clipboard();
                GuiBase.setUsingAppDirectory(false); //obb directory on android uses the package name as entrypoint
                GuiBase.setInterface(new GuiMobile("../forge-gui/"));
                GuiBase.enablePropertyConfig(true);
                isPortraitMode = true;
                totalDeviceRAM = 0;
        }
        @Override
        public void create()
        {
                //install our error handler
                ExceptionHandler.registerErrorHandling();

                GuiBase.setIsAndroid(Gdx.app.getType() == Application.ApplicationType.Android);

                graphics = new Graphics();
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
                ForgePreferences prefs = new ForgePreferences();

                String skinName;
                if (FileUtil.doesFileExist(ForgeConstants.MAIN_PREFS_FILE)) {
                        skinName = prefs.getPref(ForgePreferences.FPref.UI_SKIN);
                }
                else {
                        skinName = "default"; //use default skin if preferences file doesn't exist yet
                }
                FSkin.loadLight(skinName, splashScreen);

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

                                                FSkin.loadFull(splashScreen);

                                                SoundSystem.instance.setBackgroundMusic(MusicPlaylist.MENUS); //start background music
                                                destroyThis = false; //Allow back()
                                                Gdx.input.setCatchKey(Input.Keys.MENU, true);
                                                splashScreen = null;

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
                                                /*  call preloadExtendedArt here, if we put it above we will  *
                                                 *  get error: No OpenGL context found in the current thread. */

                                        }
                                });
                        }
                });
                super.create();
        }
}
public class Main {

        public static void main(String[] args) {





                AdventureApplicationConfiguration config=new AdventureApplicationConfiguration();

                config.SetPlane("Shandalar");
                config.setFullScreen(false);

                new Lwjgl3Application(new StartAdvanture(config.Plane), config);

        }
}
