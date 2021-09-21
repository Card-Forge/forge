package forge.assets;

import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.Gdx;
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
import forge.toolbox.FProgressBar;
import forge.util.WordUtil;

public class FSkin {
    private static final Map<FSkinProp, FSkinImage> images = new HashMap<>(512);
    private static final Map<Integer, TextureRegion> avatars = new HashMap<>(150);
    private static final Map<Integer, TextureRegion> sleeves = new HashMap<>(64);
    private static final Map<Integer, TextureRegion> cracks = new HashMap<>(16);
    private static final Map<Integer, TextureRegion> borders = new HashMap<>();
    private static final Map<Integer, TextureRegion> deckbox = new HashMap<>();

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

        //load skin
        loaded = false; //reset this temporarily until end of loadFull()

        final LoadingOverlay loader = new LoadingOverlay("Loading new theme...");
        loader.show(); //show loading overlay then delay running remaining logic so UI can respond
        FThreads.invokeInBackgroundThread(new Runnable() {
            @Override
            public void run() {
                FThreads.invokeInEdtLater(new Runnable() {
                    @Override
                    public void run() {
                        loadLight(skinName, null);
                        loadFull(null);
                        loader.setCaption("Loading fonts...");
                        FThreads.invokeInBackgroundThread(new Runnable() {
                            @Override
                            public void run() {
                                FSkinFont.deleteCachedFiles(); //delete cached font files so font can be update for new skin
                                FSkinFont.updateAll();
                                //CardImageRenderer.forceStaticFieldUpdate();
                                FThreads.invokeInEdtLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        loader.hide();
                                    }
                                });
                            }
                        });
                    }
                });
            }
        });
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
        if (!dir.exists() || !dir.isDirectory()) {
            //if skins directory doesn't exist, point to internal assets/skin directory instead for the sake of the splash screen
            preferredDir = Gdx.files.internal("fallback_skin");
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
            preferredDir = Gdx.files.absolute(preferredName.equals("default") ? ForgeConstants.BASE_SKINS_DIR + preferredName : ForgeConstants.CACHE_SKINS_DIR + preferredName);
            if (!preferredDir.exists() || !preferredDir.isDirectory()) {
                preferredDir.mkdirs();
            }
        }

        FSkinTexture.BG_TEXTURE.load(); //load background texture early for splash screen

        //load theme logo while changing skins
        final FileHandle theme_logo = getSkinFile("hd_logo.png");
        if (theme_logo.exists()) {
            Texture txOverlay = new Texture(theme_logo, true);
            txOverlay.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
            hdLogo = txOverlay;
        } else {
            hdLogo = null;
        }
        final FileHandle duals_overlay = getDefaultSkinFile("overlay_alpha.png");
        if (duals_overlay.exists()) {
            Texture txAlphaLines = new Texture(duals_overlay, true);
            txAlphaLines.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
            overlay_alpha = txAlphaLines;
        } else {
            overlay_alpha = null;
        }
        final FileHandle splatter_overlay = getDefaultSkinFile("splatter.png");
        if (splatter_overlay.exists()) {
            Texture txSplatter = new Texture(splatter_overlay, true);
            txSplatter.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
            splatter = txSplatter;
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
                Texture txSplash = new Texture(f);
                final int w = txSplash.getWidth();
                final int h = txSplash.getHeight();

                if (f2.exists()) {
                    Texture txSplashHD = new Texture(f2, true);
                    txSplashHD.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
                    splashScreen.setBackground(new TextureRegion(txSplashHD));
                } else {
                    splashScreen.setBackground(new TextureRegion(txSplash, 0, 0, w, h - 100));
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

        boolean textureFilter = Forge.isTextureFilteringEnabled();

        final Map<String, Texture> textures = new HashMap<>();

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

        /*TODO Themeable
        final FileHandle f14 = getDefaultSkinFile(ForgeConstants.SPRITE_SETLOGO_FILE);
        final FileHandle f15 = getSkinFile(ForgeConstants.SPRITE_SETLOGO_FILE);
        final FileHandle f16 = getDefaultSkinFile(ForgeConstants.SPRITE_WATERMARK_FILE);
        */

        try {
            textures.put(f1.path(), new Texture(f1));
            Pixmap preferredIcons = new Pixmap(f1);
            if (f2.exists()) {
                textures.put(f2.path(), new Texture(f2));
                preferredIcons = new Pixmap(f2);
            }

            textures.put(f3.path(), new Texture(f3));
            if (f6.exists()) {
                textures.put(f6.path(), new Texture(f6));
            }
            else {
                textures.put(f6.path(), textures.get(f3.path()));
            }
            if (f7.exists()){
                Texture t = new Texture(f7, true);
                //t.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
                textures.put(f7.path(), t);
            }

            //hdbuttons
            if (f11.exists()) {
                if (GuiBase.isAndroid() && Forge.totalDeviceRAM <5000) {
                    Forge.hdbuttons = false;
                } else {
                    Texture t = new Texture(f11, true);
                    t.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
                    textures.put(f11.path(), t);
                    Forge.hdbuttons = true;
                }
            } else { Forge.hdbuttons = false; } //how to refresh buttons when a theme don't have hd buttons?
            if (f12.exists()) {
                if (GuiBase.isAndroid() && Forge.totalDeviceRAM <5000) {
                    Forge.hdstart = false;
                } else {
                    Texture t = new Texture(f12, true);
                    t.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
                    textures.put(f12.path(), t);
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
                        image.load(textures, preferredIcons);
                    else if (image.toString().equals("HDMULTI"))
                        image.load(textures, preferredIcons);
                    else if (!image.toString().startsWith("HD"))
                        image.load(textures, preferredIcons);
                } else {
                    image.load(textures, preferredIcons);
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
            Pixmap pxDefaultAvatars, pxPreferredAvatars, pxDefaultSleeves, pxCracks;
            Texture txDefaultAvatars, txPreferredAvatars, txDefaultSleeves, txCracks;

            pxDefaultAvatars = new Pixmap(f4);
            pxDefaultSleeves = new Pixmap(f8);
            txDefaultAvatars = new Texture(f4, textureFilter);
            if (textureFilter)
                txDefaultAvatars.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
            txDefaultSleeves = new Texture(f8, textureFilter);
            if (textureFilter)
                txDefaultSleeves.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);

            if (f5.exists()) {
                pxPreferredAvatars = new Pixmap(f5);
                txPreferredAvatars = new Texture(f5, textureFilter);
                if (textureFilter)
                    txPreferredAvatars.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);

                final int pw = pxPreferredAvatars.getWidth();
                final int ph = pxPreferredAvatars.getHeight();

                for (int j = 0; j < ph; j += 100) {
                    for (int i = 0; i < pw; i += 100) {
                        if (i == 0 && j == 0) { continue; }
                        pxTest = new Color(pxPreferredAvatars.getPixel(i + 50, j + 50));
                        if (pxTest.a == 0) { continue; }
                        FSkin.avatars.put(counter++, new TextureRegion(txPreferredAvatars, i, j, 100, 100));
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
                        FSkin.avatars.put(counter++, new TextureRegion(txDefaultAvatars, i, j, 100, 100));
                    }
                }
            }

            final int aw = pxDefaultAvatars.getWidth();
            final int ah = pxDefaultAvatars.getHeight();

            for (int j = 0; j < ah; j += 100) {
                for (int i = 0; i < aw; i += 100) {
                    if (i == 0 && j == 0) { continue; }
                    pxTest = new Color(pxDefaultAvatars.getPixel(i + 50, j + 50));
                    if (pxTest.a == 0) { continue; }
                    FSkin.avatars.put(counter++, new TextureRegion(txDefaultAvatars, i, j, 100, 100));
                }
            }


            final int sw = pxDefaultSleeves.getWidth();
            final int sh = pxDefaultSleeves.getHeight();

            for (int j = 0; j < sh; j += 500) {
                for (int i = 0; i < sw; i += 360) {
                    pxTest = new Color(pxDefaultSleeves.getPixel(i + 180, j + 250));
                    if (pxTest.a == 0) { continue; }
                    FSkin.sleeves.put(scount++, new TextureRegion(txDefaultSleeves, i, j, 360, 500));
                }
            }

            //re init second set of sleeves
            pxDefaultSleeves = new Pixmap(f9);
            txDefaultSleeves = new Texture(f9, textureFilter);
            if (textureFilter)
                txDefaultSleeves.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);

            final int sw2 = pxDefaultSleeves.getWidth();
            final int sh2 = pxDefaultSleeves.getHeight();

            for (int j = 0; j < sh2; j += 500) {
                for (int i = 0; i < sw2; i += 360) {
                    pxTest = new Color(pxDefaultSleeves.getPixel(i + 180, j + 250));
                    if (pxTest.a == 0) { continue; }
                    FSkin.sleeves.put(scount++, new TextureRegion(txDefaultSleeves, i, j, 360, 500));
                }
            }
            //cracks
            pxCracks = new Pixmap(f17);
            txCracks = new Texture(f17, textureFilter);
            int crackCount = 0;
            if (textureFilter)
                txCracks.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);

            for (int j = 0; j < 4; j++) {
                int x = j * 200;
                for(int i = 0; i < 4; i++) {
                    int y = i * 279;
                    FSkin.cracks.put(crackCount++, new TextureRegion(txCracks, x, y, 200, 279));
                }
            }

            //borders
            Texture bordersBW = new Texture(f10);
            FSkin.borders.put(0, new TextureRegion(bordersBW, 2, 2, 672, 936));
            FSkin.borders.put(1, new TextureRegion(bordersBW, 676, 2, 672, 936));
            //deckboxes
            Texture deckboxes = new Texture(f13, textureFilter);
            if (textureFilter)
                deckboxes.setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
            //gold bg
            FSkin.deckbox.put(0, new TextureRegion(deckboxes, 2, 2, 488, 680));
            //deck box for card art
            FSkin.deckbox.put(1, new TextureRegion(deckboxes, 492, 2, 488, 680));
            //generic deck box
            FSkin.deckbox.put(2, new TextureRegion(deckboxes, 982, 2, 488, 680));

            preferredIcons.dispose();
            pxDefaultAvatars.dispose();
            pxDefaultSleeves.dispose();
            pxCracks.dispose();
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

    public static boolean isLoaded() { return loaded; }
}
