package forge.gui.skin;

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
    /** The font1. */
    private Font font1 = null;

    /** The font2. */
    private Font font2 = null;

    /** The texture1. */
    private ImageIcon texture1 = null;

    /** The texture2. */
    private ImageIcon texture2 = null;

    /** The texture3. */
    private ImageIcon texture3 = null;

    /** The btn lup. */
    private ImageIcon btnLup = null;

    /** The btn mup. */
    private ImageIcon btnMup = null;

    /** The btn rup. */
    private ImageIcon btnRup = null;

    /** The btn lover. */
    private ImageIcon btnLover = null;

    /** The btn mover. */
    private ImageIcon btnMover = null;

    /** The btn rover. */
    private ImageIcon btnRover = null;

    /** The btn ldown. */
    private ImageIcon btnLdown = null;

    /** The btn mdown. */
    private ImageIcon btnMdown = null;

    /** The btn rdown. */
    private ImageIcon btnRdown = null;

    /** The splash. */
    private ImageIcon splash = null;

    /** The bg1a. */
    private Color bg1a = Color.red;

    /** The bg1b. */
    private Color bg1b = Color.red;

    /** The bg2a. */
    private Color bg2a = Color.red;

    /** The bg2b. */
    private Color bg2b = Color.red;

    /** The bg3a. */
    private Color bg3a = Color.red;

    /** The bg3b. */
    private Color bg3b = Color.red;

    /** The txt1a. */
    private Color txt1a = Color.red;

    /** The txt1b. */
    private Color txt1b = Color.red;

    /** The txt2a. */
    private Color txt2a = Color.red;

    /** The txt2b. */
    private Color txt2b = Color.red;

    /** The txt3a. */
    private Color txt3a = Color.red;

    /** The txt3b. */
    private Color txt3b = Color.red;

    /** The name. */
    private String name = "default";

    // ===== Private fields
    private final String paletteFile = "palette.jpg";
    private final String font1file = "font1.ttf";
    private final String font2file = "font2.ttf";
    private final String texture1file = "texture1.jpg";
    private final String texture2file = "texture2.jpg";
    private final String texture3file = "texture3.jpg";
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

    private ImageIcon tempImg;
    private Font tempFont;
    private String skin;
    private final String notfound = "FSkin.java: \"" + this.skin + "\" skin can't find ";

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
        this.loadFontAndImages("default");

        if (!skinName.equals("default")) {
            this.loadFontAndImages(skinName);
        }
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
        this.texture2 = this.retrieveImage(dirName + this.texture2file);
        this.texture3 = this.retrieveImage(dirName + this.texture3file);
        this.setBtnLup(this.retrieveImage(dirName + this.btnLupfile));
        this.setBtnMup(this.retrieveImage(dirName + this.btnMupfile));
        this.setBtnRup(this.retrieveImage(dirName + this.btnRupfile));
        this.setBtnLover(this.retrieveImage(dirName + this.btnLoverfile));
        this.setBtnMover(this.retrieveImage(dirName + this.btnMoverfile));
        this.setBtnRover(this.retrieveImage(dirName + this.btnRoverfile));
        this.setBtnLdown(this.retrieveImage(dirName + this.btnLdownfile));
        this.setBtnMdown(this.retrieveImage(dirName + this.btnMdownfile));
        this.setBtnRdown(this.retrieveImage(dirName + this.btnRdownfile));
        this.setSplash(this.retrieveImage(dirName + this.splashfile));

        // Color palette
        final File file = new File(dirName + this.paletteFile);
        BufferedImage image;
        try {
            image = ImageIO.read(file);
            this.setBg1a(this.getColorFromPixel(image.getRGB(10, 30)));
            this.setBg1b(this.getColorFromPixel(image.getRGB(30, 30)));
            this.bg2a = this.getColorFromPixel(image.getRGB(50, 30));
            this.bg2b = this.getColorFromPixel(image.getRGB(70, 30));
            this.bg3a = this.getColorFromPixel(image.getRGB(90, 30));
            this.bg3b = this.getColorFromPixel(image.getRGB(110, 30));

            this.setTxt1a(this.getColorFromPixel(image.getRGB(10, 70)));
            this.txt1b = this.getColorFromPixel(image.getRGB(30, 70));
            this.txt2a = this.getColorFromPixel(image.getRGB(50, 70));
            this.txt2b = this.getColorFromPixel(image.getRGB(70, 70));
            this.txt3a = this.getColorFromPixel(image.getRGB(90, 70));
            this.txt3b = this.getColorFromPixel(image.getRGB(110, 70));
        } catch (final IOException e) {
            System.err.println(this.notfound + this.paletteFile);
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
     * by GuiUtils if not found. Error also reported by this method if not
     * found.
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
        return new Color((pixel & 0x00ff0000) >> 16, (pixel & 0x0000ff00) >> 8, (pixel & 0x000000ff));
    }

    /**
     * @return the font1
     */
    public Font getFont1() {
        return font1;
    }

    /**
     * @param font1 the font1 to set
     */
    public void setFont1(Font font1) {
        this.font1 = font1; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the font2
     */
    public Font getFont2() {
        return font2;
    }

    /**
     * @param font2 the font2 to set
     */
    public void setFont2(Font font2) {
        this.font2 = font2; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the bg1a
     */
    public Color getBg1a() {
        return bg1a;
    }

    /**
     * @param bg1a the bg1a to set
     */
    public void setBg1a(Color bg1a) {
        this.bg1a = bg1a; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the splash
     */
    public ImageIcon getSplash() {
        return splash;
    }

    /**
     * @param splash the splash to set
     */
    public void setSplash(ImageIcon splash) {
        this.splash = splash; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the btnRdown
     */
    public ImageIcon getBtnRdown() {
        return btnRdown;
    }

    /**
     * @param btnRdown the btnRdown to set
     */
    public void setBtnRdown(ImageIcon btnRdown) {
        this.btnRdown = btnRdown; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the bg1b
     */
    public Color getBg1b() {
        return bg1b;
    }

    /**
     * @param bg1b the bg1b to set
     */
    public void setBg1b(Color bg1b) {
        this.bg1b = bg1b; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the texture1
     */
    public ImageIcon getTexture1() {
        return texture1;
    }

    /**
     * @param texture1 the texture1 to set
     */
    public void setTexture1(ImageIcon texture1) {
        this.texture1 = texture1; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the txt1a
     */
    public Color getTxt1a() {
        return txt1a;
    }

    /**
     * @param txt1a the txt1a to set
     */
    public void setTxt1a(Color txt1a) {
        this.txt1a = txt1a; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the btnLup
     */
    public ImageIcon getBtnLup() {
        return btnLup;
    }

    /**
     * @param btnLup the btnLup to set
     */
    public void setBtnLup(ImageIcon btnLup) {
        this.btnLup = btnLup; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the btnMup
     */
    public ImageIcon getBtnMup() {
        return btnMup;
    }

    /**
     * @param btnMup the btnMup to set
     */
    public void setBtnMup(ImageIcon btnMup) {
        this.btnMup = btnMup; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the btnLover
     */
    public ImageIcon getBtnLover() {
        return btnLover;
    }

    /**
     * @param btnLover the btnLover to set
     */
    public void setBtnLover(ImageIcon btnLover) {
        this.btnLover = btnLover; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the btnRup
     */
    public ImageIcon getBtnRup() {
        return btnRup;
    }

    /**
     * @param btnRup the btnRup to set
     */
    public void setBtnRup(ImageIcon btnRup) {
        this.btnRup = btnRup; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the btnMover
     */
    public ImageIcon getBtnMover() {
        return btnMover;
    }

    /**
     * @param btnMover the btnMover to set
     */
    public void setBtnMover(ImageIcon btnMover) {
        this.btnMover = btnMover; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the btnRover
     */
    public ImageIcon getBtnRover() {
        return btnRover;
    }

    /**
     * @param btnRover the btnRover to set
     */
    public void setBtnRover(ImageIcon btnRover) {
        this.btnRover = btnRover; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the btnLdown
     */
    public ImageIcon getBtnLdown() {
        return btnLdown;
    }

    /**
     * @param btnLdown the btnLdown to set
     */
    public void setBtnLdown(ImageIcon btnLdown) {
        this.btnLdown = btnLdown; // TODO: Add 0 to parameter's name.
    }

    /**
     * @return the btnMdown
     */
    public ImageIcon getBtnMdown() {
        return btnMdown;
    }

    /**
     * @param btnMdown the btnMdown to set
     */
    public void setBtnMdown(ImageIcon btnMdown) {
        this.btnMdown = btnMdown; // TODO: Add 0 to parameter's name.
    }
}
