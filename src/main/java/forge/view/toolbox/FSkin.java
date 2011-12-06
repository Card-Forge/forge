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

    // ===== Public fields
    /** Primary font used in titles and buttons and most text output. */
    private Font font1 = null;

    /** Primary texture used in skin. */
    private ImageIcon texture1 = null;

    /** Primary background image used during match play. */
    private ImageIcon matchBG = null;

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

    /** Splash screen image. */
    private ImageIcon splash = null;

    /** Splash screen image. */
    private ImageIcon icoShortcuts = null;
    private ImageIcon icoSettings = null;
    private ImageIcon icoConcede = null;
    private ImageIcon icoEndTurn = null;
    private ImageIcon icoViewDeckList = null;

    /** Base color used in skin. */
    private Color clrTheme = Color.red;

    /** Border color. */
    private Color clrBorders = Color.red;

    /** Color of zebra striping in grid displays. */
    private Color clrZebra = Color.red;

    /** Color of elements in mouseover state. */
    private Color clrHover = Color.red;

    /** Color of active (currently selected) elements. */
    private Color clrActive = Color.red;

    /** Color of inactive (not currently selected) elements. */
    private Color clrInactive = Color.red;

    /** Color of text in skin. */
    private Color clrText = Color.red;

    /** Color of background in progress bar if unfilled. */
    private Color clrProgress1 = Color.red;

    /** Color of text in progress bar if filled. */
    private Color clrProgress2 = Color.red;

    /** Color of background in progress bar if unfilled. */
    private Color clrProgress3 = Color.red;

    /** Color of text in progress bar if filled. */
    private Color clrProgress4 = Color.red;

    /** Name of skin. */
    private final String name = "default";

    // ===== Private fields
    private final String spriteFile = "sprite.png";
    private final String font1file = "font1.ttf";
    private final String texture1file = "bg_texture.jpg";
    private final String btnLupfile = "btnLup.png";
    private final String btnMupfile = "btnMup.png";
    private final String btnRupfile = "btnRup.png";
    private final String btnLoverfile = "btnLover.png";
    private final String btnMoverfile = "btnMover.png";
    private final String btnRoverfile = "btnRover.png";
    private final String btnLdownfile = "btnLdown.png";
    private final String btnMdownfile = "btnMdown.png";
    private final String btnRdownfile = "btnRdown.png";
    private final String splashfile = "bg_splash.jpg";
    private final String matchfile = "bg_match.jpg";

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

        // Fonts
        this.setFont1(this.retrieveFont(dirName + this.font1file));

        // Images
        this.setTexture1(this.retrieveImage(dirName + this.texture1file));
        this.setMatchBG(this.retrieveImage(dirName + this.matchfile));
        this.setBtnLup(this.retrieveImage(dirName + this.btnLupfile));
        this.setBtnMup(this.retrieveImage(dirName + this.btnMupfile));
        this.setBtnRup(this.retrieveImage(dirName + this.btnRupfile));
        this.setBtnLover(this.retrieveImage(dirName + this.btnLoverfile));
        this.setBtnMover(this.retrieveImage(dirName + this.btnMoverfile));
        this.setBtnRover(this.retrieveImage(dirName + this.btnRoverfile));
        this.setBtnLdown(this.retrieveImage(dirName + this.btnLdownfile));
        this.setBtnMdown(this.retrieveImage(dirName + this.btnMdownfile));
        this.setBtnRdown(this.retrieveImage(dirName + this.btnRdownfile));
        this.setSplashBG(this.retrieveImage(dirName + this.splashfile));

        // Color palette
        final File file = new File(dirName + this.spriteFile);
        BufferedImage image;
        try {
            image = ImageIO.read(file);
            this.setClrTheme(this.getColorFromPixel(image.getRGB(70, 10)));
            this.setClrBorders(this.getColorFromPixel(image.getRGB(70, 30)));
            this.setClrZebra(this.getColorFromPixel(image.getRGB(70, 50)));
            this.setClrHover(this.getColorFromPixel(image.getRGB(70, 70)));
            this.setClrActive(this.getColorFromPixel(image.getRGB(70, 90)));
            this.setClrInactive(this.getColorFromPixel(image.getRGB(70, 110)));
            this.setClrText(this.getColorFromPixel(image.getRGB(70, 130)));
            this.setClrProgress1(this.getColorFromPixel(image.getRGB(65, 145)));
            this.setClrProgress2(this.getColorFromPixel(image.getRGB(75, 145)));
            this.setClrProgress3(this.getColorFromPixel(image.getRGB(65, 155)));
            this.setClrProgress4(this.getColorFromPixel(image.getRGB(75, 155)));
            this.setIconShortcuts(image.getSubimage(160, 0, 80, 80));
            this.setIconEndTurn(image.getSubimage(160, 80, 80, 80));
            this.setIconViewDeckList(image.getSubimage(60, 140, 20, 20));
            this.setIconSettings(image.getSubimage(80, 0, 80, 80));
            this.setIconConcede(image.getSubimage(80, 80, 80, 80));

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
     * Splash screen image.
     * 
     * @return {@link javax.swing.ImageIcon} splash
     */
    public ImageIcon getSplashBG() {
        return this.splash;
    }

    /**
     * Splash screen image.
     * 
     * @param splash0
     *            &emsp; an image icon
     */
    public void setSplashBG(final ImageIcon splash0) {
        this.splash = splash0;
    }

    /**
     * Base color used in skin.
     * 
     * @return {@link java.awt.Color} clrTheme
     */
    public Color getClrTheme() {
        return this.clrTheme;
    }

    /**
     * Base color used in skin.
     * 
     * @param clrTheme0
     *            &emsp; an image icon
     */
    public void setClrTheme(final Color clrTheme0) {
        this.clrTheme = clrTheme0;
    }

    /**
     * Border color.
     * 
     * @return {@link java.awt.Color} clrBorders
     */
    public Color getClrBorders() {
        return this.clrBorders;
    }

    /**
     * Border color.
     * 
     * @param clrBorders0
     *            &emsp; an image icon
     */
    public void setClrBorders(final Color clrBorders0) {
        this.clrBorders = clrBorders0;
    }

    /**
     * Primary texture used in skin.
     * 
     * @return {@link javax.swing.ImageIcon} texture1
     */
    public ImageIcon getTexture1() {
        return this.texture1;
    }

    /**
     * Primary texture used in skin.
     * 
     * @param texture10
     *            &emsp; an image icon
     */
    public void setTexture1(final ImageIcon texture10) {
        this.texture1 = texture10;
    }

    /**
     * Primary background image used during match play.
     * 
     * @return ImageIcon
     */
    public ImageIcon getMatchBG() {
        return this.matchBG;
    }

    /**
     * Primary background image used during match play.
     * 
     * @param img0
     *            &emsp; an image icon
     */
    public void setMatchBG(final ImageIcon img0) {
        this.matchBG = img0;
    }

    /**
     * Color of zebra striping in grid displays.
     * 
     * @return {@link java.awt.Color} clrZebra
     */
    public Color getClrZebra() {
        return this.clrZebra;
    }

    /**
     * Color of zebra striping in grid displays.
     * 
     * @param clr0
     *            &emsp; Color obj
     */
    public void setClrZebra(final Color clr0) {
        this.clrZebra = clr0;
    }

    /**
     * Color of elements in mouseover state.
     * 
     * @return {@link java.awt.Color} clrHover
     */
    public Color getClrHover() {
        return this.clrHover;
    }

    /**
     * Color of elements in mouseover state.
     * 
     * @param clr0
     *            &emsp; Color obj
     */
    public void setClrHover(final Color clr0) {
        this.clrHover = clr0;
    }

    /**
     * Color of active (currently selected) elements.
     * 
     * @return {@link java.awt.Color} clrActive
     */
    public Color getClrActive() {
        return this.clrActive;
    }

    /**
     * Color of active (currently selected) elements.
     * 
     * @param clr0
     *            &emsp; Color obj
     */
    public void setClrActive(final Color clr0) {
        this.clrActive = clr0;
    }

    /**
     * Color of inactive (not currently selected) elements.
     * 
     * @return {@link java.awt.Color} clrHover
     */
    public Color getClrInactive() {
        return this.clrInactive;
    }

    /**
     * Color of inactive (not currently selected) elements.
     * 
     * @param clr0
     *            &emsp; Color obj
     */
    public void setClrInactive(final Color clr0) {
        this.clrInactive = clr0;
    }

    /**
     * Color of text in skin.
     * 
     * @return {@link java.awt.Color} clrText
     */
    public Color getClrText() {
        return this.clrText;
    }

    /**
     * Color of text in skin.
     * 
     * @param clr0
     *            &emsp; Color obj
     */
    public void setClrText(final Color clr0) {
        this.clrText = clr0;
    }

    /**
     * Background of progress bar, "unfilled" state.
     * 
     * @return {@link java.awt.Color} clrProgress1
     */
    public Color getClrProgress1() {
        return this.clrProgress1;
    }

    /**
     * Background of progress bar, "unfilled" state.
     * 
     * @param clr0
     *            &emsp; Color obj
     */
    public void setClrProgress1(final Color clr0) {
        this.clrProgress1 = clr0;
    }

    /**
     * Text of progress bar, "unfilled" state.
     * 
     * @return {@link java.awt.Color} clrProgress1
     */
    public Color getClrProgress2() {
        return this.clrProgress2;
    }

    /**
     * Text of progress bar, "unfilled" state.
     * 
     * @param clr0
     *            &emsp; Color obj
     */
    public void setClrProgress2(final Color clr0) {
        this.clrProgress2 = clr0;
    }

    /**
     * Background of progress bar, "filled" state.
     * 
     * @return {@link java.awt.Color} clrProgress1
     */
    public Color getClrProgress3() {
        return this.clrProgress3;
    }

    /**
     * Background of progress bar, "filled" state.
     * 
     * @param clr0
     *            &emsp; Color obj
     */
    public void setClrProgress3(final Color clr0) {
        this.clrProgress3 = clr0;
    }

    /**
     * Text of progress bar, "filled" state.
     * 
     * @return {@link java.awt.Color} clrProgress1
     */
    public Color getClrProgress4() {
        return this.clrProgress4;
    }

    /**
     * Text of progress bar, "filled" state.
     * 
     * @param clr0
     *            &emsp; Color obj
     */
    public void setClrProgress4(final Color clr0) {
        this.clrProgress4 = clr0;
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
     * Sets the icon shortcuts.
     * 
     * @param bi0
     *            &emsp; BufferedImage
     */
    public void setIconShortcuts(final BufferedImage bi0) {
        this.icoShortcuts = new ImageIcon(bi0);
    }

    /**
     * Gets the icon shortcuts.
     * 
     * @return ImageIcon
     */
    public ImageIcon getIconShortcuts() {
        return this.icoShortcuts;
    }

    /**
     * Sets the icon settings.
     * 
     * @param bi0
     *            &emsp; BufferedImage
     */
    public void setIconSettings(final BufferedImage bi0) {
        this.icoSettings = new ImageIcon(bi0);
    }

    /**
     * Gets the icon settings.
     * 
     * @return ImageIcon
     */
    public ImageIcon getIconSettings() {
        return this.icoSettings;
    }

    /**
     * Sets the icon concede.
     * 
     * @param bi0
     *            &emsp; BufferedImage
     */
    public void setIconConcede(final BufferedImage bi0) {
        this.icoConcede = new ImageIcon(bi0);
    }

    /**
     * Gets the icon concede.
     * 
     * @return ImageIcon
     */
    public ImageIcon getIconConcede() {
        return this.icoConcede;
    }

    /**
     * Sets the icon end turn.
     * 
     * @param bi0
     *            &emsp; BufferedImage
     */
    public void setIconEndTurn(final BufferedImage bi0) {
        this.icoEndTurn = new ImageIcon(bi0);
    }

    /**
     * Gets the icon end turn.
     * 
     * @return ImageIcon
     */
    public ImageIcon getIconEndTurn() {
        return this.icoEndTurn;
    }

    /**
     * Sets the icon view deck list.
     * 
     * @param bi0
     *            &emsp; BufferedImage
     */
    public void setIconViewDeckList(final BufferedImage bi0) {
        this.icoViewDeckList = new ImageIcon(bi0);
    }

    /**
     * Gets the icon view deck list.
     * 
     * @return ImageIcon
     */
    public ImageIcon getIconViewDeckList() {
        return this.icoViewDeckList;
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
     * @param s0 &emsp; String address
     * @param bi0 &emsp; BufferedImage
     */
    public void setIcon(String s0, BufferedImage bi0) {
        icons.put(s0, bi0);
    }
}
