/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.view.toolbox;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.LineBorder;

import forge.Singletons;
import forge.gui.GuiUtils;

/**
 * Assembles settings from selected or default theme as appropriate. Saves in a
 * hashtable, access using .get(settingName) method.
 * 
 */

public class FSkin {
    /** Properties of various components that make up the skin. */
    public interface SkinProp { }
    /** Add this interface for sub-sprite components, storing their coords. */
    public interface Coords {
        /** */
        int[] COORDS = null;
        /** @return int[] */
        int[] getCoords();
    }

    /** */
    public enum Backgrounds implements SkinProp { /** */
        BG_SPLASH, /** */
        BG_TEXTURE, /** */
        BG_MATCH,  /** */
    }

    /** */
    public enum Colors implements SkinProp { /** */
        CLR_THEME, /** */
        CLR_BORDERS, /** */
        CLR_ZEBRA, /** */
        CLR_HOVER, /** */
        CLR_ACTIVE, /** */
        CLR_INACTIVE, /** */
        CLR_TEXT, /** */
    }

    /** int[] can hold [xcoord, ycoord, width, height, newwidth, newheight]. */
    public enum ZoneIcons implements SkinProp, Coords { /** */
        ICO_HAND        (new int[] {280, 40, 40, 40}), /** */
        ICO_LIBRARY     (new int[] {280, 0, 40, 40}), /** */
        ICO_EXILE       (new int[] {320, 40, 40, 40}), /** */
        ICO_FLASHBACK   (new int[] {280, 80, 40, 40}), /** */
        ICO_GRAVEYARD   (new int[] {320, 0, 40, 40}), /** */
        ICO_POISON      (new int[] {320, 80, 40, 40});

        private int[] coords;
        /** @param xy &emsp; int[] coordinates */
        ZoneIcons(int[] xy) { this.coords = xy; }
        /** @return int[] */
        public int[] getCoords() { return coords; }
    }

    /** int[] can hold [xcoord, ycoord, width, height, newwidth, newheight]. */
    public enum ManaIcons implements SkinProp, Coords { /** */
        ICO_BLACK       (new int[] {360, 160, 40, 40, 13, 13}), /** */
        ICO_RED         (new int[] {400, 160, 40, 40, 13, 13}), /** */
        ICO_COLORLESS   (new int[] {440, 160, 40, 40, 13, 13}), /** */
        ICO_BLUE        (new int[] {360, 200, 40, 40, 13, 13}), /** */
        ICO_GREEN       (new int[] {400, 200, 40, 40, 13, 13}), /** */
        ICO_WHITE       (new int[] {440, 200, 40, 40, 13, 13}), /** */
        ICO_2B          (new int[] {360, 400, 40, 40, 13, 13}), /** */
        ICO_2G          (new int[] {400, 400, 40, 40, 13, 13}), /** */
        ICO_2R          (new int[] {440, 360, 40, 40, 13, 13}), /** */
        ICO_2U          (new int[] {440, 360, 40, 40, 13, 13}), /** */
        ICO_2W          (new int[] {400, 360, 40, 40, 13, 13}), /** */
        ICO_BLACK_GREEN (new int[] {360, 240, 40, 40, 13, 13}), /** */
        ICO_BLACK_RED   (new int[] {400, 240, 40, 40, 13, 13}), /** */
        ICO_GREEN_BLUE  (new int[] {360, 280, 40, 40, 13, 13}), /** */
        ICO_GREEN_WHITE (new int[] {440, 280, 40, 40, 13, 13}), /** */
        ICO_RED_GREEN   (new int[] {360, 320, 40, 40, 13, 13}), /** */
        ICO_RED_WHITE   (new int[] {400, 320, 40, 40, 13, 13}), /** */
        ICO_BLUE_BLACK  (new int[] {440, 240, 40, 40, 13, 13}), /** */
        ICO_BLUE_RED    (new int[] {440, 320, 40, 40, 13, 13}), /** */
        ICO_WHITE_BLACK (new int[] {400, 280, 40, 40, 13, 13}), /** */
        ICO_WHITE_BLUE  (new int[] {360, 360, 40, 40, 13, 13}), /** */
        ICO_PHRYX_BLUE  (new int[] {320, 200, 40, 40, 13, 13}), /** */
        ICO_PHRYX_WHITE (new int[] {320, 240, 40, 40, 13, 13}), /** */
        ICO_PHRYX_RED   (new int[] {320, 280, 40, 40, 13, 13}), /** */
        ICO_PHRYX_GREEN (new int[] {320, 320, 40, 40, 13, 13}), /** */
        ICO_PHRYX_BLACK (new int[] {320, 360, 40, 40, 13, 13});

        private int[] coords;
        /** @param xy &emsp; int[] coordinates */
        ManaIcons(int[] xy) { this.coords = xy; }
        /** @return int[] */
        public int[] getCoords() { return coords; }
    }

    /** int[] can hold [xcoord, ycoord, width, height, newwidth, newheight]. */
    public enum ColorlessManaIcons implements SkinProp, Coords { /** */
        ICO_0   (new int[] {640, 200, 20, 20, 13, 13}), /** */
        ICO_1   (new int[] {660, 200, 20, 20, 13, 13}), /** */
        ICO_2   (new int[] {640, 220, 20, 20, 13, 13}), /** */
        ICO_3   (new int[] {660, 220, 20, 20, 13, 13}), /** */
        ICO_4   (new int[] {640, 240, 20, 20, 13, 13}), /** */
        ICO_5   (new int[] {660, 240, 20, 20, 13, 13}), /** */
        ICO_6   (new int[] {640, 260, 20, 20, 13, 13}), /** */
        ICO_7   (new int[] {660, 260, 20, 20, 13, 13}), /** */
        ICO_8   (new int[] {640, 280, 20, 20, 13, 13}), /** */
        ICO_9   (new int[] {660, 280, 20, 20, 13, 13}), /** */
        ICO_10  (new int[] {640, 300, 20, 20, 13, 13}), /** */
        ICO_11  (new int[] {660, 300, 20, 20, 13, 13}), /** */
        ICO_12  (new int[] {640, 320, 20, 20, 13, 13}), /** */
        ICO_15  (new int[] {660, 340, 20, 20, 13, 13}), /** */
        ICO_16  (new int[] {640, 360, 20, 20, 13, 13}), /** */
        ICO_20  (new int[] {640, 400, 20, 20, 13, 13}), /** */
        ICO_X   (new int[] {640, 420, 20, 20, 13, 13}), /** */
        ICO_Y   (new int[] {660, 420, 20, 20, 13, 13}), /** */
        ICO_Z   (new int[] {640, 440, 20, 20, 13, 13});

        private int[] coords;
        /** @param xy &emsp; int[] coordinates */
        ColorlessManaIcons(int[] xy) { this.coords = xy; }
        /** @return int[] */
        public int[] getCoords() { return coords; }
    }

    /** int[] can hold [xcoord, ycoord, width, height, newwidth, newheight]. */
    public enum GameplayIcons implements SkinProp, Coords { /** */
        ICO_SNOW            (new int[] {320, 160, 40, 40}), /** */
        ICO_TAP             (new int[] {660, 440, 20, 20}), /** */
        ICO_UNTAP           (new int[] {640, 460, 20, 20}), /** */
        ICO_SLASH           (new int[] {660, 400, 10, 13}), /** */
        ICO_ATTACK          (new int[] {160, 320, 80, 80, 32, 32}), /** */
        ICO_DEFEND          (new int[] {160, 400, 80, 80, 32, 32}), /** */
        ICO_SUMMONSICK      (new int[] {240, 400, 80, 80, 32, 32}), /** */
        ICO_PHASING         (new int[] {240, 320, 80, 80, 32, 32}), /** */
        ICO_COUNTERS1       (new int[] {0, 320, 80, 80}), /** */
        ICO_COUNTERS2       (new int[] {0, 400, 80, 80}), /** */
        ICO_COUNTERS3       (new int[] {80, 320, 80, 80}), /** */
        ICO_COUNTERS_MULTI  (new int[] {80, 400, 80, 80});

        private int[] coords;
        /** @param xy &emsp; int[] coordinates */
        GameplayIcons(int[] xy) { this.coords = xy; }
        /** @return int[] */
        public int[] getCoords() { return coords; }
    }

    /** */
    public enum Foils implements SkinProp, Coords { /** */
        FOIL_01     (new int[] {0, 0, 400, 570}), /** */
        FOIL_02     (new int[] {400, 0, 400, 570}), /** */
        FOIL_03     (new int[] {0, 570, 400, 570}), /** */
        FOIL_04     (new int[] {400, 570, 400, 570}), /** */
        FOIL_05     (new int[] {0, 1140, 400, 570}), /** */
        FOIL_06     (new int[] {400, 1140, 400, 570}), /** */
        FOIL_07     (new int[] {0, 1710, 400, 570}), /** */
        FOIL_08     (new int[] {400, 1710, 400, 570}), /** */
        FOIL_09     (new int[] {0, 2280, 400, 570}), /** */
        FOIL_10     (new int[] {400, 2280, 400, 570});

        private int[] coords;
        /** @param xy &emsp; int[] coordinates */
        Foils(int[] xy) { this.coords = xy; }
        /** @return int[] */
        public int[] getCoords() { return coords; }
    }

    /** */
    public enum DockIcons implements SkinProp, Coords { /** */
        ICO_SHORTCUTS   (new int[] {160, 640, 80, 80}), /** */
        ICO_SETTINGS    (new int[] {80, 640, 80, 80}), /** */
        ICO_ENDTURN     (new int[] {320, 640, 80, 80}), /** */
        ICO_CONCEDE     (new int[] {240, 640, 80, 80}), /** */
        ICO_DECKLIST    (new int[] {400, 640, 80, 80});

        private int[] coords;
        /** @param xy &emsp; int[] coordinates */
        DockIcons(int[] xy) { this.coords = xy; }
        /** @return int[] */
        public int[] getCoords() { return coords; }
    }

    /** */
    public enum QuestIcons implements SkinProp, Coords { /** */
        ICO_ZEP         (new int[] {0, 480, 80, 80}), /** */
        ICO_GEAR        (new int[] {80, 480, 80, 80}), /** */
        ICO_GOLD        (new int[] {160, 480, 80, 80}), /** */
        ICO_BOOK        (new int[] {240, 480, 80, 80}), /** */
        ICO_ELIXER      (new int[] {320, 480, 80, 80}), /** */
        ICO_BOTTLES     (new int[] {400, 480, 80, 80}), /** */
        ICO_BOX         (new int[] {480, 480, 80, 80}), /** */
        ICO_COIN        (new int[] {560, 480, 80, 80}), /** */

        ICO_FOX         (new int[] {0, 560, 80, 80}), /** */
        ICO_LEAF        (new int[] {80, 560, 80, 80}), /** */
        ICO_LIFE        (new int[] {160, 560, 80, 80}), /** */
        ICO_COINSTACK   (new int[] {240, 560, 80, 80}), /** */
        ICO_MAP         (new int[] {320, 560, 80, 80}), /** */
        ICO_NOTES       (new int[] {400, 560, 80, 80}), /** */
        ICO_HEART       (new int[] {480, 560, 80, 80}), /** */

        ICO_MINUS       (new int[] {480, 640, 80, 80}), /** */
        ICO_PLUS        (new int[] {560, 640, 80, 80});

        private int[] coords;
        /** @param xy &emsp; int[] coordinates */
        QuestIcons(int[] xy) { this.coords = xy; }
        /** @return int[] */
        public int[] getCoords() { return coords; }
    }

    /** */
    public enum CreatureIcons implements SkinProp, Coords { /** */
        ICO_BIRD1         (new int[] {0, 2280, 400, 570}), /** */
        ICO_BIRD2         (new int[] {400, 2280, 400, 570}), /** */
        ICO_BIRD3         (new int[] {800, 2280, 400, 570}), /** */
        ICO_BIRD4         (new int[] {1200, 2280, 400, 570}), /** */

        ICO_PLANT1         (new int[] {0, 0, 400, 570}), /** */
        ICO_PLANT2         (new int[] {400, 0, 400, 570}), /** */
        ICO_PLANT3         (new int[] {800, 0, 400, 570}), /** */
        ICO_PLANT4         (new int[] {1200, 0, 400, 570}), /** */
        ICO_PLANT5         (new int[] {0, 570, 400, 570}), /** */
        ICO_PLANT6         (new int[] {400, 570, 400, 570}), /** */

        ICO_HOUND1         (new int[] {0, 1710, 400, 570}), /** */
        ICO_HOUND2         (new int[] {400, 1710, 400, 570}), /** */
        ICO_HOUND3         (new int[] {800, 1710, 400, 570}), /** */
        ICO_HOUND4         (new int[] {1200, 1710, 400, 570}), /** */

        ICO_CROC1         (new int[] {0, 2850, 400, 570}), /** */
        ICO_CROC2         (new int[] {400, 2850, 400, 570}), /** */
        ICO_CROC3         (new int[] {800, 2850, 400, 570}), /** */
        ICO_CROC4         (new int[] {1200, 2850, 400, 570}), /** */

        ICO_WOLF1         (new int[] {0, 1140, 400, 570}), /** */
        ICO_WOLF2         (new int[] {400, 1140, 400, 570}), /** */
        ICO_WOLF3         (new int[] {800, 1140, 400, 570}), /** */
        ICO_WOLF4         (new int[] {1200, 1140, 400, 570});

        private int[] coords;
        /** @param xy &emsp; int[] coordinates */
        CreatureIcons(int[] xy) { this.coords = xy; }
        /** @return int[] */
        public int[] getCoords() { return coords; }
    }

    /** */
    public enum ForgeIcons implements SkinProp, Coords { /** */
        ICO_EDIT            (new int[] {640, 500, 20, 20}), /** */
        ICO_EDIT_OVER       (new int[] {660, 500, 20, 20}), /** */
        ICO_DELETE          (new int[] {640, 480, 20, 20}), /** */
        ICO_DELETE_OVER     (new int[] {660, 480, 20, 20}), /** */
        ICO_UNKNOWN         (new int[] {80, 720, 80, 80}), /** */
        ICO_LOGO            (new int[] {480, 0, 200, 200}), /** */
        ICO_DEFAULT_MAGE    (new int[] {0, 720, 80, 80}), /** */
        ICO_FAVICON         (new int[] {0, 640, 80, 80});

        private int[] coords;
        /** @param xy &emsp; int[] coordinates */
        ForgeIcons(int[] xy) { this.coords = xy; }
        /** @return int[] */
        public int[] getCoords() { return coords; }
    }

    /** */
    public enum ButtonImages implements SkinProp, Coords { /** */
        IMG_BTN_START_UP        (new int[] {480, 200, 160, 80}), /** */
        IMG_BTN_START_OVER      (new int[] {480, 280, 160, 80}), /** */
        IMG_BTN_START_DOWN      (new int[] {480, 360, 160, 80}), /** */

        IMG_BTN_UP_LEFT         (new int[] {80, 0, 40, 40}), /** */
        IMG_BTN_UP_CENTER       (new int[] {120, 0, 1, 40}), /** */
        IMG_BTN_UP_RIGHT        (new int[] {160, 0, 40, 40}), /** */

        IMG_BTN_OVER_LEFT       (new int[] {80, 40, 40, 40}), /** */
        IMG_BTN_OVER_CENTER     (new int[] {120, 40, 1, 40}), /** */
        IMG_BTN_OVER_RIGHT      (new int[] {160, 40, 40, 40}), /** */

        IMG_BTN_DOWN_LEFT       (new int[] {80, 80, 40, 40}), /** */
        IMG_BTN_DOWN_CENTER     (new int[] {120, 80, 1, 40}), /** */
        IMG_BTN_DOWN_RIGHT      (new int[] {160, 80, 40, 40}), /** */

        IMG_BTN_FOCUS_LEFT      (new int[] {80, 120, 40, 40}), /** */
        IMG_BTN_FOCUS_CENTER    (new int[] {120, 120, 1, 40}), /** */
        IMG_BTN_FOCUS_RIGHT     (new int[] {160, 120, 40, 40}), /** */

        IMG_BTN_TOGGLE_LEFT     (new int[] {80, 160, 40, 40}), /** */
        IMG_BTN_TOGGLE_CENTER   (new int[] {120, 160, 1, 40}), /** */
        IMG_BTN_TOGGLE_RIGHT    (new int[] {160, 160, 40, 40}), /** */

        IMG_BTN_DISABLED_LEFT   (new int[] {80, 200, 40, 40}), /** */
        IMG_BTN_DISABLED_CENTER (new int[] {120, 200, 1, 40}), /** */
        IMG_BTN_DISABLED_RIGHT  (new int[] {160, 200, 40, 40});

        private int[] coords;
        /** @param xy &emsp; int[] coordinates */
        ButtonImages(int[] xy) { this.coords = xy; }
        /** @return int[] */
        public int[] getCoords() { return coords; }
    }

    private Map<SkinProp, ImageIcon> icons;
    private Map<SkinProp, Image> images;
    private Map<SkinProp, Color> colors;
    private Map<SkinProp, Image> foils;

    private Map<Integer, Font> plainFonts;
    private Map<Integer, Font> boldFonts;
    private Map<Integer, Font> italicFonts;

    private static final String
        FILE_SKINS_DIR = "res/images/skins/",
        FILE_ICON_SPRITE = "sprite_icons.png",
        FILE_FOIL_SPRITE = "sprite_foils.png",
        FILE_CREATURE_SPRITE = "sprite_creatures.jpg",
        FILE_FONT = "font1.ttf",
        FILE_SPLASH = "bg_splash.png",
        FILE_MATCH_BG = "bg_match.jpg",
        FILE_TEXTURE_BG = "bg_texture.jpg";

    private final String notfound = "FSkin.java: Can't find ";
    private final String preferredDir;
    private final String defaultDir;
    private final String preferredName;
    private Font font;
    private BufferedImage bimDefaultSprite, bimPreferredSprite, bimFoils, bimCreatures;
    private int preferredH, preferredW;
    private FProgressBar barProgress;

    /**
     * FSkin constructor. No arguments, will generate default skin settings,
     * fonts, and backgrounds.
     */
    public FSkin() {
        this("default");
    }

    /**
     * FSkin constructor, using skin name. Generates custom skin settings,
     * fonts, and backgrounds.
     * 
     * @param skinName
     *            the skin name
     */
    public FSkin(final String skinName) {
        this.preferredName = skinName;
        this.preferredDir = FILE_SKINS_DIR + preferredName + "/";
        this.defaultDir = FILE_SKINS_DIR + "default/";
        this.icons = new HashMap<SkinProp, ImageIcon>();
        this.images = new HashMap<SkinProp, Image>();
        this.colors = new HashMap<SkinProp, Color>();
        this.foils = new HashMap<SkinProp, Image>();

        final File f = new File(preferredDir + FILE_SPLASH);
        final BufferedImage img;
        try {
            img = ImageIO.read(f);

            final int h = img.getHeight();
            final int w = img.getWidth();

            this.setIcon(Backgrounds.BG_SPLASH, img.getSubimage(0, 0, w, h - 100));

            UIManager.put("ProgressBar.background", this.getColorFromPixel(img.getRGB(25, h - 75)));
            UIManager.put("ProgressBar.selectionBackground", this.getColorFromPixel(img.getRGB(75, h - 75)));
            UIManager.put("ProgressBar.foreground", this.getColorFromPixel(img.getRGB(25, h - 25)));
            UIManager.put("ProgressBar.selectionForeground", this.getColorFromPixel(img.getRGB(75, h - 25)));
            UIManager.put("ProgressBar.border", new LineBorder(Color.BLACK, 0));
        } catch (final IOException e) {
            System.err.println(this.notfound + preferredDir + FILE_SPLASH);
            e.printStackTrace();
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
    public void loadFontsAndImages() {
        barProgress = Singletons.getView().getProgressBar();

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                barProgress.reset();
                barProgress.setShowETA(false);
                barProgress.setDescription("Processing fonts and image sprites: ");
            }
        });

        barProgress.setMaximum(57);

        // Grab and test various sprite files.
        final File f1 = new File(defaultDir + FILE_ICON_SPRITE);
        final File f2 = new File(preferredDir + FILE_ICON_SPRITE);
        final File f3 = new File(defaultDir + FILE_CREATURE_SPRITE);
        final File f4 = new File(defaultDir + FILE_FOIL_SPRITE);

        try {
            bimDefaultSprite = ImageIO.read(f1);
            bimPreferredSprite = ImageIO.read(f2);

            bimCreatures = ImageIO.read(f3);
            bimFoils = ImageIO.read(f4);

            preferredH = bimPreferredSprite.getHeight();
            preferredW = bimPreferredSprite.getWidth();
        }
        catch (Exception e) {
            System.err.println(this.notfound + " a sprite.");
            e.printStackTrace();
        }

        // Pre-derive most fonts (plain, bold, and italic).
        // Exceptions handled inside method.
        this.font = GuiUtils.newFont(FILE_SKINS_DIR + preferredName + "/" + FILE_FONT);
        plainFonts = new HashMap<Integer, Font>();
        setFontAndIncrement(10);
        setFontAndIncrement(11);
        setFontAndIncrement(12);
        setFontAndIncrement(13);
        setFontAndIncrement(14);
        setFontAndIncrement(15);
        setFontAndIncrement(16);
        setFontAndIncrement(18);
        setFontAndIncrement(20);
        setFontAndIncrement(22);

        boldFonts = new HashMap<Integer, Font>();
        setBoldFontAndIncrement(12);
        setBoldFontAndIncrement(14);
        setBoldFontAndIncrement(16);
        setBoldFontAndIncrement(18);
        setBoldFontAndIncrement(20);

        italicFonts = new HashMap<Integer, Font>();
        setItalicFontAndIncrement(12);
        setItalicFontAndIncrement(14);

        // Put various images into map (except sprite and splash).
        // Exceptions handled inside method.
        this.setIcon(Backgrounds.BG_TEXTURE, preferredDir + FILE_TEXTURE_BG);
        this.setIcon(Backgrounds.BG_MATCH, preferredDir + FILE_MATCH_BG);

        this.setColor(Colors.CLR_THEME, this.getColorFromPixel(bimPreferredSprite.getRGB(70, 10)));
        this.setColor(Colors.CLR_BORDERS, this.getColorFromPixel(bimPreferredSprite.getRGB(70, 30)));
        this.setColor(Colors.CLR_ZEBRA, this.getColorFromPixel(bimPreferredSprite.getRGB(70, 50)));
        this.setColor(Colors.CLR_HOVER, this.getColorFromPixel(bimPreferredSprite.getRGB(70, 70)));
        this.setColor(Colors.CLR_ACTIVE, this.getColorFromPixel(bimPreferredSprite.getRGB(70, 90)));
        this.setColor(Colors.CLR_INACTIVE, this.getColorFromPixel(bimPreferredSprite.getRGB(70, 110)));
        this.setColor(Colors.CLR_TEXT, this.getColorFromPixel(bimPreferredSprite.getRGB(70, 130)));

        // Run through enums and load their coords.
        for (ZoneIcons e : ZoneIcons.values()) { this.setIcon(e); }
        for (ManaIcons e : ManaIcons.values()) { this.setImage(e); }
        for (ColorlessManaIcons e : ColorlessManaIcons.values()) { this.setImage(e); }
        for (GameplayIcons e : GameplayIcons.values()) { this.setImage(e); }
        for (DockIcons e : DockIcons.values()) { this.setIcon(e); }
        for (ForgeIcons e : ForgeIcons.values()) { this.setIcon(e); }
        for (ButtonImages e : ButtonImages.values()) { this.setIcon(e); }
        for (QuestIcons e : QuestIcons.values()) { this.setIcon(e); }

        // Foils and creatures have a separate sprite, so use specific methods.
        for (Foils e : Foils.values()) { this.setFoil(e); }
        for (CreatureIcons e : CreatureIcons.values()) { this.setCreature(e); }
    }

    /** (Should) clear memory resources for this skin object. */
    public void unloadSkin() {
        this.icons.clear();
        this.images.clear();
        this.colors.clear();
        this.foils.clear();
        this.plainFonts.clear();
        this.boldFonts.clear();
        this.italicFonts.clear();

        this.bimCreatures.flush();
        this.bimDefaultSprite.flush();
        this.bimFoils.flush();
        this.bimPreferredSprite.flush();

        this.bimCreatures = null;
        this.bimDefaultSprite = null;
        this.bimFoils = null;
        this.bimPreferredSprite = null;
    }

    /** @return {@link java.awt.font} font */
    public Font getFont() {
        return this.font;
    }

    /**
     * @param size - integer, pixel size
     * @return {@link java.awt.font} font1
     */
    public Font getFont(int size) {
        if (plainFonts.get(size) == null) {
            plainFonts.put(size, getFont().deriveFont(Font.PLAIN, size));
        }
        return plainFonts.get(size);
    }

    /**
     * @param size - integer, pixel size
     * @return {@link java.awt.font} font1
     */
    public Font getBoldFont(int size) {
        if (boldFonts.get(size) == null) {
            boldFonts.put(size, getFont().deriveFont(Font.BOLD, size));
        }
        return boldFonts.get(size);
    }

    /**
     * @param size - integer, pixel size
     * @return {@link java.awt.font} font1
     */
    public Font getItalicFont(int size) {
        if (boldFonts.get(size) == null) {
            italicFonts.put(size, getFont().deriveFont(Font.ITALIC, size));
        }
        return italicFonts.get(size);
    }

    /**
     * Gets the name.
     * 
     * @return Name of the current skin.
     */
    public String getName() {
        return this.preferredName;
    }

    /**
     * Gets an image.
     *
     * @param s0 &emsp; SkinProp enum
     * @return Image
     */
    public Image getImage(final SkinProp s0) {
        return this.images.get(s0);
    }

    /**
     * Gets an icon.
     *
     * @param s0 &emsp; SkinProp enum
     * @return ImageIcon
     */
    public ImageIcon getIcon(final SkinProp s0) {
        return this.icons.get(s0);
    }

    /**
     * Gets a scaled version of an icon from this skin's icon map.
     * 
     * @param s0
     *            String icon address
     * @param w0
     *            int new width
     * @param h0
     *            int new height
     * @return ImageIcon
     */
    public ImageIcon getIcon(final SkinProp s0, int w0, int h0) {
        w0 = (w0 < 1) ? 1 : w0;
        h0 = (h0 < 1) ? 1 : h0;

        final Image original =
         (this.icons.get(s0) == null
             ? this.images.get(s0)
             : this.icons.get(s0).getImage());

        final BufferedImage scaled = new BufferedImage(w0, h0, BufferedImage.TYPE_INT_ARGB);

        final Graphics2D g2d = scaled.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(original, 0, 0, w0, h0, 0, 0, original.getWidth(null), original.getHeight(null), null);
        g2d.dispose();

        return new ImageIcon(scaled);
    }

    /**
     * Retrieves a color from this skin's color map.
     * 
     * @param s0 &emsp; Skin property (from enum)
     * @return Color
     */
    public Color getColor(final SkinProp s0) {
        return this.colors.get(s0);
    }

    /**
     * Gets the skins.
     *
     * @return the skins
     */
    public static ArrayList<String> getSkins() {
        final ArrayList<String> mySkins = new ArrayList<String>();

        File dir = new File(FILE_SKINS_DIR);
        String[] children = dir.list();
        if (children == null) {
            System.err.println("FSkin > can't find skins directory!");
        } else {
            for (int i = 0; i < children.length; i++) {
                if (children[i].equalsIgnoreCase(".svn")) { continue; }
                if (children[i].equalsIgnoreCase(".DS_Store")) { continue; }
                mySkins.add(children[i]);
            }
        }

        return mySkins;
    }

    /**
     * <p>
     * getColorFromPixel.
     * </p>
     * 
     * @param {@link java.lang.Integer} pixel information
     */
    private Color getColorFromPixel(final int pixel) {
        int r, g, b, a;
        a = (pixel >> 24) & 0x000000ff;
        r = (pixel >> 16) & 0x000000ff;
        g = (pixel >> 8) & 0x000000ff;
        b = (pixel) & 0x000000ff;
        return new Color(r, g, b, a);
    }

    private BufferedImage testPreferredSprite(SkinProp s0) {
        int[] coords = ((Coords) s0).getCoords();
        int x0 = coords[0];
        int y0 = coords[1];
        int w0 = coords[2];
        int h0 = coords[3];

        // Test if requested sub-image in inside bounds of preferred sprite.
        // (Height and width of preferred sprite were set in loadFontAndImages.)
        if (x0 > preferredW || x0 + w0 > preferredW
                || y0 > preferredH || y0 + h0 > preferredH) {
            return bimDefaultSprite;
        }

        // Test if various points of requested sub-image are transparent.
        // If any return true, image exists.
        int x = 0, y = 0;
        Color c;

        // Center
        x = (int) (x0 + w0 / 2);
        y = (int) (y0 + h0 / 2);
        c = this.getColorFromPixel(bimPreferredSprite.getRGB(x, y));
        if (c.getAlpha() != 0) { return bimPreferredSprite; }

        x += 2;
        y += 2;
        c = this.getColorFromPixel(bimPreferredSprite.getRGB(x, y));
        if (c.getAlpha() != 0) { return bimPreferredSprite; }

        x -= 4;
        c = this.getColorFromPixel(bimPreferredSprite.getRGB(x, y));
        if (c.getAlpha() != 0) { return bimPreferredSprite; }

        y -= 4;
        c = this.getColorFromPixel(bimPreferredSprite.getRGB(x, y));
        if (c.getAlpha() != 0) { return bimPreferredSprite; }

        x += 4;
        c = this.getColorFromPixel(bimPreferredSprite.getRGB(x, y));
        if (c.getAlpha() != 0) { return bimPreferredSprite; }

        return bimDefaultSprite;
    }

    private void setFoil(final SkinProp s0) {
        int[] coords = ((Coords) s0).getCoords();
        int x0 = coords[0];
        int y0 = coords[1];
        int w0 = coords[2];
        int h0 = coords[3];

        this.foils.put(s0, bimFoils.getSubimage(x0, y0, w0, h0));
    }

    private void setCreature(final SkinProp s0) {
        int[] coords = ((Coords) s0).getCoords();
        int x0 = coords[0];
        int y0 = coords[1];
        int w0 = coords[2];
        int h0 = coords[3];

        this.icons.put(s0, new ImageIcon(bimCreatures.getSubimage(x0, y0, w0, h0)));
    }

    private void setColor(final SkinProp s0, final Color c0) {
        this.colors.put(s0, c0);
    }

    private void setFontAndIncrement(int size) {
        plainFonts.put(size, font.deriveFont(Font.PLAIN, size));
        if (barProgress != null) { barProgress.increment(); }
    }

    private void setBoldFontAndIncrement(int size) {
        boldFonts.put(size, font.deriveFont(Font.BOLD, size));
        if (barProgress != null) { barProgress.increment(); }
    }

    private void setItalicFontAndIncrement(int size) {
        italicFonts.put(size, font.deriveFont(Font.ITALIC, size));
        if (barProgress != null) { barProgress.increment(); }
    }

    private void setIcon(final SkinProp s0) {
        int[] coords = ((Coords) s0).getCoords();
        int x0 = coords[0];
        int y0 = coords[1];
        int w0 = coords[2];
        int h0 = coords[3];

        BufferedImage img = testPreferredSprite(s0);
        this.icons.put(s0, new ImageIcon(img.getSubimage(x0, y0, w0, h0)));
    }

    /**
     * Sets an icon in this skin's icon map from a file address.
     * Throws IO exception for debugging if needed.
     * 
     * @param s0
     *            &emsp; Skin property (from enum)
     * @param s1
     *            &emsp; File address
     */
    private void setIcon(final SkinProp s0, final String s1) {
        try {
            final File file = new File(s1);
            ImageIO.read(file);
        } catch (IOException e) {
            System.err.println(this.notfound + preferredDir + FILE_SPLASH);
            e.printStackTrace();
        }
        this.icons.put(s0, new ImageIcon(s1));
    }

    /**
     * Sets an icon in this skin's icon map from a buffered image.
     * 
     * @param s0 &emsp; Skin property (from enum)
     * @param bi0 &emsp; BufferedImage
     */
    private void setIcon(final SkinProp s0, final BufferedImage bi0) {
        this.icons.put(s0, new ImageIcon(bi0));
    }

    /**
     * setImage, with auto-scaling assumed true.
     * 
     * @param s0
     */
    private void setImage(final SkinProp s0) {
        setImage(s0, true);
    }

    /**
     * Checks the preferred sprite for existence of a sub-image
     * defined by X, Y, W, H.
     * 
     * If an image is not present at those coordinates, default
     * icon is substituted.
     * 
     * The result is saved in a HashMap.
     * 
     * @param s0 &emsp; An address in the hashmap, derived from SkinProp enum
     */
    private void setImage(final SkinProp s0, boolean scale) {
        int[] coords = ((Coords) s0).getCoords();
        int x0 = coords[0];
        int y0 = coords[1];
        int w0 = coords[2];
        int h0 = coords[3];
        int newW = (coords.length == 6 ? coords[4] : 0);
        int newH = (coords.length == 6 ? coords[5] : 0);

        BufferedImage img = testPreferredSprite(s0);
        BufferedImage bi0 = img.getSubimage(x0, y0, w0, h0);

        if (scale && newW != 0) {
            this.images.put(s0, bi0.getScaledInstance(newW, newH, Image.SCALE_AREA_AVERAGING));
        }
        else {
            this.images.put(s0, bi0);
        }
    }
}
