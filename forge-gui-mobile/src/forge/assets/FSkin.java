package forge.assets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.text.WordUtils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import forge.assets.FSkinImage.SourceFile;
import forge.card.CardFaceSymbols;
import forge.model.FModel;
import forge.properties.ForgeConstants;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;
import forge.screens.SplashScreen;
import forge.toolbox.FProgressBar;
import forge.util.FileUtil;

public class FSkin {
    private static final String FONT_CACHE_DIR = ForgeConstants.CACHE_DIR + "fonts/";
    private static final Map<FSkinProp, FSkinImage> images = new HashMap<FSkinProp, FSkinImage>();
    private static final Map<Integer, TextureRegion> avatars = new HashMap<Integer, TextureRegion>();

    private static ArrayList<String> allSkins;
    private static String preferredDir;
    private static String preferredFontDir;
    private static String preferredName;
    private static boolean loaded = false;
    private static boolean needReloadAfterAssetsDownloaded = false;

    public static void changeSkin(final String skinName) {
        final ForgePreferences prefs = FModel.getPreferences();
        if (skinName.equals(prefs.getPref(FPref.UI_SKIN))) { return; }

        //save skin preference
        prefs.setPref(FPref.UI_SKIN, skinName);
        prefs.save();

        //load skin
        loaded = false; //reset this temporarily until end of loadFull()
        loadLight(skinName, null);
        loadFull(null);
    }

    /*
     * Loads a "light" version of FSkin, just enough for the splash screen:
     * skin name. Generates custom skin settings, fonts, and backgrounds.
     * 
     * 
     * @param skinName
     *            the skin name
     */
    public static void loadLight(final String skinName, final SplashScreen splashScreen) {
        if (splashScreen != null) {
            if (allSkins == null) { //initialize
                allSkins = new ArrayList<String>();
                ArrayList<String> skinDirectoryNames = getSkinDirectoryNames();
                for (int i = 0; i < skinDirectoryNames.size(); i++) {
                    allSkins.add(WordUtils.capitalize(skinDirectoryNames.get(i).replace('_', ' ')));
                }
                Collections.sort(allSkins);
            }
        }

        // Non-default (preferred) skin name and dir.
        preferredName = skinName.toLowerCase().replace(' ', '_');
        preferredDir = ForgeConstants.SKINS_DIR + preferredName + "/";
        preferredFontDir = FONT_CACHE_DIR + preferredName + "/";

        FileUtil.ensureDirectoryExists(preferredFontDir);

        FSkinTexture.BG_TEXTURE.load(preferredDir, ForgeConstants.DEFAULT_SKINS_DIR); //load background texture early for splash screen

        if (splashScreen != null) {
            final FileHandle f = Gdx.files.absolute(preferredDir + "bg_splash.png");
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

                splashScreen.setBackground(new TextureRegion(txSplash, 0, 0, w, h - 100));

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

        final Map<String, Texture> textures = new HashMap<String, Texture>();

        // Grab and test various sprite files.
        String defaultDir = ForgeConstants.DEFAULT_SKINS_DIR;
        final FileHandle f1 = Gdx.files.absolute(defaultDir + SourceFile.ICONS.getFilename());
        final FileHandle f2 = Gdx.files.absolute(preferredDir + SourceFile.ICONS.getFilename());
        final FileHandle f3 = Gdx.files.absolute(defaultDir + SourceFile.FOILS.getFilename());
        final FileHandle f4 = Gdx.files.absolute(defaultDir + ForgeConstants.SPRITE_AVATARS_FILE);
        final FileHandle f5 = Gdx.files.absolute(preferredDir + ForgeConstants.SPRITE_AVATARS_FILE);
        final FileHandle f6 = Gdx.files.absolute(defaultDir + SourceFile.OLD_FOILS.getFilename());

        try {
            textures.put(f1.path(), new Texture(f1));
            textures.put(f2.path(), new Texture(f2));
            Pixmap preferredIcons = new Pixmap(f2);
            textures.put(f3.path(), new Texture(f3));
            if (f6.exists()) {
                textures.put(f6.path(), new Texture(f6));
            }
            else {
                textures.put(f6.path(), textures.get(f3.path()));
            }

            //update colors
            for (final FSkinColor.Colors c : FSkinColor.Colors.values()) {
                c.setColor(new Color(preferredIcons.getPixel(c.getX(), c.getY())));
            }

            //load images
            for (FSkinImage image : FSkinImage.values()) {
                image.load(preferredDir, ForgeConstants.DEFAULT_SKINS_DIR, textures, preferredIcons);
            }
            FSkinTexture.BG_MATCH.load(preferredDir, ForgeConstants.DEFAULT_SKINS_DIR);

            //assemble avatar textures
            int counter = 0;
            Color pxTest;
            Pixmap pxDefaultAvatars, pxPreferredAvatars;
            Texture txDefaultAvatars, txPreferredAvatars;

            pxDefaultAvatars = new Pixmap(f4);
            txDefaultAvatars = new Texture(f4);

            if (f5.exists()) {
                pxPreferredAvatars = new Pixmap(f5);
                txPreferredAvatars = new Texture(f5);

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

            preferredIcons.dispose();
            pxDefaultAvatars.dispose();
        }
        catch (final Exception e) {
            System.err.println("FSkin$loadFull: Missing a sprite (default icons, "
                    + "preferred icons, or foils.");
            e.printStackTrace();
        }

        // Update fonts if needed
        if (splashScreen == null) {
            FSkinFont.updateAll();
        }

        // Run through enums and load their coords.
        FSkinColor.updateAll();

        // Images loaded; can start UI init.
        loaded = true;

        if (splashScreen != null) {
            CardFaceSymbols.loadImages();
        }
    }

    public static boolean assetsDownloadNeeded() {
        return needReloadAfterAssetsDownloaded;
    }

    public static void reloadAfterAssetsDownload(SplashScreen splashScreen) {
        needReloadAfterAssetsDownloaded = false;
        allSkins = null;
        loadLight(preferredName, splashScreen);
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
     * Gets the directory.
     * 
     * @return Path of directory for the current skin.
     */
    public static String getDir() {
        return FSkin.preferredDir;
    }

    /**
     * Gets the directory where fonts should be cached
     * 
     * @return Path of font cache directory for the current skin.
     */
    public static String getFontDir() {
        return preferredFontDir;
    }

    /**
     * Gets the skins.
     *
     * @return the skins
     */
    public static ArrayList<String> getSkinDirectoryNames() {
        final ArrayList<String> mySkins = new ArrayList<String>();

        final FileHandle dir = Gdx.files.absolute(ForgeConstants.SKINS_DIR);
        if (!dir.exists() || !dir.isDirectory()) {
            //if skins directory doesn't exists, create a minimum directory containing skin files for the splash screen
            FileUtil.ensureDirectoryExists(ForgeConstants.DEFAULT_SKINS_DIR);
            final FileHandle defaultDir = Gdx.files.absolute(ForgeConstants.DEFAULT_SKINS_DIR);
            Gdx.files.internal("bg_splash.png").copyTo(defaultDir.child("bg_splash.png"));
            Gdx.files.internal("bg_texture.jpg").copyTo(defaultDir.child("bg_texture.jpg"));
            Gdx.files.internal("font1.ttf").copyTo(defaultDir.child("font1.ttf"));
            mySkins.add("default");
            needReloadAfterAssetsDownloaded = true; //flag that skins need to be reloaded after assets downloaded
        }
        else {
            for (FileHandle skinFile : dir.list()) {
                String skinName = skinFile.name();
                if (skinName.equalsIgnoreCase(".svn")) { continue; }
                if (skinName.equalsIgnoreCase(".DS_Store")) { continue; }
                mySkins.add(skinName);
            }
        }

        return mySkins;
    }

    public static Iterable<String> getAllSkins() {
        return allSkins;
    }

    public static Map<FSkinProp, FSkinImage> getImages() {
        return images;
    }

    public static Map<Integer, TextureRegion> getAvatars() {
        return avatars;
    }

    public static boolean isLoaded() { return loaded; }
}
