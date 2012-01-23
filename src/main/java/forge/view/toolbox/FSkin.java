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
    public enum SkinProp {
        /** These correspond to objects stored HashMaps. */
        BG_SPLASH, /** */
        BG_TEXTURE, /** */
        BG_MATCH,  /** */

        CLR_THEME, /** */
        CLR_BORDERS, /** */
        CLR_ZEBRA, /** */
        CLR_HOVER, /** */
        CLR_ACTIVE, /** */
        CLR_INACTIVE, /** */
        CLR_TEXT, /** */

        ICON_ZONE_HAND, /** */
        ICON_ZONE_LIBRARY, /** */
        ICON_ZONE_EXILE, /** */
        ICON_ZONE_FLASHBACK, /** */
        ICON_ZONE_GRAVEYARD, /** */
        ICON_ZONE_POISON, /** */

        ICON_MANA_BLACK, /** */
        ICON_MANA_BLUE, /** */
        ICON_MANA_GREEN, /** */
        ICON_MANA_RED, /** */
        ICON_MANA_WHITE, /** */
        ICON_MANA_COLORLESS, /** */

        ICON_DOCK_SHORTCUTS, /** */
        ICON_DOCK_SETTINGS, /** */
        ICON_DOCK_ENDTURN, /** */
        ICON_DOCK_CONCEDE, /** */
        ICON_DOCK_DECKLIST, /** */

        IMG_LOGO, /** */
        IMG_FAVICON, /** */

        IMG_BTN_START_UP, /** */
        IMG_BTN_START_OVER, /** */
        IMG_BTN_START_DOWN, /** */

        IMG_BTN_UP_LEFT, /** */
        IMG_BTN_UP_CENTER, /** */
        IMG_BTN_UP_RIGHT, /** */

        IMG_BTN_OVER_LEFT, /** */
        IMG_BTN_OVER_CENTER, /** */
        IMG_BTN_OVER_RIGHT, /** */

        IMG_BTN_DOWN_LEFT, /** */
        IMG_BTN_DOWN_CENTER, /** */
        IMG_BTN_DOWN_RIGHT, /** */

        IMG_BTN_FOCUS_LEFT, /** */
        IMG_BTN_FOCUS_CENTER, /** */
        IMG_BTN_FOCUS_RIGHT, /** */

        IMG_BTN_TOGGLE_LEFT, /** */
        IMG_BTN_TOGGLE_CENTER, /** */
        IMG_BTN_TOGGLE_RIGHT, /** */

        IMG_BTN_DISABLED_LEFT, /** */
        IMG_BTN_DISABLED_CENTER, /** */
        IMG_BTN_DISABLED_RIGHT, /** */
    }

    private Map<SkinProp, ImageIcon> icons;
    private Map<SkinProp, Color> colors;

    private Map<Integer, Font> plainFonts;
    private Map<Integer, Font> boldFonts;
    private Map<Integer, Font> italicFonts;

    private static final String
        FILE_SKINS_DIR = "res/images/skins/",
        FILE_SPRITE = "sprite.png",
        FILE_FONT = "font1.ttf",
        FILE_SPLASH = "bg_splash.png",
        FILE_MATCH_BG = "bg_match.jpg",
        FILE_TEXTURE_BG = "bg_texture.jpg";

    private final String notfound = "FSkin.java: Can't find ";
    private final String preferredDir;
    private final String defaultDir;
    private final String preferredName;
    private Font font;
    private BufferedImage bimDefaultSprite;
    private BufferedImage bimPreferredSprite;
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
        this.colors = new HashMap<SkinProp, Color>();

        final File f = new File(preferredDir + FILE_SPLASH);
        final BufferedImage img;
        try {
            img = ImageIO.read(f);

            final int h = img.getHeight();
            final int w = img.getWidth();

            this.setIcon(SkinProp.BG_SPLASH, img.getSubimage(0, 0, w, h - 100));

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

        // Grab and test the two sprite files.
        final File f1 = new File(defaultDir + FILE_SPRITE);
        final File f2 = new File(preferredDir + FILE_SPRITE);

        try {
            bimDefaultSprite = ImageIO.read(f1);
            bimPreferredSprite = ImageIO.read(f2);

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
        this.setIcon(SkinProp.BG_TEXTURE, preferredDir + FILE_TEXTURE_BG);
        this.setIcon(SkinProp.BG_MATCH, preferredDir + FILE_MATCH_BG);

        // Sprite
        final File file = new File(preferredDir + FILE_SPRITE);
        BufferedImage image;

        try {
            image = ImageIO.read(file);

            this.setColor(SkinProp.CLR_THEME, this.getColorFromPixel(image.getRGB(70, 10)));
            this.setColor(SkinProp.CLR_BORDERS, this.getColorFromPixel(image.getRGB(70, 30)));
            this.setColor(SkinProp.CLR_ZEBRA, this.getColorFromPixel(image.getRGB(70, 50)));
            this.setColor(SkinProp.CLR_HOVER, this.getColorFromPixel(image.getRGB(70, 70)));
            this.setColor(SkinProp.CLR_ACTIVE, this.getColorFromPixel(image.getRGB(70, 90)));
            this.setColor(SkinProp.CLR_INACTIVE, this.getColorFromPixel(image.getRGB(70, 110)));
            this.setColor(SkinProp.CLR_TEXT, this.getColorFromPixel(image.getRGB(70, 130)));
        } catch (final IOException e) {
            System.err.println(this.notfound + preferredDir + FILE_SPRITE);
            e.printStackTrace();
        }

        this.setIconAndIncrement(SkinProp.ICON_ZONE_LIBRARY, 280, 0, 40, 40);
        this.setIconAndIncrement(SkinProp.ICON_ZONE_HAND, 280, 40, 40, 40);
        this.setIconAndIncrement(SkinProp.ICON_ZONE_FLASHBACK, 280, 80, 40, 40);
        this.setIconAndIncrement(SkinProp.ICON_ZONE_GRAVEYARD, 320, 0, 40, 40);
        this.setIconAndIncrement(SkinProp.ICON_ZONE_EXILE, 320, 40, 40, 40);
        this.setIconAndIncrement(SkinProp.ICON_ZONE_POISON, 320, 80, 40, 40);

        this.setIconAndIncrement(SkinProp.ICON_MANA_BLACK, 360, 160, 40, 40);
        this.setIconAndIncrement(SkinProp.ICON_MANA_BLUE, 360, 200, 40, 40);
        this.setIconAndIncrement(SkinProp.ICON_MANA_RED, 400, 160, 40, 40);
        this.setIconAndIncrement(SkinProp.ICON_MANA_GREEN, 400, 200, 40, 40);
        this.setIconAndIncrement(SkinProp.ICON_MANA_WHITE, 440, 200, 40, 40);
        this.setIconAndIncrement(SkinProp.ICON_MANA_COLORLESS, 440, 240, 40, 40);

        this.setIconAndIncrement(SkinProp.ICON_DOCK_SETTINGS, 80, 640, 80, 80);
        this.setIconAndIncrement(SkinProp.ICON_DOCK_SHORTCUTS, 160, 640, 80, 80);
        this.setIconAndIncrement(SkinProp.ICON_DOCK_CONCEDE, 240, 640, 80, 80);
        this.setIconAndIncrement(SkinProp.ICON_DOCK_ENDTURN, 320, 640, 80, 80);
        this.setIconAndIncrement(SkinProp.ICON_DOCK_DECKLIST, 400, 640, 80, 80);

        this.setIconAndIncrement(SkinProp.IMG_LOGO, 480, 0, 200, 200);
        this.setIconAndIncrement(SkinProp.IMG_FAVICON, 0, 720, 80, 80);

        this.setIconAndIncrement(SkinProp.IMG_BTN_START_UP, 480, 200, 160, 80);
        this.setIconAndIncrement(SkinProp.IMG_BTN_START_OVER, 480, 280, 160, 80);
        this.setIconAndIncrement(SkinProp.IMG_BTN_START_DOWN, 480, 360, 160, 80);

        this.setIconAndIncrement(SkinProp.IMG_BTN_UP_LEFT, 80, 0, 40, 40);
        this.setIconAndIncrement(SkinProp.IMG_BTN_UP_CENTER, 120, 0, 1, 40);
        this.setIconAndIncrement(SkinProp.IMG_BTN_UP_RIGHT, 160, 0, 40, 40);

        this.setIconAndIncrement(SkinProp.IMG_BTN_OVER_LEFT, 80, 40, 40, 40);
        this.setIconAndIncrement(SkinProp.IMG_BTN_OVER_CENTER, 120, 40, 1, 40);
        this.setIconAndIncrement(SkinProp.IMG_BTN_OVER_RIGHT, 160, 40, 40, 40);

        this.setIconAndIncrement(SkinProp.IMG_BTN_DOWN_LEFT, 80, 80, 40, 40);
        this.setIconAndIncrement(SkinProp.IMG_BTN_DOWN_CENTER, 120, 80, 1, 40);
        this.setIconAndIncrement(SkinProp.IMG_BTN_DOWN_RIGHT, 160, 80, 40, 40);

        this.setIconAndIncrement(SkinProp.IMG_BTN_FOCUS_LEFT, 80, 120, 40, 40);
        this.setIconAndIncrement(SkinProp.IMG_BTN_FOCUS_CENTER, 120, 120, 1, 40);
        this.setIconAndIncrement(SkinProp.IMG_BTN_FOCUS_RIGHT, 160, 120, 40, 40);

        this.setIconAndIncrement(SkinProp.IMG_BTN_TOGGLE_LEFT, 80, 160, 40, 40);
        this.setIconAndIncrement(SkinProp.IMG_BTN_TOGGLE_CENTER, 120, 160, 1, 40);
        this.setIconAndIncrement(SkinProp.IMG_BTN_TOGGLE_RIGHT, 160, 160, 40, 40);

        this.setIconAndIncrement(SkinProp.IMG_BTN_DISABLED_LEFT, 80, 200, 40, 40);
        this.setIconAndIncrement(SkinProp.IMG_BTN_DISABLED_CENTER, 120, 200, 1, 40);
        this.setIconAndIncrement(SkinProp.IMG_BTN_DISABLED_RIGHT, 160, 200, 40, 40);
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
     * Gets the icon.
     *
     * @param s0 &emsp; String address
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

        final BufferedImage original = (BufferedImage) this.icons.get(s0).getImage();
        final BufferedImage scaled = new BufferedImage(w0, h0, BufferedImage.TYPE_INT_ARGB);

        final Graphics2D g2d = scaled.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(original, 0, 0, w0, h0, 0, 0, original.getWidth(), original.getHeight(), null);
        g2d.dispose();

        return new ImageIcon(scaled);
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
     * Checks the preferred sprite for existence of a sub-image
     * defined by X, Y, W, H.
     * 
     * If an image is not present at those coordinates, default
     * icon is substituted.
     * 
     * The result is saved in a HashMap.
     * 
     * @param s0 &emsp; An address in the hashmap, derived from SkinProp enum
     * @param x0 &emsp; X-coord of sub-image
     * @param y0 &emsp; Y-coord of sub-image
     * @param w0 &emsp; Width of sub-image
     * @param h0 &emsp; Height of sub-image
     */
    private void setIconAndIncrement(final SkinProp s0,
            final int x0, final int y0, final int w0, final int h0) {
        // Test if requested sub-image in inside bounds of preferred sprite.
        // (Height and width of preferred sprite were set in loadFontAndImages.)
        Boolean exists = false;

        if (x0 <= preferredW
                && x0 + w0 <= preferredW
                && y0 <= preferredH
                && y0 + h0 <= preferredH) {
            exists = true;
        }

        // Test if various points of requested sub-image are transparent.
        // If any return true, image exists.
        if (exists) {
            int x, y;
            Color c;
            exists = false;

            // Center
            x = (int) (x0 + w0 / 2);
            y = (int) (y0 + h0 / 2);
            c = this.getColorFromPixel(bimPreferredSprite.getRGB(x, y));
            if (c.getAlpha() != 0) { exists = true; }

            x += 2;
            y += 2;
            c = this.getColorFromPixel(bimPreferredSprite.getRGB(x, y));
            if (c.getAlpha() != 0) { exists = true; }

            x -= 4;
            c = this.getColorFromPixel(bimPreferredSprite.getRGB(x, y));
            if (c.getAlpha() != 0) { exists = true; }

            y -= 4;
            c = this.getColorFromPixel(bimPreferredSprite.getRGB(x, y));
            if (c.getAlpha() != 0) { exists = true; }

            x += 4;
            c = this.getColorFromPixel(bimPreferredSprite.getRGB(x, y));
            if (c.getAlpha() != 0) { exists = true; }
        }

        BufferedImage img = (exists ? bimPreferredSprite : bimDefaultSprite);
        BufferedImage bi0 = img.getSubimage(x0, y0, w0, h0);
        this.icons.put(s0, new ImageIcon(bi0));
        if (barProgress != null) { barProgress.increment(); }
    }

    /**
     * Sets an icon in this skin's icon map from a buffered image.
     * 
     * @param s0 &emsp; Skin property (from enum)
     * @param bi0 &emsp; BufferedImage
     */
    public void setIcon(final SkinProp s0, final BufferedImage bi0) {
        this.icons.put(s0, new ImageIcon(bi0));
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
     * Sets a color in this skin's color map.
     * 
     * @param s0
     *            &emsp; Skin property (from enum)
     * @param c0
     *            &emsp; Color
     */
    public void setColor(final SkinProp s0, final Color c0) {
        this.colors.put(s0, c0);
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
}
