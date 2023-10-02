package forge.assets;

import com.badlogic.gdx.Gdx;
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

import java.util.Map;

public class FSkin {
    private static Array<String> allSkins;
    private static FileHandle preferredDir;
    private static String preferredName;
    private static boolean loaded = false;

    public static Texture getLogo() {
        if (Forge.isMobileAdventureMode)
            return Forge.getAssets().getTexture(getDefaultSkinFile("adv_logo.png"), true, false);
        return Forge.getAssets().getTexture(getSkinFile("hd_logo.png"), false);
    }

    public static void changeSkin(final String skinName) {
        final ForgePreferences prefs = FModel.getPreferences();
        if (skinName.equals(prefs.getPref(FPref.UI_SKIN))) { return; }

        //save skin preference
        prefs.setPref(FPref.UI_SKIN, skinName);
        prefs.save();

        Forge.setTransitionScreen(new TransitionScreen(() -> FThreads.invokeInBackgroundThread(() -> FThreads.invokeInEdtLater(() -> {
            final LoadingOverlay loader = new LoadingOverlay(Forge.getLocalizer().getMessageorUseDefault("lblRestartInFewSeconds", "Forge will restart after a few seconds..."), true);
            loader.show();
            FThreads.invokeInBackgroundThread(() -> {
                FSkinFont.deleteCachedFiles(); //delete cached font files so font can be update for new skin
                FThreads.delayInEDT(2000, () -> {
                    Forge.clearTransitionScreen();
                    FThreads.invokeInEdtLater(() -> {
                        Forge.restart(true);
                    });
                });
            });
        })), null, false, true));
    }
    private static boolean isValidDirectory(FileHandle fileHandle) {
        if (fileHandle == null)
            return false;
        if (!fileHandle.exists())
            return false;
        if (!fileHandle.isDirectory())
            return false;
        String[] lists = fileHandle.file().list();
        if (lists == null)
            return false;
        return lists.length > 0;
    }
    private static void useFallbackDir() {
        preferredDir = GuiBase.isAndroid() ? Gdx.files.internal("fallback_skin") : Gdx.files.classpath("fallback_skin");
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
        preferredName = skinName.toLowerCase().replace(' ', '_');

        //reset hd buttons/icons
        Forge.hdbuttons = false;
        Forge.hdstart = false;

        //ensure skins directory exists
        final FileHandle dir = Gdx.files.absolute(ForgeConstants.CACHE_SKINS_DIR);
        if(preferredDir == null)
        {
            if (!isValidDirectory(dir)) {
                final FileHandle def = Gdx.files.absolute(ForgeConstants.DEFAULT_SKINS_DIR);
                if (def.exists() && def.isDirectory()) //if default skin exists
                    preferredDir = def;
                else //if skins directory doesn't exist, point to internal assets/skin directory instead for the sake of the splash screen
                    useFallbackDir();
            } else {
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
        //check preferredDir
        if (!isValidDirectory(preferredDir)) {
            useFallbackDir();
        }


        FSkinTexture.BG_TEXTURE.load(); //load background texture early for splash screen

        //load theme logo while changing skins
        Forge.getAssets().loadTexture(getSkinFile("hd_logo.png"));
        Forge.getAssets().loadTexture(getDefaultSkinFile("adv_logo.png"), new TextureLoader.TextureParameter());
        Forge.getAssets().loadTexture(getDefaultSkinFile("overlay_alpha.png"));
        Forge.getAssets().loadTexture(getDefaultSkinFile("splatter.png"));

        if (splashScreen != null) {
            final FileHandle f = getSkinFile("bg_splash.png");
            final FileHandle f2 = getSkinFile("bg_splash_hd.png"); //HD Splashscreen
            FileHandle f3 = getSkinFile("adv_bg_splash.png"); //Adventure splash
            FileHandle f4 = getSkinFile("adv_bg_texture.jpg"); //Adventure splash
            if (!f3.exists())
                f3 = getDefaultSkinFile("adv_bg_splash.png");
            if (!f4.exists())
                f4 = getDefaultSkinFile("adv_bg_texture.jpg");

            if (!f.exists()) {
                if (!skinName.equals("default")) {
                    FSkin.loadLight("default", splashScreen);
                }
                return;
            }

            try {
                int w, h;
                if (f.path().contains("fallback_skin")) {
                    Texture txSplash = Forge.getAssets().getTexture(f);
                    w = txSplash.getWidth();
                    h = txSplash.getHeight();
                    splashScreen.setSplashTexture(new TextureRegion(txSplash, 0, 0, w, h - 100));
                } else {
                    Forge.getAssets().loadTexture(f);
                    w = Forge.getAssets().getTexture(f).getWidth();
                    h = Forge.getAssets().getTexture(f).getHeight();

                    if (f2.exists()) {
                        Forge.getAssets().loadTexture(f2);
                        splashScreen.setSplashTexture(new TextureRegion(Forge.getAssets().getTexture(f2)));
                    } else {
                        splashScreen.setSplashTexture(new TextureRegion(Forge.getAssets().getTexture(f), 0, 0, w, h - 100));
                    }
                }
                Pixmap pxSplash = new Pixmap(f);
                //override splashscreen startup
                if (Forge.selector.equals("Adventure")) {
                    if (f3.exists()) {
                        Texture advSplash = Forge.getAssets().getTexture(f3, true, false);
                        w = advSplash.getWidth();
                        h = advSplash.getHeight();
                        splashScreen.setSplashTexture(new TextureRegion(advSplash, 0, 0, w, h - 100));
                        pxSplash = new Pixmap(f3);
                    }
                    if (f4.exists()) {
                        Texture advBG = Forge.getAssets().getTexture(f4, true, false);
                        advBG.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
                        splashScreen.setSplashBGTexture(advBG);
                    }
                }
                FProgressBar.BACK_COLOR = new Color(pxSplash.getPixel(25, h - 75));
                FProgressBar.FORE_COLOR = new Color(pxSplash.getPixel(75, h - 75));
                FProgressBar.SEL_BACK_COLOR = new Color(pxSplash.getPixel(25, h - 25));
                FProgressBar.SEL_FORE_COLOR = new Color(pxSplash.getPixel(75, h - 25));
            }
            catch (final Exception e) {
                //e.printStackTrace();
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

        Forge.getAssets().avatars().clear();
        Forge.getAssets().sleeves().clear();

        // Grab and test various sprite files.
        final FileHandle f1 = getDefaultSkinFile(SourceFile.ICONS.getFilename());
        final FileHandle f2 = getSkinFile(SourceFile.ICONS.getFilename());
        final FileHandle f3 = getDefaultSkinFile(SourceFile.FOILS.getFilename());
        final FileHandle f4 = getDefaultSkinFile(ForgeConstants.SPRITE_AVATARS_FILE);
        final FileHandle f5 = getSkinFile(ForgeConstants.SPRITE_AVATARS_FILE);
        final FileHandle f6 = getDefaultSkinFile(SourceFile.OLD_FOILS.getFilename());
        final FileHandle f7 = getDefaultSkinFile(ForgeConstants.SPRITE_MANAICONS_FILE);
        final FileHandle f8 = getDefaultSkinFile(ForgeConstants.SPRITE_SLEEVES_FILE);
        final FileHandle f9 = getDefaultSkinFile(ForgeConstants.SPRITE_SLEEVES2_FILE);
        final FileHandle f10 = getDefaultSkinFile(ForgeConstants.SPRITE_BORDER_FILE);
        final FileHandle f11 = getSkinFile(ForgeConstants.SPRITE_BUTTONS_FILE);
        final FileHandle f11b = getDefaultSkinFile(ForgeConstants.SPRITE_BUTTONS_FILE);
        final FileHandle f12 = getSkinFile(ForgeConstants.SPRITE_START_FILE);
        final FileHandle f12b = getDefaultSkinFile(ForgeConstants.SPRITE_START_FILE);
        final FileHandle f13 = getDefaultSkinFile(ForgeConstants.SPRITE_DECKBOX_FILE);
        final FileHandle f17 = getDefaultSkinFile(ForgeConstants.SPRITE_CRACKS_FILE);
        final FileHandle f18 = getDefaultSkinFile(ForgeConstants.SPRITE_PHYREXIAN_FILE);
        final FileHandle f19 = getDefaultSkinFile(ForgeConstants.SPRITE_CURSOR_FILE);
        final FileHandle f20 = getSkinFile(ForgeConstants.SPRITE_SLEEVES_FILE);
        final FileHandle f21 = getSkinFile(ForgeConstants.SPRITE_SLEEVES2_FILE);
        final FileHandle f22 = getDefaultSkinFile(ForgeConstants.SPRITE_ADV_BUTTONS_FILE);
        final FileHandle f23 = getSkinFile(ForgeConstants.SPRITE_ADV_BUTTONS_FILE);

        /*TODO Themeable
        final FileHandle f14 = getDefaultSkinFile(ForgeConstants.SPRITE_SETLOGO_FILE);
        final FileHandle f15 = getSkinFile(ForgeConstants.SPRITE_SETLOGO_FILE);
        final FileHandle f16 = getDefaultSkinFile(ForgeConstants.SPRITE_WATERMARK_FILE);
        */

        try {
            Forge.getAssets().loadTexture(f1);
            Pixmap adventureButtons;
            if (f23.exists()) {
                adventureButtons = new Pixmap(f23);
            } else {
                adventureButtons = new Pixmap(f22);
            }

            Pixmap preferredIcons = new Pixmap(f1);
            if (f2.exists()) {
                Forge.getAssets().loadTexture(f2);
                preferredIcons = new Pixmap(f2);
            }

            Forge.getAssets().loadTexture(f3);
            Forge.getAssets().loadTexture(f6);
            Forge.getAssets().loadTexture(f7, new TextureLoader.TextureParameter(){{genMipMaps = true;}});

            //hdbuttons
            if (f11.exists()) {
                if (!Forge.allowCardBG) {
                    Forge.hdbuttons = false;
                } else {
                    Forge.getAssets().loadTexture(f11);
                    Forge.hdbuttons = true;
                }
            } else if (f11b.exists() && Forge.allowCardBG) {
                if (FSkin.preferredName.isEmpty() || FSkin.preferredName.equalsIgnoreCase("default")) {
                    Forge.getAssets().loadTexture(f11b);
                    Forge.hdbuttons = true;
                } else {
                    Forge.hdbuttons = false;
                }
            } else { Forge.hdbuttons = false; } //how to refresh buttons when a theme don't have hd buttons?
            if (f12.exists()) {
                if (!Forge.allowCardBG) {
                    Forge.hdstart = false;
                } else {
                    Forge.getAssets().loadTexture(f12);
                    Forge.hdstart = true;
                }
            } else if (f12b.exists() && Forge.allowCardBG) {
                if (FSkin.preferredName.isEmpty() || FSkin.preferredName.equalsIgnoreCase("default")) {
                    Forge.getAssets().loadTexture(f12b);
                    Forge.hdstart = true;
                } else {
                    Forge.hdstart = false;
                }
            } else { Forge.hdstart = false; }
            //update colors
            for (final FSkinColor.Colors c : FSkinColor.Colors.values()) {
                if (c.toString().startsWith("ADV_CLR"))
                    c.setColor(new Color(adventureButtons.getPixel(c.getX(), c.getY())));
                else
                    c.setColor(new Color(preferredIcons.getPixel(c.getX(), c.getY())));
            }

            //load images
            for (FSkinImage image : FSkinImage.values()) {
                if (GuiBase.isAndroid()) {
                    if (Forge.allowCardBG)
                        image.load(preferredIcons);
                    else if (image.toString().equals("HDMULTI"))
                        image.load(preferredIcons);
                    else if (!image.toString().startsWith("HD"))
                        image.load(preferredIcons);
                } else {
                    image.load(preferredIcons);
                }
            }

            //assemble avatar textures
            int counter = 0;
            int scount = 0;
            Color pxTest;
            Pixmap pxDefaultAvatars, pxPreferredAvatars, pxDefaultSleeves, pxPreferredSleeves;

            pxDefaultAvatars = new Pixmap(f4);
            pxDefaultSleeves = new Pixmap(f8);
            //default avatar
            Forge.getAssets().loadTexture(f4);
            //sleeves first set
            Forge.getAssets().loadTexture(f8);
            //preferred avatar
            if (f5.exists()) {
                pxPreferredAvatars = new Pixmap(f5);
                Forge.getAssets().loadTexture(f5);

                final int pw = pxPreferredAvatars.getWidth();
                final int ph = pxPreferredAvatars.getHeight();

                for (int j = 0; j < ph; j += 100) {
                    for (int i = 0; i < pw; i += 100) {
                        if (i == 0 && j == 0) { continue; }
                        pxTest = new Color(pxPreferredAvatars.getPixel(i + 50, j + 50));
                        if (pxTest.a == 0) { continue; }
                        Forge.getAssets().avatars().put(counter++, new TextureRegion(Forge.getAssets().getTexture(f5), i, j, 100, 100));
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
                        Forge.getAssets().avatars().put(counter++, new TextureRegion(Forge.getAssets().getTexture(f4), i, j, 100, 100));
                    }
                }
            }
            if (f20.exists()) {
                pxPreferredSleeves = new Pixmap(f20);
                Forge.getAssets().loadTexture(f20);

                final int sw = pxPreferredSleeves.getWidth();
                final int sh = pxPreferredSleeves.getHeight();

                for (int j = 0; j < sh; j += 500) {
                    for (int i = 0; i < sw; i += 360) {
                        pxTest = new Color(pxPreferredSleeves.getPixel(i + 180, j + 250));
                        if (pxTest.a == 0) { continue; }
                        Forge.getAssets().sleeves().put(scount++, new TextureRegion(Forge.getAssets().getTexture(f20), i, j, 360, 500));
                    }
                }
                pxPreferredSleeves.dispose();
            } else {
                final int sw = pxDefaultSleeves.getWidth();
                final int sh = pxDefaultSleeves.getHeight();

                for (int j = 0; j < sh; j += 500) {
                    for (int i = 0; i < sw; i += 360) {
                        pxTest = new Color(pxDefaultSleeves.getPixel(i + 180, j + 250));
                        if (pxTest.a == 0) { continue; }
                        Forge.getAssets().sleeves().put(scount++, new TextureRegion(Forge.getAssets().getTexture(f8), i, j, 360, 500));
                    }
                }
            }
            if (f21.exists()) {
                pxPreferredSleeves = new Pixmap(f21);
                Forge.getAssets().loadTexture(f21);

                final int sw = pxPreferredSleeves.getWidth();
                final int sh = pxPreferredSleeves.getHeight();

                for (int j = 0; j < sh; j += 500) {
                    for (int i = 0; i < sw; i += 360) {
                        pxTest = new Color(pxPreferredSleeves.getPixel(i + 180, j + 250));
                        if (pxTest.a == 0) { continue; }
                        Forge.getAssets().sleeves().put(scount++, new TextureRegion(Forge.getAssets().getTexture(f21), i, j, 360, 500));
                    }
                }
                pxPreferredSleeves.dispose();
            } else {
                //re init second set of sleeves
                pxDefaultSleeves = new Pixmap(f9);
                Forge.getAssets().loadTexture(f9);

                final int sw2 = pxDefaultSleeves.getWidth();
                final int sh2 = pxDefaultSleeves.getHeight();

                for (int j = 0; j < sh2; j += 500) {
                    for (int i = 0; i < sw2; i += 360) {
                        pxTest = new Color(pxDefaultSleeves.getPixel(i + 180, j + 250));
                        if (pxTest.a == 0) { continue; }
                        Forge.getAssets().sleeves().put(scount++, new TextureRegion(Forge.getAssets().getTexture(f9), i, j, 360, 500));
                    }
                }
            }

            //cracks
            Forge.getAssets().loadTexture(f17);
            int crackCount = 0;
            for (int j = 0; j < 4; j++) {
                int x = j * 200;
                for(int i = 0; i < 4; i++) {
                    int y = i * 279;
                    Forge.getAssets().cracks().put(crackCount++, new TextureRegion(Forge.getAssets().getTexture(f17), x, y, 200, 279));
                }
            }

            //borders
            Forge.getAssets().loadTexture(f10);
            Forge.getAssets().borders().put(0, new TextureRegion(Forge.getAssets().getTexture(f10), 2, 2, 672, 936));
            Forge.getAssets().borders().put(1, new TextureRegion(Forge.getAssets().getTexture(f10), 676, 2, 672, 936));
            //deckboxes
            Forge.getAssets().loadTexture(f13);
            //gold bg
            Forge.getAssets().deckbox().put(0, new TextureRegion(Forge.getAssets().getTexture(f13), 2, 2, 488, 680));
            //deck box for card art
            Forge.getAssets().deckbox().put(1, new TextureRegion(Forge.getAssets().getTexture(f13), 492, 2, 488, 680));
            //generic deck box
            Forge.getAssets().deckbox().put(2, new TextureRegion(Forge.getAssets().getTexture(f13), 982, 2, 488, 680));
            //cursor
            Forge.getAssets().loadTexture(f19);
            Forge.getAssets().cursor().put(0, new TextureRegion(Forge.getAssets().getTexture(f19), 0, 0, 32, 32)); //default
            Forge.getAssets().cursor().put(1, new TextureRegion(Forge.getAssets().getTexture(f19), 32, 0, 32, 32)); //magnify on
            Forge.getAssets().cursor().put(2, new TextureRegion(Forge.getAssets().getTexture(f19), 64, 0, 32, 32)); // magnify off

            Forge.setCursor(Forge.getAssets().cursor().get(0), "0");
            //set adv_progress bar colors
            FProgressBar.ADV_BACK_COLOR = new Color(adventureButtons.getPixel(FSkinColor.Colors.ADV_CLR_BORDERS.getX(), FSkinColor.Colors.ADV_CLR_BORDERS.getY()));
            FProgressBar.ADV_FORE_COLOR = new Color(adventureButtons.getPixel(FSkinColor.Colors.ADV_CLR_THEME.getX(), FSkinColor.Colors.ADV_CLR_THEME.getY()));
            FProgressBar.ADV_SEL_BACK_COLOR = new Color(adventureButtons.getPixel(FSkinColor.Colors.ADV_CLR_ACTIVE.getX(), FSkinColor.Colors.ADV_CLR_ACTIVE.getY()));
            FProgressBar.ADV_SEL_FORE_COLOR = new Color(adventureButtons.getPixel(FSkinColor.Colors.ADV_CLR_BORDERS.getX(), FSkinColor.Colors.ADV_CLR_BORDERS.getY()));

            preferredIcons.dispose();
            pxDefaultAvatars.dispose();
            pxDefaultSleeves.dispose();
            adventureButtons.dispose();
        }
        catch (final Exception e) {
            System.err.println("FSkin$loadFull: Missing a sprite (default icons, "
                    + "preferred icons, or foils.");
            //e.printStackTrace();
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
        return Forge.getAssets().images();
    }

    public static Map<Integer, TextureRegion> getAvatars() {
        return Forge.getAssets().avatars();
    }

    public static Map<Integer, TextureRegion> getSleeves() {
        return Forge.getAssets().sleeves();
    }

    public static Map<Integer, TextureRegion> getCracks() {
        return Forge.getAssets().cracks();
    }

    public static Map<Integer, TextureRegion> getBorders() {
        return Forge.getAssets().borders();
    }

    public static Map<Integer, TextureRegion> getDeckbox() {
        return Forge.getAssets().deckbox();
    }

    public static Map<Integer, TextureRegion> getCursor() {
        return Forge.getAssets().cursor();
    }

    public static boolean isLoaded() { return loaded; }
}
