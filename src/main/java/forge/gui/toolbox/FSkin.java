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
package forge.gui.toolbox;

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

import forge.gui.GuiUtils;
import forge.view.SplashFrame;

/**
 * Assembles settings from selected or default theme as appropriate. Saves in a
 * hashtable, access using .get(settingName) method.
 * 
 */

public enum FSkin {
    /** Singleton instance of skin. */
    SINGLETON_INSTANCE;

    /** */
    public enum Backgrounds implements SkinProp { /** */
        BG_SPLASH (null), /** */
        BG_TEXTURE (null), /** */
        BG_MATCH (null);  /** */

        private int[] coords;
        /** @param xy &emsp; int[] coordinates */
        Backgrounds(final int[] xy) { this.coords = xy; }
        /** @return int[] */
        public int[] getCoords() { return coords; }
    }

    /** */
    public enum Colors implements SkinProp { /** */
        CLR_THEME                   (new int[] {70, 10}), /** */
        CLR_BORDERS                 (new int[] {70, 30}), /** */
        CLR_ZEBRA                   (new int[] {70, 50}), /** */
        CLR_HOVER                   (new int[] {70, 70}), /** */
        CLR_ACTIVE                  (new int[] {70, 90}), /** */
        CLR_INACTIVE                (new int[] {70, 110}), /** */
        CLR_TEXT                    (new int[] {70, 130}), /** */
        CLR_PHASE_INACTIVE_ENABLED  (new int[] {70, 150}), /** */
        CLR_PHASE_INACTIVE_DISABLED (new int[] {70, 170}), /** */
        CLR_PHASE_ACTIVE_ENABLED    (new int[] {70, 190}), /** */
        CLR_PHASE_ACTIVE_DISABLED   (new int[] {70, 210}), /** */
        CLR_THEME2                  (new int[] {70, 230}), /** */
        CLR_OVERLAY                 (new int[] {70, 250});

        private int[] coords;
        /** @param xy &emsp; int[] coordinates */
        Colors(final int[] xy) { this.coords = xy; }
        /** @return int[] */
        public int[] getCoords() { return coords; }
    }

    /** int[] can hold [xcoord, ycoord, width, height, newwidth, newheight]. */
    public enum ZoneImages implements SkinProp { /** */
        ICO_HAND        (new int[] {280, 40, 40, 40}), /** */
        ICO_LIBRARY     (new int[] {280, 0, 40, 40}), /** */
        ICO_EXILE       (new int[] {320, 40, 40, 40}), /** */
        ICO_FLASHBACK   (new int[] {280, 80, 40, 40}), /** */
        ICO_GRAVEYARD   (new int[] {320, 0, 40, 40}), /** */
        ICO_POISON      (new int[] {320, 80, 40, 40});

        private int[] coords;
        /** @param xy &emsp; int[] coordinates */
        ZoneImages(final int[] xy) { this.coords = xy; }
        /** @return int[] */
        public int[] getCoords() { return coords; }
    }

    /** int[] can hold [xcoord, ycoord, width, height, newwidth, newheight]. */
    public enum ManaImages implements SkinProp { /** */
        IMG_BLACK       (new int[] {360, 160, 40, 40}), /** */
        IMG_RED         (new int[] {400, 160, 40, 40}), /** */
        IMG_COLORLESS   (new int[] {440, 160, 40, 40}), /** */
        IMG_BLUE        (new int[] {360, 200, 40, 40}), /** */
        IMG_GREEN       (new int[] {400, 200, 40, 40}), /** */
        IMG_WHITE       (new int[] {440, 200, 40, 40}), /** */
        IMG_2B          (new int[] {360, 400, 40, 40}), /** */
        IMG_2G          (new int[] {400, 400, 40, 40}), /** */
        IMG_2R          (new int[] {440, 400, 40, 40}), /** */
        IMG_2U          (new int[] {440, 360, 40, 40}), /** */
        IMG_2W          (new int[] {400, 360, 40, 40}), /** */
        IMG_BLACK_GREEN (new int[] {360, 240, 40, 40}), /** */
        IMG_BLACK_RED   (new int[] {400, 240, 40, 40}), /** */
        IMG_GREEN_BLUE  (new int[] {360, 280, 40, 40}), /** */
        IMG_GREEN_WHITE (new int[] {440, 280, 40, 40}), /** */
        IMG_RED_GREEN   (new int[] {360, 320, 40, 40}), /** */
        IMG_RED_WHITE   (new int[] {400, 320, 40, 40}), /** */
        IMG_BLUE_BLACK  (new int[] {440, 240, 40, 40}), /** */
        IMG_BLUE_RED    (new int[] {440, 320, 40, 40}), /** */
        IMG_WHITE_BLACK (new int[] {400, 280, 40, 40}), /** */
        IMG_WHITE_BLUE  (new int[] {360, 360, 40, 40}), /** */
        IMG_PHRYX_BLUE  (new int[] {320, 200, 40, 40}), /** */
        IMG_PHRYX_WHITE (new int[] {320, 240, 40, 40}), /** */
        IMG_PHRYX_RED   (new int[] {320, 280, 40, 40}), /** */
        IMG_PHRYX_GREEN (new int[] {320, 320, 40, 40}), /** */
        IMG_PHRYX_BLACK (new int[] {320, 360, 40, 40});

        private int[] coords;
        /** @param xy &emsp; int[] coordinates */
        ManaImages(final int[] xy) { this.coords = xy; }
        /** @return int[] */
        public int[] getCoords() { return coords; }
    }

    /** int[] can hold [xcoord, ycoord, width, height, newwidth, newheight]. */
    public enum ColorlessManaImages implements SkinProp { /** */
        IMG_0   (new int[] {640, 200, 20, 20}), /** */
        IMG_1   (new int[] {660, 200, 20, 20}), /** */
        IMG_2   (new int[] {640, 220, 20, 20}), /** */
        IMG_3   (new int[] {660, 220, 20, 20}), /** */
        IMG_4   (new int[] {640, 240, 20, 20}), /** */
        IMG_5   (new int[] {660, 240, 20, 20}), /** */
        IMG_6   (new int[] {640, 260, 20, 20}), /** */
        IMG_7   (new int[] {660, 260, 20, 20}), /** */
        IMG_8   (new int[] {640, 280, 20, 20}), /** */
        IMG_9   (new int[] {660, 280, 20, 20}), /** */
        IMG_10  (new int[] {640, 300, 20, 20}), /** */
        IMG_11  (new int[] {660, 300, 20, 20}), /** */
        IMG_12  (new int[] {640, 320, 20, 20}), /** */
        IMG_15  (new int[] {660, 340, 20, 20}), /** */
        IMG_16  (new int[] {640, 360, 20, 20}), /** */
        IMG_20  (new int[] {640, 400, 20, 20}), /** */
        IMG_X   (new int[] {640, 420, 20, 20}), /** */
        IMG_Y   (new int[] {660, 420, 20, 20}), /** */
        IMG_Z   (new int[] {640, 440, 20, 20});

        private int[] coords;
        /** @param xy &emsp; int[] coordinates */
        ColorlessManaImages(final int[] xy) { this.coords = xy; }
        /** @return int[] */
        public int[] getCoords() { return coords; }
    }

    /** int[] can hold [xcoord, ycoord, width, height, newwidth, newheight]. */
    public enum GameplayImages implements SkinProp { /** */
        IMG_SNOW            (new int[] {320, 160, 40, 40}), /** */
        IMG_TAP             (new int[] {660, 440, 20, 20}), /** */
        IMG_UNTAP           (new int[] {640, 460, 20, 20}), /** */
        IMG_SLASH           (new int[] {660, 400, 10, 13}), /** */
        IMG_ATTACK          (new int[] {160, 320, 80, 80, 32, 32}), /** */
        IMG_DEFEND          (new int[] {160, 400, 80, 80, 32, 32}), /** */
        IMG_SUMMONSICK      (new int[] {240, 400, 80, 80, 32, 32}), /** */
        IMG_PHASING         (new int[] {240, 320, 80, 80, 32, 32}), /** */
        IMG_COUNTERS1       (new int[] {0, 320, 80, 80}), /** */
        IMG_COUNTERS2       (new int[] {0, 400, 80, 80}), /** */
        IMG_COUNTERS3       (new int[] {80, 320, 80, 80}), /** */
        IMG_COUNTERS_MULTI  (new int[] {80, 400, 80, 80});

        private int[] coords;
        /** @param xy &emsp; int[] coordinates */
        GameplayImages(final int[] xy) { this.coords = xy; }
        /** @return int[] */
        public int[] getCoords() { return coords; }
    }

    /** */
    public enum Foils implements SkinProp { /** */
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
        Foils(final int[] xy) { this.coords = xy; }
        /** @return int[] */
        public int[] getCoords() { return coords; }
    }

    /** */
    public enum DockIcons implements SkinProp { /** */
        ICO_SHORTCUTS   (new int[] {160, 640, 80, 80}), /** */
        ICO_SETTINGS    (new int[] {80, 640, 80, 80}), /** */
        ICO_ENDTURN     (new int[] {320, 640, 80, 80}), /** */
        ICO_CONCEDE     (new int[] {240, 640, 80, 80}), /** */
        ICO_REVERTLAYOUT (new int[] {400, 720, 80, 80}), /** */
        ICO_DECKLIST    (new int[] {400, 640, 80, 80});

        private int[] coords;
        /** @param xy &emsp; int[] coordinates */
        DockIcons(final int[] xy) { this.coords = xy; }
        /** @return int[] */
        public int[] getCoords() { return coords; }
    }

    /** */
    public enum QuestIcons implements SkinProp { /** */
        ICO_ZEP         (new int[] {0, 480, 80, 80}), /** */
        ICO_GEAR        (new int[] {80, 480, 80, 80}), /** */
        ICO_GOLD        (new int[] {160, 480, 80, 80}), /** */
        ICO_ELIXIR      (new int[] {240, 480, 80, 80}), /** */
        ICO_BOOK        (new int[] {320, 480, 80, 80}), /** */
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
        ICO_BREW        (new int[] {560, 560, 80, 80}), /** */

        ICO_MINUS       (new int[] {560, 640, 80, 80}), /** */
        ICO_PLUS        (new int[] {480, 640, 80, 80}), /** */
        ICO_PLUSPLUS    (new int[] {480, 720, 80, 80});

        private int[] coords;
        /** @param xy &emsp; int[] coordinates */
        QuestIcons(final int[] xy) { this.coords = xy; }
        /** @return int[] */
        public int[] getCoords() { return coords; }
    }

    /** */
    public enum ForgeIcons implements SkinProp { /** */
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
        ForgeIcons(final int[] xy) { this.coords = xy; }
        /** @return int[] */
        public int[] getCoords() { return coords; }
    }

    /** */
    public enum LayoutImages implements SkinProp { /** */
        IMG_HANDLE  (new int[] {320, 450, 80, 20}), /** */
        IMG_CUR_L   (new int[] {644, 524, 32, 32}), /** */
        IMG_CUR_R   (new int[] {644, 564, 32, 32}), /** */
        IMG_CUR_T   (new int[] {644, 604, 32, 32}), /** */
        IMG_CUR_B   (new int[] {644, 644, 32, 32}), /** */
        IMG_CUR_TAB (new int[] {644, 684, 32, 32});

        private int[] coords;
        /** @param xy &emsp; int[] coordinates */
        LayoutImages(final int[] xy) { this.coords = xy; }
        /** @return int[] */
        public int[] getCoords() { return coords; }
    }

    /** */
    public enum ButtonImages implements SkinProp { /** */
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
        ButtonImages(final int[] xy) { this.coords = xy; }
        /** @return int[] */
        public int[] getCoords() { return coords; }
    }

    /** Properties of various components that make up the skin.
     * This interface allows all enums to be under the same roof.
     * It also enforces a getter for coordinate locations in sprites. */
    public interface SkinProp {
        /** @return int[] */
        int[] getCoords();
    }

    private static Map<SkinProp, ImageIcon> icons;
    private static Map<SkinProp, Image> images;
    private static Map<SkinProp, Color> colors;

    private static Map<Integer, Font> plainFonts;
    private static Map<Integer, Font> boldFonts;
    private static Map<Integer, Font> italicFonts;
    private static Map<Integer, Image> avatars;

    private static final String
        FILE_SKINS_DIR = "res/skins/",
        FILE_ICON_SPRITE = "sprite_icons.png",
        FILE_FOIL_SPRITE = "sprite_foils.png",
        FILE_AVATAR_SPRITE = "sprite_avatars.png",
        FILE_FONT = "font1.ttf",
        FILE_SPLASH = "bg_splash.png",
        FILE_MATCH_BG = "bg_match.jpg",
        FILE_TEXTURE_BG = "bg_texture.jpg",
        DEFAULT_DIR = FILE_SKINS_DIR + "default/";

    private static String preferredDir;
    private static String preferredName;
    private static Font font;
    private static BufferedImage bimDefaultSprite, bimPreferredSprite, bimFoils,
        bimDefaultAvatars, bimPreferredAvatars;
    private static int x0, y0, w0, h0, newW, newH, preferredW, preferredH;
    private static int[] tempCoords;

    /**
     * Loads a "light" version of FSkin, just enough for the splash screen:
     * skin name. Generates custom skin settings, fonts, and backgrounds.
     * 
     * 
     * @param skinName
     *            the skin name
     */
    public static void loadLight(final String skinName) {
        // No need for this method to be loaded while on the EDT.
        GuiUtils.checkEDT("FSkin$constructor", false);

        // Non-default (preferred) skin name and dir.
        FSkin.preferredName = skinName.toLowerCase().replace(' ', '_');
        FSkin.preferredDir = FILE_SKINS_DIR + preferredName + "/";

        if (FSkin.icons != null) { FSkin.icons.clear(); }
        if (FSkin.images != null) { FSkin.images.clear(); }
        if (FSkin.colors != null) { FSkin.colors.clear(); }

        FSkin.icons = new HashMap<SkinProp, ImageIcon>();
        FSkin.images = new HashMap<SkinProp, Image>();
        FSkin.colors = new HashMap<SkinProp, Color>();

        final File f = new File(preferredDir + FILE_SPLASH);
        if (!f.exists()) {
            FSkin.loadLight("default");
        }
        else {
            final BufferedImage img;
            try {
                img = ImageIO.read(f);

                final int h = img.getHeight();
                final int w = img.getWidth();

                FSkin.setIcon(Backgrounds.BG_SPLASH, img.getSubimage(0, 0, w, h - 100));

                UIManager.put("ProgressBar.background", FSkin.getColorFromPixel(img.getRGB(25, h - 75)));
                UIManager.put("ProgressBar.selectionBackground", FSkin.getColorFromPixel(img.getRGB(75, h - 75)));
                UIManager.put("ProgressBar.foreground", FSkin.getColorFromPixel(img.getRGB(25, h - 25)));
                UIManager.put("ProgressBar.selectionForeground", FSkin.getColorFromPixel(img.getRGB(75, h - 25)));
                UIManager.put("ProgressBar.border", new LineBorder(Color.BLACK, 0));
            } catch (final IOException e) {
                e.printStackTrace();
            }
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
    public static void loadFull() {
        // No need for this method to be loaded while on the EDT.
        GuiUtils.checkEDT("FSkin$load", false);

        // Preferred skin name must be called via loadLight() method,
        // which does some cleanup and init work.
        if (FSkin.preferredName.isEmpty()) { FSkin.loadLight("default"); }

        // Everything OK?
        final FProgressBar barProgress = SplashFrame.PROGRESS_BAR;

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                barProgress.reset();
                barProgress.setShowETA(false);
                barProgress.setDescription("Processing image sprites: ");
            }
        });

        // Grab and test various sprite files.
        barProgress.setMaximum(4);
        final File f1 = new File(DEFAULT_DIR + FILE_ICON_SPRITE);
        final File f2 = new File(preferredDir + FILE_ICON_SPRITE);
        final File f3 = new File(DEFAULT_DIR + FILE_FOIL_SPRITE);
        final File f4 = new File(DEFAULT_DIR + FILE_AVATAR_SPRITE);
        final File f5 = new File(preferredDir + FILE_AVATAR_SPRITE);

        try {
            bimDefaultSprite = ImageIO.read(f1);
            barProgress.increment();
            bimPreferredSprite = ImageIO.read(f2);
            barProgress.increment();
            bimFoils = ImageIO.read(f3);
            barProgress.increment();
            bimDefaultAvatars = ImageIO.read(f4);

            if (f5.exists()) { bimPreferredAvatars = ImageIO.read(f5); }

            barProgress.increment();

            preferredH = bimPreferredSprite.getHeight();
            preferredW = bimPreferredSprite.getWidth();
        }
        catch (final Exception e) {
            System.err.println("FSkin$loadFull: Missing a sprite (default icons, "
                    + "preferred icons, or foils.");
            e.printStackTrace();
        }

        // Pre-derive most fonts (plain, bold, and italic).
        // Exceptions handled inside method.
        FSkin.font = GuiUtils.newFont(FILE_SKINS_DIR + preferredName + "/" + FILE_FONT);
        plainFonts = new HashMap<Integer, Font>();
        boldFonts = new HashMap<Integer, Font>();
        italicFonts = new HashMap<Integer, Font>();
        for (int i = 10; i <= 22; i++) { setFont(i); }
        for (int i = 10; i <= 20; i += 2) { setBoldFont(i); }
        for (int i = 12; i <= 14; i += 2) { setItalicFont(i); }

        // Put various images into map (except sprite and splash).
        // Exceptions handled inside method.
        FSkin.setIcon(Backgrounds.BG_TEXTURE, preferredDir + FILE_TEXTURE_BG);
        FSkin.setIcon(Backgrounds.BG_MATCH, preferredDir + FILE_MATCH_BG);

        // Run through enums and load their coords.
        for (final Colors e : Colors.values()) { FSkin.setColor(e); }
        for (final ZoneImages e : ZoneImages.values())                    { FSkin.setImage(e); }
        for (final DockIcons e : DockIcons.values())                      { FSkin.setIcon(e); }
        for (final ForgeIcons e : ForgeIcons.values())                    { FSkin.setIcon(e); }
        for (final ButtonImages e : ButtonImages.values())                { FSkin.setIcon(e); }
        for (final QuestIcons e : QuestIcons.values())                    { FSkin.setIcon(e); }

        for (final ManaImages e : ManaImages.values())                    { FSkin.setImage(e); }
        for (final ColorlessManaImages e : ColorlessManaImages.values())  { FSkin.setImage(e); }
        for (final GameplayImages e : GameplayImages.values())            { FSkin.setImage(e); }
        for (final LayoutImages e : LayoutImages.values())                { FSkin.setImage(e); }

        // Foils have a separate sprite, so uses a specific method.
        for (final Foils e : Foils.values()) { FSkin.setFoil(e); }

        // Assemble avatar images
        FSkin.assembleAvatars();

        // Images loaded; can start UI init.
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                barProgress.setShowETA(false);
                barProgress.setShowCount(false);
                barProgress.setDescription("Creating display components.");
            }
        });

        // Clear references to buffered images
        FSkin.bimDefaultSprite.flush();
        FSkin.bimFoils.flush();
        FSkin.bimPreferredSprite.flush();
        FSkin.bimDefaultAvatars.flush();

        if (FSkin.bimPreferredAvatars != null) { FSkin.bimPreferredAvatars.flush(); }

        FSkin.bimDefaultSprite = null;
        FSkin.bimFoils = null;
        FSkin.bimPreferredSprite = null;
        FSkin.bimDefaultAvatars = null;
        FSkin.bimPreferredAvatars = null;
    }

    /** @return {@link java.awt.font} font */
    public static Font getFont() {
        return FSkin.font;
    }

    /**
     * @param size - integer, pixel size
     * @return {@link java.awt.font} font1
     */
    public static Font getFont(final int size) {
        if (plainFonts.get(size) == null) {
            plainFonts.put(size, getFont().deriveFont(Font.PLAIN, size));
        }
        return plainFonts.get(size);
    }

    /**
     * @param size - integer, pixel size
     * @return {@link java.awt.font} font1
     */
    public static Font getBoldFont(final int size) {
        if (boldFonts.get(size) == null) {
            boldFonts.put(size, getFont().deriveFont(Font.BOLD, size));
        }
        return boldFonts.get(size);
    }

    /**
     * @param size - integer, pixel size
     * @return {@link java.awt.font} font1
     */
    public static Font getItalicFont(final int size) {
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
    public static String getName() {
        return FSkin.preferredName;
    }

    /**
     * Gets an image.
     *
     * @param s0 &emsp; SkinProp enum
     * @return {@link java.awt.Image}
     */
    public static Image getImage(final SkinProp s0) {
        if (FSkin.images.get(s0) == null) {
            throw new NullPointerException("Can't find an image for SkinProp " + s0);
         }
        return FSkin.images.get(s0);
    }

    /**
     * Gets an icon.
     *
     * @param s0 &emsp; SkinProp enum
     * @return {@link javax.swing.ImageIcon}
     */
    public static ImageIcon getIcon(final SkinProp s0) {
        if (FSkin.icons.get(s0) == null) {
           throw new NullPointerException("Can't find an icon for SkinProp " + s0);
        }
        return FSkin.icons.get(s0);
    }

    /**
     * Gets a scaled version of an image from this skin's image map.
     * 
     * @param s0
     *            String image enum
     * @param w0
     *            int new width
     * @param h0
     *            int new height
     * @return {@link java.awt.Image}
     */
    public static Image getImage(final SkinProp s0, int w0, int h0) {
        w0 = (w0 < 1) ? 1 : w0;
        h0 = (h0 < 1) ? 1 : h0;

        final Image original = FSkin.images.get(s0);

        final BufferedImage scaled = new BufferedImage(w0, h0, BufferedImage.TYPE_INT_ARGB);

        final Graphics2D g2d = scaled.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(original, 0, 0, w0, h0, 0, 0, original.getWidth(null), original.getHeight(null), null);
        g2d.dispose();

        return scaled;
    }

    /**
     * Retrieves a color from this skin's color map.
     * 
     * @param s0 &emsp; Skin property (from enum)
     * @return Color
     */
    public static Color getColor(final SkinProp s0) {
        return colors.get(s0);
    }

    /**
     * Gets the skins.
     *
     * @return the skins
     */
    public static ArrayList<String> getSkins() {
        final ArrayList<String> mySkins = new ArrayList<String>();

        final File dir = new File(FILE_SKINS_DIR);
        final String[] children = dir.list();
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

    /** @return Map<Integer, Image> */
    public static Map<Integer, Image> getAvatars() {
        return avatars;
    }

    /**
     * <p>
     * getColorFromPixel.
     * </p>
     * 
     * @param {@link java.lang.Integer} pixel information
     */
    private static Color getColorFromPixel(final int pixel) {
        int r, g, b, a;
        a = (pixel >> 24) & 0x000000ff;
        r = (pixel >> 16) & 0x000000ff;
        g = (pixel >> 8) & 0x000000ff;
        b = (pixel) & 0x000000ff;
        return new Color(r, g, b, a);
    }

    private static BufferedImage testPreferredSprite(final SkinProp s0) {
        tempCoords = s0.getCoords();
        x0 = tempCoords[0];
        y0 = tempCoords[1];
        w0 = tempCoords[2];
        h0 = tempCoords[3];

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
        c = FSkin.getColorFromPixel(bimPreferredSprite.getRGB(x, y));
        if (c.getAlpha() != 0) { return bimPreferredSprite; }

        x += 2;
        y += 2;
        c = FSkin.getColorFromPixel(bimPreferredSprite.getRGB(x, y));
        if (c.getAlpha() != 0) { return bimPreferredSprite; }

        x -= 4;
        c = FSkin.getColorFromPixel(bimPreferredSprite.getRGB(x, y));
        if (c.getAlpha() != 0) { return bimPreferredSprite; }

        y -= 4;
        c = FSkin.getColorFromPixel(bimPreferredSprite.getRGB(x, y));
        if (c.getAlpha() != 0) { return bimPreferredSprite; }

        x += 4;
        c = FSkin.getColorFromPixel(bimPreferredSprite.getRGB(x, y));
        if (c.getAlpha() != 0) { return bimPreferredSprite; }

        return bimDefaultSprite;
    }

    private static void assembleAvatars() {
        FSkin.avatars = new HashMap<Integer, Image>();
        int counter = 0;
        Color pxTest;

        if (bimPreferredAvatars != null) {
            final int pw = bimPreferredAvatars.getWidth();
            final int ph = bimPreferredAvatars.getHeight();

            for (int j = 0; j < ph; j += 100) {
                for (int i = 0; i < pw; i += 100) {
                    if (i == 0 && j == 0) { continue; }
                    pxTest = FSkin.getColorFromPixel(bimPreferredAvatars.getRGB(i + 50, j + 50));
                    if (pxTest.getAlpha() == 0) { continue; }
                    FSkin.avatars.put(counter++, bimPreferredAvatars.getSubimage(i, j, 100, 100));
                }
            }
        }

        final int aw = bimDefaultAvatars.getWidth();
        final int ah = bimDefaultAvatars.getHeight();

        for (int j = 0; j < ah; j += 100) {
            for (int i = 0; i < aw; i += 100) {
                if (i == 0 && j == 0) { continue; }
                pxTest = FSkin.getColorFromPixel(bimDefaultAvatars.getRGB(i + 50, j + 50));
                if (pxTest.getAlpha() == 0) { continue; }
                FSkin.avatars.put(counter++, bimDefaultAvatars.getSubimage(i, j, 100, 100));
            }
        }
    }

    private static void setFoil(final SkinProp s0) {
        tempCoords = s0.getCoords();
        x0 = tempCoords[0];
        y0 = tempCoords[1];
        w0 = tempCoords[2];
        h0 = tempCoords[3];

        FSkin.images.put(s0, bimFoils.getSubimage(x0, y0, w0, h0));
    }

    private static void setColor(final SkinProp s0) {
        tempCoords = s0.getCoords();
        x0 = tempCoords[0];
        y0 = tempCoords[1];

        FSkin.colors.put(s0, FSkin.getColorFromPixel(bimPreferredSprite.getRGB(x0, y0)));
    }

    private static void setFont(final int size) {
        plainFonts.put(size, font.deriveFont(Font.PLAIN, size));
    }

    private static void setBoldFont(final int size) {
        boldFonts.put(size, font.deriveFont(Font.BOLD, size));
    }

    private static void setItalicFont(final int size) {
        italicFonts.put(size, FSkin.font.deriveFont(Font.ITALIC, size));
    }

    private static void setIcon(final SkinProp s0) {
        tempCoords = s0.getCoords();
        x0 = tempCoords[0];
        y0 = tempCoords[1];
        w0 = tempCoords[2];
        h0 = tempCoords[3];

        final BufferedImage img = testPreferredSprite(s0);

        FSkin.icons.put(s0, new ImageIcon(img.getSubimage(x0, y0, w0, h0)));
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
    private static void setIcon(final SkinProp s0, final String s1) {
        try {
            final File file = new File(s1);
            ImageIO.read(file);
        } catch (final IOException e) {
            e.printStackTrace();
        }
        FSkin.icons.put(s0, new ImageIcon(s1));
    }

    /**
     * Sets an icon in this skin's icon map from a buffered image.
     * 
     * @param s0 &emsp; Skin property (from enum)
     * @param bi0 &emsp; BufferedImage
     */
    private static void setIcon(final SkinProp s0, final BufferedImage bi0) {
        FSkin.icons.put(s0, new ImageIcon(bi0));
    }

    /**
     * setImage, with auto-scaling assumed true.
     * 
     * @param s0
     */
    private static void setImage(final SkinProp s0) {
        FSkin.setImage(s0, true);
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
    private static void setImage(final SkinProp s0, final boolean scale) {
        tempCoords = s0.getCoords();
        x0 = tempCoords[0];
        y0 = tempCoords[1];
        w0 = tempCoords[2];
        h0 = tempCoords[3];
        newW = (tempCoords.length == 6 ? tempCoords[4] : 0);
        newH = (tempCoords.length == 6 ? tempCoords[5] : 0);

        final BufferedImage img = FSkin.testPreferredSprite(s0);
        final BufferedImage bi0 = img.getSubimage(x0, y0, w0, h0);

        if (scale && newW != 0) {
            FSkin.images.put(s0, bi0.getScaledInstance(newW, newH, Image.SCALE_AREA_AVERAGING));
        }
        else {
            FSkin.images.put(s0, bi0);
        }
    }
}
