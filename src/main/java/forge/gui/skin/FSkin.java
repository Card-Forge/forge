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
    public Font font1 = null;

    /** The font2. */
    public Font font2 = null;

    /** The texture1. */
    public ImageIcon texture1 = null;

    /** The texture2. */
    public ImageIcon texture2 = null;

    /** The texture3. */
    public ImageIcon texture3 = null;

    /** The btn lup. */
    public ImageIcon btnLup = null;

    /** The btn mup. */
    public ImageIcon btnMup = null;

    /** The btn rup. */
    public ImageIcon btnRup = null;

    /** The btn lover. */
    public ImageIcon btnLover = null;

    /** The btn mover. */
    public ImageIcon btnMover = null;

    /** The btn rover. */
    public ImageIcon btnRover = null;

    /** The btn ldown. */
    public ImageIcon btnLdown = null;

    /** The btn mdown. */
    public ImageIcon btnMdown = null;

    /** The btn rdown. */
    public ImageIcon btnRdown = null;

    /** The splash. */
    public ImageIcon splash = null;

    /** The bg1a. */
    public Color bg1a = Color.red;

    /** The bg1b. */
    public Color bg1b = Color.red;

    /** The bg2a. */
    public Color bg2a = Color.red;

    /** The bg2b. */
    public Color bg2b = Color.red;

    /** The bg3a. */
    public Color bg3a = Color.red;

    /** The bg3b. */
    public Color bg3b = Color.red;

    /** The txt1a. */
    public Color txt1a = Color.red;

    /** The txt1b. */
    public Color txt1b = Color.red;

    /** The txt2a. */
    public Color txt2a = Color.red;

    /** The txt2b. */
    public Color txt2b = Color.red;

    /** The txt3a. */
    public Color txt3a = Color.red;

    /** The txt3b. */
    public Color txt3b = Color.red;

    /** The name. */
    public String name = "default";

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
    private String notfound = "FSkin.java: \"" + skin + "\" skin can't find ";

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
    public FSkin(String skinName) throws Exception {
        loadFontAndImages("default");

        if (!skinName.equals("default")) {
            loadFontAndImages(skinName);
        }
    }

    /**
     * Loads objects from skin folder, prints brief error if not found.
     * 
     * @param skinName
     */
    private void loadFontAndImages(String skinName) {
        String dirName = "res/images/skins/" + skinName + "/";

        // Fonts
        font1 = retrieveFont(dirName + font1file);
        font2 = retrieveFont(dirName + font2file);

        // Images
        texture1 = retrieveImage(dirName + texture1file);
        texture2 = retrieveImage(dirName + texture2file);
        texture3 = retrieveImage(dirName + texture3file);
        btnLup = retrieveImage(dirName + btnLupfile);
        btnMup = retrieveImage(dirName + btnMupfile);
        btnRup = retrieveImage(dirName + btnRupfile);
        btnLover = retrieveImage(dirName + btnLoverfile);
        btnMover = retrieveImage(dirName + btnMoverfile);
        btnRover = retrieveImage(dirName + btnRoverfile);
        btnLdown = retrieveImage(dirName + btnLdownfile);
        btnMdown = retrieveImage(dirName + btnMdownfile);
        btnRdown = retrieveImage(dirName + btnRdownfile);
        splash = retrieveImage(dirName + splashfile);

        // Color palette
        File file = new File(dirName + paletteFile);
        BufferedImage image;
        try {
            image = ImageIO.read(file);
            bg1a = getColorFromPixel(image.getRGB(10, 30));
            bg1b = getColorFromPixel(image.getRGB(30, 30));
            bg2a = getColorFromPixel(image.getRGB(50, 30));
            bg2b = getColorFromPixel(image.getRGB(70, 30));
            bg3a = getColorFromPixel(image.getRGB(90, 30));
            bg3b = getColorFromPixel(image.getRGB(110, 30));

            txt1a = getColorFromPixel(image.getRGB(10, 70));
            txt1b = getColorFromPixel(image.getRGB(30, 70));
            txt2a = getColorFromPixel(image.getRGB(50, 70));
            txt2b = getColorFromPixel(image.getRGB(70, 70));
            txt3a = getColorFromPixel(image.getRGB(90, 70));
            txt3b = getColorFromPixel(image.getRGB(110, 70));
        } catch (IOException e) {
            System.err.println(notfound + paletteFile);
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
    private ImageIcon retrieveImage(String address) {
        tempImg = new ImageIcon(address);
        if (tempImg.getIconWidth() == -1) {
            System.err.println(notfound + address);
        }

        return tempImg;
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
    private Font retrieveFont(String address) {
        tempFont = GuiUtils.newFont(address);

        return tempFont;
    }

    /**
     * <p>
     * getColorFromPixel.
     * </p>
     * 
     * @param {@link java.lang.Integer} pixel information
     */
    private Color getColorFromPixel(int pixel) {
        return new Color((pixel & 0x00ff0000) >> 16, (pixel & 0x0000ff00) >> 8, (pixel & 0x000000ff));
    }
}
