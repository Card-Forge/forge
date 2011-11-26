package forge.view.toolbox;

import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import forge.gui.GuiUtils;

/**
 * Assembles settings from selected or default theme as appropriate. Saves in a
 * hashtable, access using .get(settingName) method.
 * 
 */

public class FSkin {
    // ===== Public fields
    /** Primary font used in titles and buttons and most text output. */
    private Font font1 = null;

    /** Secondary font used where a sub-block of text needs it. */
    private Font font2 = null;

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
    private ImageIcon icoEnabled    = null;
    private ImageIcon icoDisabled   = null;
    private ImageIcon icoTap        = null;
    private ImageIcon icoUntap      = null;
    private ImageIcon icoPlus       = null;
    private ImageIcon icoShortcuts  = null;
    private ImageIcon icoSettings   = null;
    private ImageIcon icoConcede    = null;
    private ImageIcon icoEndTurn    = null;

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
    private String name = "default";

    // ===== Private fields
    private final String spriteFile = "sprite.png";
    private final String font1file = "font1.ttf";
    private final String font2file = "font2.ttf";
    private final String texture1file = "texture1.jpg";
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

        // Fonts
        this.setFont1(this.retrieveFont(dirName + this.font1file));
        this.setFont2(this.retrieveFont(dirName + this.font2file));

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
            this.setIconEnabled(image.getSubimage(80, 0, 40, 40));
            this.setIconDisabled(image.getSubimage(120, 0, 40, 40));
            this.setIconTap(image.getSubimage(80, 40, 40, 40));
            this.setIconUntap(image.getSubimage(120, 40, 40, 40));
            this.setIconPlus(image.getSubimage(80, 80, 40, 40));
            this.setIconShortcuts(image.getSubimage(160, 0, 80, 80));
            this.setIconEndTurn(image.getSubimage(160, 80, 80, 80));
            this.setIconSettings(image.getSubimage(80, 0, 80, 80));
            this.setIconConcede(image.getSubimage(80, 80, 80, 80));
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
     * @return {@link java.awt.font} font1
     */
    public Font getFont1() {
        return font1;
    }

    /**
     * Primary font used in titles and buttons and most text output.
     * @param font10 &emsp; an image icon
     */
    public void setFont1(Font font10) {
        this.font1 = font10;
    }

    /**
     * Secondary font used where a sub-block of text needs it.
     * @return {@link java.awt.Font} font2
     */
    public Font getFont2() {
        return font2;
    }

    /**
     * Secondary font used where a sub-block of text needs it.
     * @param font20 &emsp; an image icon
     */
    public void setFont2(Font font20) {
        this.font2 = font20;
    }

    /**
     * Splash screen image.
     * @return {@link javax.swing.ImageIcon} splash
     */
    public ImageIcon getSplashBG() {
        return splash;
    }

    /**
     * Splash screen image.
     * @param splash0 &emsp; an image icon
     */
    public void setSplashBG(ImageIcon splash0) {
        this.splash = splash0;
    }

    /**
     * Base color used in skin.
     * @return {@link java.awt.Color} clrTheme
     */
    public Color getClrTheme() {
        return clrTheme;
    }

    /**
     * Base color used in skin.
     * @param clrTheme0 &emsp; an image icon
     */
    public void setClrTheme(Color clrTheme0) {
        this.clrTheme = clrTheme0;
    }

    /**
     * Border color.
     * @return {@link java.awt.Color} clrBorders
     */
    public Color getClrBorders() {
        return clrBorders;
    }

    /**
     * Border color.
     * @param clrBorders0 &emsp; an image icon
     */
    public void setClrBorders(Color clrBorders0) {
        this.clrBorders = clrBorders0;
    }

    /**
     * Primary texture used in skin.
     * @return {@link javax.swing.ImageIcon} texture1
     */
    public ImageIcon getTexture1() {
        return texture1;
    }

    /**
     * Primary texture used in skin.
     * @param texture10 &emsp; an image icon
     */
    public void setTexture1(ImageIcon texture10) {
        this.texture1 = texture10;
    }

    /**
     * Primary background image used during match play.
     * @return ImageIcon
     */
    public ImageIcon getMatchBG() {
        return matchBG;
    }

    /**
     * Primary background image used during match play.
     * @param img0 &emsp; an image icon
     */
    public void setMatchBG(ImageIcon img0) {
        this.matchBG = img0;
    }

    /**
     * Color of zebra striping in grid displays.
     * @return {@link java.awt.Color} clrZebra
     */
    public Color getClrZebra() {
        return clrZebra;
    }

    /**
     * Color of zebra striping in grid displays.
     * @param clr0 &emsp; Color obj
     */
    public void setClrZebra(Color clr0) {
        this.clrZebra = clr0;
    }

    /**
     * Color of elements in mouseover state.
     * @return {@link java.awt.Color} clrHover
     */
    public Color getClrHover() {
        return clrHover;
    }

    /**
     * Color of elements in mouseover state.
     * @param clr0 &emsp; Color obj
     */
    public void setClrHover(Color clr0) {
        this.clrHover = clr0;
    }

    /**
     * Color of active (currently selected) elements.
     * @return {@link java.awt.Color} clrActive
     */
    public Color getClrActive() {
        return clrActive;
    }

    /**
     * Color of active (currently selected) elements.
     * @param clr0 &emsp; Color obj
     */
    public void setClrActive(Color clr0) {
        this.clrActive = clr0;
    }

    /**
     * Color of inactive (not currently selected) elements.
     * @return {@link java.awt.Color} clrHover
     */
    public Color getClrInactive() {
        return clrInactive;
    }

    /**
     * Color of inactive (not currently selected) elements.
     * @param clr0 &emsp; Color obj
     */
    public void setClrInactive(Color clr0) {
        this.clrInactive = clr0;
    }

    /**
     * Color of text in skin.
     * @return {@link java.awt.Color} clrText
     */
    public Color getClrText() {
        return clrText;
    }

    /**
     * Color of text in skin.
     * @param clr0 &emsp; Color obj
     */
    public void setClrText(Color clr0) {
        this.clrText = clr0;
    }

    /**
     * Background of progress bar, "unfilled" state.
     * @return {@link java.awt.Color} clrProgress1
     */
    public Color getClrProgress1() {
        return clrProgress1;
    }

    /**
     * Background of progress bar, "unfilled" state.
     * @param clr0 &emsp; Color obj
     */
    public void setClrProgress1(Color clr0) {
        this.clrProgress1 = clr0;
    }

    /**
     * Text of progress bar, "unfilled" state.
     * @return {@link java.awt.Color} clrProgress1
     */
    public Color getClrProgress2() {
        return clrProgress2;
    }

    /**
     * Text of progress bar, "unfilled" state.
     * @param clr0 &emsp; Color obj
     */
    public void setClrProgress2(Color clr0) {
        this.clrProgress2 = clr0;
    }

    /**
     * Background of progress bar, "filled" state.
     * @return {@link java.awt.Color} clrProgress1
     */
    public Color getClrProgress3() {
        return clrProgress3;
    }

    /**
     * Background of progress bar, "filled" state.
     * @param clr0 &emsp; Color obj
     */
    public void setClrProgress3(Color clr0) {
        this.clrProgress3 = clr0;
    }

    /**
     * Text of progress bar, "filled" state.
     * @return {@link java.awt.Color} clrProgress1
     */
    public Color getClrProgress4() {
        return clrProgress4;
    }

    /**
     * Text of progress bar, "filled" state.
     * @param clr0 &emsp; Color obj
     */
    public void setClrProgress4(Color clr0) {
        this.clrProgress4 = clr0;
    }

    /**
     * Left side of button, up state.
     * @return {@link javax.swing.ImageIcon} btnLup
     */
    public ImageIcon getBtnLup() {
        return btnLup;
    }

    /**
     * Left side of button, up state.
     * @param btnLup0 &emsp; an image icon
     */
    public void setBtnLup(ImageIcon btnLup0) {
        this.btnLup = btnLup0;
    }

    /**
     * Middle of button, up state.
     * @return {@link javax.swing.ImageIcon} btnMup
     */
    public ImageIcon getBtnMup() {
        return btnMup;
    }

    /**
     * Middle of button, up state.
     * @param btnMup0 &emsp; an image icon
     */
    public void setBtnMup(ImageIcon btnMup0) {
        this.btnMup = btnMup0;
    }

    /**
     * Right side of button, up state.
     * @return {@link javax.swing.ImageIcon} btnRup
     */
    public ImageIcon getBtnRup() {
        return btnRup;
    }

    /**
     * Right side of button, up state.
     * @param btnRup0 &emsp; an image icon
     */
    public void setBtnRup(ImageIcon btnRup0) {
        this.btnRup = btnRup0;
    }

    /**
     * Left side of button, over state.
     * @return {@link javax.swing.ImageIcon} btnLover
     */
    public ImageIcon getBtnLover() {
        return btnLover;
    }

    /**
     * Left side of button, over state.
     * @param btnLover0 &emsp; an image icon
     */
    public void setBtnLover(ImageIcon btnLover0) {
        this.btnLover = btnLover0;
    }

    /**
     * Middle of button, over state.
     * @return {@link javax.swing.ImageIcon}  btnMover
     */
    public ImageIcon getBtnMover() {
        return btnMover;
    }

    /**
     * Middle of button, over state.
     * @param btnMover0 &emsp; an image icon
     */
    public void setBtnMover(ImageIcon btnMover0) {
        this.btnMover = btnMover0;
    }

    /**
     * Right side of button, over state.
     * @return {@link javax.swing.ImageIcon}  btnRover
     */
    public ImageIcon getBtnRover() {
        return btnRover;
    }

    /**
     * Right side of button, over state.
     * @param btnRover0 &emsp; an image icon
     */
    public void setBtnRover(ImageIcon btnRover0) {
        this.btnRover = btnRover0;
    }

    /**
     * Left side of button, down state.
     * @return an image icon
     */
    public ImageIcon getBtnLdown() {
        return btnLdown;
    }

    /**
     * Left side of button, down state.
     * @param btnLdown0 &emsp; an image icon
     */
    public void setBtnLdown(ImageIcon btnLdown0) {
        this.btnLdown = btnLdown0;
    }

    /**
     * Right side of button, down state.
     * @return an image icon
     */
    public ImageIcon getBtnRdown() {
        return btnRdown;
    }

    /**
     * Right side of button, down state.
     * @param btnRdown0 an image icon
     */
    public void setBtnRdown(ImageIcon btnRdown0) {
        this.btnRdown = btnRdown0;
    }

    /**
     * Middle of button, down state.
     * @return an image icon
     */
    public ImageIcon getBtnMdown() {
        return btnMdown;
    }

    /**
     * @param btnMdown0 &emsp; an image icon
     */
    public void setBtnMdown(ImageIcon btnMdown0) {
        this.btnMdown = btnMdown0;
    }

    /**
     * @return Name of skin.
     */
    public String getName() {
        return name;
    }

    /** @param bi0 &emsp; BufferedImage */
    public void setIconEnabled(BufferedImage bi0) {
        this.icoEnabled = new ImageIcon(bi0);
    }

    /** @return ImageIcon */
    public ImageIcon getIconEnabled() {
        return icoEnabled;
    }

    /** @param bi0 &emsp; BufferedImage */
    public void setIconDisabled(BufferedImage bi0) {
        this.icoDisabled = new ImageIcon(bi0);
    }

    /** @return ImageIcon */
    public ImageIcon getIconDisabled() {
        return icoDisabled;
    }

    /** @param bi0 &emsp; BufferedImage */
    public void setIconTap(BufferedImage bi0) {
        this.icoTap = new ImageIcon(bi0);
    }

    /** @return ImageIcon */
    public ImageIcon getIconTap() {
        return icoTap;
    }

    /** @param bi0 &emsp; BufferedImage */
    public void setIconUntap(BufferedImage bi0) {
        this.icoUntap = new ImageIcon(bi0);
    }

    /** @return ImageIcon */
    public ImageIcon getIconUntap() {
        return icoUntap;
    }

    /** @param bi0 &emsp; BufferedImage */
    public void setIconPlus(BufferedImage bi0) {
        this.icoPlus = new ImageIcon(bi0);
    }

    /** @return ImageIcon */
    public ImageIcon getIconPlus() {
        return icoPlus;
    }

    /** @param bi0 &emsp; BufferedImage */
    public void setIconShortcuts(BufferedImage bi0) {
        this.icoShortcuts = new ImageIcon(bi0);
    }

    /** @return ImageIcon */
    public ImageIcon getIconShortcuts() {
        return icoShortcuts;
    }

    /** @param bi0 &emsp; BufferedImage */
    public void setIconSettings(BufferedImage bi0) {
        this.icoSettings = new ImageIcon(bi0);
    }

    /** @return ImageIcon */
    public ImageIcon getIconSettings() {
        return icoSettings;
    }

    /** @param bi0 &emsp; BufferedImage */
    public void setIconConcede(BufferedImage bi0) {
        this.icoConcede = new ImageIcon(bi0);
    }

    /** @return ImageIcon */
    public ImageIcon getIconConcede() {
        return icoConcede;
    }

    /** @param bi0 &emsp; BufferedImage */
    public void setIconEndTurn(BufferedImage bi0) {
        this.icoEndTurn = new ImageIcon(bi0);
    }

    /** @return ImageIcon */
    public ImageIcon getIconEndTurn() {
        return icoEndTurn;
    }
}
