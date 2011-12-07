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
    private Map<String, BufferedImage> icons;
    private Map<String, Color> colors;
    private Map<String, ImageIcon> images;

    // ===== Public fields
    /** Primary font used in titles and buttons and most text output. */
    private Font font1 = null;

    /** Left side of button, up state. */
    private ImageIcon btnLup = null;

    /** Middle of button, up state. */
    private ImageIcon btnMup = null;

    /** Right side of button, up state. */
    private ImageIcon btnRup = null;

    /** Button, left side, over state. */
    private ImageIcon btnLover = null;

    /** Button, middle, over state. */
    private ImageIcon btnMover = null;

    /** Button, right side, over state. */
    private ImageIcon btnRover = null;

    /** Button, left side, down state. */
    private ImageIcon btnLdown = null;

    /** Button, middle, down state. */
    private ImageIcon btnMdown = null;

    /** Button, right side, down state. */
    private ImageIcon btnRdown = null;

    /** Name of skin. */
    private final String name = "default";

    // ===== Private fields
    private final String spriteFile = "";
    private final String font1file = "font1.ttf";

    private final String btnLupfile = "btnLup.png";
    private final String btnMupfile = "btnMup.png";
    private final String btnRupfile = "btnRup.png";
    private final String btnLoverfile = "btnLover.png";
    private final String btnMoverfile = "btnMover.png";
    private final String btnRoverfile = "btnRover.png";
    private final String btnLdownfile = "btnLdown.png";
    private final String btnMdownfile = "btnMdown.png";
    private final String btnRdownfile = "btnRdown.png";

    private ImageIcon tempImg;
    private Font tempFont;
    private final String notfound = "FSkin.java: Can't find ";

    /**
     * FSkin constructor. No arguments, will generate default skin settings,
     * fonts, and backgrounds.
     * 
     * @throws Exception
     *             the exception
     */
    public FSkin() throws Exception {
        this("default");
    }

    /**
     * FSkin constructor, using skin name. Generates custom skin settings,
     * fonts, and backgrounds.
     * 
     * @param skinName
     *            the skin name
     * @throws Exception
     *             the exception
     */
    public FSkin(final String skinName) throws Exception {
        this.loadFontAndImages(skinName);
    }

    /**
     * Loads objects from skin folder, prints brief error if not found.
     * 
     * @param skinName
     */
    private void loadFontAndImages(final String skinName) {
        final String dirName = "res/images/skins/" + skinName + "/";

        icons = new HashMap<String, BufferedImage>();
        colors = new HashMap<String, Color>();
        images = new HashMap<String, ImageIcon>();

        // Fonts
        this.setFont1(this.retrieveFont(dirName + this.font1file));

        // Images
        this.setImage("bg.texture", this.retrieveImage(dirName + "bg_texture.jpg"));
        this.setImage("bg.match", this.retrieveImage(dirName + "bg_match.jpg"));
        this.setImage("bg.splash", this.retrieveImage(dirName + "bg_splash.jpg"));

        this.setBtnLup(this.retrieveImage(dirName + this.btnLupfile));
        this.setBtnMup(this.retrieveImage(dirName + this.btnMupfile));
        this.setBtnRup(this.retrieveImage(dirName + this.btnRupfile));
        this.setBtnLover(this.retrieveImage(dirName + this.btnLoverfile));
        this.setBtnMover(this.retrieveImage(dirName + this.btnMoverfile));
        this.setBtnRover(this.retrieveImage(dirName + this.btnRoverfile));
        this.setBtnLdown(this.retrieveImage(dirName + this.btnLdownfile));
        this.setBtnMdown(this.retrieveImage(dirName + this.btnMdownfile));
        this.setBtnRdown(this.retrieveImage(dirName + this.btnRdownfile));

        // Sprite
        final File file = new File(dirName + "sprite.png");
        BufferedImage image;
        try {
            image = ImageIO.read(file);
            this.setColor("theme", this.getColorFromPixel(image.getRGB(70, 10)));
            this.setColor("borders", this.getColorFromPixel(image.getRGB(70, 30)));
            this.setColor("zebra", this.getColorFromPixel(image.getRGB(70, 50)));
            this.setColor("hover", this.getColorFromPixel(image.getRGB(70, 70)));
            this.setColor("active", this.getColorFromPixel(image.getRGB(70, 90)));
            this.setColor("inactive", this.getColorFromPixel(image.getRGB(70, 110)));
            this.setColor("text", this.getColorFromPixel(image.getRGB(70, 130)));
            this.setColor("progress1", this.getColorFromPixel(image.getRGB(65, 145)));
            this.setColor("progress2", this.getColorFromPixel(image.getRGB(75, 145)));
            this.setColor("progress3", this.getColorFromPixel(image.getRGB(65, 155)));
            this.setColor("progress4", this.getColorFromPixel(image.getRGB(75, 155)));

            // All icons should eventually be set and retrieved using this method.
            // Doublestrike 6-12-11
            this.setIcon("zone.hand", image.getSubimage(280, 40, 40, 40));
            this.setIcon("zone.library", image.getSubimage(280, 0, 40, 40));
            this.setIcon("zone.graveyard", image.getSubimage(320, 0, 40, 40));
            this.setIcon("zone.exile", image.getSubimage(320, 40, 40, 40));
            this.setIcon("zone.flashback", image.getSubimage(320, 120, 40, 40));
            this.setIcon("zone.poison", image.getSubimage(320, 80, 40, 40));

            this.setIcon("mana.black", image.getSubimage(240, 0, 40, 40));
            this.setIcon("mana.blue", image.getSubimage(240, 40, 40, 40));
            this.setIcon("mana.green", image.getSubimage(240, 120, 40, 40));
            this.setIcon("mana.red", image.getSubimage(240, 80, 40, 40));
            this.setIcon("mana.white", image.getSubimage(280, 120, 40, 40));
            this.setIcon("mana.colorless", image.getSubimage(280, 80, 40, 40));

            this.setIcon("dock.shortcuts", image.getSubimage(160, 0, 80, 80));
            this.setIcon("dock.settings", image.getSubimage(160, 80, 80, 80));
            this.setIcon("dock.decklist", image.getSubimage(60, 140, 20, 20));
            this.setIcon("dock.concede", image.getSubimage(80, 80, 80, 80));
            this.setIcon("dock.endturn", image.getSubimage(80, 0, 80, 80));
            this.setIcon("dock.concede", image.getSubimage(80, 80, 80, 80));
            this.setIcon("dock.decklist", image.getSubimage(60, 140, 20, 20));
        } catch (final IOException e) {
            System.err.println(this.notfound + this.spriteFile);
        }

    }

    /**
     * <p>
     * retrieveImage.
     * </p>
     * Tries to instantiate an image icon from a filename. Error reported if not
     * found.
     * 
     * @param {@link java.lang.String} address
     * @return a ImageIcon
     */
    private ImageIcon retrieveImage(final String address) {
        this.tempImg = new ImageIcon(address);

        if (this.tempImg.getIconWidth() == -1) {
            System.err.println(this.notfound + address);
        }

        return this.tempImg;
    }

    /**
     * <p>
     * retrieveFont.
     * </p>
     * Uses GuiUtils to grab a font file at an address. Error will be reported
     * by GuiUtils if not found.
     * 
     * @param {@link java.lang.String} address
     * @return a Font
     */
    private Font retrieveFont(final String address) {
        this.tempFont = GuiUtils.newFont(address);

        return this.tempFont;
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

    /**
     * Primary font used in titles and buttons and most text output.
     * 
     * @return {@link java.awt.font} font1
     */
    public Font getFont1() {
        return this.font1;
    }

    /**
     * Primary font used in titles and buttons and most text output.
     * 
     * @param font10
     *            &emsp; an image icon
     */
    public void setFont1(final Font font10) {
        this.font1 = font10;
    }

    /**
     * Left side of button, up state.
     * 
     * @return {@link javax.swing.ImageIcon} btnLup
     */
    public ImageIcon getBtnLup() {
        return this.btnLup;
    }

    /**
     * Left side of button, up state.
     * 
     * @param btnLup0
     *            &emsp; an image icon
     */
    public void setBtnLup(final ImageIcon btnLup0) {
        this.btnLup = btnLup0;
    }

    /**
     * Middle of button, up state.
     * 
     * @return {@link javax.swing.ImageIcon} btnMup
     */
    public ImageIcon getBtnMup() {
        return this.btnMup;
    }

    /**
     * Middle of button, up state.
     * 
     * @param btnMup0
     *            &emsp; an image icon
     */
    public void setBtnMup(final ImageIcon btnMup0) {
        this.btnMup = btnMup0;
    }

    /**
     * Right side of button, up state.
     * 
     * @return {@link javax.swing.ImageIcon} btnRup
     */
    public ImageIcon getBtnRup() {
        return this.btnRup;
    }

    /**
     * Right side of button, up state.
     * 
     * @param btnRup0
     *            &emsp; an image icon
     */
    public void setBtnRup(final ImageIcon btnRup0) {
        this.btnRup = btnRup0;
    }

    /**
     * Left side of button, over state.
     * 
     * @return {@link javax.swing.ImageIcon} btnLover
     */
    public ImageIcon getBtnLover() {
        return this.btnLover;
    }

    /**
     * Left side of button, over state.
     * 
     * @param btnLover0
     *            &emsp; an image icon
     */
    public void setBtnLover(final ImageIcon btnLover0) {
        this.btnLover = btnLover0;
    }

    /**
     * Middle of button, over state.
     * 
     * @return {@link javax.swing.ImageIcon} btnMover
     */
    public ImageIcon getBtnMover() {
        return this.btnMover;
    }

    /**
     * Middle of button, over state.
     * 
     * @param btnMover0
     *            &emsp; an image icon
     */
    public void setBtnMover(final ImageIcon btnMover0) {
        this.btnMover = btnMover0;
    }

    /**
     * Right side of button, over state.
     * 
     * @return {@link javax.swing.ImageIcon} btnRover
     */
    public ImageIcon getBtnRover() {
        return this.btnRover;
    }

    /**
     * Right side of button, over state.
     * 
     * @param btnRover0
     *            &emsp; an image icon
     */
    public void setBtnRover(final ImageIcon btnRover0) {
        this.btnRover = btnRover0;
    }

    /**
     * Left side of button, down state.
     * 
     * @return an image icon
     */
    public ImageIcon getBtnLdown() {
        return this.btnLdown;
    }

    /**
     * Left side of button, down state.
     * 
     * @param btnLdown0
     *            &emsp; an image icon
     */
    public void setBtnLdown(final ImageIcon btnLdown0) {
        this.btnLdown = btnLdown0;
    }

    /**
     * Right side of button, down state.
     * 
     * @return an image icon
     */
    public ImageIcon getBtnRdown() {
        return this.btnRdown;
    }

    /**
     * Right side of button, down state.
     * 
     * @param btnRdown0
     *            an image icon
     */
    public void setBtnRdown(final ImageIcon btnRdown0) {
        this.btnRdown = btnRdown0;
    }

    /**
     * Middle of button, down state.
     * 
     * @return an image icon
     */
    public ImageIcon getBtnMdown() {
        return this.btnMdown;
    }

    /**
     * Sets the btn mdown.
     * 
     * @param btnMdown0
     *            &emsp; an image icon
     */
    public void setBtnMdown(final ImageIcon btnMdown0) {
        this.btnMdown = btnMdown0;
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
     * @param s0 &emsp; String address
     * @return ImageIcon
     */
    public ImageIcon getIcon(String s0) {
        return new ImageIcon(icons.get(s0));
    }

    /**
     * Gets a scaled version of an icon from this skin's icon map.
     * 
     * @param s0 String icon address
     * @param w0 int new width
     * @param h0 int new height
     * @return ImageIcon
     */
    public ImageIcon getIcon(String s0, int w0, int h0) {
        w0 = (w0 < 1) ? 1 : w0;
        h0 = (h0 < 1) ? 1 : h0;

        BufferedImage original = icons.get(s0);
        BufferedImage scaled = new BufferedImage(w0, h0, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = scaled.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(original, 0, 0, w0, h0, 0, 0, original.getWidth(), original.getHeight(), null);
        g2d.dispose();

        return new ImageIcon(scaled);
    }

    /** 
     * Sets an icon in this skin's icon map.
     * 
     * @param s0 &emsp; String address
     * @param bi0 &emsp; BufferedImage
     */
    public void setIcon(String s0, BufferedImage bi0) {
        icons.put(s0, bi0);
    }

    /**
     * Retrieves a color from this skin's color map.
     * 
     * @param s0 &emsp; String color address
     * @return Color
     */
    public Color getColor(String s0) {
        return colors.get(s0);
    }

    /** 
     * Sets a color in this skin's color map.
     * 
     * @param s0 &emsp; String address
     * @param c0 &emsp; Color
     */
    public void setColor(String s0, Color c0) {
        colors.put(s0, c0);
    }

    /**
     * Retrieves an image from this skin's image map.
     * 
     * @param s0 &emsp; String color address
     * @return Color
     */
    public ImageIcon getImage(String s0) {
        return images.get(s0);
    }

    /** 
     * Sets an image in this skin's image map.
     * 
     * @param s0 &emsp; String address
     * @param i0 &emsp; ImageIcon
     */
    public void setImage(String s0, ImageIcon i0) {
        images.put(s0, i0);
    }
}
