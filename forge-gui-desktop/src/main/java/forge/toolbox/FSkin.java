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
package forge.toolbox;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
//import java.awt.image.BaseMultiResolutionImage;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ComboBoxModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.JTextComponent;

import forge.Singletons;
import forge.gui.FThreads;
import forge.gui.GuiUtils;
import forge.gui.framework.ILocalRepaint;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.localinstance.skin.FSkinProp;
import forge.localinstance.skin.ISkinImage;
import forge.model.FModel;
import forge.util.Localizer;
import forge.util.OperatingSystem;
import forge.util.WordUtil;
import forge.view.FView;


/**
 * Assembles settings from selected or default theme as appropriate. Saves in a
 * hashtable, access using .get(settingName) method.
 */
public class FSkin {

    public static final int SYMBOL_WIDTH = 13;
    public static final int SYMBOL_HEIGHT = 13;

    /**
     * Retrieves a color from this skin's color map.
     *
     * @param c0 &emsp; Colors property (from enum)
     * @return {@link forge.toolbox.FSkin.SkinColor}
     */
    public static SkinColor getColor(final Colors c0) {
        return SkinColor.baseColors.get(c0);
    }

    /** Steps RGB components of a color up or down.
     * Returns opaque (non-alpha) stepped color.
     * Plus for lighter, minus for darker.
     *
     * @param clr0 {@link java.awt.Color}
     * @param step int
     * @return {@link java.awt.Color}
     */
    public static Color stepColor(final Color clr0, final int step) {
        int r = clr0.getRed();
        int g = clr0.getGreen();
        int b = clr0.getBlue();

        // Darker
        if (step < 0) {
            r =  ((r + step > 0) ? r + step : 0);
            g =  ((g + step > 0) ? g + step : 0);
            b =  ((b + step > 0) ? b + step : 0);
        }
        else {
            r =  ((r + step < 255) ? r + step : 255);
            g =  ((g + step < 255) ? g + step : 255);
            b =  ((b + step < 255) ? b + step : 255);
        }

        return new Color(r, g, b);
    }

    /** Returns RGB components of a color, with a new
     * value for alpha. 0 = transparent, 255 = opaque.
     *
     * @param clr0 {@link java.awt.Color}
     * @param alpha int
     * @return {@link java.awt.Color}
     */
    public static Color alphaColor(final Color clr0, final int alpha) {
        return new Color(clr0.getRed(), clr0.getGreen(), clr0.getBlue(), alpha);
    }

    /**
     * @see <a href="http://www.nbdtech.com/Blog/archive/2008/04/27/Calculating-the-Perceived-Brightness-of-a-Color.aspx">
     *     http://www.nbdtech.com/Blog/archive/2008/04/27/Calculating-the-Perceived-Brightness-of-a-Color.aspx</a>
     */
    public static boolean isColorBright(final Color c) {
        final int v = (int)Math.sqrt(
                c.getRed() * c.getRed() * 0.241 +
                c.getGreen() * c.getGreen() * 0.691 +
                c.getBlue() * c.getBlue() * 0.068);
        return v >= 130;
    }

    public static Color getHighContrastColor(final Color c) {
        return isColorBright(c) ? Color.BLACK : Color.WHITE;
    }

    public static void setGraphicsColor(final Graphics g, final SkinColor skinColor) {
        g.setColor(skinColor.color);
    }

    public static void setGraphicsGradientPaint(final Graphics2D g2d, final float x1, final float y1, final SkinColor skinColor1, final float x2, final float y2, final SkinColor skinColor2) {
        g2d.setPaint(new GradientPaint(x1, y1, skinColor1.color, x2, y2, skinColor2.color));
    }
    public static void setGraphicsGradientPaint(final Graphics2D g2d, final float x1, final float y1, final Color color1, final float x2, final float y2, final SkinColor skinColor2) {
        g2d.setPaint(new GradientPaint(x1, y1, color1, x2, y2, skinColor2.color));
    }
    public static void setGraphicsGradientPaint(final Graphics2D g2d, final float x1, final float y1, final SkinColor skinColor1, final float x2, final float y2, final Color color2) {
        g2d.setPaint(new GradientPaint(x1, y1, skinColor1.color, x2, y2, color2));
    }

    //set background color for component that's temporary
    //only use if can't use ISkinnedComponent class
    public static void setTempBackground(final Component comp, final SkinColor skinColor) {
        comp.setBackground(skinColor.color);
    }

    public static class SkinColor {
        private static final HashMap<Colors, SkinColor> baseColors = new HashMap<>();
        private static final HashMap<String, SkinColor> derivedColors = new HashMap<>();
        private static final int NO_BRIGHTNESS_DELTA = 0;
        private static final int NO_STEP = -999; //needs to be large negative since small negative values are valid
        private static final int NO_ALPHA = -1;

        private final Colors baseColor;
        private final int brightnessDelta;
        private final int step;
        private final int contrastStep;
        private final int alpha;
        protected Color color;

        public Color getColor() { return color; }

        //private constructors for color that changes with skin (use getColor())
        private SkinColor(final Colors baseColor0) {
            this(baseColor0, NO_BRIGHTNESS_DELTA, NO_STEP, NO_STEP, NO_ALPHA);
        }
        private SkinColor(final Colors baseColor0, final int brightnessDelta0, final int step0, final int contrastStep0, final int alpha0) {
            this.baseColor = baseColor0;
            this.brightnessDelta = brightnessDelta0;
            this.step = step0;
            this.contrastStep = contrastStep0;
            this.alpha = alpha0;
            this.updateColor();
        }

        private SkinColor getDerivedColor(final int brightnessDelta0, final int step0, final int contrastStep0, final int alpha0) {
            final String key = this.baseColor.name() + "|" + brightnessDelta0 + "|" + step0 + "|" + contrastStep0 + "|" + alpha0;
            SkinColor derivedColor = derivedColors.get(key);
            if (derivedColor == null) {
                derivedColor = new SkinColor(this.baseColor, brightnessDelta0, step0, contrastStep0, alpha0);
                derivedColors.put(key, derivedColor);
            }
            return derivedColor;
        }

        public SkinColor brighter() {
            return getDerivedColor(this.brightnessDelta + 1, this.step, this.contrastStep, this.alpha);
        }

        public SkinColor darker() {
            return getDerivedColor(this.brightnessDelta - 1, this.step, this.contrastStep, this.alpha);
        }

        public SkinColor stepColor(int step0) {
            if (this.step != NO_STEP) {
                step0 += this.step;
            }
            return getDerivedColor(this.brightnessDelta, step0, this.contrastStep, this.alpha);
        }

        public SkinColor getContrastColor(int contrastStep0) {
            if (this.contrastStep != NO_STEP) {
                contrastStep0 += this.contrastStep;
            }
            return getDerivedColor(this.brightnessDelta, this.step, contrastStep0, this.alpha);
        }

        public SkinColor getHighContrastColor() {
            return getContrastColor(255);
        }

        public SkinColor alphaColor(final int alpha0) {
            return getDerivedColor(this.brightnessDelta, this.step, this.contrastStep, alpha0);
        }

        protected void updateColor() {
            this.color = this.baseColor.color;
            if (this.brightnessDelta != NO_BRIGHTNESS_DELTA) {
                if (this.brightnessDelta < 0) {
                    for (int i = 0; i > this.brightnessDelta; i--) {
                        this.color = this.color.darker();
                    }
                }
                else {
                    for (int i = 0; i < this.brightnessDelta; i++) {
                        this.color = this.color.brighter();
                    }
                }
            }
            if (this.step != NO_STEP) {
                this.color = FSkin.stepColor(this.color, this.step);
            }
            if (this.contrastStep != NO_STEP) {
                this.color = FSkin.stepColor(this.color, isColorBright(this.color) ? -this.contrastStep : this.contrastStep);
            }
            if (this.alpha != NO_ALPHA) {
                this.color = FSkin.alphaColor(this.color, this.alpha);
            }
        }
    }

    //set border for component that's temporary
    //only use if can't use ISkinnedComponent class
    public static void setTempBorder(final JComponent comp, final SkinBorder skinBorder) {
        comp.setBorder(skinBorder.createBorder());
    }

    public static abstract class SkinBorder {
        protected abstract Border createBorder();
    }
    public static class LineSkinBorder extends SkinBorder {
        private final SkinColor skinColor;
        private final int thickness;

        public LineSkinBorder(final SkinColor skinColor0) {
            this(skinColor0, 1);
        }
        public LineSkinBorder(final SkinColor skinColor0, final int thickness0) {
            this.skinColor = skinColor0;
            this.thickness = thickness0;
        }

        @Override
        protected Border createBorder() {
            return BorderFactory.createLineBorder(this.skinColor.color, this.thickness);
        }
    }
    public static class MatteSkinBorder extends SkinBorder {
        private final int top, left, bottom, right;
        private final SkinColor skinColor;

        public MatteSkinBorder(final int top0, final int left0, final int bottom0, final int right0, final SkinColor skinColor0) {
            this.top = top0;
            this.left = left0;
            this.bottom = bottom0;
            this.right = right0;
            this.skinColor = skinColor0;
        }

        @Override
        protected Border createBorder() {
            return BorderFactory.createMatteBorder(this.top, this.left, this.bottom, this.right, this.skinColor.color);
        }
    }
    public static class CompoundSkinBorder extends SkinBorder {
        private Border outsideBorder, insideBorder;
        private SkinBorder outsideSkinBorder, insideSkinBorder;

        public CompoundSkinBorder(final SkinBorder outsideSkinBorder0, final SkinBorder insideSkinBorder0) {
            this.outsideSkinBorder = outsideSkinBorder0;
            this.insideSkinBorder = insideSkinBorder0;
        }
        public CompoundSkinBorder(final SkinBorder outsideSkinBorder0, final Border insideBorder0) {
            this.outsideSkinBorder = outsideSkinBorder0;
            this.insideBorder = insideBorder0;
        }
        public CompoundSkinBorder(final Border outsideBorder0, final SkinBorder insideSkinBorder0) {
            this.outsideBorder = outsideBorder0;
            this.insideSkinBorder = insideSkinBorder0;
        }

        @Override
        protected Border createBorder() {
            Border outBorder = this.outsideBorder;
            if (this.outsideSkinBorder != null) {
                outBorder = this.outsideSkinBorder.createBorder();
            }
            Border inBorder = this.insideBorder;
            if (this.insideSkinBorder != null) {
                inBorder = this.insideSkinBorder.createBorder();
            }
            return BorderFactory.createCompoundBorder(outBorder, inBorder);
        }
    }
    public static class TitledSkinBorder extends SkinBorder {
        private final SkinColor foreColor;
        private final String title;
        private Border insideBorder;
        private SkinBorder insideSkinBorder;

        public TitledSkinBorder(final Border insideBorder0, final String title0, final SkinColor foreColor0) {
            this.insideBorder = insideBorder0;
            this.title = title0;
            this.foreColor = foreColor0;
        }

        public TitledSkinBorder(final SkinBorder insideSkinBorder0, final String title0, final SkinColor foreColor0) {
            this.insideSkinBorder = insideSkinBorder0;
            this.title = title0;
            this.foreColor = foreColor0;
        }

        @Override
        protected Border createBorder() {
            Border inBorder = this.insideBorder;
            if (this.insideSkinBorder != null) {
                inBorder = this.insideSkinBorder.createBorder();
            }
            final TitledBorder border = new TitledBorder(inBorder, this.title);
            border.setTitleColor(foreColor.color);
            return border;
        }
    }

    public enum Colors {
        CLR_THEME(FSkinProp.CLR_THEME),
        CLR_BORDERS(FSkinProp.CLR_BORDERS),
        CLR_ZEBRA(FSkinProp.CLR_ZEBRA),
        CLR_HOVER(FSkinProp.CLR_HOVER),
        CLR_ACTIVE(FSkinProp.CLR_ACTIVE),
        CLR_INACTIVE(FSkinProp.CLR_INACTIVE),
        CLR_TEXT(FSkinProp.CLR_TEXT),
        CLR_PHASE_INACTIVE_ENABLED(FSkinProp.CLR_PHASE_INACTIVE_ENABLED),
        CLR_PHASE_INACTIVE_DISABLED(FSkinProp.CLR_PHASE_INACTIVE_DISABLED),
        CLR_PHASE_ACTIVE_ENABLED(FSkinProp.CLR_PHASE_ACTIVE_ENABLED),
        CLR_PHASE_ACTIVE_DISABLED(FSkinProp.CLR_PHASE_ACTIVE_DISABLED),
        CLR_THEME2(FSkinProp.CLR_THEME2),
        CLR_OVERLAY(FSkinProp.CLR_OVERLAY),
        CLR_COMBAT_TARGETING_ARROW(FSkinProp.CLR_COMBAT_TARGETING_ARROW),
        CLR_NORMAL_TARGETING_ARROW(FSkinProp.CLR_NORMAL_TARGETING_ARROW),
        CLR_PWATTK_TARGETING_ARROW(FSkinProp.CLR_PWATTK_TARGETING_ARROW);

        private Color color;
        private final FSkinProp skinProp;

        Colors(final FSkinProp skinProp0) {
            skinProp = skinProp0;
        }

        public static Colors fromSkinProp(final FSkinProp skinProp) {
            for (final Colors c : Colors.values()) {
                if (c.skinProp == skinProp) {
                    return c;
                }
            }
            return null;
        }

        public static void updateAll() {
            for (final Colors c : Colors.values()) {
                c.updateColor();
            }
            if (SkinColor.baseColors.isEmpty()) { //initialize base skin colors if needed
                for (final Colors c : Colors.values()) {
                    SkinColor.baseColors.put(c, new SkinColor(c));
                }
            }
            else { //update existing SkinColors if baseColors already initialized
                for (final SkinColor c : SkinColor.baseColors.values()) {
                    c.updateColor();
                }
                for (final SkinColor c : SkinColor.derivedColors.values()) {
                    c.updateColor();
                }
            }
        }

        private void updateColor() {
            int[] tempCoords = skinProp.getCoords();
            x0 = tempCoords[0];
            y0 = tempCoords[1];

            color = bimPreferredSprite.getData().getBounds().contains(x0, y0) ? getColorFromPixel(bimPreferredSprite.getRGB(x0, y0)) : new Color(0, 0, 0, 0);
        }
    }

    public static void drawImage(final Graphics g, final SkinImage skinImage, final int x, final int y) {
        skinImage.draw(g, x, y);
    }
    public static void drawImage(final Graphics g, final SkinImage skinImage, final int x, final int y, final int w, final int h) {
        skinImage.draw(g, x, y, w, h);
    }
    public static void drawImage(final Graphics g, final SkinImage skinImage, final int dx1, final int dy1, final int dx2, final int dy2, final int sx1, final int sy1, final int sx2, final int sy2) {
        skinImage.draw(g, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2);
    }

    /**
     * Gets an image.
     *
     * @param s0 &emsp; FSkinProp enum
     * @return {@link forge.toolbox.FSkin.SkinImage}
     */
    public static SkinImage getImage(final FSkinProp s0) {
        SkinImage image = SkinImage.images.get(s0);
        if (image == null) {
            image = SkinIcon.icons.get(s0);
            if (image == null) {
                throw new NullPointerException("Can't find an image for FSkinProp " + s0);
            }
        }
        return image;
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
     * @return {@link forge.toolbox.FSkin.SkinImage}
     */
    public static SkinImage getImage(final FSkinProp s0, int w0, int h0) {
        w0 = (w0 < 1) ? 1 : w0;
        h0 = (h0 < 1) ? 1 : h0;
        return getImage(s0).resize(w0, h0);
    }

    public static class SkinImage implements ISkinImage {
        private static final Map<FSkinProp, SkinImage> images = new HashMap<>();

        private static void setImage(final FSkinProp s0, final Image image0) {
            SkinImage skinImage = images.get(s0);
            if (skinImage == null) {
                skinImage = new SkinImage(image0);
                images.put(s0, skinImage);
            }
            else {
                skinImage.changeImage(image0, null);
            }
        }

        /**
         * setImage, with auto-scaling assumed true.
         * @param s0
         */
        private static void setImage(final FSkinProp s0) {
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
         * @param s0 &emsp; An address in the hashmap, derived from FSkinProp enum
         */
        private static void setImage(final FSkinProp s0, final boolean scale) {
            int[] tempCoords = s0.getCoords();
            x0 = tempCoords[0];
            y0 = tempCoords[1];
            w0 = tempCoords[2];
            h0 = tempCoords[3];
            newW = (tempCoords.length == 6 ? tempCoords[4] : 0);
            newH = (tempCoords.length == 6 ? tempCoords[5] : 0);
            final BufferedImage img = testPreferredSprite(s0);
            final BufferedImage bi0 = img.getSubimage(x0, y0, w0, h0);

            if (scale && newW != 0) {
                setImage(s0, bi0.getScaledInstance(newW, newH, Image.SCALE_SMOOTH));
            }
            else {
                setImage(s0, bi0);
            }
        }

        protected Image image;
        protected ImageIcon imageIcon;
        protected HashMap<String, SkinImage> scaledImages = new HashMap<>();
        private HashMap<String, SkinCursor> cursors = new HashMap<>();

        private SkinImage(final Image image0) {
            this.image = image0;
        }

        protected void changeImage(final Image image0, final ImageIcon imageIcon0) {
            this.image = image0;
            this.imageIcon = imageIcon0;
            this.updateScaledImages();
            this.updateCursors();
        }

        @Override
        protected SkinImage clone() {
            return new SkinImage(this.image);
        }

        public SkinImage resize(final int w, final int h) {
            final String key = w + "x" + h;
            SkinImage scaledImage = this.scaledImages.get(key);
            if (scaledImage == null) {
                scaledImage = this.clone();
                scaledImage.createResizedImage(this, w, h);
                this.scaledImages.put(key, scaledImage);
            }
            return scaledImage;
        }

        public boolean save(final String path, final int w, final int h) {
            final Image scaledImage = this.image.getScaledInstance(w, h, Image.SCALE_SMOOTH);

            final BufferedImage resizedImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

            final Graphics2D g2d = resizedImage.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.drawImage(scaledImage, 0, 0, null);
            g2d.dispose();

            final File outputfile = new File(path);
            try {
                ImageIO.write(resizedImage, "png", outputfile);
                return true;
            } catch (final IOException e) {
                e.printStackTrace();
            }
            return false;
        }

        public SkinImage scale(final double scale) {
            return scale(scale, scale);
        }
        public SkinImage scale(final double scaleX, final double scaleY) {
            final String key = scaleX + "|" + scaleY;
            SkinImage scaledImage = this.scaledImages.get(key);
            if (scaledImage == null) {
                scaledImage = this.clone();
                scaledImage.createScaledImage(this, scaleX, scaleY);
                this.scaledImages.put(key, scaledImage);
            }
            return scaledImage;
        }

        protected void updateScaledImages() {
            for (final Entry<String, SkinImage> i : this.scaledImages.entrySet()) {
                String[] dims = i.getKey().split("x");
                if (dims.length == 2) { //static scale
                    i.getValue().createResizedImage(this, Integer.parseInt(dims[0]), Integer.parseInt(dims[1]));
                }
                else { //dynamic scale
                    dims = i.getKey().split("\\|"); //must escape since "|" is regex operator
                    i.getValue().createScaledImage(this, Double.parseDouble(dims[0]), Double.parseDouble(dims[1]));
                }
            }
        }

        protected void createResizedImage(final SkinImage baseImage, final int w, final int h) {
            this.changeImage(baseImage.image.getScaledInstance(w, h, Image.SCALE_SMOOTH), null);
        }

        private void createScaledImage(final SkinImage baseImage, final double scaleX, final double scaleY) {
            createResizedImage(baseImage, (int)(baseImage.getWidth() * scaleX), (int)(baseImage.getHeight() * scaleY));
        }

        private SkinCursor toCursor(final int hotSpotX, final int hotSpotY, final String name) {
            final String key = hotSpotX + "|" + hotSpotY + "|" + name;
            SkinCursor cursor = this.cursors.get(key);
            if (cursor == null) {
                cursor = new SkinCursor(new Point(hotSpotX, hotSpotY), name);
                cursor.updateCursor(this.image);
                this.cursors.put(key, cursor);
            }
            return cursor;
        }

        private void updateCursors() {
            for (final SkinCursor cursor : this.cursors.values()) {
                cursor.updateCursor(this.image);
            }
        }

        public Dimension getSizeForPaint(final Graphics g) {
            if (g == null) {
                throw new NullPointerException("Must pass Graphics to get size for paint");
            }
            return new Dimension(this.getWidth(), this.getHeight());
        }

        protected int getWidth() {
            return this.image.getWidth(null);
        }

        protected int getHeight() {
            return this.image.getHeight(null);
        }

        protected ImageIcon getIcon() {
            if (this.imageIcon == null) {
                /*float screenScale = GuiBase.getInterface().getScreenScale();
                int iconWidth = Math.round(image.getWidth(null) / screenScale);
                int iconHeight = Math.round(image.getHeight(null) / screenScale);
                Image [] iconImages = new Image[2];
                iconImages[0] = image.getScaledInstance(iconWidth, iconHeight, Image.SCALE_SMOOTH);
                iconImages[1] = image;
                BaseMultiResolutionImage multiImage = new BaseMultiResolutionImage(iconImages);*/
                this.imageIcon = new ImageIcon(this.image/*multiImage*/);
            }
            return this.imageIcon;
        }

        protected void draw(final Graphics g, final int x, final int y) {
            g.drawImage(image, x, y, null);
        }
        protected void draw(final Graphics g, final int x, final int y, final int w, final int h) {
            g.drawImage(image, x, y, w, h, null);
        }
        protected void draw(final Graphics g, final int dx1, final int dy1, final int dx2, final int dy2, final int sx1, final int sy1, final int sx2, final int sy2) {
            g.drawImage(image, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, null);
        }
    }

    /**
     * Gets an image.
     *
     * @param s0 &emsp; FSkinProp enum
     * @return {@link forge.toolbox.FSkin.SkinCursor}
     */
    public static SkinCursor getCursor(final FSkinProp s0, final int hotSpotX, final int hotSpotY, final String name) {
        return getImage(s0).toCursor(hotSpotX, hotSpotY, name);
    }

    public static class SkinCursor {
        private static final Toolkit TOOLS = Toolkit.getDefaultToolkit();

        private final Point hotSpot;
        private final String name;
        private Cursor cursor;

        private SkinCursor(final Point hotSpot0, final String name0) {
            this.hotSpot = hotSpot0;
            this.name = name0;
        }

        private void updateCursor(final Image image) {
            this.cursor = TOOLS.createCustomCursor(image, this.hotSpot, this.name);
        }
    }

    /**
     * Gets an icon.
     *
     * @param s0 &emsp; FSkinProp enum
     * @return {@link forge.toolbox.FSkin.SkinImage}
     */
    public static SkinIcon getIcon(final FSkinProp s0) {
        final SkinIcon icon = SkinIcon.icons.get(s0);
        if (icon == null) {
            throw new NullPointerException("Can't find an icon for FSkinProp " + s0);
        }
        return icon;
    }

    public static class SkinIcon extends SkinImage {
        private static final Map<FSkinProp, SkinIcon> icons = new HashMap<>();

        private static void setIcon(final FSkinProp s0, final ImageIcon imageIcon0) {
            SkinIcon skinIcon = icons.get(s0);
            if (skinIcon == null) {
                skinIcon = new SkinIcon(imageIcon0);
                icons.put(s0, skinIcon);
            }
            else {
                skinIcon.changeImage(imageIcon0.getImage(), imageIcon0);
            }
        }

        private static void setIcon(final FSkinProp s0) {
            int[] tempCoords = s0.getCoords();
            x0 = tempCoords[0];
            y0 = tempCoords[1];
            w0 = tempCoords[2];
            h0 = tempCoords[3];

            final BufferedImage img = testPreferredSprite(s0);

            setIcon(s0, new ImageIcon(img.getSubimage(x0, y0, w0, h0)));
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
        private static void setIcon(final FSkinProp s0, final String s1) {
            try {
                final File file = new File(s1);
                ImageIO.read(file);
            } catch (final IOException e) {
                e.printStackTrace();
            }
            setIcon(s0, new ImageIcon(s1));
        }

        /**
         * Sets an icon in this skin's icon map from a buffered image.
         *
         * @param s0 &emsp; Skin property (from enum)
         * @param bi0 &emsp; BufferedImage
         */
        private static void setIcon(final FSkinProp s0, final BufferedImage bi0) {
            setIcon(s0, new ImageIcon(bi0));
        }

        private SkinIcon(final ImageIcon imageIcon0) {
            super(imageIcon0.getImage());
            this.imageIcon = imageIcon0;
        }

        @Override
        protected SkinIcon clone() {
            return new SkinIcon(this.imageIcon);
        }

        @Override
        public SkinIcon resize(final int w, final int h) {
            return (SkinIcon)super.resize(w, h);
        }

        @Override
        public SkinIcon scale(final double scale) {
            return scale(scale, scale);
        }
        @Override
        public SkinIcon scale(final double scaleX, final double scaleY) {
            return (SkinIcon)super.scale(scaleX, scaleY);
        }

        @Override
        protected void createResizedImage(final SkinImage baseImage, final int w, final int h) {
            final Image image0 = baseImage.image.getScaledInstance(w, h, java.awt.Image.SCALE_SMOOTH);
            this.changeImage(image0, new ImageIcon(image0));
        }

        @Override
        protected int getWidth() {
            return this.imageIcon.getIconWidth();
        }

        @Override
        protected int getHeight() {
            return this.imageIcon.getIconHeight();
        }

        @Override
        protected ImageIcon getIcon() { //can skip null check since imageIcon will always be set
            return this.imageIcon;
        }
    }

    //allow creating dynamic icons that can be used in place of skin icons
    public static class UnskinnedIcon extends SkinIcon {
        private final float opacity;

        public UnskinnedIcon(final String path0) {
            super(new ImageIcon(path0));
            opacity = 1;
        }
        public UnskinnedIcon(final BufferedImage i0) {
            this(i0, 1);
        }
        public UnskinnedIcon(final BufferedImage i0, final float opacity0) {
            super(new ImageIcon(i0));
            opacity = opacity0;
        }

        @Override
        public ImageIcon getIcon() {
            return super.getIcon();
        }

        @Override
        protected void draw(final Graphics g, final int x, final int y) {
            if (opacity == 1) {
                super.draw(g, x, y);
                return;
            }
            final Graphics2D g2d = (Graphics2D)g;
            final Composite oldComp = g2d.getComposite();
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
            super.draw(g2d, x, y);
            g2d.setComposite(oldComp);
        }
        @Override
        protected void draw(final Graphics g, final int x, final int y, final int w, final int h) {
            if (opacity == 1) {
                super.draw(g, x, y, w, h);
                return;
            }
            final Graphics2D g2d = (Graphics2D)g;
            final Composite oldComp = g2d.getComposite();
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
            super.draw(g, x, y, w, h);
            g2d.setComposite(oldComp);
        }
        @Override
        protected void draw(final Graphics g, final int dx1, final int dy1, final int dx2, final int dy2, final int sx1, final int sy1, final int sx2, final int sy2) {
            if (opacity == 1) {
                super.draw(g, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2);
                return;
            }
            final Graphics2D g2d = (Graphics2D)g;
            final Composite oldComp = g2d.getComposite();
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
            super.draw(g, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2);
            g2d.setComposite(oldComp);
        }
    }

    private static Map<Integer, SkinImage> avatars;
    private static Map<Integer, SkinImage> sleeves;
    private static Map<Integer, Font> fixedFonts = new HashMap<>();

    public static Font getFixedFont() {
        return getFixedFont(defaultFontSize);
    }

    /** @return {@link java.awt.font} */
    private static Font getFixedFont(final int size) {
        Font fixedFont = fixedFonts.get(size);
        if (fixedFont == null) {
            fixedFont = new Font("Monospaced", Font.PLAIN, size);
            fixedFonts.put(size, fixedFont);
        }
        return fixedFont;
    }

    public static Font getRelativeFixedFont(final int relative) {
        return getFixedFont(getRelativeFontSize(relative));
    }

    private static double getMultiplier(final int relative) {
        // don't know of a good way to get the preference default value
        return relative / 12.0;
    }

    public static Integer getRelativeFontSize(final int relative) {
        double multiplier = getMultiplier(relative);
        return (int)(defaultFontSize * multiplier);
    }

    /**
     * @return {@link forge.toolbox.FSkin.SkinFont}
     */
    public static SkinFont getFont() {
        return getFont(defaultFontSize);
    }

    /**
     * @param size - integer, pixel size
     * @return {@link forge.toolbox.FSkin.SkinFont}
     */
    public static SkinFont getFont(final int size) {
        return SkinFont.get(Font.PLAIN, size);
    }

    public static SkinFont getRelativeFont(final int relative) {
        return SkinFont.get(Font.PLAIN, getRelativeFontSize(relative));
    }

    /**
     * @return {@link forge.toolbox.FSkin.SkinFont}
     */
    public static SkinFont getBoldFont() {
        return getBoldFont(defaultFontSize);
    }

    /**
     * @param size - integer, pixel size
     * @return {@link forge.toolbox.FSkin.SkinFont}
     */
    public static SkinFont getBoldFont(final int size) {
        return SkinFont.get(Font.BOLD, size);
    }

    public static SkinFont getRelativeBoldFont(final int relative) {
        return SkinFont.get(Font.BOLD, getRelativeFontSize(relative));
    }

    /**
     * @return {@link forge.toolbox.FSkin.SkinFont}
     */
    public static SkinFont getItalicFont() {
        return getItalicFont(defaultFontSize);
    }

    /**
     * @param size - integer, pixel size
     * @return {@link forge.toolbox.FSkin.SkinFont}
     */
    public static SkinFont getItalicFont(final int size) {
        return SkinFont.get(Font.ITALIC, size);
    }

    public static SkinFont getRelativeItalicFont(final int relative) {
        return SkinFont.get(Font.ITALIC, getRelativeFontSize(relative));
    }

    public static void setGraphicsFont(final Graphics g, final SkinFont skinFont) {
        g.setFont(skinFont.font);
    }

    public static class SkinFont {
        private static Font baseFont;
        private static Map<String, SkinFont> fonts = new HashMap<>();

        private static SkinFont get(final int style0, final int size0) {
            final String key = style0 + "|" + size0;
            SkinFont skinFont = fonts.get(key);
            if (skinFont == null) {
                skinFont = new SkinFont(style0, size0);
                fonts.put(key, skinFont);
            }
            return skinFont;
        }

        private static void setBaseFont(final Font baseFont0) {
            baseFont = baseFont0;

            //update all cached skin fonts
            for (final SkinFont skinFont : fonts.values()) {
                skinFont.updateFont();
            }
        }

        private final int style, size;
        private Font font;

        private SkinFont(final int style0, final int size0) {
            this.style = style0;
            this.size = size0;
            this.updateFont();
        }

        public Font getBaseFont() {
            return font;
        }

        public int getSize() {
            return this.font.getSize();
        }

        public int measureTextWidth(final Graphics g, final String text) {
            return g.getFontMetrics(this.font).stringWidth(text);
        }

        public FontMetrics getFontMetrics() {
            return Singletons.getView().getFrame().getGraphics().getFontMetrics(this.font);
        }

        private void updateFont() {
            this.font = baseFont.deriveFont(this.style, this.size);
        }
    }

    private static void addEncodingSymbol(final String key, final FSkinProp skinProp) {
        final String path = ForgeConstants.CACHE_SYMBOLS_DIR + "/" + key.replace("/", "") + ".png";
        getImage(skinProp).save(path, SYMBOL_WIDTH, SYMBOL_HEIGHT);
    }

    public static String encodeSymbols(String str, final boolean formatReminderText) {
        String pattern, replacement;

        if (formatReminderText) {
            //format reminder text in italics (or hide if preference set)
            pattern = " \\((.+?)\\)";
            replacement = FModel.getPreferences().getPrefBoolean(FPref.UI_HIDE_REMINDER_TEXT) ?
                    "" : " <i>\\($1\\)</i>";
            str = str.replaceAll(pattern, replacement);
        }
        // Just return the string unencoded if we're optimizing for screen readers.
        if (FModel.getPreferences().getPrefBoolean(FPref.UI_SR_OPTIMIZE)) {
            return str;
        }
        //format mana symbols to display as icons
        pattern = "\\{([A-Z0-9]+)\\}|\\{([A-Z0-9]+)/([A-Z0-9]+)\\}|(A-)|\\{([A-Z0-9]+)/([A-Z0-9]+)/([A-Z0-9]+)\\}"; //fancy pattern needed so "/" can be omitted from replacement
        try {
            replacement = "<img src=\"" + new File(ForgeConstants.CACHE_SYMBOLS_DIR + "/$1$2$3$4$5$6$7.png").toURI().toURL().toString() + "\" width=" + SYMBOL_WIDTH + " height=" + SYMBOL_HEIGHT + ">";
            str = str.replaceAll(pattern, replacement);
        } catch (final MalformedURLException e) {
            e.printStackTrace();
        }

        //ensure paragraph and line breaks aren't lost
        str = str.replaceAll("\r?\n", "<br>");

        return "<html>" + str + "</html>"; //must wrap in <html> tag for images to appear
    }

    private static List<String> allSkins;
    private static int currentSkinIndex;
    private static String preferredDir;
    private static String preferredName;
    private static BufferedImage bimDefaultSprite, bimFavIcon, bimPreferredSprite, bimFoils, bimQuestDraftDeck, bimOldFoils,
    bimDefaultAvatars, bimPreferredAvatars, bimTrophies, bimAbilities, bimManaIcons, bimPhyrexian, bimColorlessHybrid, bimDefaultSleeve,
            bimDefaultSleeve2, bimDefaultDeckbox, bimPrefferedSetLogo, bimDefaultWatermark, bimDefaultDraftRank;
    private static int x0, y0, w0, h0, newW, newH, preferredW, preferredH;
    private static int defaultFontSize = 12;
    private static boolean loaded = false;

    public static void changeSkin(final String skinName) {
        final ForgePreferences prefs = FModel.getPreferences();
        if (skinName.equals(prefs.getPref(FPref.UI_SKIN))) { return; }

        //save skin preference
        prefs.setPref(FPref.UI_SKIN, skinName);
        prefs.save();

        //load skin
        loaded = false; //reset this temporarily until end of loadFull()
        loadLight(skinName, false);
        loadFull(false);

        //refresh certain components skinned via look and feel
        Singletons.getControl().getForgeMenu().refresh();
        FComboBoxWrapper.refreshAllSkins();
        FComboBoxPanel.refreshAllSkins();

        //repaint main frame to ensure all visible components apply the new skin
        Singletons.getView().getFrame().repaint();
    }

    /**
     * Loads a "light" version of FSkin, just enough for the splash screen:
     * skin name. Generates custom skin settings, fonts, and backgrounds.
     *
     *
     * @param skinName
     *            the skin name
     */
    public static void loadLight(final String skinName, final boolean onInit) {
        if (onInit) {
            // No need for this method to be loaded while on the EDT.
            FThreads.assertExecutedByEdt(false);

            if (allSkins == null) { //initialize
                allSkins = new ArrayList<>();
                allSkins.add("Default");//init default
                final List<String> skinDirectoryNames = getSkinDirectoryNames();
                for (String skinDirectoryName : skinDirectoryNames) {
                    allSkins.add(WordUtil.capitalize(skinDirectoryName.replace('_', ' ')));
                }
                Collections.sort(allSkins);
            }
        }

        currentSkinIndex = allSkins.indexOf(skinName);

        // Non-default (preferred) skin name and dir.
        preferredName = skinName.toLowerCase().replace(' ', '_');
        preferredDir = preferredName.equals("default") ? ForgeConstants.DEFAULT_SKINS_DIR : ForgeConstants.CACHE_SKINS_DIR + preferredName + "/";

        if (onInit) {
            final File f = new File(preferredDir + ForgeConstants.SPLASH_BG_FILE);
            if (!f.exists()) {
                if (skinName.equals("default")) {
                    throw new RuntimeException(String.format("Cannot find default skin at %s", f.getAbsolutePath()));
                }
                loadLight("default", true);
                return;
            }

            final BufferedImage img;
            try {
                img = ImageIO.read(f);

                final int h = img.getHeight();
                final int w = img.getWidth();

                SkinIcon.setIcon(FSkinProp.BG_SPLASH, img.getSubimage(0, 0, w, h - 100));

                UIManager.put("ProgressBar.background", getColorFromPixel(img.getRGB(25, h - 75)));
                UIManager.put("ProgressBar.selectionBackground", getColorFromPixel(img.getRGB(75, h - 75)));
                UIManager.put("ProgressBar.foreground", getColorFromPixel(img.getRGB(25, h - 25)));
                UIManager.put("ProgressBar.selectionForeground", getColorFromPixel(img.getRGB(75, h - 25)));
                UIManager.put("ProgressBar.border", new LineBorder(Color.BLACK, 0));
            }
            catch (final IOException e) {
                e.printStackTrace();
            }
            loaded = true;
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
    public static void loadFull(final boolean onInit) {
        if (onInit) {
            // No need for this method to be loaded while on the EDT.
            FThreads.assertExecutedByEdt(false);

            // Preferred skin name must be called via loadLight() method,
            // which does some cleanup and init work.
            if (preferredName.isEmpty()) { loadLight("default", true); }
        }

        final Localizer localizer = Localizer.getInstance();
        FView.SINGLETON_INSTANCE.setSplashProgessBarMessage(localizer.getMessage("splash.loading.processingimagesprites") + ": ", 12);

        // Grab and test various sprite files.
        final String defaultDir = ForgeConstants.DEFAULT_SKINS_DIR;
        final File f1 = new File(defaultDir + ForgeConstants.SPRITE_ICONS_FILE);
        final File f2 = new File(preferredDir + ForgeConstants.SPRITE_ICONS_FILE);
        final File f3 = new File(defaultDir + ForgeConstants.SPRITE_FOILS_FILE);
        final File f4 = new File(defaultDir + ForgeConstants.SPRITE_AVATARS_FILE);
        final File f5 = new File(preferredDir + ForgeConstants.SPRITE_AVATARS_FILE);
        final File f6 = new File(defaultDir + ForgeConstants.SPRITE_OLD_FOILS_FILE);
        final File f7 = new File(defaultDir + ForgeConstants.SPRITE_TROPHIES_FILE);
        final File f8 = new File(defaultDir + ForgeConstants.DRAFT_DECK_IMG_FILE);
        final File f9 = new File(defaultDir + ForgeConstants.SPRITE_FAVICONS_FILE);
        final File f10 = new File(defaultDir + ForgeConstants.SPRITE_ABILITY_FILE);
        final File f11 = new File(defaultDir + ForgeConstants.SPRITE_MANAICONS_FILE);
        final File f12 = new File(defaultDir + ForgeConstants.SPRITE_SLEEVES_FILE);
        final File f13 = new File(defaultDir + ForgeConstants.SPRITE_SLEEVES2_FILE);
        final File f14 = new File(defaultDir + ForgeConstants.SPRITE_DECKBOX_FILE);
        final File f15 = new File(defaultDir + ForgeConstants.SPRITE_SETLOGO_FILE);
        final File f16 = new File(preferredDir + ForgeConstants.SPRITE_SETLOGO_FILE);
        final File f17 = new File(defaultDir + ForgeConstants.SPRITE_WATERMARK_FILE);
        final File f18 = new File(defaultDir + ForgeConstants.SPRITE_PHYREXIAN_FILE);
        final File f19 = new File(defaultDir + ForgeConstants.SPRITE_COLORLESS_HYBRID_FILE);
        final File f20 = new File(defaultDir + ForgeConstants.SPRITE_DRAFTRANKS_FILE);

        try {
            int p = 0;
            bimDefaultSprite = ImageIO.read(f1);
            FView.SINGLETON_INSTANCE.incrementSplashProgessBar(++p);
            bimAbilities = ImageIO.read(f10);
            FView.SINGLETON_INSTANCE.incrementSplashProgessBar(++p);
            bimManaIcons = ImageIO.read(f11);
            FView.SINGLETON_INSTANCE.incrementSplashProgessBar(++p);
            bimPhyrexian = ImageIO.read(f18);
            FView.SINGLETON_INSTANCE.incrementSplashProgessBar(++p);
            bimColorlessHybrid = ImageIO.read(f19);
            FView.SINGLETON_INSTANCE.incrementSplashProgessBar(++p);
            bimPreferredSprite = ImageIO.read(f2);
            FView.SINGLETON_INSTANCE.incrementSplashProgessBar(++p);
            bimFoils = ImageIO.read(f3);
            FView.SINGLETON_INSTANCE.incrementSplashProgessBar(++p);
            bimOldFoils = f6.exists() ? ImageIO.read(f6) : ImageIO.read(f3);
            FView.SINGLETON_INSTANCE.incrementSplashProgessBar(++p);
            bimDefaultAvatars = ImageIO.read(f4);
            FView.SINGLETON_INSTANCE.incrementSplashProgessBar(++p);
            bimDefaultSleeve = ImageIO.read(f12);
            FView.SINGLETON_INSTANCE.incrementSplashProgessBar(++p);
            bimDefaultSleeve2 = ImageIO.read(f13);
            FView.SINGLETON_INSTANCE.incrementSplashProgessBar(++p);
            bimDefaultDeckbox = ImageIO.read(f14);
            FView.SINGLETON_INSTANCE.incrementSplashProgessBar(++p);
            bimDefaultDraftRank = ImageIO.read(f20);
            FView.SINGLETON_INSTANCE.incrementSplashProgessBar(++p);
            bimPrefferedSetLogo = f16.exists() ? ImageIO.read(f16) : ImageIO.read(f15);
            FView.SINGLETON_INSTANCE.incrementSplashProgessBar(++p);
            bimDefaultWatermark = ImageIO.read(f17);
            FView.SINGLETON_INSTANCE.incrementSplashProgessBar(++p);
            bimTrophies = ImageIO.read(f7);
            FView.SINGLETON_INSTANCE.incrementSplashProgessBar(++p);
            bimQuestDraftDeck = ImageIO.read(f8);
            FView.SINGLETON_INSTANCE.incrementSplashProgessBar(++p);
            bimFavIcon = ImageIO.read(f9);
            FView.SINGLETON_INSTANCE.incrementSplashProgessBar(++p);
            if (f5.exists()) { bimPreferredAvatars = ImageIO.read(f5); }

            FView.SINGLETON_INSTANCE.incrementSplashProgessBar(++p);

            preferredH = bimPreferredSprite.getHeight();
            preferredW = bimPreferredSprite.getWidth();
        }
        catch (final Exception e) {
            System.err.println("FSkin$loadFull: Missing a sprite (default icons, "
                    + "preferred icons, or foils.");
            e.printStackTrace();
        }

        // Initialize fonts
        if (onInit) { //set default font size only once onInit
            defaultFontSize = FModel.getPreferences().getPrefInt(FPref.UI_DEFAULT_FONT_SIZE);
        }
        SkinFont.setBaseFont(GuiUtils.newFont(preferredDir + ForgeConstants.FONT_FILE));

        // Put various images into map (except sprite and splash).
        // Exceptions handled inside method.
        SkinIcon.setIcon(FSkinProp.BG_TEXTURE, preferredDir + ForgeConstants.TEXTURE_BG_FILE);
        SkinIcon.setIcon(FSkinProp.BG_MATCH, preferredDir + ForgeConstants.MATCH_BG_FILE);
        //daynight bg
        SkinIcon.setIcon(FSkinProp.BG_DAY, defaultDir + ForgeConstants.MATCH_BG_DAY_FILE);
        SkinIcon.setIcon(FSkinProp.BG_NIGHT, defaultDir + ForgeConstants.MATCH_BG_NIGHT_FILE);

        // Run through enums and load their coords.
        Colors.updateAll();
        for (final FSkinProp prop : FSkinProp.values()) {
            switch (prop.getType()) {
                case IMAGE:
                    SkinImage.setImage(prop);
                    break;
                case ICON:
                    SkinIcon.setIcon(prop);
                    break;
                case FOIL:
                    setImage(prop, bimFoils);
                    break;
                case OLD_FOIL:
                    setImage(prop, bimOldFoils);
                    break;
                case TROPHY:
                    setImage(prop, bimTrophies);
                    break;
                case FAVICON:
                    setImage(prop, bimFavIcon);
                    break;
                case ABILITY:
                    setImage(prop, bimAbilities);
                    break;
                case MANAICONS:
                    setImage(prop, bimManaIcons);
                    break;
                case PHYREXIAN:
                    setImage(prop, bimPhyrexian);
                    break;
                case COLORLESS_HYBRID:
                    setImage(prop, bimColorlessHybrid);
                    break;
                case DECKBOX:
                    setImage(prop, bimDefaultDeckbox);
                    break;
                case DRAFTRANKS:
                    setImage(prop, bimDefaultDraftRank);
                    break;
                case SETLOGO:
                    setImage(prop, bimPrefferedSetLogo);
                    break;
                case WATERMARKS:
                    setImage(prop, bimDefaultWatermark);
                    break;
                default:
                    break;
            }
        }

        // Assemble avatar images
        assembleAvatars();
        // Sleeves
        assembleSleeves();

        // Images loaded; can start UI init.
        FView.SINGLETON_INSTANCE.setSplashProgessBarMessage("Creating display components.");
        loaded = true;

        // Clear references to buffered images
        bimDefaultSprite.flush();
        bimFoils.flush();
        bimOldFoils.flush();
        bimPreferredSprite.flush();
        bimDefaultAvatars.flush();
        bimDefaultSleeve.flush();
        bimDefaultSleeve2.flush();
        bimDefaultDeckbox.flush();
        bimDefaultDraftRank.flush();
        bimPrefferedSetLogo.flush();
        bimDefaultWatermark.flush();
        bimQuestDraftDeck.flush();
        bimTrophies.flush();
        bimAbilities.flush();
        bimPhyrexian.flush();
        bimColorlessHybrid.flush();
        bimManaIcons.flush();

        if (bimPreferredAvatars != null) { bimPreferredAvatars.flush(); }

        bimDefaultSprite = null;
        bimFoils = null;
        bimOldFoils = null;
        bimPreferredSprite = null;
        bimDefaultAvatars = null;
        bimDefaultSleeve = null;
        bimDefaultSleeve2 = null;
        bimDefaultDeckbox = null;
        bimDefaultDraftRank = null;
        bimPrefferedSetLogo = null;
        bimDefaultWatermark = null;
        bimPreferredAvatars = null;
        bimQuestDraftDeck = null;
        bimTrophies = null;
        bimAbilities = null;
        bimPhyrexian = null;
        bimColorlessHybrid = null;
        bimManaIcons = null;

        //establish encoding symbols
        final File dir = new File(ForgeConstants.CACHE_SYMBOLS_DIR);
        if (!dir.mkdir()) { //ensure symbols directory exists and is empty
            File[] files = dir.listFiles();
            assert files != null;
            for (final File file : files) {
                file.delete();
            }
        }

        for (Map.Entry<String, FSkinProp> e : FSkinProp.MANA_IMG.entrySet()) {
            addEncodingSymbol(e.getKey(), e.getValue());
        }

        addEncodingSymbol("E", FSkinProp.IMG_ENERGY);
        addEncodingSymbol("TK", FSkinProp.IMG_TICKET);
        addEncodingSymbol("EXPERIENCE", FSkinProp.IMG_EXPERIENCE);
        addEncodingSymbol("A-", FSkinProp.IMG_ALCHEMY);

        // Set look and feel after skin loaded
        FView.SINGLETON_INSTANCE.setSplashProgessBarMessage("Setting look and feel...");
        final ForgeLookAndFeel laf = new ForgeLookAndFeel();
        laf.setForgeLookAndFeel(Singletons.getView().getFrame());
    }

    /**
     * Gets the skins.
     *
     * @return the skins
     */
    public static List<String> getSkinDirectoryNames() {
        final List<String> mySkins = new ArrayList<>();

        final File dir = new File(ForgeConstants.CACHE_SKINS_DIR);
        final String[] children = dir.list();
        if (children == null) {
            System.err.println("FSkin > can't find skins directory!");
        } else {
            for (String aChildren : children) {
                if (aChildren.equalsIgnoreCase(".svn")) {
                    continue;
                }
                if (aChildren.equalsIgnoreCase(".DS_Store")) {
                    continue;
                }
                mySkins.add(aChildren);
            }
        }

        return mySkins;
    }

    public static Iterable<String> getAllSkins() {
        return allSkins;
    }

    public static Map<Integer, SkinImage> getAvatars() {
        return avatars;
    }

    public static Map<Integer, SkinImage> getSleeves() {
        return sleeves;
    }

    public static boolean isLoaded() { return loaded; }

    /**
     * getColorFromPixel
     * @param pixel
     * @return
     */
    public static Color getColorFromPixel(final int pixel) {
        int r, g, b, a;
        a = (pixel >> 24) & 0x000000ff;
        r = (pixel >> 16) & 0x000000ff;
        g = (pixel >> 8) & 0x000000ff;
        b = (pixel) & 0x000000ff;
        return new Color(r, g, b, a);
    }

    private static BufferedImage testPreferredSprite(final FSkinProp s0) {
        int[] tempCoords = s0.getCoords();
        x0 = tempCoords[0];
        y0 = tempCoords[1];
        w0 = tempCoords[2];
        h0 = tempCoords[3];

        if (s0.equals(FSkinProp.IMG_QUEST_DRAFT_DECK)) {
            final Color c = getColorFromPixel(bimQuestDraftDeck.getRGB((x0 + w0 / 2), (y0 + h0 / 2)));
            if (c.getAlpha() != 0) { return bimQuestDraftDeck; }
        }

        // Test if requested sub-image in inside bounds of preferred sprite.
        // (Height and width of preferred sprite were set in loadFontAndImages.)
        if (x0 > preferredW || x0 + w0 > preferredW
                || y0 > preferredH || y0 + h0 > preferredH) {
            return bimDefaultSprite;
        }

        // Test if various points of requested sub-image are transparent.
        // If any return true, image exists.
        int x, y;
        Color c;

        // Center
        x = (x0 + w0 / 2);
        y = (y0 + h0 / 2);
        c = getColorFromPixel(bimPreferredSprite.getRGB(x, y));
        if (c.getAlpha() != 0) { return bimPreferredSprite; }

        x += 2;
        y += 2;
        c = getColorFromPixel(bimPreferredSprite.getRGB(x, y));
        if (c.getAlpha() != 0) { return bimPreferredSprite; }

        x -= 4;
        c = getColorFromPixel(bimPreferredSprite.getRGB(x, y));
        if (c.getAlpha() != 0) { return bimPreferredSprite; }

        y -= 4;
        c = getColorFromPixel(bimPreferredSprite.getRGB(x, y));
        if (c.getAlpha() != 0) { return bimPreferredSprite; }

        x += 4;
        c = getColorFromPixel(bimPreferredSprite.getRGB(x, y));
        if (c.getAlpha() != 0) { return bimPreferredSprite; }

        return bimDefaultSprite;
    }

    private static void assembleAvatars() {
        avatars = new HashMap<>();
        int counter = 0;
        Color pxTest;

        if (bimPreferredAvatars != null) {
            final int pw = bimPreferredAvatars.getWidth();
            final int ph = bimPreferredAvatars.getHeight();

            for (int j = 0; j < ph; j += 100) {
                for (int i = 0; i < pw; i += 100) {
                    if (i == 0 && j == 0) { continue; }
                    pxTest = getColorFromPixel(bimPreferredAvatars.getRGB(i + 50, j + 50));
                    if (pxTest.getAlpha() == 0) { continue; }
                    avatars.put(counter++, new SkinImage(bimPreferredAvatars.getSubimage(i, j, 100, 100)));
                }
            }
        } else {

            final int aw = bimDefaultAvatars.getWidth();
            final int ah = bimDefaultAvatars.getHeight();

            for (int j = 0; j < ah; j += 100) {
                for (int i = 0; i < aw; i += 100) {
                    if (i == 0 && j == 0) {
                        continue;
                    }
                    pxTest = getColorFromPixel(bimDefaultAvatars.getRGB(i + 50, j + 50));
                    if (pxTest.getAlpha() == 0) {
                        continue;
                    }
                    avatars.put(counter++, new SkinImage(bimDefaultAvatars.getSubimage(i, j, 100, 100)));
                }
            }
        }
    }

    private static void assembleSleeves() {
        sleeves = new HashMap<>();
        int counter = 0;
        Color pxTest;

        final int pw = bimDefaultSleeve.getWidth();
        final int ph = bimDefaultSleeve.getHeight();

        for (int j = 0; j < ph; j += 500) {
            for (int i = 0; i < pw; i += 360) {
                pxTest = getColorFromPixel(bimDefaultSleeve.getRGB(i + 180, j + 250));
                if (pxTest.getAlpha() == 0) { continue; }
                sleeves.put(counter++, new SkinImage(bimDefaultSleeve.getSubimage(i, j, 360, 500)));
            }
        }
        //2nd set
        final int aw = bimDefaultSleeve2.getWidth();
        final int ah = bimDefaultSleeve2.getHeight();

        for (int j = 0; j < ah; j += 500) {
            for (int i = 0; i < aw; i += 360) {
                pxTest = getColorFromPixel(bimDefaultSleeve2.getRGB(i + 180, j + 250));
                if (pxTest.getAlpha() == 0) { continue; }
                sleeves.put(counter++, new SkinImage(bimDefaultSleeve2.getSubimage(i, j, 360, 500)));
            }
        }
    }

    private static void setImage(final FSkinProp s0, final BufferedImage bim) {
        int[] tempCoords = s0.getCoords();
        x0 = tempCoords[0];
        y0 = tempCoords[1];
        w0 = tempCoords[2];
        h0 = tempCoords[3];

        if (bim != null)
            SkinImage.setImage(s0, bim.getSubimage(x0, y0, w0, h0));
    }

    /**
     * Sets the look and feel of the GUI based on the selected Forge theme.
     *
     * @see <a href="http://tips4java.wordpress.com/2008/10/09/uimanager-defaults/">UIManager Defaults</a>
     */
    private static class ForgeLookAndFeel { //needs to live in FSkin for access to skin colors
        private static boolean onInit = true;
        private static boolean isMetalLafSet = false;

        private final Color FORE_COLOR = getColor(Colors.CLR_TEXT).color;
        private final Color BACK_COLOR = getColor(Colors.CLR_THEME2).color;
        private final Color HIGHLIGHT_COLOR = BACK_COLOR.brighter();
        private final Border LINE_BORDER = BorderFactory.createLineBorder(FORE_COLOR.darker(), 1);
        private final Border EMPTY_BORDER = BorderFactory.createEmptyBorder(2, 2, 2, 2);

        /**
         * Sets the look and feel of the GUI based on the selected Forge theme.
         */
        private void setForgeLookAndFeel(final JFrame appFrame) {
            if (setMetalLookAndFeel(appFrame)) {
                setMenusLookAndFeel();
                setComboBoxLookAndFeel();
                setTabbedPaneLookAndFeel();
                setButtonLookAndFeel();
                setToolTipLookAndFeel();
                setTextEditLookAndFeel();
            }
            onInit = false;
        }

        /**
         * Sets the standard "Java L&F" (also called "Metal") that looks the same on all platforms.
         * <p>
         * If not explicitly set then the Mac uses its native L&F which does
         * not support various settings (eg. combobox background color).
         */
        private boolean setMetalLookAndFeel(final JFrame appFrame) {
            if (onInit) { //only attempt to set Metal Look and Feel the first time
                try {
                    UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
                    SwingUtilities.updateComponentTreeUI(appFrame);
                    isMetalLafSet = true;
                } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
                    // Auto-generated catch block ignores the exception, but sends it to System.err and probably forge.log.
                    e.printStackTrace();
                }
            }
            return isMetalLafSet;
        }

        /**
         * Sets the look and feel for a JMenuBar, JMenu, JMenuItem & variations.
         */
        private void setMenusLookAndFeel() {
            // JMenuBar
            final Color clrTheme = getColor(Colors.CLR_THEME).color;
            final Color backgroundColor = stepColor(clrTheme, 0);
            final Color menuBarEdgeColor = stepColor(clrTheme, -80);
            UIManager.put("MenuBar.foreground", FORE_COLOR);
            UIManager.put("MenuBar.gradient", getColorGradients(backgroundColor.darker(), backgroundColor));
            UIManager.put("MenuBar.border", BorderFactory.createMatteBorder(0, 0, 1, 0, menuBarEdgeColor));
            // JMenu
            UIManager.put("Menu.foreground", FORE_COLOR);
            UIManager.put("Menu.background", BACK_COLOR);
            UIManager.put("Menu.borderPainted", false);
            UIManager.put("Menu.selectionBackground", HIGHLIGHT_COLOR);
            UIManager.put("Menu.selectionForeground", FORE_COLOR);
            UIManager.put("Menu.border", EMPTY_BORDER);
            UIManager.put("Menu.opaque", false);
            // JPopupMenu
            UIManager.put("PopupMenu.border", LINE_BORDER);
            UIManager.put("PopupMenu.background", BACK_COLOR);
            UIManager.put("PopupMenu.foreground", FORE_COLOR);
            // JMenuItem
            UIManager.put("MenuItem.foreground", FORE_COLOR);
            UIManager.put("MenuItem.background", BACK_COLOR);
            UIManager.put("MenuItem.border", EMPTY_BORDER);
            UIManager.put("MenuItem.selectionBackground", HIGHLIGHT_COLOR);
            UIManager.put("MenuItem.selectionForeground", FORE_COLOR);
            UIManager.put("MenuItem.acceleratorForeground", FORE_COLOR.darker());
            UIManager.put("MenuItem.opaque", true);
            // JSeparator (needs to be opaque!).
            UIManager.put("Separator.foreground", FORE_COLOR.darker());
            UIManager.put("Separator.background", BACK_COLOR);
            // JRadioButtonMenuItem
            UIManager.put("RadioButtonMenuItem.foreground", FORE_COLOR);
            UIManager.put("RadioButtonMenuItem.background", BACK_COLOR);
            UIManager.put("RadioButtonMenuItem.selectionBackground", HIGHLIGHT_COLOR);
            UIManager.put("RadioButtonMenuItem.selectionForeground", FORE_COLOR);
            UIManager.put("RadioButtonMenuItem.border", EMPTY_BORDER);
            UIManager.put("RadioButtonMenuItem.acceleratorForeground", FORE_COLOR.darker());
            // JCheckboxMenuItem
            UIManager.put("CheckBoxMenuItem.foreground", FORE_COLOR);
            UIManager.put("CheckBoxMenuItem.background", BACK_COLOR);
            UIManager.put("CheckBoxMenuItem.selectionBackground", HIGHLIGHT_COLOR);
            UIManager.put("CheckBoxMenuItem.selectionForeground", FORE_COLOR);
            UIManager.put("CheckBoxMenuItem.border", EMPTY_BORDER);
            UIManager.put("CheckBoxMenuItem.acceleratorForeground", FORE_COLOR.darker());
        }

        private void setTabbedPaneLookAndFeel() {
            UIManager.put("TabbedPane.selected", HIGHLIGHT_COLOR);
            UIManager.put("TabbedPane.contentOpaque", getColor(Colors.CLR_THEME));
            UIManager.put("TabbedPane.unselectedBackground", BACK_COLOR);
        }

        /**
         * Sets the look and feel for a <b>non-editable</b> JComboBox.
         */
        private void setComboBoxLookAndFeel() {
            UIManager.put("ComboBox.background", BACK_COLOR);
            UIManager.put("ComboBox.foreground", FORE_COLOR);
            UIManager.put("ComboBox.selectionBackground", HIGHLIGHT_COLOR);
            UIManager.put("ComboBox.selectionForeground", FORE_COLOR);
            UIManager.put("ComboBox.disabledBackground", BACK_COLOR);
            UIManager.put("ComboBox.disabledForeground", BACK_COLOR.darker());
            UIManager.put("ComboBox.font", getDefaultFont("ComboBox.font"));
            final boolean isBright = isColorBright(FORE_COLOR);
            UIManager.put("ComboBox.border", BorderFactory.createLineBorder(isBright ? FORE_COLOR.darker() : FORE_COLOR.brighter(), 1));
        }

        private void setButtonLookAndFeel() {
            UIManager.put("Button.foreground", FORE_COLOR);
            UIManager.put("Button.background", BACK_COLOR);
            UIManager.put("Button.select", HIGHLIGHT_COLOR);
            UIManager.put("Button.focus", FORE_COLOR.darker());
            UIManager.put("Button.rollover", false);
        }

        private void setToolTipLookAndFeel() {
            UIManager.put("ToolTip.background", BACK_COLOR);
            UIManager.put("ToolTip.foreground", FORE_COLOR);
            UIManager.put("ToolTip.border", LINE_BORDER);
        }

        private void setTextEditLookAndFeel() {
            // Set up correct Mac keyboard shortcuts for text editing - to use the Command key
            // rather than Control, which is what we get by default.
            if (OperatingSystem.isMac()) {
                for (String key : new String[] {"TextField.focusInputMap", "TextArea.focusInputMap"})  {
                    InputMap im = (InputMap) UIManager.get(key);
                    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.META_DOWN_MASK), DefaultEditorKit.copyAction);
                    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.META_DOWN_MASK), DefaultEditorKit.pasteAction);
                    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.META_DOWN_MASK), DefaultEditorKit.cutAction);
                    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.META_DOWN_MASK), DefaultEditorKit.selectAllAction);
                }
            }
        }

        private static Font getDefaultFont(final String component) {
            return getFont(UIManager.getFont(component).getSize()).font;
        }

        private static List<Object> getColorGradients(final Color bottom, final Color top) {
            final List<Object> gradients = new ArrayList<>();
            gradients.add(0.0);
            gradients.add(0.0);
            gradients.add(top);
            gradients.add(bottom);
            gradients.add(bottom);
            return gradients;
        }
    }

    public static class ComponentSkin<T extends Component> {
        private SkinColor foreground, background;
        private SkinFont font;
        private SkinCursor cursor;
        private int appliedSkinIndex = currentSkinIndex;

        protected ComponentSkin() {
        }

        protected boolean update(final T comp) {
            if (appliedSkinIndex == currentSkinIndex) { return false; }
            appliedSkinIndex = currentSkinIndex;
            reapply(comp);
            return true;
        }

        public SkinColor getForeground() { return this.foreground; }
        protected void setForeground(final T comp, final SkinColor skinColor) { comp.setForeground(skinColor != null ? skinColor.color : null); this.foreground = skinColor; }
        protected void resetForeground() { this.foreground = null; }

        public SkinColor getBackground() { return this.background; }
        protected void setBackground(final T comp, final SkinColor skinColor) { comp.setBackground(skinColor != null ? skinColor.color : null); this.background = skinColor; }
        protected void resetBackground() { this.background = null; }

        public SkinFont getFont() { return this.font; }
        protected void setFont(final T comp, final SkinFont skinFont) { comp.setFont(skinFont != null ? skinFont.font : null); this.font = skinFont; }
        protected void resetFont() { this.font = null; }

        protected void setCursor(final T comp, final SkinCursor skinCursor) { comp.setCursor(skinCursor != null ? skinCursor.cursor : null); this.cursor = skinCursor; }
        protected void resetCursor() { this.cursor = null; }

        protected void reapply(final T comp) {
            if (this.foreground != null) { setForeground(comp, this.foreground); }
            if (this.background != null) { setBackground(comp, this.background); }
            if (this.font != null) { setFont(comp, this.font); }
            if (this.cursor != null) { setCursor(comp, this.cursor); }
        }
    }
    public static class WindowSkin<T extends Window> extends ComponentSkin<T> {
        private SkinImage iconImage;

        protected WindowSkin() {
        }

        protected void setIconImage(final T comp, final SkinImage skinImage) { comp.setIconImage(skinImage != null ? skinImage.image : null); this.iconImage = skinImage; }
        protected void resetIconImage() { this.iconImage = null; }

        @Override
        protected void reapply(final T comp) {
            if (this.iconImage != null) { setIconImage(comp, this.iconImage); }
            super.reapply(comp);
        }
    }
    public static class JComponentSkin<T extends JComponent> extends ComponentSkin<T> {
        private SkinBorder border;

        protected JComponentSkin() {
        }

        protected void setBorder(final T comp, final SkinBorder skinBorder) { comp.setBorder(skinBorder != null ? skinBorder.createBorder() : null); this.border = skinBorder; }
        protected void resetBorder() { this.border = null; }

        @Override
        protected void reapply(final T comp) {
            if (this.border != null) { setBorder(comp, this.border); }
            super.reapply(comp);
        }
    }
    public static class JLabelSkin<T extends JLabel> extends JComponentSkin<T> {
        private SkinImage icon;

        protected JLabelSkin() {
        }

        protected void setIcon(final T comp, final SkinImage skinImage) { comp.setIcon(skinImage != null ? skinImage.getIcon() : null); this.icon = skinImage; }
        protected void resetIcon() { this.icon = null; }

        @Override
        protected void reapply(final T comp) {
            if (this.icon != null) { setIcon(comp, this.icon); }
            super.reapply(comp);
        }
    }
    public static class AbstractButtonSkin<T extends AbstractButton> extends JComponentSkin<T> {
        private SkinImage icon, pressedIcon, rolloverIcon;

        protected AbstractButtonSkin() {
        }

        protected void setIcon(final T comp, final SkinImage skinImage) { comp.setIcon(skinImage != null ? skinImage.getIcon() : null); this.icon = skinImage; }
        protected void resetIcon() { this.icon = null; }

        protected void setPressedIcon(final T comp, final SkinImage skinImage) { comp.setPressedIcon(skinImage != null ? skinImage.getIcon() : null); this.pressedIcon = skinImage; }
        protected void resetPressedIcon() { this.pressedIcon = null; }

        protected void setRolloverIcon(final T comp, final SkinImage skinImage) { comp.setRolloverIcon(skinImage != null ? skinImage.getIcon() : null); this.rolloverIcon = skinImage; }
        protected void resetRolloverIcon() { this.rolloverIcon = null; }

        @Override
        protected void reapply(final T comp) {
            if (this.icon != null) { setIcon(comp, this.icon); }
            if (this.pressedIcon != null) { setPressedIcon(comp, this.pressedIcon); }
            if (this.rolloverIcon != null) { setRolloverIcon(comp, this.rolloverIcon); }
            super.reapply(comp);
        }
    }
    public static class JTextComponentSkin<T extends JTextComponent> extends JComponentSkin<T> {
        private SkinColor caretColor;

        protected JTextComponentSkin() {
        }

        protected void setCaretColor(final T comp, final SkinColor skinColor) { comp.setCaretColor(skinColor != null ? skinColor.color : null); this.caretColor = skinColor; }
        protected void resetCaretColor() { this.caretColor = null; }

        @Override
        protected void reapply(final T comp) {
            if (this.caretColor != null) { setCaretColor(comp, this.caretColor); }
            super.reapply(comp);
        }
    }
    public static class JTableSkin<T extends JTable> extends JComponentSkin<T> {
        private SkinColor selectionForeground, selectionBackground, gridColor;

        protected JTableSkin() {
        }

        protected void setSelectionForeground(final T comp, final SkinColor skinColor) { comp.setSelectionForeground(skinColor != null ? skinColor.color : null); this.selectionForeground = skinColor; }
        protected void resetSelectionForeground() { this.selectionForeground = null; }

        protected void setSelectionBackground(final T comp, final SkinColor skinColor) { comp.setSelectionBackground(skinColor != null ? skinColor.color : null); this.selectionBackground = skinColor; }
        protected void resetSelectionBackground() { this.selectionBackground = null; }

        protected void setGridColor(final T comp, final SkinColor skinColor) { comp.setGridColor(skinColor != null ? skinColor.color : null); this.gridColor = skinColor; }
        protected void resetGridColor() { this.gridColor = null; }

        @Override
        protected void reapply(final T comp) {
            if (this.selectionForeground != null) { setSelectionForeground(comp, this.selectionForeground); }
            if (this.selectionBackground != null) { setSelectionBackground(comp, this.selectionBackground); }
            if (this.gridColor != null) { setGridColor(comp, this.gridColor); }
            super.reapply(comp);
        }
    }
    public static class JSkinSkin<T extends JList<?>> extends JComponentSkin<T> {
        private SkinColor selectionForeground, selectionBackground;

        protected JSkinSkin() {
        }

        protected void setSelectionForeground(final T comp, final SkinColor skinColor) { comp.setSelectionForeground(skinColor != null ? skinColor.color : null); this.selectionForeground = skinColor; }
        protected void resetSelectionForeground() { this.selectionForeground = null; }

        protected void setSelectionBackground(final T comp, final SkinColor skinColor) { comp.setSelectionBackground(skinColor != null ? skinColor.color : null); this.selectionBackground = skinColor; }
        protected void resetSelectionBackground() { this.selectionBackground = null; }

        @Override
        protected void reapply(final T comp) {
            if (this.selectionForeground != null) { setSelectionForeground(comp, this.selectionForeground); }
            if (this.selectionBackground != null) { setSelectionBackground(comp, this.selectionBackground); }
            super.reapply(comp);
        }
    }

    public interface ISkinnedComponent<T extends Component> {
        ComponentSkin<T> getSkin();
    }

    public static class SkinnedFrame extends JFrame implements ISkinnedComponent<JFrame> {
        private static final long serialVersionUID = -7737786252990479019L;

        private SkinBorder border;

        private WindowSkin<JFrame> skin;
        @Override
        public WindowSkin<JFrame> getSkin() {
            if (skin == null) { skin = new WindowSkin<>(); }
            return skin;
        }

        public SkinnedFrame() { super(); }

        public void setForeground(final SkinColor skinColor) { getSkin().setForeground(this, skinColor); }
        @Override public void setForeground(final Color color) { getSkin().resetForeground(); super.setForeground(color); }

        public void setBackground(final SkinColor skinColor) { getSkin().setBackground(this, skinColor); }
        @Override public void setBackground(final Color color) { getSkin().resetBackground(); super.setBackground(color); }

        public void setFont(final SkinFont skinFont) { getSkin().setFont(this, skinFont); }
        @Override public void setFont(final Font font) { getSkin().resetFont(); super.setFont(font); }

        public void setCursor(final SkinCursor skinCursor) { getSkin().setCursor(this, skinCursor); }
        @Override public void setCursor(final Cursor cursor) { getSkin().resetCursor(); super.setCursor(cursor); }

        public void setIconImage(final SkinImage skinImage) { getSkin().setIconImage(this, skinImage); }
        @Override public void setIconImage(final Image image) { getSkin().resetIconImage(); super.setIconImage(image); }

        //relay border to root pane
        public void setBorder(final SkinBorder skinBorder) { getRootPane().setBorder(skinBorder != null ? skinBorder.createBorder() : null); this.border = skinBorder; }
        public void setBorder(final Border border) { getRootPane().setBorder(border); this.border = null; }

        @Override
        public void paint(final Graphics g) {
            if (getSkin().update(this)) {
                if (this.border != null) { setBorder(this.border); }
            }
            super.paint(g);
        }
    }
    public static class SkinnedDialog extends JDialog implements ISkinnedComponent<JDialog> {
        private static final long serialVersionUID = -1086360770925335844L;

        private SkinBorder border;

        private WindowSkin<JDialog> skin;
        @Override
        public WindowSkin<JDialog> getSkin() {
            if (skin == null) { skin = new WindowSkin<>(); }
            return skin;
        }

        public SkinnedDialog() { super(); }
        public SkinnedDialog(final Frame owner, final boolean modal) { super(owner, modal); }

        public void setForeground(final SkinColor skinColor) { getSkin().setForeground(this, skinColor); }
        @Override public void setForeground(final Color color) { getSkin().resetForeground(); super.setForeground(color); }

        public void setBackground(final SkinColor skinColor) { getSkin().setBackground(this, skinColor); }
        @Override public void setBackground(final Color color) { getSkin().resetBackground(); super.setBackground(color); }

        public void setFont(final SkinFont skinFont) { getSkin().setFont(this, skinFont); }
        @Override public void setFont(final Font font) { getSkin().resetFont(); super.setFont(font); }

        public void setCursor(final SkinCursor skinCursor) { getSkin().setCursor(this, skinCursor); }
        @Override public void setCursor(final Cursor cursor) { getSkin().resetCursor(); super.setCursor(cursor); }

        public void setIconImage(final SkinImage skinImage) { getSkin().setIconImage(this, skinImage); }
        @Override public void setIconImage(final Image image) { getSkin().resetIconImage(); super.setIconImage(image); }

        //relay border to root pane
        public void setBorder(final SkinBorder skinBorder) { getRootPane().setBorder(skinBorder != null ? skinBorder.createBorder() : null); this.border = skinBorder; }
        public void setBorder(final Border border) { getRootPane().setBorder(border); this.border = null; }

        @Override
        public void paint(final Graphics g) {
            if (getSkin().update(this)) {
                if (this.border != null) { setBorder(this.border); }
            }
            super.paint(g);
        }
    }
    public static class SkinnedLayeredPane extends JLayeredPane implements ISkinnedComponent<JLayeredPane> {
        private static final long serialVersionUID = -8325505112790327931L;

        private JComponentSkin<JLayeredPane> skin;
        @Override
        public JComponentSkin<JLayeredPane> getSkin() {
            if (skin == null) { skin = new JComponentSkin<>(); }
            return skin;
        }

        public SkinnedLayeredPane() { super(); }

        public void setCursor(final SkinCursor skinCursor) { getSkin().setCursor(this, skinCursor); }
        @Override public void setCursor(final Cursor cursor) { getSkin().resetCursor(); super.setCursor(cursor); }

        @Override
        protected void paintComponent(final Graphics g) {
            getSkin().update(this);
            super.paintComponent(g);
        }
    }
    public static class SkinnedMenuBar extends JMenuBar implements ISkinnedComponent<JMenuBar> {
        private static final long serialVersionUID = -183434586261989294L;

        private JComponentSkin<JMenuBar> skin;
        @Override
        public JComponentSkin<JMenuBar> getSkin() {
            if (skin == null) {skin = new JComponentSkin<>(); }
            return skin;
        }

        public SkinnedMenuBar() { super(); }

        public void setForeground(final SkinColor skinColor) { getSkin().setForeground(this, skinColor); }
        @Override public void setForeground(final Color color) { getSkin().resetForeground(); super.setForeground(color); }

        public void setBackground(final SkinColor skinColor) { getSkin().setBackground(this, skinColor); }
        @Override public void setBackground(final Color color) { getSkin().resetBackground(); super.setBackground(color); }

        public void setFont(final SkinFont skinFont) { getSkin().setFont(this, skinFont); }
        @Override public void setFont(final Font font) { getSkin().resetFont(); super.setFont(font); }

        public void setCursor(final SkinCursor skinCursor) { getSkin().setCursor(this, skinCursor); }
        @Override public void setCursor(final Cursor cursor) { getSkin().resetCursor(); super.setCursor(cursor); }

        public void setBorder(final SkinBorder skinBorder) { getSkin().setBorder(this, skinBorder); }
        @Override public void setBorder(final Border border) { getSkin().resetBorder(); super.setBorder(border); }

        @Override
        protected void paintComponent(final Graphics g) {
            getSkin().update(this);
            super.paintComponent(g);
        }
    }
    public static class SkinnedLabel extends JLabel implements ISkinnedComponent<JLabel> {
        private static final long serialVersionUID = 7046941724535782054L;

        private JLabelSkin<JLabel> skin;
        @Override
        public JLabelSkin<JLabel> getSkin() {
            if (skin == null) { skin = new JLabelSkin<>(); }
            return skin;
        }

        public SkinnedLabel() { super(); }
        public SkinnedLabel(final String text) { super(text); }

        public void setForeground(final SkinColor skinColor) { getSkin().setForeground(this, skinColor); }
        @Override public void setForeground(final Color color) { getSkin().resetForeground(); super.setForeground(color); }

        public void setBackground(final SkinColor skinColor) { getSkin().setBackground(this, skinColor); }
        @Override public void setBackground(final Color color) { getSkin().resetBackground(); super.setBackground(color); }

        public void setFont(final SkinFont skinFont) { getSkin().setFont(this, skinFont); }
        @Override public void setFont(final Font font) { getSkin().resetFont(); super.setFont(font); }

        public void setCursor(final SkinCursor skinCursor) { getSkin().setCursor(this, skinCursor); }
        @Override public void setCursor(final Cursor cursor) { getSkin().resetCursor(); super.setCursor(cursor); }

        public void setBorder(final SkinBorder skinBorder) { getSkin().setBorder(this, skinBorder); }
        @Override public void setBorder(final Border border) { getSkin().resetBorder(); super.setBorder(border); }

        public void setIcon(final SkinImage skinImage) { getSkin().setIcon(this, skinImage); }
        @Override public void setIcon(final Icon icon) { getSkin().resetIcon(); super.setIcon(icon); }

        @Override
        protected void paintComponent(final Graphics g) {
            getSkin().update(this);
            super.paintComponent(g);
        }
    }
    public static class SkinnedComboBox<E> extends JComboBox<E> implements ISkinnedComponent<JComboBox<E>> {
        private static final long serialVersionUID = 9032839876990765149L;

        private JComponentSkin<JComboBox<E>> skin;
        @Override
        public JComponentSkin<JComboBox<E>> getSkin() {
            if (skin == null) { skin = new JComponentSkin<>(); }
            return skin;
        }

        public SkinnedComboBox() { super(); }
        public SkinnedComboBox(final ComboBoxModel<E> model0) { super(model0); }
        public SkinnedComboBox(final E[] items) { super(items); }
        public SkinnedComboBox(final Vector<E> items) { super(items); }

        public void setForeground(final SkinColor skinColor) { getSkin().setForeground(this, skinColor); }
        @Override public void setForeground(final Color color) { getSkin().resetForeground(); super.setForeground(color); }

        public void setBackground(final SkinColor skinColor) { getSkin().setBackground(this, skinColor); }
        @Override public void setBackground(final Color color) { getSkin().resetBackground(); super.setBackground(color); }

        public void setFont(final SkinFont skinFont) { getSkin().setFont(this, skinFont); }
        @Override public void setFont(final Font font) { getSkin().resetFont(); super.setFont(font); }

        public void setCursor(final SkinCursor skinCursor) { getSkin().setCursor(this, skinCursor); }
        @Override public void setCursor(final Cursor cursor) { getSkin().resetCursor(); super.setCursor(cursor); }

        public void setBorder(final SkinBorder skinBorder) { getSkin().setBorder(this, skinBorder); }
        @Override public void setBorder(final Border border) { getSkin().resetBorder(); super.setBorder(border); }

        @Override
        protected void paintComponent(final Graphics g) {
            getSkin().update(this);
            super.paintComponent(g);
        }
    }
    public static class SkinnedList<E> extends JList<E> implements ISkinnedComponent<JList<E>> {
        private static final long serialVersionUID = -2449981390420167627L;

        private JSkinSkin<JList<E>> skin;
        @Override
        public JSkinSkin<JList<E>> getSkin() {
            if (skin == null) { skin = new JSkinSkin<>(); }
            return skin;
        }

        public SkinnedList() { super(); }
        public SkinnedList(final ListModel<E> model0) { super(model0); }
        public SkinnedList(final E[] items) { super(items); }

        public void setForeground(final SkinColor skinColor) { getSkin().setForeground(this, skinColor); }
        @Override public void setForeground(final Color color) { getSkin().resetForeground(); super.setForeground(color); }

        public void setBackground(final SkinColor skinColor) { getSkin().setBackground(this, skinColor); }
        @Override public void setBackground(final Color color) { getSkin().resetBackground(); super.setBackground(color); }

        public void setFont(final SkinFont skinFont) { getSkin().setFont(this, skinFont); }
        @Override public void setFont(final Font font) { getSkin().resetFont(); super.setFont(font); }

        public void setCursor(final SkinCursor skinCursor) { getSkin().setCursor(this, skinCursor); }
        @Override public void setCursor(final Cursor cursor) { getSkin().resetCursor(); super.setCursor(cursor); }

        public void setBorder(final SkinBorder skinBorder) { getSkin().setBorder(this, skinBorder); }
        @Override public void setBorder(final Border border) { getSkin().resetBorder(); super.setBorder(border); }

        public void setSelectionForeground(final SkinColor skinColor) { getSkin().setSelectionForeground(this, skinColor); }
        @Override public void setSelectionForeground(final Color color) { getSkin().resetSelectionForeground(); super.setSelectionForeground(color); }

        public void setSelectionBackground(final SkinColor skinColor) { getSkin().setSelectionBackground(this, skinColor); }
        @Override public void setSelectionBackground(final Color color) { getSkin().resetSelectionBackground(); super.setSelectionBackground(color); }

        @Override
        protected void paintComponent(final Graphics g) {
            getSkin().update(this);
            super.paintComponent(g);
        }
    }
    public static class SkinnedPanel extends JPanel implements ISkinnedComponent<JPanel> {
        private static final long serialVersionUID = -1842620489613307379L;

        private JComponentSkin<JPanel> skin;
        @Override
        public JComponentSkin<JPanel> getSkin() {
            if (skin == null) { skin = new JComponentSkin<>(); }
            return skin;
        }

        public SkinnedPanel() { super(); }
        public SkinnedPanel(final LayoutManager layoutManager) { super(layoutManager); }

        public void setForeground(final SkinColor skinColor) { getSkin().setForeground(this, skinColor); }
        @Override public void setForeground(final Color color) { getSkin().resetForeground(); super.setForeground(color); }

        public void setBackground(final SkinColor skinColor) { getSkin().setBackground(this, skinColor); }
        @Override public void setBackground(final Color color) { getSkin().resetBackground(); super.setBackground(color); }

        public void setFont(final SkinFont skinFont) { getSkin().setFont(this, skinFont); }
        @Override public void setFont(final Font font) { getSkin().resetFont(); super.setFont(font); }

        public void setCursor(final SkinCursor skinCursor) { getSkin().setCursor(this, skinCursor); }
        @Override public void setCursor(final Cursor cursor) { getSkin().resetCursor(); super.setCursor(cursor); }

        public void setBorder(final SkinBorder skinBorder) { getSkin().setBorder(this, skinBorder); }
        @Override public void setBorder(final Border border) { getSkin().resetBorder(); super.setBorder(border); }

        @Override
        protected void paintComponent(final Graphics g) {
            getSkin().update(this);
            super.paintComponent(g);
        }
    }
    public static abstract class FPanelBase extends SkinnedPanel {
        private static final long serialVersionUID = -7223626737375474132L;

        private SkinImage foregroundImage, backgroundTexture;
        private SkinColor backgroundTextureOverlay;

        protected FPanelBase() { super(); }
        public FPanelBase(final LayoutManager layoutManager) { super(layoutManager); }

        protected abstract void onSetForegroundImage(final Image image, boolean stretch);
        public final void setForegroundImage(final SkinImage skinImage, final boolean stretch) { onSetForegroundImage(skinImage.image, stretch); this.foregroundImage = skinImage; }
        public final void setForegroundImage(final SkinImage skinImage) { onSetForegroundImage(skinImage.image, false); this.foregroundImage = skinImage; }
        public final void setForegroundImage(final Image image) { onSetForegroundImage(image, false); this.foregroundImage = null; }
        public final void setForegroundImage(final ImageIcon imageIcon) { onSetForegroundImage(imageIcon.getImage(), false); this.foregroundImage = null; }

        protected abstract void onSetBackgroundTexture(final Image image);
        public final void setBackgroundTexture(final SkinImage skinImage) { onSetBackgroundTexture(skinImage.image); this.backgroundTexture = skinImage; }
        public final void setBackgroundTexture(final Image image) { onSetBackgroundTexture(image); this.backgroundTexture = null; }
        public final void setBackgroundTexture(final ImageIcon imageIcon) { onSetBackgroundTexture(imageIcon.getImage()); this.backgroundTexture = null; }

        protected abstract void onSetBackgroundTextureOverlay(final Color color);
        public final void setBackgroundTextureOverlay(final SkinColor skinColor) { onSetBackgroundTextureOverlay(skinColor.color); this.backgroundTextureOverlay = skinColor; }
        public final void setBackgroundTextureOverlay(final Color color) { onSetBackgroundTextureOverlay(color); this.backgroundTextureOverlay = null; }

        @Override
        protected void paintComponent(final Graphics g) {
            if (getSkin().update(this)) {
                if (this.foregroundImage != null) { this.setForegroundImage(this.foregroundImage); }
                if (this.backgroundTexture != null) { this.setBackgroundTexture(this.backgroundTexture); }
                if (this.backgroundTextureOverlay != null) { this.setBackgroundTextureOverlay(this.backgroundTextureOverlay); }
            }
            super.paintComponent(g);
        }
    }
    public static class SkinnedScrollPane extends JScrollPane implements ISkinnedComponent<JScrollPane> {
        private static final long serialVersionUID = 8958616297664604107L;

        private JComponentSkin<JScrollPane> skin;
        @Override
        public JComponentSkin<JScrollPane> getSkin() {
            if (skin == null) { skin = new JComponentSkin<>(); }
            return skin;
        }

        public SkinnedScrollPane() { super(); init(); }
        public SkinnedScrollPane(final Component comp) { super(comp); init(); }
        public SkinnedScrollPane(final int vsbPolicy, final int hsbPolicy) { super(vsbPolicy, hsbPolicy); init(); }
        public SkinnedScrollPane(final Component comp, final int vsbPolicy, final int hsbPolicy) { super(comp, vsbPolicy, hsbPolicy); init(); }

        private void init() {
            new SkinScrollBarUI(getVerticalScrollBar(), true);
            new SkinScrollBarUI(getHorizontalScrollBar(), false);
        }
        private static class SkinScrollBarUI extends BasicScrollBarUI implements ILocalRepaint {
            @SuppressWarnings("serial")
            private static class HiddenButton extends JButton {
                @Override
                public Dimension getPreferredSize() {
                    return new Dimension(0, 0);
                }
            };

            private static final SkinColor backColor = getColor(Colors.CLR_THEME2);
            private static final SkinColor borderColor = getColor(Colors.CLR_TEXT);
            private static final SkinColor grooveColor = borderColor.alphaColor(200);
            private static final AlphaComposite alphaDim = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f);
            private static final int grooveSpace = 3;

            private final boolean vertical;
            private boolean hovered;
            private MouseAdapter hoverListener;

            private static class SkinScrollBarListener extends MouseAdapter {
                private SkinScrollBarUI barUI;

                private SkinScrollBarListener(SkinScrollBarUI barUI) {
                    this.barUI = barUI;
                }

                @Override
                public void mouseEntered(final MouseEvent e) {
                    barUI.hovered = true;
                    barUI.repaintSelf();
                }

                @Override
                public void mouseExited(final MouseEvent e) {
                    barUI.hovered = false;
                    barUI.repaintSelf();
                }
            }

            private SkinScrollBarUI(final JScrollBar scrollbar, final boolean vertical0) {
                vertical = vertical0;

                hoverListener = new SkinScrollBarListener(this);
                scrollbar.setOpaque(false);
                scrollbar.addMouseListener(hoverListener);
                scrollbar.setUI(this);
            }

            protected void uninstallListeners() {
                super.uninstallListeners();
                scrollbar.removeMouseListener(hoverListener);
            }

            @Override
            public void repaintSelf() {
                final Dimension d = scrollbar.getSize();
                scrollbar.repaint(0, 0, d.width, d.height);
            }

            @Override
            protected JButton createIncreaseButton(final int orientation) {
                return new HiddenButton(); //hide increase button
            }

            @Override
            protected JButton createDecreaseButton(final int orientation) {
                return new HiddenButton(); //hide decrease button
            }

            @Override
            protected void paintTrack(final Graphics g, final JComponent c, final Rectangle trackBounds) {
                //make track transparent
            }

            @Override
            protected void paintThumb(final Graphics g, final JComponent c, final Rectangle thumbBounds) {
                int x = thumbBounds.x;
                int y = thumbBounds.y;
                int width = thumbBounds.width - 1;
                int height = thumbBounds.height - 1;

                //build polygon for thumb
                int[] xPoints, yPoints;
                if (vertical) {
                    x += 2;
                    width -= 4;
                    final int x2 = x + width / 2;
                    final int x3 = x + width;

                    int arrowThickness = width / 2;
                    int maxArrowThickness = height / 2 - grooveSpace * 2;
                    if (maxArrowThickness < 0) {
                        maxArrowThickness = 0;
                    }
                    if (arrowThickness > maxArrowThickness) {
                        arrowThickness = maxArrowThickness;
                    }
                    final int y2 = y + arrowThickness;
                    final int y3 = y + height - arrowThickness;
                    final int y4 = y + height;

                    xPoints = new int[] { x, x2, x3, x3, x2, x };
                    yPoints = new int[] { y2, y, y2, y3, y4, y3 };
                }
                else {
                    y += 2;
                    height -= 4;
                    final int y2 = y + height / 2;
                    final int y3 = y + height;

                    int arrowThickness = height / 2;
                    int maxArrowThickness = width / 2 - grooveSpace * 2;
                    if (maxArrowThickness < 0) {
                        maxArrowThickness = 0;
                    }
                    if (arrowThickness > maxArrowThickness) {
                        arrowThickness = maxArrowThickness;
                    }
                    final int x2 = x + arrowThickness;
                    final int x3 = x + width - arrowThickness;
                    final int x4 = x + width;

                    yPoints = new int[] { y, y2, y3, y3, y2, y };
                    xPoints = new int[] { x2, x, x2, x3, x4, x3 };
                }

                //draw thumb
                final Graphics2D g2d = (Graphics2D) g;
                if (!hovered) {
                    g2d.setComposite(alphaDim);
                }
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                setGraphicsColor(g2d, backColor);
                g2d.fillPolygon(xPoints, yPoints, xPoints.length);
                setGraphicsColor(g2d, borderColor);
                g2d.drawPolygon(xPoints, yPoints, xPoints.length);

                //draw grooves if needed
                if (vertical) {
                    if (height > grooveSpace * 4) {
                        setGraphicsColor(g2d, grooveColor);
                        final int x2 = x + grooveSpace;
                        final int x3 = x + width - grooveSpace;
                        final int y3 = y + height / 2;
                        final int y2 = y3 - grooveSpace;
                        final int y4 = y3 + grooveSpace;
                        g2d.drawLine(x2, y2, x3, y2);
                        g2d.drawLine(x2, y3, x3, y3);
                        g2d.drawLine(x2, y4, x3, y4);
                    }
                }
                else if (width > grooveSpace * 4) {
                    setGraphicsColor(g2d, grooveColor);
                    final int y2 = y + grooveSpace;
                    final int y3 = y + height - grooveSpace;
                    final int x3 = x + width / 2;
                    final int x2 = x3 - grooveSpace;
                    final int x4 = x3 + grooveSpace;
                    g2d.drawLine(x2, y2, x2, y3);
                    g2d.drawLine(x3, y2, x3, y3);
                    g2d.drawLine(x4, y2, x4, y3);
                }
            }
        }

        public void setForeground(final SkinColor skinColor) { getSkin().setForeground(this, skinColor); }
        @Override public void setForeground(final Color color) { getSkin().resetForeground(); super.setForeground(color); }

        public void setBackground(final SkinColor skinColor) { getSkin().setBackground(this, skinColor); }
        @Override public void setBackground(final Color color) { getSkin().resetBackground(); super.setBackground(color); }

        public void setFont(final SkinFont skinFont) { getSkin().setFont(this, skinFont); }
        @Override public void setFont(final Font font) { getSkin().resetFont(); super.setFont(font); }

        public void setCursor(final SkinCursor skinCursor) { getSkin().setCursor(this, skinCursor); }
        @Override public void setCursor(final Cursor cursor) { getSkin().resetCursor(); super.setCursor(cursor); }

        public void setBorder(final SkinBorder skinBorder) { getSkin().setBorder(this, skinBorder); }
        @Override public void setBorder(final Border border) { getSkin().resetBorder(); super.setBorder(border); }

        @Override
        protected void paintComponent(final Graphics g) {
            getSkin().update(this);
            super.paintComponent(g);
        }
    }
    public static class SkinnedTabbedPane extends JTabbedPane implements ISkinnedComponent<JTabbedPane> {
        private static final long serialVersionUID = 6069807433509074270L;

        private JComponentSkin<JTabbedPane> skin;
        @Override
        public JComponentSkin<JTabbedPane> getSkin() {
            if (skin == null) { skin = new JComponentSkin<>(); }
            return skin;
        }

        public SkinnedTabbedPane() { super(); }

        public void setForeground(final SkinColor skinColor) { getSkin().setForeground(this, skinColor); }
        @Override public void setForeground(final Color color) { getSkin().resetForeground(); super.setForeground(color); }

        public void setBackground(final SkinColor skinColor) { getSkin().setBackground(this, skinColor); }
        @Override public void setBackground(final Color color) { getSkin().resetBackground(); super.setBackground(color); }

        public void setFont(final SkinFont skinFont) { getSkin().setFont(this, skinFont); }
        @Override public void setFont(final Font font) { getSkin().resetFont(); super.setFont(font); }

        public void setCursor(final SkinCursor skinCursor) { getSkin().setCursor(this, skinCursor); }
        @Override public void setCursor(final Cursor cursor) { getSkin().resetCursor(); super.setCursor(cursor); }

        public void setBorder(final SkinBorder skinBorder) { getSkin().setBorder(this, skinBorder); }
        @Override public void setBorder(final Border border) { getSkin().resetBorder(); super.setBorder(border); }

        @Override
        protected void paintComponent(final Graphics g) {
            getSkin().update(this);
            super.paintComponent(g);
        }
    }
    public static class SkinnedButton extends JButton implements ISkinnedComponent<JButton> {
        private static final long serialVersionUID = -1868724405885582324L;

        private AbstractButtonSkin<JButton> skin;
        @Override
        public AbstractButtonSkin<JButton> getSkin() {
            if (skin == null) { skin = new AbstractButtonSkin<>(); }
            return skin;
        }

        public SkinnedButton() { super(); }
        public SkinnedButton(final String text) { super(text); }

        public void setForeground(final SkinColor skinColor) { getSkin().setForeground(this, skinColor); }
        @Override public void setForeground(final Color color) { getSkin().resetForeground(); super.setForeground(color); }

        public void setBackground(final SkinColor skinColor) { getSkin().setBackground(this, skinColor); }
        @Override public void setBackground(final Color color) { getSkin().resetBackground(); super.setBackground(color); }

        public void setFont(final SkinFont skinFont) { getSkin().setFont(this, skinFont); }
        @Override public void setFont(final Font font) { getSkin().resetFont(); super.setFont(font); }

        public void setCursor(final SkinCursor skinCursor) { getSkin().setCursor(this, skinCursor); }
        @Override public void setCursor(final Cursor cursor) { getSkin().resetCursor(); super.setCursor(cursor); }

        public void setBorder(final SkinBorder skinBorder) { getSkin().setBorder(this, skinBorder); }
        @Override public void setBorder(final Border border) { getSkin().resetBorder(); super.setBorder(border); }

        public void setIcon(final SkinImage skinImage) { getSkin().setIcon(this, skinImage); }
        @Override public void setIcon(final Icon icon) { getSkin().resetIcon(); super.setIcon(icon); }

        public void setPressedIcon(final SkinImage skinImage) { getSkin().setPressedIcon(this, skinImage); }
        @Override public void setPressedIcon(final Icon icon) { getSkin().resetPressedIcon(); super.setPressedIcon(icon); }

        public void setRolloverIcon(final SkinImage skinImage) { getSkin().setRolloverIcon(this, skinImage); }
        @Override public void setRolloverIcon(final Icon icon) { getSkin().resetRolloverIcon(); super.setRolloverIcon(icon); }

        @Override
        protected void paintComponent(final Graphics g) {
            getSkin().update(this);
            super.paintComponent(g);
        }
    }
    public static class SkinnedCheckBox extends JCheckBox implements ISkinnedComponent<JCheckBox> {
        private static final long serialVersionUID = 6283239481504889377L;

        private AbstractButtonSkin<JCheckBox> skin;
        @Override
        public AbstractButtonSkin<JCheckBox> getSkin() {
            if (skin == null) { skin = new AbstractButtonSkin<>(); }
            return skin;
        }

        public SkinnedCheckBox() { super(); }
        public SkinnedCheckBox(final String text) { super(text); }

        public void setForeground(final SkinColor skinColor) { getSkin().setForeground(this, skinColor); }
        @Override public void setForeground(final Color color) { getSkin().resetForeground(); super.setForeground(color); }

        public void setBackground(final SkinColor skinColor) { getSkin().setBackground(this, skinColor); }
        @Override public void setBackground(final Color color) { getSkin().resetBackground(); super.setBackground(color); }

        public void setFont(final SkinFont skinFont) { getSkin().setFont(this, skinFont); }
        @Override public void setFont(final Font font) { getSkin().resetFont(); super.setFont(font); }

        public void setCursor(final SkinCursor skinCursor) { getSkin().setCursor(this, skinCursor); }
        @Override public void setCursor(final Cursor cursor) { getSkin().resetCursor(); super.setCursor(cursor); }

        public void setBorder(final SkinBorder skinBorder) { getSkin().setBorder(this, skinBorder); }
        @Override public void setBorder(final Border border) { getSkin().resetBorder(); super.setBorder(border); }

        @Override
        protected void paintComponent(final Graphics g) {
            getSkin().update(this);
            super.paintComponent(g);
        }
    }
    public static class SkinnedRadioButton extends JRadioButton implements ISkinnedComponent<JRadioButton> {
        private static final long serialVersionUID = 2724598726704588129L;

        private AbstractButtonSkin<JRadioButton> skin;
        @Override
        public AbstractButtonSkin<JRadioButton> getSkin() {
            if (skin == null) { skin = new AbstractButtonSkin<>(); }
            return skin;
        }

        public SkinnedRadioButton() { super(); }
        public SkinnedRadioButton(final String text) { super(text); }

        public void setForeground(final SkinColor skinColor) { getSkin().setForeground(this, skinColor); }
        @Override public void setForeground(final Color color) { getSkin().resetForeground(); super.setForeground(color); }

        public void setBackground(final SkinColor skinColor) { getSkin().setBackground(this, skinColor); }
        @Override public void setBackground(final Color color) { getSkin().resetBackground(); super.setBackground(color); }

        public void setFont(final SkinFont skinFont) { getSkin().setFont(this, skinFont); }
        @Override public void setFont(final Font font) { getSkin().resetFont(); super.setFont(font); }

        public void setCursor(final SkinCursor skinCursor) { getSkin().setCursor(this, skinCursor); }
        @Override public void setCursor(final Cursor cursor) { getSkin().resetCursor(); super.setCursor(cursor); }

        public void setBorder(final SkinBorder skinBorder) { getSkin().setBorder(this, skinBorder); }
        @Override public void setBorder(final Border border) { getSkin().resetBorder(); super.setBorder(border); }

        @Override
        protected void paintComponent(final Graphics g) {
            getSkin().update(this);
            super.paintComponent(g);
        }
    }
    public static class SkinnedMenu extends JMenu implements ISkinnedComponent<JMenu> {
        private static final long serialVersionUID = -1067731457894672601L;

        private AbstractButtonSkin<JMenu> skin;
        @Override
        public AbstractButtonSkin<JMenu> getSkin() {
            if (skin == null) { skin = new AbstractButtonSkin<>(); }
            return skin;
        }

        public SkinnedMenu() { super(); }
        public SkinnedMenu(final String text) { super(text); }
        public SkinnedMenu(final Action a) { super(a); }

        public void setIcon(final SkinImage skinImage) { getSkin().setIcon(this, skinImage); }
        @Override public void setIcon(final Icon icon) { getSkin().resetIcon(); super.setIcon(icon); }

        @Override
        protected void paintComponent(final Graphics g) {
            getSkin().update(this);
            super.paintComponent(g);
        }
    }
    public static class SkinnedMenuItem extends JMenuItem implements ISkinnedComponent<JMenuItem> {
        private static final long serialVersionUID = 3738616219203986847L;

        private AbstractButtonSkin<JMenuItem> skin;
        @Override
        public AbstractButtonSkin<JMenuItem> getSkin() {
            if (skin == null) { skin = new AbstractButtonSkin<>(); }
            return skin;
        }

        public SkinnedMenuItem() { super(); }
        public SkinnedMenuItem(final String text) { super(text); }
        public SkinnedMenuItem(final Action a) { super(a); }

        public void setIcon(final SkinImage skinImage) { getSkin().setIcon(this, skinImage); }
        @Override public void setIcon(final Icon icon) { getSkin().resetIcon(); super.setIcon(icon); }

        @Override
        protected void paintComponent(final Graphics g) {
            getSkin().update(this);
            super.paintComponent(g);
        }
    }
    public static class SkinnedCheckBoxMenuItem extends JCheckBoxMenuItem implements ISkinnedComponent<JCheckBoxMenuItem> {
        private static final long serialVersionUID = 7972531296466954594L;

        private AbstractButtonSkin<JCheckBoxMenuItem> skin;
        @Override
        public AbstractButtonSkin<JCheckBoxMenuItem> getSkin() {
            if (skin == null) { skin = new AbstractButtonSkin<>(); }
            return skin;
        }

        public SkinnedCheckBoxMenuItem() { super(); }
        public SkinnedCheckBoxMenuItem(final String text) { super(text); }
        public SkinnedCheckBoxMenuItem(final Action a) { super(a); }

        public void setIcon(final SkinImage skinImage) { getSkin().setIcon(this, skinImage); }
        @Override public void setIcon(final Icon icon) { getSkin().resetIcon(); super.setIcon(icon); }

        @Override
        protected void paintComponent(final Graphics g) {
            getSkin().update(this);
            super.paintComponent(g);
        }
    }
    public static class SkinnedRadioButtonMenuItem extends JRadioButtonMenuItem implements ISkinnedComponent<JRadioButtonMenuItem> {
        private static final long serialVersionUID = -3609854793671399210L;

        private AbstractButtonSkin<JRadioButtonMenuItem> skin;
        @Override
        public AbstractButtonSkin<JRadioButtonMenuItem> getSkin() {
            if (skin == null) { skin = new AbstractButtonSkin<>(); }
            return skin;
        }

        public SkinnedRadioButtonMenuItem() { super(); }
        public SkinnedRadioButtonMenuItem(final String text) { super(text); }
        public SkinnedRadioButtonMenuItem(final Action a) { super(a); }

        public void setIcon(final SkinImage skinImage) { getSkin().setIcon(this, skinImage); }
        @Override public void setIcon(final Icon icon) { getSkin().resetIcon(); super.setIcon(icon); }

        @Override
        protected void paintComponent(final Graphics g) {
            getSkin().update(this);
            super.paintComponent(g);
        }
    }
    public static class SkinnedTextField extends JTextField implements ISkinnedComponent<JTextField> {
        private static final long serialVersionUID = 5133370343400427635L;

        private JTextComponentSkin<JTextField> skin;
        @Override
        public JTextComponentSkin<JTextField> getSkin() {
            if (skin == null) { skin = new JTextComponentSkin<>(); }
            return skin;
        }

        public SkinnedTextField() { super(); }
        public SkinnedTextField(final String text) { super(text); }

        public void setForeground(final SkinColor skinColor) { getSkin().setForeground(this, skinColor); }
        @Override public void setForeground(final Color color) { getSkin().resetForeground(); super.setForeground(color); }

        public void setBackground(final SkinColor skinColor) { getSkin().setBackground(this, skinColor); }
        @Override public void setBackground(final Color color) { getSkin().resetBackground(); super.setBackground(color); }

        public void setFont(final SkinFont skinFont) { getSkin().setFont(this, skinFont); }
        @Override public void setFont(final Font font) { getSkin().resetFont(); super.setFont(font); }

        public void setCursor(final SkinCursor skinCursor) { getSkin().setCursor(this, skinCursor); }
        @Override public void setCursor(final Cursor cursor) { getSkin().resetCursor(); super.setCursor(cursor); }

        public void setBorder(final SkinBorder skinBorder) { getSkin().setBorder(this, skinBorder); }
        @Override public void setBorder(final Border border) { getSkin().resetBorder(); super.setBorder(border); }

        public void setCaretColor(final SkinColor skinColor) { getSkin().setCaretColor(this, skinColor); }
        @Override public void setCaretColor(final Color color) { getSkin().resetCaretColor(); super.setCaretColor(color); }

        @Override
        protected void paintComponent(final Graphics g) {
            getSkin().update(this);
            super.paintComponent(g);
        }
    }
    public static class SkinnedPasswordField extends JPasswordField implements ISkinnedComponent<JTextField> {
        private static final long serialVersionUID = 1557674285031452868L;

        private JTextComponentSkin<JTextField> skin;
        @Override
        public JTextComponentSkin<JTextField> getSkin() {
            if (skin == null) { skin = new JTextComponentSkin<>(); }
            return skin;
        }

        public SkinnedPasswordField() { super(); }

        public void setForeground(final SkinColor skinColor) { getSkin().setForeground(this, skinColor); }
        @Override public void setForeground(final Color color) { getSkin().resetForeground(); super.setForeground(color); }

        public void setBackground(final SkinColor skinColor) { getSkin().setBackground(this, skinColor); }
        @Override public void setBackground(final Color color) { getSkin().resetBackground(); super.setBackground(color); }

        public void setFont(final SkinFont skinFont) { getSkin().setFont(this, skinFont); }
        @Override public void setFont(final Font font) { getSkin().resetFont(); super.setFont(font); }

        public void setCursor(final SkinCursor skinCursor) { getSkin().setCursor(this, skinCursor); }
        @Override public void setCursor(final Cursor cursor) { getSkin().resetCursor(); super.setCursor(cursor); }

        public void setBorder(final SkinBorder skinBorder) { getSkin().setBorder(this, skinBorder); }
        @Override public void setBorder(final Border border) { getSkin().resetBorder(); super.setBorder(border); }

        public void setCaretColor(final SkinColor skinColor) { getSkin().setCaretColor(this, skinColor); }
        @Override public void setCaretColor(final Color color) { getSkin().resetCaretColor(); super.setCaretColor(color); }

        @Override
        protected void paintComponent(final Graphics g) {
            getSkin().update(this);
            super.paintComponent(g);
        }
    }
    public static class SkinnedTextArea extends JTextArea implements ISkinnedComponent<JTextArea> {
        private static final long serialVersionUID = 4191648156716570907L;

        private JTextComponentSkin<JTextArea> skin;
        @Override
        public JTextComponentSkin<JTextArea> getSkin() {
            if (skin == null) { skin = new JTextComponentSkin<>(); }
            return skin;
        }

        public SkinnedTextArea() { super(); }
        public SkinnedTextArea(final String text) { super(text); }

        public void setForeground(final SkinColor skinColor) { getSkin().setForeground(this, skinColor); }
        @Override public void setForeground(final Color color) { getSkin().resetForeground(); super.setForeground(color); }

        public void setBackground(final SkinColor skinColor) { getSkin().setBackground(this, skinColor); }
        @Override public void setBackground(final Color color) { getSkin().resetBackground(); super.setBackground(color); }

        public void setFont(final SkinFont skinFont) { getSkin().setFont(this, skinFont); }
        @Override public void setFont(final Font font) { getSkin().resetFont(); super.setFont(font); }

        public void setCursor(final SkinCursor skinCursor) { getSkin().setCursor(this, skinCursor); }
        @Override public void setCursor(final Cursor cursor) { getSkin().resetCursor(); super.setCursor(cursor); }

        public void setBorder(final SkinBorder skinBorder) { getSkin().setBorder(this, skinBorder); }
        @Override public void setBorder(final Border border) { getSkin().resetBorder(); super.setBorder(border); }

        public void setCaretColor(final SkinColor skinColor) { getSkin().setCaretColor(this, skinColor); }
        @Override public void setCaretColor(final Color color) { getSkin().resetCaretColor(); super.setCaretColor(color); }

        @Override
        protected void paintComponent(final Graphics g) {
            getSkin().update(this);
            super.paintComponent(g);
        }
    }
    public static class SkinnedTextPane extends JTextPane implements ISkinnedComponent<JTextPane> {
        private static final long serialVersionUID = -209191600467610844L;

        private JTextComponentSkin<JTextPane> skin;
        @Override
        public JTextComponentSkin<JTextPane> getSkin() {
            if (skin == null) { skin = new JTextComponentSkin<>(); }
            return skin;
        }

        public SkinnedTextPane() { super(); }

        public void setForeground(final SkinColor skinColor) { getSkin().setForeground(this, skinColor); }
        @Override public void setForeground(final Color color) { getSkin().resetForeground(); super.setForeground(color); }

        public void setBackground(final SkinColor skinColor) { getSkin().setBackground(this, skinColor); }
        @Override public void setBackground(final Color color) { getSkin().resetBackground(); super.setBackground(color); }

        public void setFont(final SkinFont skinFont) { getSkin().setFont(this, skinFont); }
        @Override public void setFont(final Font font) { getSkin().resetFont(); super.setFont(font); }

        public void setCursor(final SkinCursor skinCursor) { getSkin().setCursor(this, skinCursor); }
        @Override public void setCursor(final Cursor cursor) { getSkin().resetCursor(); super.setCursor(cursor); }

        public void setBorder(final SkinBorder skinBorder) { getSkin().setBorder(this, skinBorder); }
        @Override public void setBorder(final Border border) { getSkin().resetBorder(); super.setBorder(border); }

        public void setCaretColor(final SkinColor skinColor) { getSkin().setCaretColor(this, skinColor); }
        @Override public void setCaretColor(final Color color) { getSkin().resetCaretColor(); super.setCaretColor(color); }

        @Override
        protected void paintComponent(final Graphics g) {
            getSkin().update(this);
            super.paintComponent(g);
        }
    }
    public static class SkinnedEditorPane extends JEditorPane implements ISkinnedComponent<JEditorPane> {
        private static final long serialVersionUID = 88434642461539322L;

        private JTextComponentSkin<JEditorPane> skin;
        @Override
        public JTextComponentSkin<JEditorPane> getSkin() {
            if (skin == null) { skin = new JTextComponentSkin<>(); }
            return skin;
        }

        public SkinnedEditorPane() { super(); }

        public void setForeground(final SkinColor skinColor) { getSkin().setForeground(this, skinColor); }
        @Override public void setForeground(final Color color) { getSkin().resetForeground(); super.setForeground(color); }

        public void setBackground(final SkinColor skinColor) { getSkin().setBackground(this, skinColor); }
        @Override public void setBackground(final Color color) { getSkin().resetBackground(); super.setBackground(color); }

        public void setFont(final SkinFont skinFont) { getSkin().setFont(this, skinFont); }
        @Override public void setFont(final Font font) { getSkin().resetFont(); super.setFont(font); }

        public void setCursor(final SkinCursor skinCursor) { getSkin().setCursor(this, skinCursor); }
        @Override public void setCursor(final Cursor cursor) { getSkin().resetCursor(); super.setCursor(cursor); }

        public void setBorder(final SkinBorder skinBorder) { getSkin().setBorder(this, skinBorder); }
        @Override public void setBorder(final Border border) { getSkin().resetBorder(); super.setBorder(border); }

        public void setCaretColor(final SkinColor skinColor) { getSkin().setCaretColor(this, skinColor); }
        @Override public void setCaretColor(final Color color) { getSkin().resetCaretColor(); super.setCaretColor(color); }

        @Override
        protected void paintComponent(final Graphics g) {
            getSkin().update(this);
            super.paintComponent(g);
        }
    }
    public static class SkinnedSpinner extends JSpinner implements ISkinnedComponent<JFormattedTextField> {
        private static final long serialVersionUID = -7379547491760852368L;

        //special case to treat as text component
        private JTextComponentSkin<JFormattedTextField> skin;
        @Override
        public JTextComponentSkin<JFormattedTextField> getSkin() {
            if (skin == null) { skin = new JTextComponentSkin<>(); }
            return skin;
        }

        private JFormattedTextField textField;
        public JFormattedTextField getTextField() { return textField; }

        public SkinnedSpinner() {
            updateTextField();
        }

        @Override
        public void setEditor(final JComponent editor) {
            super.setEditor(editor);
            updateTextField();
        }

        private void updateTextField() {
            try {
                textField = ((JSpinner.NumberEditor)this.getEditor()).getTextField();
            }
            catch (final Exception ex) {
                textField = null;
            }
        }

        public void setForeground(final SkinColor skinColor) { getSkin().setForeground(textField, skinColor); }
        @Override public void setForeground(final Color color) { if (textField == null) { super.setForeground(color); return; } getSkin().resetForeground(); textField.setForeground(color); }

        public void setBackground(final SkinColor skinColor) { getSkin().setBackground(textField, skinColor); }
        @Override public void setBackground(final Color color) { if (textField == null) { super.setBackground(color); return; } getSkin().resetBackground(); textField.setBackground(color); }

        public void setFont(final SkinFont skinFont) { getSkin().setFont(textField, skinFont); }
        @Override public void setFont(final Font font) { if (textField == null) { super.setFont(font); return; } getSkin().resetFont(); textField.setFont(font); }

        public void setCursor(final SkinCursor skinCursor) { getSkin().setCursor(textField, skinCursor); }
        @Override public void setCursor(final Cursor cursor) { if (textField == null) { super.setCursor(cursor); return; } getSkin().resetCursor(); textField.setCursor(cursor); }

        public void setCaretColor(final SkinColor skinColor) { getSkin().setCaretColor(textField, skinColor); }
        public void setCaretColor(final Color color) { if (textField == null) { return; } getSkin().resetCaretColor(); textField.setCaretColor(color); }

        @Override
        protected void paintComponent(final Graphics g) {
            getSkin().update(textField);
            super.paintComponent(g);
        }
    }
    public static class SkinnedSlider extends JSlider implements ISkinnedComponent<JSlider> {
        private static final long serialVersionUID = -7846549500200072420L;

        private JComponentSkin<JSlider> skin;
        @Override
        public JComponentSkin<JSlider> getSkin() {
            if (skin == null) { skin = new JComponentSkin<>(); }
            return skin;
        }

        public SkinnedSlider() { super(); }
        public SkinnedSlider(final int orientation) { super(orientation); }
        public SkinnedSlider(final int min, final int max) { super(min, max); }
        public SkinnedSlider(final int min, final int max, final int value) { super(min, max, value); }
        public SkinnedSlider(final int orientation, final int min, final int max, final int value) { super(orientation, min, max, value); }

        public void setForeground(final SkinColor skinColor) { getSkin().setForeground(this, skinColor); }
        @Override public void setForeground(final Color color) { getSkin().resetForeground(); super.setForeground(color); }

        public void setBackground(final SkinColor skinColor) { getSkin().setBackground(this, skinColor); }
        @Override public void setBackground(final Color color) { getSkin().resetBackground(); super.setBackground(color); }

        public void setFont(final SkinFont skinFont) { getSkin().setFont(this, skinFont); }
        @Override public void setFont(final Font font) { getSkin().resetFont(); super.setFont(font); }

        public void setCursor(final SkinCursor skinCursor) { getSkin().setCursor(this, skinCursor); }
        @Override public void setCursor(final Cursor cursor) { getSkin().resetCursor(); super.setCursor(cursor); }

        public void setBorder(final SkinBorder skinBorder) { getSkin().setBorder(this, skinBorder); }
        @Override public void setBorder(final Border border) { getSkin().resetBorder(); super.setBorder(border); }

        @Override
        protected void paintComponent(final Graphics g) {
            getSkin().update(this);
            super.paintComponent(g);
        }
    }
    public static class SkinnedTable extends JTable implements ISkinnedComponent<JTable> {
        private static final long serialVersionUID = -4194423897092773473L;

        private JTableSkin<JTable> skin;
        @Override
        public JTableSkin<JTable> getSkin() {
            if (skin == null) { skin = new JTableSkin<>(); }
            return skin;
        }

        public SkinnedTable() { super(); }

        public void setForeground(final SkinColor skinColor) { getSkin().setForeground(this, skinColor); }
        @Override public void setForeground(final Color color) { getSkin().resetForeground(); super.setForeground(color); }

        public void setBackground(final SkinColor skinColor) { getSkin().setBackground(this, skinColor); }
        @Override public void setBackground(final Color color) { getSkin().resetBackground(); super.setBackground(color); }

        public void setFont(final SkinFont skinFont) { getSkin().setFont(this, skinFont); }
        @Override public void setFont(final Font font) { getSkin().resetFont(); super.setFont(font); }

        public void setCursor(final SkinCursor skinCursor) { getSkin().setCursor(this, skinCursor); }
        @Override public void setCursor(final Cursor cursor) { getSkin().resetCursor(); super.setCursor(cursor); }

        public void setBorder(final SkinBorder skinBorder) { getSkin().setBorder(this, skinBorder); }
        @Override public void setBorder(final Border border) { getSkin().resetBorder(); super.setBorder(border); }

        public void setSelectionForeground(final SkinColor skinColor) { getSkin().setSelectionForeground(this, skinColor); }
        @Override public void setSelectionForeground(final Color color) { getSkin().resetSelectionForeground(); super.setSelectionForeground(color); }

        public void setSelectionBackground(final SkinColor skinColor) { getSkin().setSelectionBackground(this, skinColor); }
        @Override public void setSelectionBackground(final Color color) { getSkin().resetSelectionBackground(); super.setSelectionBackground(color); }

        public void setGridColor(final SkinColor skinColor) { getSkin().setGridColor(this, skinColor); }
        @Override public void setGridColor(final Color color) { getSkin().resetGridColor(); super.setGridColor(color); }

        @Override
        protected void paintComponent(final Graphics g) {
            getSkin().update(this);
            super.paintComponent(g);
        }
    }
    public static class SkinnedTableHeader extends JTableHeader implements ISkinnedComponent<JTableHeader> {
        private static final long serialVersionUID = -1842620489613307379L;

        private JComponentSkin<JTableHeader> skin;
        @Override
        public JComponentSkin<JTableHeader> getSkin() {
            if (skin == null) { skin = new JComponentSkin<>(); }
            return skin;
        }

        public SkinnedTableHeader() { super(); }
        public SkinnedTableHeader(final TableColumnModel columnModel0) { super(columnModel0); }

        public void setForeground(final SkinColor skinColor) { getSkin().setForeground(this, skinColor); }
        @Override public void setForeground(final Color color) { getSkin().resetForeground(); super.setForeground(color); }

        public void setBackground(final SkinColor skinColor) { getSkin().setBackground(this, skinColor); }
        @Override public void setBackground(final Color color) { getSkin().resetBackground(); super.setBackground(color); }

        public void setFont(final SkinFont skinFont) { getSkin().setFont(this, skinFont); }
        @Override public void setFont(final Font font) { getSkin().resetFont(); super.setFont(font); }

        public void setCursor(final SkinCursor skinCursor) { getSkin().setCursor(this, skinCursor); }
        @Override public void setCursor(final Cursor cursor) { getSkin().resetCursor(); super.setCursor(cursor); }

        public void setBorder(final SkinBorder skinBorder) { getSkin().setBorder(this, skinBorder); }
        @Override public void setBorder(final Border border) { getSkin().resetBorder(); super.setBorder(border); }

        @Override
        protected void paintComponent(final Graphics g) {
            getSkin().update(this);
            super.paintComponent(g);
        }
    }
}
