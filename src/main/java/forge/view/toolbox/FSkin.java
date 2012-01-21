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

        PRELOAD_EMPTY_BG, /** */
        PRELOAD_EMPTY_TXT, /** */
        PRELOAD_FULL_BG, /** */
        PRELOAD_FULL_TXT, /** */

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
    private final String dirName;
    private Font font;
    private String name = "default";

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
     * FSkin constructor. No arguments, will generate default skin settings,
     * fonts, and backgrounds.
     * @throws IOException for splash image file
     */
    public FSkin() throws IOException {
        this("default");
    }

    /**
     * FSkin constructor, using skin name. Generates custom skin settings,
     * fonts, and backgrounds.
     * 
     * @param skinName
     *            the skin name
     * @throws IOException for splash image file
     */
    public FSkin(final String skinName) throws IOException {
        this.name = skinName;
        this.dirName = FILE_SKINS_DIR + skinName + "/";
        this.icons = new HashMap<SkinProp, ImageIcon>();
        this.colors = new HashMap<SkinProp, Color>();

        final File file = new File(dirName + FILE_SPLASH);
        BufferedImage image;
        try {
            image = ImageIO.read(file);
            final int h = image.getHeight();
            final int w = image.getWidth();
            this.setIcon(SkinProp.BG_SPLASH, image.getSubimage(0, 0, w, h - 100));

            this.setColor(SkinProp.PRELOAD_EMPTY_BG, this.getColorFromPixel(image.getRGB(25, h - 75)));
            this.setColor(SkinProp.PRELOAD_EMPTY_TXT, this.getColorFromPixel(image.getRGB(75, h - 75)));
            this.setColor(SkinProp.PRELOAD_FULL_BG, this.getColorFromPixel(image.getRGB(25, h - 25)));
            this.setColor(SkinProp.PRELOAD_FULL_TXT, this.getColorFromPixel(image.getRGB(75, h - 25)));
        } catch (final IOException e) {
            throw new IOException(this.notfound + dirName + FILE_SPLASH, e);
        }
    }

    /**
     * Loads objects from skin folder, prints brief error if not found.
     * @throws IOException for sprite image filec
     */
    public void loadFontAndImages() throws IOException {
        // Fonts
        this.font = GuiUtils.newFont(dirName + FILE_FONT);
        plainFonts = new HashMap<Integer, Font>();
        plainFonts.put(10, font.deriveFont(Font.PLAIN, 10));
        plainFonts.put(11, font.deriveFont(Font.PLAIN, 11));
        plainFonts.put(12, font.deriveFont(Font.PLAIN, 12));
        plainFonts.put(13, font.deriveFont(Font.PLAIN, 13));
        plainFonts.put(14, font.deriveFont(Font.PLAIN, 14));
        plainFonts.put(15, font.deriveFont(Font.PLAIN, 15));
        plainFonts.put(16, font.deriveFont(Font.PLAIN, 16));
        plainFonts.put(18, font.deriveFont(Font.PLAIN, 18));
        plainFonts.put(20, font.deriveFont(Font.PLAIN, 20));
        plainFonts.put(22, font.deriveFont(Font.PLAIN, 22));

        boldFonts = new HashMap<Integer, Font>();
        boldFonts.put(12, font.deriveFont(Font.BOLD, 12));
        boldFonts.put(14, font.deriveFont(Font.BOLD, 14));
        boldFonts.put(16, font.deriveFont(Font.BOLD, 16));
        boldFonts.put(18, font.deriveFont(Font.BOLD, 18));
        boldFonts.put(20, font.deriveFont(Font.BOLD, 20));

        italicFonts = new HashMap<Integer, Font>();
        italicFonts.put(12, font.deriveFont(Font.ITALIC, 12));
        italicFonts.put(14, font.deriveFont(Font.ITALIC, 14));

        // Images
        this.setIcon(SkinProp.BG_TEXTURE, dirName + FILE_TEXTURE_BG);
        this.setIcon(SkinProp.BG_MATCH, dirName + FILE_MATCH_BG);

        // Sprite
        final File file = new File(dirName + FILE_SPRITE);
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

            // All icons should eventually be set and retrieved using this
            // method. Doublestrike 6-12-11
            this.setIcon(SkinProp.ICON_ZONE_HAND, image.getSubimage(280, 40, 40, 40));
            this.setIcon(SkinProp.ICON_ZONE_LIBRARY, image.getSubimage(280, 0, 40, 40));
            this.setIcon(SkinProp.ICON_ZONE_GRAVEYARD, image.getSubimage(320, 0, 40, 40));
            this.setIcon(SkinProp.ICON_ZONE_EXILE, image.getSubimage(320, 40, 40, 40));
            this.setIcon(SkinProp.ICON_ZONE_FLASHBACK, image.getSubimage(320, 120, 40, 40));
            this.setIcon(SkinProp.ICON_ZONE_POISON, image.getSubimage(320, 80, 40, 40));

            this.setIcon(SkinProp.ICON_MANA_BLACK, image.getSubimage(240, 0, 40, 40));
            this.setIcon(SkinProp.ICON_MANA_BLUE, image.getSubimage(240, 40, 40, 40));
            this.setIcon(SkinProp.ICON_MANA_GREEN, image.getSubimage(240, 120, 40, 40));
            this.setIcon(SkinProp.ICON_MANA_RED, image.getSubimage(240, 80, 40, 40));
            this.setIcon(SkinProp.ICON_MANA_WHITE, image.getSubimage(280, 120, 40, 40));
            this.setIcon(SkinProp.ICON_MANA_COLORLESS, image.getSubimage(280, 80, 40, 40));

            this.setIcon(SkinProp.ICON_DOCK_SHORTCUTS, image.getSubimage(160, 0, 80, 80));
            this.setIcon(SkinProp.ICON_DOCK_SETTINGS, image.getSubimage(80, 0, 80, 80));
            this.setIcon(SkinProp.ICON_DOCK_ENDTURN, image.getSubimage(160, 80, 80, 80));
            this.setIcon(SkinProp.ICON_DOCK_CONCEDE, image.getSubimage(80, 80, 80, 80));
            this.setIcon(SkinProp.ICON_DOCK_DECKLIST, image.getSubimage(80, 160, 80, 80));

            this.setIcon(SkinProp.IMG_LOGO, image.getSubimage(280, 240, 200, 200));

            this.setIcon(SkinProp.IMG_BTN_START_UP, image.getSubimage(0, 240, 160, 80));
            this.setIcon(SkinProp.IMG_BTN_START_OVER, image.getSubimage(0, 320, 160, 80));
            this.setIcon(SkinProp.IMG_BTN_START_DOWN, image.getSubimage(0, 400, 160, 80));

            this.setIcon(SkinProp.IMG_BTN_UP_LEFT, image.getSubimage(360, 0, 40, 40));
            this.setIcon(SkinProp.IMG_BTN_UP_CENTER, image.getSubimage(400, 0, 1, 40));
            this.setIcon(SkinProp.IMG_BTN_UP_RIGHT, image.getSubimage(440, 0, 40, 40));

            this.setIcon(SkinProp.IMG_BTN_OVER_LEFT, image.getSubimage(360, 40, 40, 40));
            this.setIcon(SkinProp.IMG_BTN_OVER_CENTER, image.getSubimage(400, 40, 1, 40));
            this.setIcon(SkinProp.IMG_BTN_OVER_RIGHT, image.getSubimage(440, 40, 40, 40));

            this.setIcon(SkinProp.IMG_BTN_DOWN_LEFT, image.getSubimage(360, 80, 40, 40));
            this.setIcon(SkinProp.IMG_BTN_DOWN_CENTER, image.getSubimage(400, 80, 1, 40));
            this.setIcon(SkinProp.IMG_BTN_DOWN_RIGHT, image.getSubimage(440, 80, 40, 40));

            this.setIcon(SkinProp.IMG_BTN_FOCUS_LEFT, image.getSubimage(360, 120, 40, 40));
            this.setIcon(SkinProp.IMG_BTN_FOCUS_CENTER, image.getSubimage(400, 120, 1, 40));
            this.setIcon(SkinProp.IMG_BTN_FOCUS_RIGHT, image.getSubimage(440, 120, 40, 40));

            this.setIcon(SkinProp.IMG_BTN_TOGGLE_LEFT, image.getSubimage(360, 160, 40, 40));
            this.setIcon(SkinProp.IMG_BTN_TOGGLE_CENTER, image.getSubimage(400, 160, 1, 40));
            this.setIcon(SkinProp.IMG_BTN_TOGGLE_RIGHT, image.getSubimage(440, 160, 40, 40));

            this.setIcon(SkinProp.IMG_BTN_DISABLED_LEFT, image.getSubimage(360, 200, 40, 40));
            this.setIcon(SkinProp.IMG_BTN_DISABLED_CENTER, image.getSubimage(400, 200, 1, 40));
            this.setIcon(SkinProp.IMG_BTN_DISABLED_RIGHT, image.getSubimage(440, 200, 40, 40));
        } catch (final IOException e) {
            throw new IOException(this.notfound + FILE_SPRITE, e);
        }
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
     * @return Name of skin.
     */
    public String getName() {
        return this.name;
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
     * 
     * @param s0
     *            &emsp; Skin property (from enum)
     * @param s1
     *            &emsp; File address
     */
    public void setIcon(final SkinProp s0, final String s1) {
        this.icons.put(s0, new ImageIcon(s1));
    }
    /**
     * Sets an icon in this skin's icon map from a BufferedImage.
     * 
     * @param s0
     *            &emsp; Skin property (from enum)
     * @param bi0
     *            &emsp; BufferedImage
     */
    public void setIcon(final SkinProp s0, final BufferedImage bi0) {
        this.icons.put(s0, new ImageIcon(bi0));
    }

    /**
     * Sets an icon in this skin's icon map from an ImageIcon.
     * 
     * @param s0
     *            &emsp; Skin property (from enum)
     * @param i0
     *            &emsp; ImageIcon
     */
    public void setIcon(final SkinProp s0, final ImageIcon i0) {
        this.icons.put(s0, i0);
    }

    /**
     * Retrieves a color from this skin's color map.
     * 
     * @param s0
     *            &emsp; Skin property (from enum)
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
}
