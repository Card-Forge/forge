package forge.assets;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.TextureLoader;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import forge.Forge;
import forge.assets.FSkinImage.SourceFile;
import forge.card.CardFaceSymbols;
import forge.gui.FThreads;
import forge.gui.GuiBase;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.localinstance.skin.FSkinProp;
import forge.model.FModel;
import forge.screens.LoadingOverlay;
import forge.screens.SplashScreen;
import forge.screens.TransitionScreen;
import forge.toolbox.FProgressBar;
import forge.util.WordUtil;

import java.util.HashMap;
import java.util.Map;

public class FSkin {
    private static final Map<FSkinProp, FSkinImage> images = new HashMap<>(512);
    private static final Map<Integer, TextureRegion> avatars = new HashMap<>(150);
    private static final Map<Integer, TextureRegion> sleeves = new HashMap<>(64);
    private static final Map<Integer, TextureRegion> cracks = new HashMap<>(16);
    private static final Map<Integer, TextureRegion> borders = new HashMap<>();
    private static final Map<Integer, TextureRegion> deckbox = new HashMap<>();
    private static final Map<Integer, TextureRegion> cursor = new HashMap<>();

    private static Array<String> allSkins;
    private static FileHandle preferredDir;
    private static String preferredName;
    private static boolean loaded = false;
    public static Texture hdLogo = null;
    public static Texture overlay_alpha = null;
    public static Texture splatter = null;

    public static void changeSkin(final String skinName) {
        final ForgePreferences prefs = FModel.getPreferences();
        if (skinName.equals(prefs.getPref(FPref.UI_SKIN))) { return; }

        //save skin preference
        prefs.setPref(FPref.UI_SKIN, skinName);
        prefs.save();

        Forge.setTransitionScreen(new TransitionScreen(new Runnable() {
            @Override
            public void run() {
                FThreads.invokeInBackgroundThread(() -> FThreads.invokeInEdtLater(() -> {
                    final LoadingOverlay loader = new LoadingOverlay(Forge.getLocalizer().getMessageorUseDefault("lblRestartInFewSeconds", "Forge will restart after a few seconds..."), true);
                    loader.show();
                    FThreads.invokeInBackgroundThread(() -> {
                        FSkinFont.deleteCachedFiles(); //delete cached font files so font can be update for new skin
                        FThreads.delayInEDT(2000, new Runnable() {
                            @Override
                            public void run() {
                                Forge.clearTransitionScreen();
                                FThreads.invokeInEdtLater(() -> {
                                    Forge.restart(true);
                                });
                            }
                        });
                    });
                }));
            }
        }, null, false, true));
    }
    public static void loadLight(String skinName, final SplashScreen splashScreen,FileHandle prefDir) {
        preferredDir = prefDir;
        loadLight(skinName,splashScreen);
    }
    /*
     * Loads a "light" version of FSkin, just enough for the splash screen:
     * skin name. Generates custom skin settings, fonts, and backgrounds.
     * 
     * 
     * @param skinName
     *            the skin name
     */
    public static void loadLight(String skinName, final SplashScreen splashScreen) {
        AssetManager manager = Forge.getAssets(true).manager;
        preferredName = skinName.toLowerCase().replace(' ', '_');

        //reset hd buttons/icons
        Forge.hdbuttons = false;
        Forge.hdstart = false;

        //ensure skins directory exists
        final FileHandle dir = Gdx.files.absolute(ForgeConstants.CACHE_SKINS_DIR);
        if(preferredDir==null)
        {
            if (!dir.exists() || !dir.isDirectory()) {
                //if skins directory doesn't exist, point to internal assets/skin directory instead for the sake of the splash screen
                preferredDir = GuiBase.isAndroid() ? Gdx.files.internal("fallback_skin") : Gdx.files.classpath("fallback_skin");
            }
            else {
                if (splashScreen != null) {
                    if (allSkins == null) { //initialize
                        allSkins = new Array<>();
                        allSkins.add("Default"); //init default
                        final Array<String> skinDirectoryNames = getSkinDirectoryNames();
                        for (final String skinDirectoryName : skinDirectoryNames) {
                            allSkins.add(WordUtil.capitalize(skinDirectoryName.replace('_', ' ')));
                        }
                        allSkins.sort();
                    }
                }

                // Non-default (preferred) skin name and dir.
                preferredDir = Gdx.files.absolute(preferredName.equalsIgnoreCase("default") ? ForgeConstants.BASE_SKINS_DIR + preferredName : ForgeConstants.CACHE_SKINS_DIR + preferredName);
                if (!preferredDir.exists() || !preferredDir.isDirectory()) {
                    preferredDir.mkdirs();
                }
            }
        }


        FSkinTexture.BG_TEXTURE.load(); //load background texture early for splash screen

        //load theme logo while changing skins
        final FileHandle theme_logo = getSkinFile("hd_logo.png");
        if (theme_logo.exists()) {
            manager.load(theme_logo.path(), Texture.class, new TextureLoader.TextureParameter(){{genMipMaps = true; minFilter = Texture.TextureFilter.MipMapLinearLinear; magFilter = Texture.TextureFilter.Linear;}});
            manager.finishLoadingAsset(theme_logo.path());
            hdLogo = manager.get(theme_logo.path());
        } else {
            hdLogo = null;
        }
        final FileHandle duals_overlay = getDefaultSkinFile("overlay_alpha.png");
        if (duals_overlay.exists()) {
            manager.load(duals_overlay.path(), Texture.class, new TextureLoader.TextureParameter(){{genMipMaps = true; minFilter = Texture.TextureFilter.MipMapLinearLinear; magFilter = Texture.TextureFilter.Linear;}});
            manager.finishLoadingAsset(duals_overlay.path());
            overlay_alpha = manager.get(duals_overlay.path());
        } else {
            overlay_alpha = null;
        }
        final FileHandle splatter_overlay = getDefaultSkinFile("splatter.png");
        if (splatter_overlay.exists()) {
            manager.load(splatter_overlay.path(), Texture.class, new TextureLoader.TextureParameter(){{genMipMaps = true; minFilter = Texture.TextureFilter.MipMapLinearLinear; magFilter = Texture.TextureFilter.Linear;}});
            manager.finishLoadingAsset(splatter_overlay.path());
            splatter = manager.get(splatter_overlay.path());
        } else {
            splatter = null;
        }

        if (splashScreen != null) {
            final FileHandle f = getSkinFile("bg_splash.png");
            final FileHandle f2 = getSkinFile("bg_splash_hd.png"); //HD Splashscreen

            if (!f.exists()) {
                if (!skinName.equals("default")) {
                    FSkin.loadLight("default", splashScreen);
                }
                return;
            }

            try {
                int w, h;
                if (f.path().contains("fallback_skin")) {
                    //the file is not accesible by the manager since it not on absolute path...
                    Texture txSplash = new Texture(f);
                    w = txSplash.getWidth();
                    h = txSplash.getHeight();
                    splashScreen.setBackground(new TextureRegion(txSplash, 0, 0, w, h - 100));
                } else {
                    manager.load(f.path(), Texture.class);
                    manager.finishLoadingAsset(f.path());
                    w = manager.get(f.path(), Texture.class).getWidth();
                    h = manager.get(f.path(), Texture.class).getHeight();

                    if (f2.exists()) {
                        manager.load(f2.path(), Texture.class, new TextureLoader.TextureParameter(){{genMipMaps = true; minFilter = Texture.TextureFilter.MipMapLinearLinear; magFilter = Texture.TextureFilter.Linear;}});
                        manager.finishLoadingAsset(f2.path());
                        splashScreen.setBackground(new TextureRegion(manager.get(f2.path(), Texture.class)));
                    } else {
                        splashScreen.setBackground(new TextureRegion(manager.get(f.path(), Texture.class), 0, 0, w, h - 100));
                    }
                }

                Pixmap pxSplash = new Pixmap(f);
                FProgressBar.BACK_COLOR = new Color(pxSplash.getPixel(25, h - 75));
                FProgressBar.FORE_COLOR = new Color(pxSplash.getPixel(75, h - 75));
                FProgressBar.SEL_BACK_COLOR = new Color(pxSplash.getPixel(25, h - 25));
                FProgressBar.SEL_FORE_COLOR = new Color(pxSplash.getPixel(75, h - 25));
            }
            catch (final Exception e) {
                e.printStackTrace();
            }
            loaded = true;
        }
    }

    /**
     * Loads two sprites: the default (which should be a complete
     * collection of all symbols) and the preferred (which may be
     * incomplete).
     * 
     * Font must be present in the skin folder, and will not
     * be replaced by default.  The fonts are pre-derived
     * in this method and saved in a HashMap for future access.
     * 
     * Color swatches must be present in the preferred
     * sprite, and will not be replaced by default.
     * 
     * Background images must be present in skin folder,
     * and will not be replaced by default.
     * 
     * Icons, however, will be pulled from the two sprites. Obviously,
     * preferred takes precedence over default, but if something is
     * missing, the default picture is retrieved.
     */
    public static void loadFull(final SplashScreen splashScreen) {
        if (splashScreen != null) {
            // Preferred skin name must be called via loadLight() method,
            // which does some cleanup and init work.
            if (FSkin.preferredName.isEmpty()) { FSkin.loadLight("default", splashScreen); }
        }

        avatars.clear();
        sleeves.clear();

        TextureLoader.TextureParameter parameter = new TextureLoader.TextureParameter();
        if (Forge.isTextureFilteringEnabled()) {
            parameter.genMipMaps = true;
            parameter.minFilter = Texture.TextureFilter.MipMapLinearLinear;
            parameter.magFilter = Texture.TextureFilter.Linear;
        }

        AssetManager manager = Forge.getAssets(true).manager;

        // Grab and test various sprite files.
        final FileHandle f1 = getDefaultSkinFile(SourceFile.ICONS.getFilename());
        final FileHandle f2 = getSkinFile(SourceFile.ICONS.getFilename());
        final FileHandle f3 = getDefaultSkinFile(SourceFile.FOILS.getFilename());
        final FileHandle f4 = getDefaultSkinFile(ForgeConstants.SPRITE_AVATARS_FILE);
        final FileHandle f5 = getSkinFile(ForgeConstants.SPRITE_AVATARS_FILE);
        final FileHandle f6 = getDefaultSkinFile(SourceFile.OLD_FOILS.getFilename());
        final FileHandle f7 = getSkinFile(ForgeConstants.SPRITE_MANAICONS_FILE);
        final FileHandle f8 = getDefaultSkinFile(ForgeConstants.SPRITE_SLEEVES_FILE);
        final FileHandle f9 = getDefaultSkinFile(ForgeConstants.SPRITE_SLEEVES2_FILE);
        final FileHandle f10 = getDefaultSkinFile(ForgeConstants.SPRITE_BORDER_FILE);
        final FileHandle f11 = getSkinFile(ForgeConstants.SPRITE_BUTTONS_FILE);
        final FileHandle f12 = getSkinFile(ForgeConstants.SPRITE_START_FILE);
        final FileHandle f13 = getDefaultSkinFile(ForgeConstants.SPRITE_DECKBOX_FILE);
        final FileHandle f17 = getDefaultSkinFile(ForgeConstants.SPRITE_CRACKS_FILE);
        final FileHandle f18 = getDefaultSkinFile(ForgeConstants.SPRITE_PHYREXIAN_FILE);
        final FileHandle f19 = getDefaultSkinFile(ForgeConstants.SPRITE_CURSOR_FILE);

        /*TODO Themeable
        final FileHandle f14 = getDefaultSkinFile(ForgeConstants.SPRITE_SETLOGO_FILE);
        final FileHandle f15 = getSkinFile(ForgeConstants.SPRITE_SETLOGO_FILE);
        final FileHandle f16 = getDefaultSkinFile(ForgeConstants.SPRITE_WATERMARK_FILE);
        */

        try {
            manager.load(f1.path(), Texture.class);
            manager.finishLoadingAsset(f1.path());
            Pixmap preferredIcons = new Pixmap(f1);
            if (f2.exists()) {
                manager.load(f2.path(), Texture.class);
                manager.finishLoadingAsset(f2.path());
                preferredIcons = new Pixmap(f2);
            }

            manager.load(f3.path(), Texture.class);
            manager.finishLoadingAsset(f3.path());
            if (f6.exists()) {
                manager.load(f6.path(), Texture.class);
                manager.finishLoadingAsset(f6.path());
            }
            if (f7.exists()){
                manager.load(f7.path(), Texture.class, new TextureLoader.TextureParameter(){{genMipMaps = true;}});
                manager.finishLoadingAsset(f7.path());
            }

            //hdbuttons
            if (f11.exists()) {
                if (GuiBase.isAndroid() && Forge.totalDeviceRAM <5000) {
                    Forge.hdbuttons = false;
                } else {
                    manager.load(f11.path(), Texture.class, new TextureLoader.TextureParameter(){{genMipMaps = true; minFilter = Texture.TextureFilter.MipMapLinearLinear; magFilter = Texture.TextureFilter.Linear;}});
                    manager.finishLoadingAsset(f11.path());
                    Forge.hdbuttons = true;
                }
            } else { Forge.hdbuttons = false; } //how to refresh buttons when a theme don't have hd buttons?
            if (f12.exists()) {
                if (GuiBase.isAndroid() && Forge.totalDeviceRAM <5000) {
                    Forge.hdstart = false;
                } else {
                    manager.load(f12.path(), Texture.class, new TextureLoader.TextureParameter(){{genMipMaps = true; minFilter = Texture.TextureFilter.MipMapLinearLinear; magFilter = Texture.TextureFilter.Linear;}});
                    manager.finishLoadingAsset(f12.path());
                    Forge.hdstart = true;
                }
            } else { Forge.hdstart = false; }
            //update colors
            for (final FSkinColor.Colors c : FSkinColor.Colors.values()) {
                c.setColor(new Color(preferredIcons.getPixel(c.getX(), c.getY())));
            }

            //load images
            for (FSkinImage image : FSkinImage.values()) {
                if (GuiBase.isAndroid()) {
                    if (Forge.totalDeviceRAM>5000)
                        image.load(manager, preferredIcons);
                    else if (image.toString().equals("HDMULTI"))
                        image.load(manager, preferredIcons);
                    else if (!image.toString().startsWith("HD"))
                        image.load(manager, preferredIcons);
                } else {
                    image.load(manager, preferredIcons);
                }
            }
            for (FSkinTexture texture : FSkinTexture.values()) {
                if (texture != FSkinTexture.BG_TEXTURE) {
                    texture.load();
                }
            }

            //assemble avatar textures
            int counter = 0;
            int scount = 0;
            Color pxTest;
            Pixmap pxDefaultAvatars, pxPreferredAvatars, pxDefaultSleeves;

            pxDefaultAvatars = new Pixmap(f4);
            pxDefaultSleeves = new Pixmap(f8);
            //default avatar
            manager.load(f4.path(), Texture.class, parameter);
            manager.finishLoadingAsset(f4.path());
            //sleeves first set
            manager.load(f8.path(), Texture.class, parameter);
            manager.finishLoadingAsset(f8.path());
            //preferred avatar
            if (f5.exists()) {
                pxPreferredAvatars = new Pixmap(f5);
                manager.load(f5.path(), Texture.class, parameter);
                manager.finishLoadingAsset(f5.path());

                final int pw = pxPreferredAvatars.getWidth();
                final int ph = pxPreferredAvatars.getHeight();

                for (int j = 0; j < ph; j += 100) {
                    for (int i = 0; i < pw; i += 100) {
                        if (i == 0 && j == 0) { continue; }
                        pxTest = new Color(pxPreferredAvatars.getPixel(i + 50, j + 50));
                        if (pxTest.a == 0) { continue; }
                        FSkin.avatars.put(counter++, new TextureRegion(manager.get(f5.path(), Texture.class), i, j, 100, 100));
                    }
                }
                pxPreferredAvatars.dispose();
            } else if (!FSkin.preferredName.isEmpty()){
                //workaround bug crash fix if missing sprite avatar on preferred theme for quest tournament...
                //i really don't know why it needs to populate the avatars twice.... needs investigation
                final int pw = pxDefaultAvatars.getWidth();
                final int ph = pxDefaultAvatars.getHeight();

                for (int j = 0; j < ph; j += 100) {
                    for (int i = 0; i < pw; i += 100) {
                        if (i == 0 && j == 0) { continue; }
                        pxTest = new Color(pxDefaultAvatars.getPixel(i + 50, j + 50));
                        if (pxTest.a == 0) { continue; }
                        FSkin.avatars.put(counter++, new TextureRegion(manager.get(f4.path(), Texture.class), i, j, 100, 100));
                    }
                }
            }

            final int sw = pxDefaultSleeves.getWidth();
            final int sh = pxDefaultSleeves.getHeight();

            for (int j = 0; j < sh; j += 500) {
                for (int i = 0; i < sw; i += 360) {
                    pxTest = new Color(pxDefaultSleeves.getPixel(i + 180, j + 250));
                    if (pxTest.a == 0) { continue; }
                    FSkin.sleeves.put(scount++, new TextureRegion(manager.get(f8.path(), Texture.class), i, j, 360, 500));
                }
            }

            //re init second set of sleeves
            pxDefaultSleeves = new Pixmap(f9);
            manager.load(f9.path(), Texture.class, parameter);
            manager.finishLoadingAsset(f9.path());

            final int sw2 = pxDefaultSleeves.getWidth();
            final int sh2 = pxDefaultSleeves.getHeight();

            for (int j = 0; j < sh2; j += 500) {
                for (int i = 0; i < sw2; i += 360) {
                    pxTest = new Color(pxDefaultSleeves.getPixel(i + 180, j + 250));
                    if (pxTest.a == 0) { continue; }
                    FSkin.sleeves.put(scount++, new TextureRegion(manager.get(f9.path(), Texture.class), i, j, 360, 500));
                }
            }
            //cracks
            manager.load(f17.path(), Texture.class, parameter);
            manager.finishLoadingAsset(f17.path());
            int crackCount = 0;
            for (int j = 0; j < 4; j++) {
                int x = j * 200;
                for(int i = 0; i < 4; i++) {
                    int y = i * 279;
                    FSkin.cracks.put(crackCount++, new TextureRegion(manager.get(f17.path(), Texture.class), x, y, 200, 279));
                }
            }

            //borders
            manager.load(f10.path(), Texture.class);
            manager.finishLoadingAsset(f10.path());
            FSkin.borders.put(0, new TextureRegion(manager.get(f10.path(), Texture.class), 2, 2, 672, 936));
            FSkin.borders.put(1, new TextureRegion(manager.get(f10.path(), Texture.class), 676, 2, 672, 936));
            //deckboxes
            manager.load(f13.path(), Texture.class, parameter);
            manager.finishLoadingAsset(f13.path());
            //gold bg
            FSkin.deckbox.put(0, new TextureRegion(manager.get(f13.path(), Texture.class), 2, 2, 488, 680));
            //deck box for card art
            FSkin.deckbox.put(1, new TextureRegion(manager.get(f13.path(), Texture.class), 492, 2, 488, 680));
            //generic deck box
            FSkin.deckbox.put(2, new TextureRegion(manager.get(f13.path(), Texture.class), 982, 2, 488, 680));
            //cursor
            manager.load(f19.path(), Texture.class);
            manager.finishLoadingAsset(f19.path());
            FSkin.cursor.put(0, new TextureRegion(manager.get(f19.path(), Texture.class), 0, 0, 32, 32)); //default
            FSkin.cursor.put(1, new TextureRegion(manager.get(f19.path(), Texture.class), 32, 0, 32, 32)); //magnify on
            FSkin.cursor.put(2, new TextureRegion(manager.get(f19.path(), Texture.class), 64, 0, 32, 32)); // magnify off

            Forge.setCursor(cursor.get(0), "0");

            preferredIcons.dispose();
            pxDefaultAvatars.dispose();
            pxDefaultSleeves.dispose();
        }
        catch (final Exception e) {
            System.err.println("FSkin$loadFull: Missing a sprite (default icons, "
                    + "preferred icons, or foils.");
            e.printStackTrace();
        }

        // Run through enums and load their coords.
        FSkinColor.updateAll();

        // Images loaded; can start UI init.
        loaded = true;

        if (splashScreen != null) {
            CardFaceSymbols.loadImages();
        }
    }

    /**
     * Gets the name.
     * 
     * @return Name of the current skin.
     */
    public static String getName() {
        return FSkin.preferredName;
    }

    /**
     * Gets a FileHandle for a file within the directory where skin files should be stored
     */
    public static FileHandle getSkinFile(String filename) {
        return preferredDir.child(filename);
    }

    /**
     * Gets a FileHandle for a file within the directory where the default skin files should be stored
     */
    public static FileHandle getDefaultSkinFile(String filename) {
        return Gdx.files.absolute(ForgeConstants.DEFAULT_SKINS_DIR + filename);
    }

    /**
     * Gets a FileHandle for a file within the planechase cache directory
     */
    public static FileHandle getCachePlanechaseFile(String filename) {
        return Gdx.files.absolute(ForgeConstants.CACHE_PLANECHASE_PICS_DIR + filename);
    }

    public static FileHandle getSkinDir() {
        return preferredDir;
    }

    /**
     * Gets the skins.
     *
     * @return the skins
     */
    public static Array<String> getSkinDirectoryNames() {
        final Array<String> mySkins = new Array<>();

        final FileHandle dir = Gdx.files.absolute(ForgeConstants.CACHE_SKINS_DIR);
        for (FileHandle skinFile : dir.list()) {
            String skinName = skinFile.name();
            if (skinName.equalsIgnoreCase(".svn")) { continue; }
            if (skinName.equalsIgnoreCase(".DS_Store")) { continue; }
            mySkins.add(skinName);
        }

        return mySkins;
    }

    public static Iterable<String> getAllSkins() {
        if (allSkins != null) {
            allSkins.clear();
            allSkins.add("Default"); //init default
            final Array<String> skinDirectoryNames = getSkinDirectoryNames();
            for (final String skinDirectoryName : skinDirectoryNames) {
                allSkins.add(WordUtil.capitalize(skinDirectoryName.replace('_', ' ')));
            }
            allSkins.sort();
        }
        return allSkins;
    }

    public static Map<FSkinProp, FSkinImage> getImages() {
        return images;
    }

    public static Map<Integer, TextureRegion> getAvatars() {
        return avatars;
    }

    public static Map<Integer, TextureRegion> getSleeves() {
        return sleeves;
    }

    public static Map<Integer, TextureRegion> getCracks() {
        return cracks;
    }

    public static Map<Integer, TextureRegion> getBorders() {
        return borders;
    }

    public static Map<Integer, TextureRegion> getDeckbox() {
        return deckbox;
    }

    public static Map<Integer, TextureRegion> getCursor() {
        return cursor;
    }

    public static boolean isLoaded() { return loaded; }
}
