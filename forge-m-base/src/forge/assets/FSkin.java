package forge.assets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import forge.assets.FSkinImage.SourceFile;

public class FSkin {
    private static final String
        FILE_SKINS_DIR = "skins/",
        FILE_AVATAR_SPRITE = "sprite_avatars.png",
        DEFAULT_DIR = FILE_SKINS_DIR + "default/";

    private static final Map<FSkinImage, TextureRegion> images = new HashMap<FSkinImage, TextureRegion>();
    private static final Map<Integer, TextureRegion> avatars = new HashMap<Integer, TextureRegion>();

    private static ArrayList<String> allSkins;
    private static String preferredDir;
    private static String preferredName;
    private static boolean loaded = false;

    public static void changeSkin(final String skinName) {
        /*final ForgePreferences prefs = Singletons.getModel().getPreferences();
        if (skinName.equals(prefs.getPref(FPref.UI_SKIN))) { return; }

        //save skin preference
        prefs.setPref(FPref.UI_SKIN, skinName);
        prefs.save();*/

        //load skin
        loaded = false; //reset this temporarily until end of loadFull()
        loadLight(skinName, false);
        loadFull(false);
    }

    /*
     * Loads a "light" version of FSkin, just enough for the splash screen:
     * skin name. Generates custom skin settings, fonts, and backgrounds.
     * 
     * 
     * @param skinName
     *            the skin name
     */
    public static void loadLight(final String skinName, final boolean onInit) {
        if (onInit) {
            if (allSkins == null) { //initialize
                allSkins = new ArrayList<String>();
                ArrayList<String> skinDirectoryNames = getSkinDirectoryNames();
                for (int i = 0; i < skinDirectoryNames.size(); i++) {
                    allSkins.add(skinDirectoryNames.get(i).replace('_', ' '));
                }
                Collections.sort(allSkins);
            }
        }

        images.clear();

        // Non-default (preferred) skin name and dir.
        preferredName = skinName.toLowerCase().replace(' ', '_');
        preferredDir = FILE_SKINS_DIR + preferredName + "/";

        if (onInit) {
            final FileHandle f = Gdx.files.internal(preferredDir + SourceFile.SPLASH.getFilename());
            if (!f.exists()) {
                if (!skinName.equals("default")) {
                    FSkin.loadLight("default", onInit);
                }
                return;
            }

            try {
                FSkinImage image = FSkinImage.BG_SPLASH;
                Texture texture = new Texture(f);

                final int h = texture.getHeight();
                final int w = texture.getWidth();

                images.put(FSkinImage.BG_SPLASH, new TextureRegion(texture, image.getX(), image.getY(),
                        image.getWidth(w), image.getHeight(h)));

                /*UIManager.put("ProgressBar.background", new Color(img.getRGB(25, h - 75)));
                UIManager.put("ProgressBar.selectionBackground", new Color(img.getRGB(75, h - 75)));
                UIManager.put("ProgressBar.foreground", new Color(img.getRGB(25, h - 25)));
                UIManager.put("ProgressBar.selectionForeground", new Color(img.getRGB(75, h - 25)));
                UIManager.put("ProgressBar.border", new LineBorder(Color.BLACK, 0));*/
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
    public static void loadFull(final boolean onInit) {
        if (onInit) {
            // Preferred skin name must be called via loadLight() method,
            // which does some cleanup and init work.
            if (FSkin.preferredName.isEmpty()) { FSkin.loadLight("default", onInit); }
        }

        avatars.clear();

        final Map<String, Texture> textures = new HashMap<String, Texture>();

        //FView.SINGLETON_INSTANCE.setSplashProgessBarMessage("Processing image sprites: ", 5);

        // Grab and test various sprite files.
        final FileHandle f1 = Gdx.files.internal(DEFAULT_DIR + SourceFile.ICONS.getFilename());
        final FileHandle f2 = Gdx.files.internal(preferredDir + SourceFile.ICONS.getFilename());
        final FileHandle f3 = Gdx.files.internal(DEFAULT_DIR + SourceFile.FOILS.getFilename());
        final FileHandle f4 = Gdx.files.internal(DEFAULT_DIR + FILE_AVATAR_SPRITE);
        final FileHandle f5 = Gdx.files.internal(preferredDir + FILE_AVATAR_SPRITE);
        final FileHandle f6 = Gdx.files.internal(DEFAULT_DIR + SourceFile.OLD_FOILS.getFilename());

        try {
            textures.put(f1.path(), new Texture(f1));
            //FView.SINGLETON_INSTANCE.incrementSplashProgessBar(++p);
            textures.put(f2.path(), new Texture(f2));
            Pixmap preferredIcons = new Pixmap(f2);
            //FView.SINGLETON_INSTANCE.incrementSplashProgessBar(++p);
            textures.put(f3.path(), new Texture(f3));
            //FView.SINGLETON_INSTANCE.incrementSplashProgessBar(++p);
            if (f6.exists()) {
                textures.put(f6.path(), new Texture(f6));
            }
            else {
                textures.put(f6.path(), textures.get(f3.path()));
            }
            //FView.SINGLETON_INSTANCE.incrementSplashProgessBar(++p);

            //update colors
            for (final FSkinColor.Colors c : FSkinColor.Colors.values()) {
                c.setColor(new Color(preferredIcons.getPixel(c.getX(), c.getY())));
            }

            //add images besides splash background
            for (FSkinImage image : FSkinImage.values()) {
                if (image != FSkinImage.BG_SPLASH) {
                    TextureRegion textureRegion = loadTextureRegion(image, textures, preferredIcons);
                    if (textureRegion != null) {
                        images.put(image, textureRegion);
                    }
                }
            }

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

            //FView.SINGLETON_INSTANCE.incrementSplashProgessBar(++p);
        }
        catch (final Exception e) {
            System.err.println("FSkin$loadFull: Missing a sprite (default icons, "
                    + "preferred icons, or foils.");
            e.printStackTrace();
        }

        // Update fonts if needed
        if (!onInit) {
            FSkinFont.updateAll();
        }

        // Run through enums and load their coords.
        FSkinColor.updateAll();

        // Images loaded; can start UI init.
        //FView.SINGLETON_INSTANCE.setSplashProgessBarMessage("Creating display components.");
        loaded = true;

        //establish encoding symbols
        /*addEncodingSymbol("W", ManaImages.IMG_WHITE);
        addEncodingSymbol("U", ManaImages.IMG_BLUE);
        addEncodingSymbol("B", ManaImages.IMG_BLACK);
        addEncodingSymbol("R", ManaImages.IMG_RED);
        addEncodingSymbol("G", ManaImages.IMG_GREEN);
        addEncodingSymbol("W/U", ManaImages.IMG_WHITE_BLUE);
        addEncodingSymbol("U/B", ManaImages.IMG_BLUE_BLACK);
        addEncodingSymbol("B/R", ManaImages.IMG_BLACK_RED);
        addEncodingSymbol("R/G", ManaImages.IMG_RED_GREEN);
        addEncodingSymbol("G/W", ManaImages.IMG_GREEN_WHITE);
        addEncodingSymbol("W/B", ManaImages.IMG_WHITE_BLACK);
        addEncodingSymbol("U/R", ManaImages.IMG_BLUE_RED);
        addEncodingSymbol("B/G", ManaImages.IMG_BLACK_GREEN);
        addEncodingSymbol("R/W", ManaImages.IMG_RED_WHITE);
        addEncodingSymbol("G/U", ManaImages.IMG_GREEN_BLUE);
        addEncodingSymbol("2/W", ManaImages.IMG_2W);
        addEncodingSymbol("2/U", ManaImages.IMG_2U);
        addEncodingSymbol("2/B", ManaImages.IMG_2B);
        addEncodingSymbol("2/R", ManaImages.IMG_2R);
        addEncodingSymbol("2/G", ManaImages.IMG_2G);
        addEncodingSymbol("W/P", ManaImages.IMG_PHRYX_WHITE);
        addEncodingSymbol("U/P", ManaImages.IMG_PHRYX_BLUE);
        addEncodingSymbol("B/P", ManaImages.IMG_PHRYX_BLACK);
        addEncodingSymbol("R/P", ManaImages.IMG_PHRYX_RED);
        addEncodingSymbol("G/P", ManaImages.IMG_PHRYX_GREEN);
        for (int i = 0; i <= 20; i++) {
            addEncodingSymbol(String.valueOf(i), ColorlessManaImages.valueOf("IMG_" + i));
        }
        addEncodingSymbol("X", ColorlessManaImages.IMG_X);
        addEncodingSymbol("Y", ColorlessManaImages.IMG_Y);
        addEncodingSymbol("Z", ColorlessManaImages.IMG_Z);
        addEncodingSymbol("C", GameplayImages.IMG_CHAOS);
        addEncodingSymbol("Q", GameplayImages.IMG_UNTAP);
        addEncodingSymbol("S", GameplayImages.IMG_SNOW);
        addEncodingSymbol("T", GameplayImages.IMG_TAP);*/
    }

    private static TextureRegion loadTextureRegion(FSkinImage image, Map<String, Texture> textures, Pixmap preferredIcons) {
        SourceFile sourceFile = image.getSourceFile();
        String filename = sourceFile.getFilename();
        String preferredFile = preferredDir + filename;
        Texture texture = textures.get(preferredFile);
        if (texture == null) {
            FileHandle file = Gdx.files.internal(preferredFile);
            if (file.exists()) {
                try {
                    texture = new Texture(file);
                }
                catch (final Exception e) {
                    System.err.println("Failed to load skin file: " + preferredFile);
                    e.printStackTrace();
                }
            }
        }
        if (texture != null) {
            int fullWidth = texture.getWidth();
            int fullHeight = texture.getHeight();
            int x0 = image.getX();
            int y0 = image.getY();
            int w0 = image.getWidth(fullWidth);
            int h0 = image.getHeight(fullHeight);

            if (sourceFile != SourceFile.ICONS) { //just return region for preferred file if not icons file
                return new TextureRegion(texture, x0, y0, w0, h0);
            }
            else {
                // Test if requested sub-image in inside bounds of preferred sprite.
                // (Height and width of preferred sprite were set in loadFontAndImages.)
                if (x0 + w0 <= fullWidth && y0 + h0 <= fullHeight) {
                    // Test if various points of requested sub-image are transparent.
                    // If any return true, image exists.
                    int x = 0, y = 0;
                    Color c;
        
                    // Center
                    x = (x0 + w0 / 2);
                    y = (y0 + h0 / 2);
                    c = new Color(preferredIcons.getPixel(x, y));
                    if (c.a != 0) { return new TextureRegion(texture, x0, y0, w0, h0); }
        
                    x += 2;
                    y += 2;
                    c = new Color(preferredIcons.getPixel(x, y));
                    if (c.a != 0) { return new TextureRegion(texture, x0, y0, w0, h0); }
        
                    x -= 4;
                    c = new Color(preferredIcons.getPixel(x, y));
                    if (c.a != 0) { return new TextureRegion(texture, x0, y0, w0, h0); }
        
                    y -= 4;
                    c = new Color(preferredIcons.getPixel(x, y));
                    if (c.a != 0) { return new TextureRegion(texture, x0, y0, w0, h0); }
        
                    x += 4;
                    c = new Color(preferredIcons.getPixel(x, y));
                    if (c.a != 0) { return new TextureRegion(texture, x0, y0, w0, h0); }
                }
            }
        }

        //use default file if can't use preferred file
        String defaultFile = DEFAULT_DIR + filename;
        texture = textures.get(defaultFile);
        if (texture == null) {
            FileHandle file = Gdx.files.internal(defaultFile);
            if (file.exists()) {
                try {
                    texture = new Texture(file);
                }
                catch (final Exception e) {
                    System.err.println("Failed to load skin file: " + defaultFile);
                    e.printStackTrace();
                }
            }
        }
        if (texture != null) {
            return new TextureRegion(texture, image.getX(), image.getY(),
                    image.getWidth(texture.getWidth()), image.getHeight(texture.getHeight()));
        }
        return null;
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
     * Gets the skins.
     *
     * @return the skins
     */
    public static ArrayList<String> getSkinDirectoryNames() {
        final ArrayList<String> mySkins = new ArrayList<String>();

        final FileHandle dir;
        if (Gdx.app.getType() == ApplicationType.Desktop) {
            dir = Gdx.files.internal("./bin/" + FILE_SKINS_DIR); //needed to iterate over directory for Desktop
        }
        else {
            dir = Gdx.files.internal(FILE_SKINS_DIR);
        }
        if (!dir.exists() || !dir.isDirectory()) {
            System.err.println("FSkin > can't find skins directory!");
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

    public static Map<FSkinImage, TextureRegion> getImages() {
        return images;
    }

    public static Map<Integer, TextureRegion> getAvatars() {
        return avatars;
    }

    public static boolean isLoaded() { return loaded; }
}
