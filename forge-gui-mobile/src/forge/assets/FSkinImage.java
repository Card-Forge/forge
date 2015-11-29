package forge.assets;

import java.util.Map;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import forge.Graphics;
import forge.properties.ForgeConstants;
import forge.util.ImageUtil;

/** Properties of various components that make up the skin.
 * This interface allows all enums to be under the same roof.
 * It also enforces a getter for coordinate locations in sprites. */
public enum FSkinImage implements FImage {
    //Zones
    HAND        (FSkinProp.IMG_ZONE_HAND, SourceFile.ICONS),
    LIBRARY     (FSkinProp.IMG_ZONE_LIBRARY, SourceFile.ICONS),
    EXILE       (FSkinProp.IMG_ZONE_EXILE, SourceFile.ICONS),
    FLASHBACK   (FSkinProp.IMG_ZONE_FLASHBACK, SourceFile.ICONS),
    GRAVEYARD   (FSkinProp.IMG_ZONE_GRAVEYARD, SourceFile.ICONS),
    POISON      (FSkinProp.IMG_ZONE_POISON, SourceFile.ICONS),

    //Mana symbols
    MANA_COLORLESS (FSkinProp.IMG_MANA_COLORLESS, SourceFile.ICONS),
    MANA_B         (FSkinProp.IMG_MANA_B, SourceFile.ICONS),
    MANA_R         (FSkinProp.IMG_MANA_R, SourceFile.ICONS),
    MANA_U         (FSkinProp.IMG_MANA_U, SourceFile.ICONS),
    MANA_G         (FSkinProp.IMG_MANA_G, SourceFile.ICONS),
    MANA_W         (FSkinProp.IMG_MANA_W, SourceFile.ICONS),
    MANA_2B        (FSkinProp.IMG_MANA_2B, SourceFile.ICONS),
    MANA_2G        (FSkinProp.IMG_MANA_2G, SourceFile.ICONS),
    MANA_2R        (FSkinProp.IMG_MANA_2R, SourceFile.ICONS),
    MANA_2U        (FSkinProp.IMG_MANA_2U, SourceFile.ICONS),
    MANA_2W        (FSkinProp.IMG_MANA_2W, SourceFile.ICONS),
    MANA_HYBRID_BG (FSkinProp.IMG_MANA_HYBRID_BG, SourceFile.ICONS),
    MANA_HYBRID_BR (FSkinProp.IMG_MANA_HYBRID_BR, SourceFile.ICONS),
    MANA_HYBRID_GU (FSkinProp.IMG_MANA_HYBRID_GU, SourceFile.ICONS),
    MANA_HYBRID_GW (FSkinProp.IMG_MANA_HYBRID_GW, SourceFile.ICONS),
    MANA_HYBRID_RG (FSkinProp.IMG_MANA_HYBRID_RG, SourceFile.ICONS),
    MANA_HYBRID_RW (FSkinProp.IMG_MANA_HYBRID_RW, SourceFile.ICONS),
    MANA_HYBRID_UB (FSkinProp.IMG_MANA_HYBRID_UB, SourceFile.ICONS),
    MANA_HYBRID_UR (FSkinProp.IMG_MANA_HYBRID_UR, SourceFile.ICONS),
    MANA_HYBRID_WB (FSkinProp.IMG_MANA_HYBRID_WB, SourceFile.ICONS),
    MANA_HYBRID_WU (FSkinProp.IMG_MANA_HYBRID_WU, SourceFile.ICONS),
    MANA_PHRYX_U   (FSkinProp.IMG_MANA_PHRYX_U, SourceFile.ICONS),
    MANA_PHRYX_W   (FSkinProp.IMG_MANA_PHRYX_W, SourceFile.ICONS),
    MANA_PHRYX_R   (FSkinProp.IMG_MANA_PHRYX_R, SourceFile.ICONS),
    MANA_PHRYX_G   (FSkinProp.IMG_MANA_PHRYX_G, SourceFile.ICONS),
    MANA_PHRYX_B   (FSkinProp.IMG_MANA_PHRYX_B, SourceFile.ICONS),
    MANA_SNOW      (FSkinProp.IMG_MANA_SNOW, SourceFile.ICONS),
    MANA_0         (FSkinProp.IMG_MANA_0, SourceFile.ICONS),
    MANA_1         (FSkinProp.IMG_MANA_1, SourceFile.ICONS),
    MANA_2         (FSkinProp.IMG_MANA_2, SourceFile.ICONS),
    MANA_3         (FSkinProp.IMG_MANA_3, SourceFile.ICONS),
    MANA_4         (FSkinProp.IMG_MANA_4, SourceFile.ICONS),
    MANA_5         (FSkinProp.IMG_MANA_5, SourceFile.ICONS),
    MANA_6         (FSkinProp.IMG_MANA_6, SourceFile.ICONS),
    MANA_7         (FSkinProp.IMG_MANA_7, SourceFile.ICONS),
    MANA_8         (FSkinProp.IMG_MANA_8, SourceFile.ICONS),
    MANA_9         (FSkinProp.IMG_MANA_9, SourceFile.ICONS),
    MANA_10        (FSkinProp.IMG_MANA_10, SourceFile.ICONS),
    MANA_11        (FSkinProp.IMG_MANA_11, SourceFile.ICONS),
    MANA_12        (FSkinProp.IMG_MANA_12, SourceFile.ICONS),
    MANA_13        (FSkinProp.IMG_MANA_13, SourceFile.ICONS),
    MANA_14        (FSkinProp.IMG_MANA_14, SourceFile.ICONS),
    MANA_15        (FSkinProp.IMG_MANA_15, SourceFile.ICONS),
    MANA_16        (FSkinProp.IMG_MANA_16, SourceFile.ICONS),
    MANA_17        (FSkinProp.IMG_MANA_17, SourceFile.ICONS),
    MANA_18        (FSkinProp.IMG_MANA_18, SourceFile.ICONS),
    MANA_19        (FSkinProp.IMG_MANA_19, SourceFile.ICONS),
    MANA_20        (FSkinProp.IMG_MANA_20, SourceFile.ICONS),
    MANA_X         (FSkinProp.IMG_MANA_X, SourceFile.ICONS),
    MANA_Y         (FSkinProp.IMG_MANA_Y, SourceFile.ICONS),
    MANA_Z         (FSkinProp.IMG_MANA_Z, SourceFile.ICONS),

    //Gameplay
    TAP             (FSkinProp.IMG_TAP, SourceFile.ICONS),
    UNTAP           (FSkinProp.IMG_UNTAP, SourceFile.ICONS),
    CHAOS           (FSkinProp.IMG_CHAOS, SourceFile.ICONS),
    SLASH           (FSkinProp.IMG_SLASH, SourceFile.ICONS),
    ATTACK          (FSkinProp.IMG_ATTACK, SourceFile.ICONS),
    DEFEND          (FSkinProp.IMG_DEFEND, SourceFile.ICONS),
    SUMMONSICK      (FSkinProp.IMG_SUMMONSICK, SourceFile.ICONS),
    PHASING         (FSkinProp.IMG_PHASING, SourceFile.ICONS),
    COSTRESERVED    (FSkinProp.IMG_COSTRESERVED, SourceFile.ICONS),
    COUNTERS1       (FSkinProp.IMG_COUNTERS1, SourceFile.ICONS),
    COUNTERS2       (FSkinProp.IMG_COUNTERS2, SourceFile.ICONS),
    COUNTERS3       (FSkinProp.IMG_COUNTERS3, SourceFile.ICONS),
    COUNTERS_MULTI  (FSkinProp.IMG_COUNTERS_MULTI, SourceFile.ICONS),

    //Dock Icons
    SHORTCUTS    (FSkinProp.ICO_SHORTCUTS, SourceFile.ICONS),
    SETTINGS     (FSkinProp.ICO_SETTINGS, SourceFile.ICONS),
    ENDTURN      (FSkinProp.ICO_ENDTURN, SourceFile.ICONS),
    CONCEDE      (FSkinProp.ICO_CONCEDE, SourceFile.ICONS),
    REVERTLAYOUT (FSkinProp.ICO_REVERTLAYOUT, SourceFile.ICONS),
    OPENLAYOUT   (FSkinProp.ICO_OPENLAYOUT, SourceFile.ICONS),
    SAVELAYOUT   (FSkinProp.ICO_SAVELAYOUT, SourceFile.ICONS),
    DECKLIST     (FSkinProp.ICO_DECKLIST, SourceFile.ICONS),
    ALPHASTRIKE  (FSkinProp.ICO_ALPHASTRIKE, SourceFile.ICONS),
    ARCSOFF      (FSkinProp.ICO_ARCSOFF, SourceFile.ICONS),
    ARCSON       (FSkinProp.ICO_ARCSON, SourceFile.ICONS),
    ARCSHOVER    (FSkinProp.ICO_ARCSHOVER, SourceFile.ICONS),

    //Achievement Trophies
    COMMON_TROPHY    (FSkinProp.IMG_COMMON_TROPHY, SourceFile.TROPHIES),
    UNCOMMON_TROPHY  (FSkinProp.IMG_UNCOMMON_TROPHY, SourceFile.TROPHIES),
    RARE_TROPHY      (FSkinProp.IMG_RARE_TROPHY, SourceFile.TROPHIES),
    MYTHIC_TROPHY    (FSkinProp.IMG_MYTHIC_TROPHY, SourceFile.TROPHIES),
    SPECIAL_TROPHY   (FSkinProp.IMG_SPECIAL_TROPHY, SourceFile.TROPHIES),
    TROPHY_PLATE     (FSkinProp.IMG_TROPHY_PLATE, SourceFile.TROPHIES),
    TROPHY_CASE_TOP  (FSkinProp.IMG_TROPHY_CASE_TOP, SourceFile.TROPHIES),
    TROPHY_SHELF     (FSkinProp.IMG_TROPHY_SHELF, SourceFile.TROPHIES),

    //Planar Conquest Images
    PLANAR_PORTAL (FSkinProp.IMG_PLANAR_PORTAL, SourceFile.PLANAR_CONQUEST),

    //Quest Icons
    QUEST_ZEP         (FSkinProp.ICO_QUEST_ZEP, SourceFile.ICONS),
    QUEST_GEAR        (FSkinProp.ICO_QUEST_GEAR, SourceFile.ICONS),
    QUEST_GOLD        (FSkinProp.ICO_QUEST_GOLD, SourceFile.ICONS),
    QUEST_ELIXIR      (FSkinProp.ICO_QUEST_ELIXIR, SourceFile.ICONS),
    QUEST_BOOK        (FSkinProp.ICO_QUEST_BOOK, SourceFile.ICONS),
    QUEST_BOTTLES     (FSkinProp.ICO_QUEST_BOTTLES, SourceFile.ICONS),
    QUEST_BOX         (FSkinProp.ICO_QUEST_BOX, SourceFile.ICONS),
    QUEST_COIN        (FSkinProp.ICO_QUEST_COIN, SourceFile.ICONS),
    QUEST_CHARM       (FSkinProp.ICO_QUEST_CHARM, SourceFile.ICONS),
    QUEST_FOX         (FSkinProp.ICO_QUEST_FOX, SourceFile.ICONS),
    QUEST_LEAF        (FSkinProp.ICO_QUEST_LEAF, SourceFile.ICONS),
    QUEST_LIFE        (FSkinProp.ICO_QUEST_LIFE, SourceFile.ICONS),
    QUEST_COINSTACK   (FSkinProp.ICO_QUEST_COINSTACK, SourceFile.ICONS),
    QUEST_MAP         (FSkinProp.ICO_QUEST_MAP, SourceFile.ICONS),
    QUEST_NOTES       (FSkinProp.ICO_QUEST_NOTES, SourceFile.ICONS),
    QUEST_HEART       (FSkinProp.ICO_QUEST_HEART, SourceFile.ICONS),
    QUEST_BREW        (FSkinProp.ICO_QUEST_BREW, SourceFile.ICONS),
    QUEST_STAKES      (FSkinProp.ICO_QUEST_STAKES, SourceFile.ICONS),
    QUEST_MINUS       (FSkinProp.ICO_QUEST_MINUS, SourceFile.ICONS),
    QUEST_PLUS        (FSkinProp.ICO_QUEST_PLUS, SourceFile.ICONS),
    QUEST_PLUSPLUS    (FSkinProp.ICO_QUEST_PLUSPLUS, SourceFile.ICONS),

    //Interface icons
    QUESTION        (FSkinProp.ICO_QUESTION, SourceFile.ICONS),
    INFORMATION     (FSkinProp.ICO_INFORMATION, SourceFile.ICONS),
    WARNING         (FSkinProp.ICO_WARNING, SourceFile.ICONS),
    ERROR           (FSkinProp.ICO_ERROR, SourceFile.ICONS),
    DELETE          (FSkinProp.ICO_DELETE, SourceFile.ICONS),
    DELETE_OVER     (FSkinProp.ICO_DELETE_OVER, SourceFile.ICONS),
    EDIT            (FSkinProp.ICO_EDIT, SourceFile.ICONS),
    EDIT_OVER       (FSkinProp.ICO_EDIT_OVER, SourceFile.ICONS),
    OPEN            (FSkinProp.ICO_OPEN, SourceFile.ICONS),
    MINUS           (FSkinProp.ICO_MINUS, SourceFile.ICONS),
    NEW             (FSkinProp.ICO_NEW, SourceFile.ICONS),
    PLUS            (FSkinProp.ICO_PLUS, SourceFile.ICONS),
    PRINT           (FSkinProp.ICO_PRINT, SourceFile.ICONS),
    SAVE            (FSkinProp.ICO_SAVE, SourceFile.ICONS),
    SAVEAS          (FSkinProp.ICO_SAVEAS, SourceFile.ICONS),
    CLOSE           (FSkinProp.ICO_CLOSE, SourceFile.ICONS),
    LIST            (FSkinProp.ICO_LIST, SourceFile.ICONS),
    CARD_IMAGE      (FSkinProp.ICO_CARD_IMAGE, SourceFile.ICONS),
    FOLDER          (FSkinProp.ICO_FOLDER, SourceFile.ICONS),
    SEARCH          (FSkinProp.ICO_SEARCH, SourceFile.ICONS),
    UNKNOWN         (FSkinProp.ICO_UNKNOWN, SourceFile.ICONS),
    LOGO            (FSkinProp.ICO_LOGO, SourceFile.ICONS),
    FLIPCARD        (FSkinProp.ICO_FLIPCARD, SourceFile.ICONS),
    FAVICON         (FSkinProp.ICO_FAVICON, SourceFile.ICONS),
    LOCK            (FSkinProp.ICO_LOCK, SourceFile.ICONS),

    //Layout images
    HANDLE  (FSkinProp.IMG_HANDLE, SourceFile.ICONS),
    CUR_L   (FSkinProp.IMG_CUR_L, SourceFile.ICONS),
    CUR_R   (FSkinProp.IMG_CUR_R, SourceFile.ICONS),
    CUR_T   (FSkinProp.IMG_CUR_T, SourceFile.ICONS),
    CUR_B   (FSkinProp.IMG_CUR_B, SourceFile.ICONS),
    CUR_TAB (FSkinProp.IMG_CUR_TAB, SourceFile.ICONS),

    //Editor images
    STAR_OUTINE     (FSkinProp.IMG_STAR_OUTINE, SourceFile.ICONS),
    STAR_FILLED     (FSkinProp.IMG_STAR_FILLED, SourceFile.ICONS),
    ARTIFACT        (FSkinProp.IMG_ARTIFACT, SourceFile.ICONS),
    CREATURE        (FSkinProp.IMG_CREATURE, SourceFile.ICONS),
    ENCHANTMENT     (FSkinProp.IMG_ENCHANTMENT, SourceFile.ICONS),
    INSTANT         (FSkinProp.IMG_INSTANT, SourceFile.ICONS),
    LAND            (FSkinProp.IMG_LAND, SourceFile.ICONS),
    MULTI           (FSkinProp.IMG_MULTI, SourceFile.ICONS),
    PLANESWALKER    (FSkinProp.IMG_PLANESWALKER, SourceFile.ICONS),
    PACK            (FSkinProp.IMG_PACK, SourceFile.ICONS),
    SORCERY         (FSkinProp.IMG_SORCERY, SourceFile.ICONS),

    //Buttons
    BTN_START_UP        (FSkinProp.IMG_BTN_START_UP, SourceFile.ICONS),
    BTN_START_OVER      (FSkinProp.IMG_BTN_START_OVER, SourceFile.ICONS),
    BTN_START_DOWN      (FSkinProp.IMG_BTN_START_DOWN, SourceFile.ICONS),
    BTN_UP_LEFT         (FSkinProp.IMG_BTN_UP_LEFT, SourceFile.ICONS),
    BTN_UP_CENTER       (FSkinProp.IMG_BTN_UP_CENTER, SourceFile.ICONS),
    BTN_UP_RIGHT        (FSkinProp.IMG_BTN_UP_RIGHT, SourceFile.ICONS),
    BTN_OVER_LEFT       (FSkinProp.IMG_BTN_OVER_LEFT, SourceFile.ICONS),
    BTN_OVER_CENTER     (FSkinProp.IMG_BTN_OVER_CENTER, SourceFile.ICONS),
    BTN_OVER_RIGHT      (FSkinProp.IMG_BTN_OVER_RIGHT, SourceFile.ICONS),
    BTN_DOWN_LEFT       (FSkinProp.IMG_BTN_DOWN_LEFT, SourceFile.ICONS),
    BTN_DOWN_CENTER     (FSkinProp.IMG_BTN_DOWN_CENTER, SourceFile.ICONS),
    BTN_DOWN_RIGHT      (FSkinProp.IMG_BTN_DOWN_RIGHT, SourceFile.ICONS),
    BTN_FOCUS_LEFT      (FSkinProp.IMG_BTN_FOCUS_LEFT, SourceFile.ICONS),
    BTN_FOCUS_CENTER    (FSkinProp.IMG_BTN_FOCUS_CENTER, SourceFile.ICONS),
    BTN_FOCUS_RIGHT     (FSkinProp.IMG_BTN_FOCUS_RIGHT, SourceFile.ICONS),
    BTN_TOGGLE_LEFT     (FSkinProp.IMG_BTN_TOGGLE_LEFT, SourceFile.ICONS),
    BTN_TOGGLE_CENTER   (FSkinProp.IMG_BTN_TOGGLE_CENTER, SourceFile.ICONS),
    BTN_TOGGLE_RIGHT    (FSkinProp.IMG_BTN_TOGGLE_RIGHT, SourceFile.ICONS),
    BTN_DISABLED_LEFT   (FSkinProp.IMG_BTN_DISABLED_LEFT, SourceFile.ICONS),
    BTN_DISABLED_CENTER (FSkinProp.IMG_BTN_DISABLED_CENTER, SourceFile.ICONS),
    BTN_DISABLED_RIGHT  (FSkinProp.IMG_BTN_DISABLED_RIGHT, SourceFile.ICONS),

    //Foils
    FOIL_01     (FSkinProp.FOIL_01, SourceFile.FOILS),
    FOIL_02     (FSkinProp.FOIL_02, SourceFile.FOILS),
    FOIL_03     (FSkinProp.FOIL_03, SourceFile.FOILS),
    FOIL_04     (FSkinProp.FOIL_04, SourceFile.FOILS),
    FOIL_05     (FSkinProp.FOIL_05, SourceFile.FOILS),
    FOIL_06     (FSkinProp.FOIL_06, SourceFile.FOILS),
    FOIL_07     (FSkinProp.FOIL_07, SourceFile.FOILS),
    FOIL_08     (FSkinProp.FOIL_08, SourceFile.FOILS),
    FOIL_09     (FSkinProp.FOIL_09, SourceFile.FOILS),
    FOIL_10     (FSkinProp.FOIL_10, SourceFile.FOILS),

    //Old Foils
    FOIL_11     (FSkinProp.FOIL_11, SourceFile.OLD_FOILS),
    FOIL_12     (FSkinProp.FOIL_12, SourceFile.OLD_FOILS),
    FOIL_13     (FSkinProp.FOIL_13, SourceFile.OLD_FOILS),
    FOIL_14     (FSkinProp.FOIL_14, SourceFile.OLD_FOILS),
    FOIL_15     (FSkinProp.FOIL_15, SourceFile.OLD_FOILS),
    FOIL_16     (FSkinProp.FOIL_16, SourceFile.OLD_FOILS),
    FOIL_17     (FSkinProp.FOIL_17, SourceFile.OLD_FOILS),
    FOIL_18     (FSkinProp.FOIL_18, SourceFile.OLD_FOILS),
    FOIL_19     (FSkinProp.FOIL_19, SourceFile.OLD_FOILS),
    FOIL_20     (FSkinProp.FOIL_20, SourceFile.OLD_FOILS);

    public enum SourceFile {
        ICONS(ForgeConstants.SPRITE_ICONS_FILE),
        FOILS(ForgeConstants.SPRITE_FOILS_FILE),
        OLD_FOILS(ForgeConstants.SPRITE_OLD_FOILS_FILE),
        TROPHIES(ForgeConstants.SPRITE_TROPHIES_FILE),
        PLANAR_CONQUEST(ForgeConstants.SPRITE_PLANAR_CONQUEST_FILE);

        private final String filename;

        SourceFile(String filename0) {
            filename = filename0;
        }

        public String getFilename() {
            return filename;
        }
    }

    private final int x, y, w, h;
    private final SourceFile sourceFile;
    private TextureRegion textureRegion;

    FSkinImage(FSkinProp skinProp, SourceFile sourceFile0) {
        int[] coords = skinProp.getCoords();
        x = coords[0];
        y = coords[1];
        w = coords[2];
        h = coords[3];
        sourceFile = sourceFile0;
        FSkin.getImages().put(skinProp, this);
    }

    public void load(Map<String, Texture> textures, Pixmap preferredIcons) {
        String filename = sourceFile.getFilename();
        FileHandle preferredFile = FSkin.getSkinFile(filename);
        Texture texture = textures.get(preferredFile.path());
        if (texture == null) {
            if (preferredFile.exists()) {
                try {
                    texture = new Texture(preferredFile);
                }
                catch (final Exception e) {
                    System.err.println("Failed to load skin file: " + preferredFile);
                    e.printStackTrace();
                }
            }
        }
        if (texture != null) {
            if (sourceFile != SourceFile.ICONS) { //just return region for preferred file if not icons file
                textureRegion = new TextureRegion(texture, x, y, w, h);
                return;
            }

            int fullWidth = texture.getWidth();
            int fullHeight = texture.getHeight();

            // Test if requested sub-image in inside bounds of preferred sprite.
            // (Height and width of preferred sprite were set in loadFontAndImages.)
            if (x + w <= fullWidth && y + h <= fullHeight) {
                // Test if various points of requested sub-image are transparent.
                // If any return true, image exists.
                int x0 = 0, y0 = 0;
                Color c;
    
                // Center
                x0 = (x + w / 2);
                y0 = (y + h / 2);
                c = new Color(preferredIcons.getPixel(x0, y0));
                if (c.a != 0) {
                    textureRegion = new TextureRegion(texture, x, y, w, h);
                    return;
                }
    
                x0 += 2;
                y0 += 2;
                c = new Color(preferredIcons.getPixel(x0, y0));
                if (c.a != 0) {
                    textureRegion = new TextureRegion(texture, x, y, w, h);
                    return;
                }
    
                x0 -= 4;
                c = new Color(preferredIcons.getPixel(x0, y0));
                if (c.a != 0) {
                    textureRegion = new TextureRegion(texture, x, y, w, h);
                    return;
                }
    
                y0 -= 4;
                c = new Color(preferredIcons.getPixel(x0, y0));
                if (c.a != 0) {
                    textureRegion = new TextureRegion(texture, x, y, w, h);
                    return;
                }
    
                x0 += 4;
                c = new Color(preferredIcons.getPixel(x0, y0));
                if (c.a != 0) {
                    textureRegion = new TextureRegion(texture, x, y, w, h);
                    return;
                }
            }
        }

        //use default file if can't use preferred file
        FileHandle defaultFile = FSkin.getDefaultSkinFile(filename);
        texture = textures.get(defaultFile.path());
        if (texture == null) {
            if (defaultFile.exists()) {
                try {
                    texture = new Texture(defaultFile);
                }
                catch (final Exception e) {
                    System.err.println("Failed to load skin file: " + defaultFile);
                    e.printStackTrace();
                }
            }
        }
        if (texture != null) {
            textureRegion = new TextureRegion(texture, x, y, w, h);
        }
    }

    @Override
    public float getWidth() {
        return w;
    }

    @Override
    public float getHeight() {
        return h;
    }

    public TextureRegion getTextureRegion() {
        return textureRegion;
    }

    public float getNearestHQWidth(float baseWidth) {
        return ImageUtil.getNearestHQSize(baseWidth, w);
    }

    public float getNearestHQHeight(float baseHeight) {
        return ImageUtil.getNearestHQSize(baseHeight, h);
    }

    @Override
    public void draw(Graphics g, float x, float y, float w, float h) {
        g.drawImage(textureRegion, x, y, w, h);
    }
}