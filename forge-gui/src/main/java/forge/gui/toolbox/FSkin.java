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
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.text.JTextComponent;

import org.apache.commons.lang.WordUtils;

import forge.FThreads;
import forge.Singletons;
import forge.gui.GuiUtils;
import forge.properties.ForgePreferences;
import forge.properties.NewConstants;
import forge.properties.ForgePreferences.FPref;
import forge.view.FView;

/**
 * Assembles settings from selected or default theme as appropriate. Saves in a
 * hashtable, access using .get(settingName) method.
 * 
 */

public enum FSkin {
    /** Singleton instance of skin. */
    SINGLETON_INSTANCE;

    public static class ComponentSkin<T extends Component> {
        @SuppressWarnings("rawtypes")
        private static HashMap<Component, ComponentSkin> skins = new HashMap<Component, ComponentSkin>();

        @SuppressWarnings("rawtypes")
        private static void reapplyAll() {
            for (ComponentSkin compSkin : ComponentSkin.skins.values()) {
                compSkin.reapply();
            }
            for (JFrameSkin compSkin : JFrameSkin.skins.values()) {
                compSkin.reapply();
            }
            for (JComponentSkin compSkin : JComponentSkin.skins.values()) {
                compSkin.reapply();
            }
            for (JLabelSkin compSkin : JLabelSkin.skins.values()) {
                compSkin.reapply();
            }
            for (AbstractButtonSkin compSkin : AbstractButtonSkin.skins.values()) {
                compSkin.reapply();
            }
            for (JTextComponentSkin compSkin : JTextComponentSkin.skins.values()) {
                compSkin.reapply();
            }
            for (JTableSkin compSkin : JTableSkin.skins.values()) {
                compSkin.reapply();
            }
            for (FPanelSkin compSkin : FPanelSkin.skins.values()) {
                compSkin.reapply();
            }
        }

        protected T comp;
        private SkinColor foreground, background;
        private SkinFont font;
        private SkinCursor cursor;
        protected boolean needRepaintOnReapply;

        private ComponentSkin(T comp0) {
            this.comp = comp0;
        }

        public SkinColor getForeground() {
            return this.foreground;
        }

        public void setForeground(SkinColor skinColor) {
            this.foreground = skinColor;
            this.comp.setForeground(skinColor.color);
        }
        public void setForeground(Color color) {
            this.foreground = null; //ensure this field is reset when static color set
            this.comp.setForeground(color);
        }

        public SkinColor getBackground() {
            return this.background;
        }

        public void setBackground(SkinColor skinColor) {
            this.background = skinColor;
            this.comp.setBackground(skinColor.color);
        }
        public void setBackground(Color color) {
            this.background = null;  //ensure this field is reset when static color set
            this.comp.setBackground(color);
        }

        public void setFont(SkinFont skinFont) {
            this.font = skinFont;
            this.comp.setFont(skinFont.font);
        }
        public void setFont(Font font) {
            this.font = null;  //ensure this field is reset when static font set
            this.comp.setFont(font);
        }

        public void setCursor(SkinCursor skinCursor) {
            this.cursor = skinCursor;
            this.comp.setCursor(skinCursor != null ? skinCursor.cursor : null);
        }
        public void setCursor(Cursor cursor0) {
            this.cursor = null;  //ensure this field is reset when static cursor set
            this.comp.setCursor(cursor0);
        }
        public void resetCursor() {
            this.cursor = null;
        }

        public void setGraphicsColor(Graphics g, SkinColor skinColor) {
            this.needRepaintOnReapply = true;
            g.setColor(skinColor.color);
        }

        public void setGraphicsGradientPaint(Graphics2D g2d, float x1, float y1, SkinColor skinColor1, float x2, float y2, SkinColor skinColor2) {
            this.needRepaintOnReapply = true;
            g2d.setPaint(new GradientPaint(x1, y1, skinColor1.color, x2, y2, skinColor2.color));
        }
        public void setGraphicsGradientPaint(Graphics2D g2d, float x1, float y1, Color color1, float x2, float y2, SkinColor skinColor2) {
            this.needRepaintOnReapply = true;
            g2d.setPaint(new GradientPaint(x1, y1, color1, x2, y2, skinColor2.color));
        }
        public void setGraphicsGradientPaint(Graphics2D g2d, float x1, float y1, SkinColor skinColor1, float x2, float y2, Color color2) {
            this.needRepaintOnReapply = true;
            g2d.setPaint(new GradientPaint(x1, y1, skinColor1.color, x2, y2, color2));
        }

        public void drawImage(Graphics g, SkinImage skinImage, int x, int y) {
            this.needRepaintOnReapply = true;
            g.drawImage(skinImage.image, x, y, null);
        }
        public void drawImage(Graphics g, SkinImage skinImage, int x, int y, int w, int h) {
            this.needRepaintOnReapply = true;
            g.drawImage(skinImage.image, x, y, w, h, null);
        }
        public void drawImage(Graphics g, SkinImage skinImage, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2) {
            this.needRepaintOnReapply = true;
            g.drawImage(skinImage.image, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, null);
        }

        protected void reapply() {
            if (this.foreground != null) {
                comp.setForeground(this.foreground.color);
            }
            if (this.background != null) {
                comp.setBackground(this.background.color);
            }
            if (this.font != null) {
                comp.setFont(this.font.font);
            }
            if (this.cursor != null) {
                comp.setCursor(this.cursor.cursor);
            }
            if (this.needRepaintOnReapply) {
                comp.repaint();
            }
        }
    }
    public static class JFrameSkin<T extends JFrame> extends ComponentSkin<T> {
        @SuppressWarnings("rawtypes")
        private static HashMap<JFrame, JFrameSkin> skins = new HashMap<JFrame, JFrameSkin>();

        private SkinImage iconImage;

        private JFrameSkin(T comp0) {
            super(comp0);
        }

        public void setIconImage(SkinImage skinImage) {
            this.iconImage = skinImage;
            this.comp.setIconImage(skinImage.image);
        }
        public void setIconImage(Image image) {
            this.iconImage = null; //ensure this field is reset when static image set
            this.comp.setIconImage(image);
        }

        @Override
        protected void reapply() {
            if (this.iconImage != null) {
                this.comp.setIconImage(this.iconImage.image);
            }
            super.reapply();
        }
    }
    public static class JComponentSkin<T extends JComponent> extends ComponentSkin<T> {
        @SuppressWarnings("rawtypes")
        private static HashMap<JComponent, JComponentSkin> skins = new HashMap<JComponent, JComponentSkin>();

        private SkinBorder border;

        private JComponentSkin(T comp0) {
            super(comp0);
        }

        public void setBorder(SkinBorder border0) {
            this.border = border0;
            this.comp.setBorder(this.border.createBorder());
        }

        public void setBorder(Border border0) {
            this.border = null; //ensure this field is reset when static border set
            this.comp.setBorder(border0);
        }

        public void removeBorder() {
            this.border = null;
            this.comp.setBorder(null);
        }

        public void setLineBorder(SkinColor skinColor) {
            setBorder(new LineSkinBorder(skinColor));
        }
        public void setLineBorder(SkinColor skinColor, int thickness) {
            setBorder(new LineSkinBorder(skinColor, thickness));
        }

        public void setMatteBorder(int top, int left, int bottom, int right, SkinColor skinColor) {
            setBorder(new MatteSkinBorder(top, left, bottom, right, skinColor));
        }

        @Override
        protected void reapply() {
            if (this.border != null) {
                this.comp.setBorder(this.border.createBorder());
            }
            super.reapply();
        }
    }
    public static class JLabelSkin<T extends JLabel> extends JComponentSkin<T> {
        @SuppressWarnings("rawtypes")
        private static HashMap<JLabel, JLabelSkin> skins = new HashMap<JLabel, JLabelSkin>();

        private SkinImage icon;

        private JLabelSkin(T comp0) {
            super(comp0);
        }

        public void setIcon(SkinImage skinImage) {
            this.icon = skinImage;
            this.comp.setIcon(skinImage != null ? skinImage.getIcon() : null);
        }
        public void setIcon(ImageIcon imageIcon) {
            this.icon = null; //ensure this field is reset when static icon set
            this.comp.setIcon(imageIcon);
        }

        @Override
        protected void reapply() {
            if (this.icon != null) {
                this.comp.setIcon(this.icon.getIcon());
            }
            super.reapply();
        }
    }
    public static class AbstractButtonSkin<T extends AbstractButton> extends JComponentSkin<T> {
        @SuppressWarnings("rawtypes")
        private static HashMap<AbstractButton, AbstractButtonSkin> skins = new HashMap<AbstractButton, AbstractButtonSkin>();

        private SkinImage icon, pressedIcon, rolloverIcon;

        private AbstractButtonSkin(T comp0) {
            super(comp0);
        }

        public void setIcon(SkinImage skinImage) {
            this.icon = skinImage;
            this.comp.setIcon(skinImage != null ? skinImage.getIcon() : null);
        }
        public void setIcon(ImageIcon imageIcon) {
            this.icon = null; //ensure this field is reset when static icon set
            this.comp.setIcon(imageIcon);
        }

        public void setPressedIcon(SkinImage skinImage) {
            this.pressedIcon = skinImage;
            this.comp.setPressedIcon(skinImage != null ? skinImage.getIcon() : null);
        }
        public void setPressedIcon(ImageIcon imageIcon) {
            this.pressedIcon = null; //ensure this field is reset when static icon set
            this.comp.setPressedIcon(imageIcon);
        }

        public void setRolloverIcon(SkinImage skinImage) {
            this.rolloverIcon = skinImage;
            this.comp.setRolloverIcon(skinImage != null ? skinImage.getIcon() : null);
        }
        public void setRolloverIcon(ImageIcon imageIcon) {
            this.rolloverIcon = null; //ensure this field is reset when static icon set
            this.comp.setRolloverIcon(imageIcon);
        }

        @Override
        protected void reapply() {
            if (this.icon != null) {
                this.comp.setIcon(this.icon.getIcon());
            }
            if (this.pressedIcon != null) {
                this.comp.setPressedIcon(this.pressedIcon.getIcon());
            }
            if (this.rolloverIcon != null) {
                this.comp.setRolloverIcon(this.rolloverIcon.getIcon());
            }
            super.reapply();
        }
    }
    public static class JTextComponentSkin<T extends JTextComponent> extends JComponentSkin<T> {
        @SuppressWarnings("rawtypes")
        private static HashMap<JTextComponent, JTextComponentSkin> skins = new HashMap<JTextComponent, JTextComponentSkin>();

        private SkinColor caretColor;

        private JTextComponentSkin(T comp0) {
            super(comp0);
        }

        public SkinColor getCaretColor() {
            return this.caretColor;
        }

        public void setCaretColor(SkinColor skinColor) {
            this.caretColor = skinColor;
            this.comp.setCaretColor(skinColor.color);
        }
        public void setCaretColor(Color color) {
            this.caretColor = null;  //ensure this field is reset when static color set
            this.comp.setCaretColor(color);
        }

        @Override
        protected void reapply() {
            if (this.caretColor != null) {
                this.comp.setCaretColor(this.caretColor.color);
            }
            super.reapply();
        }
    }
    public static class JTableSkin<T extends JTable> extends JComponentSkin<T> {
        @SuppressWarnings("rawtypes")
        private static HashMap<JTable, JTableSkin> skins = new HashMap<JTable, JTableSkin>();

        private SkinColor selectionForeground, selectionBackground;

        private JTableSkin(T comp0) {
            super(comp0);
        }

        public SkinColor getSelectionForeground() {
            return this.selectionForeground;
        }

        public void setSelectionForeground(SkinColor skinColor) {
            this.selectionForeground = skinColor;
            this.comp.setSelectionForeground(skinColor.color);
        }
        public void setSelectionForeground(Color color) {
            this.selectionForeground = null; //ensure this field is reset when static color set
            this.comp.setSelectionForeground(color);
        }

        public SkinColor getSelectionBackground() {
            return this.selectionBackground;
        }

        public void setSelectionBackground(SkinColor skinColor) {
            this.selectionBackground = skinColor;
            this.comp.setSelectionBackground(skinColor.color);
        }
        public void setSelectionBackground(Color color) {
            this.selectionBackground = null;  //ensure this field is reset when static color set
            this.comp.setSelectionBackground(color);
        }

        @Override
        protected void reapply() {
            if (this.selectionForeground != null) {
                this.comp.setSelectionForeground(this.selectionForeground.color);
            }
            if (this.selectionBackground != null) {
                this.comp.setSelectionBackground(this.selectionBackground.color);
            }
            super.reapply();
        }
    }
    public static class FPanelSkin<T extends FPanel> extends JComponentSkin<T> {
        @SuppressWarnings("rawtypes")
        private static HashMap<FPanel, FPanelSkin> skins = new HashMap<FPanel, FPanelSkin>();

        private SkinImage foregroundImage, backgroundTexture;

        private FPanelSkin(T comp0) {
            super(comp0);
        }

        public void setForegroundImage(SkinImage skinIcon) {
            this.comp.setForegroundImage(skinIcon.image); //must call this first since it will call resetForegroundImage
            this.foregroundImage = skinIcon;
            this.needRepaintOnReapply = true; //foreground image draw during paint
        }

        public void resetForegroundImage() {
            this.foregroundImage = null;
        }

        public void setBackgroundTexture(SkinImage skinIcon) {
            this.comp.setBackgroundTexture(skinIcon.image); //must call this first since it will call resetBackgroundTexture
            this.backgroundTexture = skinIcon;
            this.needRepaintOnReapply = true; //background texture drawn during paint
        }

        public void resetBackgroundTexture() {
            this.backgroundTexture = null;
        }

        @Override
        protected void reapply() {
            if (this.foregroundImage != null) {
                this.setForegroundImage(this.foregroundImage); //use skin function so foregroundImage field retained
            }
            if (this.backgroundTexture != null) {
                this.setBackgroundTexture(this.backgroundTexture); //use skin function so backgroundTexture field retained
            }
            super.reapply();
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends Component> ComponentSkin<T> get(T comp) {
        ComponentSkin<T> compSkin = ComponentSkin.skins.get(comp);
        if (compSkin == null) {
            compSkin = new ComponentSkin<T>(comp);
            ComponentSkin.skins.put(comp, compSkin);
        }
        return compSkin;
    }
    @SuppressWarnings("unchecked")
    public static <T extends JFrame> JFrameSkin<T> get(T comp) {
        JFrameSkin<T> compSkin = JFrameSkin.skins.get(comp);
        if (compSkin == null) {
            compSkin = new JFrameSkin<T>(comp);
            JFrameSkin.skins.put(comp, compSkin);
        }
        return compSkin;
    }
    @SuppressWarnings("unchecked")
    public static <T extends JComponent> JComponentSkin<T> get(T comp) {
        JComponentSkin<T> compSkin = JComponentSkin.skins.get(comp);
        if (compSkin == null) {
            compSkin = new JComponentSkin<T>(comp);
            JComponentSkin.skins.put(comp, compSkin);
        }
        return compSkin;
    }
    @SuppressWarnings("unchecked")
    public static <T extends JLabel> JLabelSkin<T> get(T comp) {
        JLabelSkin<T> compSkin = JLabelSkin.skins.get(comp);
        if (compSkin == null) {
            compSkin = new JLabelSkin<T>(comp);
            JLabelSkin.skins.put(comp, compSkin);
        }
        return compSkin;
    }
    @SuppressWarnings("unchecked")
    public static <T extends AbstractButton> AbstractButtonSkin<T> get(T comp) {
        AbstractButtonSkin<T> compSkin = AbstractButtonSkin.skins.get(comp);
        if (compSkin == null) {
            compSkin = new AbstractButtonSkin<T>(comp);
            AbstractButtonSkin.skins.put(comp, compSkin);
        }
        return compSkin;
    }
    @SuppressWarnings("unchecked")
    public static <T extends JTextComponent> JTextComponentSkin<T> get(T comp) {
        JTextComponentSkin<T> compSkin = JTextComponentSkin.skins.get(comp);
        if (compSkin == null) {
            compSkin = new JTextComponentSkin<T>(comp);
            JTextComponentSkin.skins.put(comp, compSkin);
        }
        return compSkin;
    }
    @SuppressWarnings("unchecked")
    public static <T extends JTable> JTableSkin<T> get(T comp) {
        JTableSkin<T> compSkin = JTableSkin.skins.get(comp);
        if (compSkin == null) {
            compSkin = new JTableSkin<T>(comp);
            JTableSkin.skins.put(comp, compSkin);
        }
        return compSkin;
    }
    @SuppressWarnings("unchecked")
    public static <T extends FPanel> FPanelSkin<T> get(T comp) {
        FPanelSkin<T> compSkin = FPanelSkin.skins.get(comp);
        if (compSkin == null) {
            compSkin = new FPanelSkin<T>(comp);
            FPanelSkin.skins.put(comp, compSkin);
        }
        return compSkin;
    }

    /** */
    public enum Backgrounds implements SkinProp { /** */
        BG_SPLASH (null), /** */
        BG_TEXTURE (null), /** */
        BG_MATCH (null);  /** */

        private int[] coords;
        /** @param xy &emsp; int[] coordinates */
        Backgrounds(final int[] xy) { this.coords = xy; }
        /** @return int[] */
        @Override
        public int[] getCoords() { return coords; }
    }

    /**
     * Retrieves a color from this skin's color map.
     * 
     * @param c0 &emsp; Colors property (from enum)
     * @return {@link forge.gui.toolbox.FSkin.SkinColor}
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
    public static Color stepColor(Color clr0, int step) {
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
    public static Color alphaColor(Color clr0, int alpha) {
        return new Color(clr0.getRed(), clr0.getGreen(), clr0.getBlue(), alpha);
    }

    /**
     * @see http://www.nbdtech.com/Blog/archive/2008/04/27/Calculating-the-Perceived-Brightness-of-a-Color.aspx
     */
    public static boolean isColorBright(Color c) {
        int v = (int)Math.sqrt(
                c.getRed() * c.getRed() * 0.241 +
                c.getGreen() * c.getGreen() * 0.691 +
                c.getBlue() * c.getBlue() * 0.068);
        return v >= 130;
    }
    
    public static Color getHighContrastColor(Color c) {
        return isColorBright(c) ? Color.BLACK : Color.WHITE;
    }
    
    public static class SkinColor {
        private static final HashMap<Colors, SkinColor> baseColors = new HashMap<Colors, SkinColor>();
        private static final HashMap<String, SkinColor> derivedColors = new HashMap<String, SkinColor>();
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

        //private constructors for color that changes with skin (use FSkin.getColor())
        private SkinColor(Colors baseColor0) {
            this(baseColor0, NO_BRIGHTNESS_DELTA, NO_STEP, NO_STEP, NO_ALPHA);
        }
        private SkinColor(Colors baseColor0, int brightnessDelta0, int step0, int contrastStep0, int alpha0) {
            this.baseColor = baseColor0;
            this.brightnessDelta = brightnessDelta0;
            this.step = step0;
            this.contrastStep = contrastStep0;
            this.alpha = alpha0;
            this.updateColor();
        }

        private SkinColor getDerivedColor(int brightnessDelta0, int step0, int contrastStep0, int alpha0) {
            String key = this.baseColor.name() + "|" + brightnessDelta0 + "|" + step0 + "|" + contrastStep0 + "|" + alpha0;
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

        public SkinColor alphaColor(int alpha0) {
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

    public static abstract class SkinBorder {
        protected abstract Border createBorder();
    }
    public static class LineSkinBorder extends SkinBorder {
        private final SkinColor skinColor;
        private final int thickness;

        public LineSkinBorder(SkinColor skinColor0) {
            this(skinColor0, 1);
        }
        public LineSkinBorder(SkinColor skinColor0, int thickness0) {
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

        public MatteSkinBorder(int top0, int left0, int bottom0, int right0, SkinColor skinColor0) {
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

        public CompoundSkinBorder(SkinBorder outsideSkinBorder0, SkinBorder insideSkinBorder0) {
            this.outsideSkinBorder = outsideSkinBorder0;
            this.insideSkinBorder = insideSkinBorder0;
        }
        public CompoundSkinBorder(SkinBorder outsideSkinBorder0, Border insideBorder0) {
            this.outsideSkinBorder = outsideSkinBorder0;
            this.insideBorder = insideBorder0;
        }
        public CompoundSkinBorder(Border outsideBorder0, SkinBorder insideSkinBorder0) {
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

        private Color color;
        private int[] coords;

        /** @param xy &emsp; int[] coordinates */
        Colors(final int[] xy) { this.coords = xy; }

        /** @return int[] */
        @Override
        public int[] getCoords() { return coords; }

        public static void updateAll() {
            for (final Colors c : Colors.values()) {
                c.updateColor();
            }
            if (SkinColor.baseColors.size() == 0) { //initialize base skin colors if needed
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
            tempCoords = this.getCoords();
            x0 = tempCoords[0];
            y0 = tempCoords[1];

            color = FSkin.getColorFromPixel(bimPreferredSprite.getRGB(x0, y0));
        }
    }

    /**
     * Gets an image.
     *
     * @param s0 &emsp; SkinProp enum
     * @return {@link forge.gui.toolbox.FSkin.SkinImage}
     */
    public static SkinImage getImage(final SkinProp s0) {
        SkinImage image = SkinImage.images.get(s0);
        if (image == null) {
            throw new NullPointerException("Can't find an image for SkinProp " + s0);
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
     * @return {@link forge.gui.toolbox.FSkin.SkinImage}
     */
    public static SkinImage getImage(final SkinProp s0, int w0, int h0) {
        w0 = (w0 < 1) ? 1 : w0;
        h0 = (h0 < 1) ? 1 : h0;
        return getImage(s0).resize(w0, h0);
    }

    public static class SkinImage {
        private static final Map<SkinProp, SkinImage> images = new HashMap<SkinProp, SkinImage>();

        private static void setImage(final SkinProp s0, Image image0) {
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
         * 
         * @param s0
         */
        private static void setImage(final SkinProp s0) {
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
                setImage(s0, bi0.getScaledInstance(newW, newH, Image.SCALE_AREA_AVERAGING));
            }
            else {
                setImage(s0, bi0);
            }
        }

        protected Image image;
        protected ImageIcon imageIcon;
        protected HashMap<String, SkinImage> scaledImages;
        private HashMap<String, SkinCursor> cursors;

        private SkinImage(Image image0) {
            this.image = image0;
        }

        protected void changeImage(Image image0, ImageIcon imageIcon0) {
            this.image = image0;
            this.imageIcon = imageIcon0;
            this.updateScaledImages();
            this.updateCursors();
        }

        protected SkinImage clone() {
            return new SkinImage(this.image);
        }

        public SkinImage resize(int w, int h) {
            if (this.scaledImages == null) {
                this.scaledImages = new HashMap<String, SkinImage>();
            }
            String key = w + "x" + h;
            SkinImage scaledImage = this.scaledImages.get(key);
            if (scaledImage == null) {
                scaledImage = this.clone();
                scaledImage.createResizedImage(this, w, h);
                this.scaledImages.put(key, scaledImage);
            }
            return scaledImage;
        }

        public boolean save(String path, int w, int h) {
        	final BufferedImage resizedImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

            final Graphics2D g2d = resizedImage.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.drawImage(this.image, 0, 0, w, h, 0, 0, this.getWidth(), this.getHeight(), null);
            g2d.dispose();

            File outputfile = new File(path);
            try {
				ImageIO.write(resizedImage, "png", outputfile);
				return true;
			} catch (IOException e) {
				e.printStackTrace();
			}
            return false;
        }

        public SkinImage scale(double scale) {
            return scale(scale, scale);
        }
        public SkinImage scale(double scaleX, double scaleY) {
            if (this.scaledImages == null) {
                this.scaledImages = new HashMap<String, SkinImage>();
            }
            String key = scaleX + "|" + scaleY;
            SkinImage scaledImage = this.scaledImages.get(key);
            if (scaledImage == null) {
                scaledImage = this.clone();
                scaledImage.createScaledImage(this, scaleX, scaleY);
                this.scaledImages.put(key, scaledImage);
            }
            return scaledImage;
        }

        protected void updateScaledImages() {
            if (this.scaledImages == null) { return; }

            for (Entry<String, SkinImage> i : this.scaledImages.entrySet()) {
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

        protected void createResizedImage(SkinImage baseImage, int w, int h) {
            final BufferedImage resizedImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

            final Graphics2D g2d = resizedImage.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.drawImage(baseImage.image, 0, 0, w, h, 0, 0, baseImage.getWidth(), baseImage.getHeight(), null);
            g2d.dispose();

            this.changeImage(resizedImage, null);
        }

        private void createScaledImage(SkinImage baseImage, double scaleX, double scaleY) {
            createResizedImage(baseImage, (int)(baseImage.getWidth() * scaleX), (int)(baseImage.getHeight() * scaleY));
        }

        private SkinCursor toCursor(int hotSpotX, int hotSpotY, String name) {
            if (this.cursors == null) {
                this.cursors = new HashMap<String, SkinCursor>();
            }
            String key = hotSpotX + "|" + hotSpotY + "|" + name;
            SkinCursor cursor = this.cursors.get(key);
            if (cursor == null) {
                cursor = new SkinCursor(new Point(hotSpotX, hotSpotY), name);
                cursor.updateCursor(this.image);
                this.cursors.put(key, cursor);
            }
            return cursor;
        }

        private void updateCursors() {
            if (this.cursors == null) { return; }

            for (SkinCursor cursor : this.cursors.values()) {
                cursor.updateCursor(this.image);
            }
        }

        public Dimension getSizeForPaint(Graphics g) {
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
                this.imageIcon = new ImageIcon(this.image);
            }
            return this.imageIcon;
        }
    }

    /**
     * Gets an image.
     *
     * @param s0 &emsp; SkinProp enum
     * @return {@link forge.gui.toolbox.FSkin.SkinCursor}
     */
    public static SkinCursor getCursor(final SkinProp s0, int hotSpotX, int hotSpotY, String name) {
        return getImage(s0).toCursor(hotSpotX, hotSpotY, name);
    }

    public static class SkinCursor {
        private static final Toolkit TOOLS = Toolkit.getDefaultToolkit();

        private final Point hotSpot;
        private final String name;
        private Cursor cursor;

        private SkinCursor(Point hotSpot0, String name0) {
            this.hotSpot = hotSpot0;
            this.name = name0;
        }

        private void updateCursor(Image image) {
            this.cursor = TOOLS.createCustomCursor(image, this.hotSpot, this.name);
        }
    }

    /**
     * Gets an icon.
     *
     * @param s0 &emsp; SkinProp enum
     * @return {@link forge.gui.toolbox.FSkin.SkinImage}
     */
    public static SkinIcon getIcon(final SkinProp s0) {
        SkinIcon icon = SkinIcon.icons.get(s0);
        if (icon == null) {
            throw new NullPointerException("Can't find an icon for SkinProp " + s0);
        }
        return icon;
    }

    public static class SkinIcon extends SkinImage {
        private static final Map<SkinProp, SkinIcon> icons = new HashMap<SkinProp, SkinIcon>();

        private static void setIcon(final SkinProp s0, ImageIcon imageIcon0) {
            SkinIcon skinIcon = icons.get(s0);
            if (skinIcon == null) {
                skinIcon = new SkinIcon(imageIcon0);
                icons.put(s0, skinIcon);
            }
            else {
                skinIcon.changeImage(imageIcon0.getImage(), imageIcon0);
            }
        }

        private static void setIcon(final SkinProp s0) {
            tempCoords = s0.getCoords();
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
        private static void setIcon(final SkinProp s0, final String s1) {
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
        private static void setIcon(final SkinProp s0, final BufferedImage bi0) {
            setIcon(s0, new ImageIcon(bi0));
        }

        private SkinIcon(ImageIcon imageIcon0) {
            super(imageIcon0.getImage());
            this.imageIcon = imageIcon0;
        }

        @Override
        protected SkinIcon clone() {
            return new SkinIcon(this.imageIcon);
        }

        @Override
        public SkinIcon resize(int w, int h) {
            return (SkinIcon)super.resize(w, h);
        }

        @Override
        public SkinIcon scale(double scale) {
            return scale(scale, scale);
        }
        @Override
        public SkinIcon scale(double scaleX, double scaleY) {
            return (SkinIcon)super.scale(scaleX, scaleY);
        }

        @Override
        protected void createResizedImage(SkinImage baseImage, int w, int h) {
            Image image0 = baseImage.image.getScaledInstance(w, h, java.awt.Image.SCALE_SMOOTH);
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
        public UnskinnedIcon(String path0) {
            super(new ImageIcon(path0));
        }
        public UnskinnedIcon(BufferedImage i0) {
            super(new ImageIcon(i0));
        }

        @Override
        public ImageIcon getIcon() {
            return super.getIcon();
        }
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
        @Override
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
        @Override
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
        @Override
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
        IMG_COSTRESERVED    (new int[] {240, 240, 80, 80, 40, 40}), /** */
        IMG_COUNTERS1       (new int[] {0, 320, 80, 80}), /** */
        IMG_COUNTERS2       (new int[] {0, 400, 80, 80}), /** */
        IMG_COUNTERS3       (new int[] {80, 320, 80, 80}), /** */
        IMG_COUNTERS_MULTI  (new int[] {80, 400, 80, 80});

        private int[] coords;
        /** @param xy &emsp; int[] coordinates */
        GameplayImages(final int[] xy) { this.coords = xy; }
        /** @return int[] */
        @Override
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
        FOIL_10     (new int[] {400, 2280, 400, 570}); /** */

        private int[] coords;
        /** @param xy &emsp; int[] coordinates */
        Foils(final int[] xy) { this.coords = xy; }
        /** @return int[] */
        @Override
        public int[] getCoords() { return coords; }
    }

    public enum OldFoils implements SkinProp { /** */
        FOIL_11     (new int[] {0, 0, 400, 570}), /** */
        FOIL_12     (new int[] {400, 0, 400, 570}), /** */
        FOIL_13     (new int[] {0, 570, 400, 570}), /** */
        FOIL_14     (new int[] {400, 570, 400, 570}), /** */
        FOIL_15     (new int[] {0, 1140, 400, 570}), /** */
        FOIL_16     (new int[] {400, 1140, 400, 570}), /** */
        FOIL_17     (new int[] {0, 1710, 400, 570}), /** */
        FOIL_18     (new int[] {400, 1710, 400, 570}), /** */
        FOIL_19     (new int[] {0, 2280, 400, 570}), /** */
        FOIL_20     (new int[] {400, 2280, 400, 570}); /** */

        private int[] coords;
        /** @param xy &emsp; int[] coordinates */
        OldFoils(final int[] xy) { this.coords = xy; }
        /** @return int[] */
        @Override
        public int[] getCoords() { return coords; }
    }

    /** */
    public enum DockIcons implements SkinProp { /** */
        ICO_SHORTCUTS    (new int[] {160, 640, 80, 80}), /** */
        ICO_SETTINGS     (new int[] {80, 640, 80, 80}), /** */
        ICO_ENDTURN      (new int[] {320, 640, 80, 80}), /** */
        ICO_CONCEDE      (new int[] {240, 640, 80, 80}), /** */
        ICO_REVERTLAYOUT (new int[] {400, 720, 80, 80}), /** */
        ICO_OPENLAYOUT   (new int[] {0, 800, 80, 80}), /** */
        ICO_SAVELAYOUT   (new int[] {80, 800, 80, 80}), /** */
        ICO_DECKLIST     (new int[] {400, 640, 80, 80}), /** */
        ICO_ALPHASTRIKE  (new int[] {160, 800, 80, 80}), /** */
        ICO_ARCSOFF      (new int[] {240, 800, 80, 80}), /** */
        ICO_ARCSON       (new int[] {320, 800, 80, 80}), /** */
        ICO_ARCSHOVER    (new int[] {400, 800, 80, 80});

        private int[] coords;
        /** @param xy &emsp; int[] coordinates */
        DockIcons(final int[] xy) { this.coords = xy; }
        /** @return int[] */
        @Override
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
        @Override
        public int[] getCoords() { return coords; }
    }

    /** */
    public enum InterfaceIcons implements SkinProp { /** */
        ICO_DELETE          (new int[] {640, 480, 20, 20}), /** */
        ICO_DELETE_OVER     (new int[] {660, 480, 20, 20}), /** */
        ICO_EDIT            (new int[] {640, 500, 20, 20}), /** */
        ICO_EDIT_OVER       (new int[] {660, 500, 20, 20}), /** */
        ICO_OPEN            (new int[] {660, 520, 20, 20}), /** */
        ICO_MINUS           (new int[] {660, 620, 20, 20}), /** */
        ICO_NEW             (new int[] {660, 540, 20, 20}), /** */
        ICO_PLUS            (new int[] {660, 600, 20, 20}), /** */
        ICO_PRINT           (new int[] {660, 640, 20, 20}), /** */
        ICO_SAVE            (new int[] {660, 560, 20, 20}), /** */
        ICO_SAVEAS          (new int[] {660, 580, 20, 20}), /** */
        ICO_UNKNOWN         (new int[] {0, 720, 80, 80}), /** */
        ICO_LOGO            (new int[] {480, 0, 200, 200}), /** */
        ICO_FLIPCARD        (new int[] {400, 0, 80, 120}), /** */
        ICO_FAVICON         (new int[] {0, 640, 80, 80});

        private int[] coords;
        /** @param xy &emsp; int[] coordinates */
        InterfaceIcons(final int[] xy) { this.coords = xy; }
        /** @return int[] */
        @Override
        public int[] getCoords() { return coords; }
    }

    /** */
    public enum LayoutImages implements SkinProp { /** */
        IMG_HANDLE  (new int[] {320, 450, 80, 20}), /** */
        IMG_CUR_L   (new int[] {564, 724, 32, 32}), /** */
        IMG_CUR_R   (new int[] {564, 764, 32, 32}), /** */
        IMG_CUR_T   (new int[] {604, 724, 32, 32}), /** */
        IMG_CUR_B   (new int[] {604, 764, 32, 32}), /** */
        IMG_CUR_TAB (new int[] {644, 764, 32, 32});

        private int[] coords;
        /** @param xy &emsp; int[] coordinates */
        LayoutImages(final int[] xy) { this.coords = xy; }
        /** @return int[] */
        @Override
        public int[] getCoords() { return coords; }
    }

    /** */
    public enum EditorImages implements SkinProp { /** */
        IMG_ARTIFACT        (new int[] {280, 720, 40, 40}), /** */
        IMG_CREATURE        (new int[] {240, 720, 40, 40}), /** */
        IMG_ENCHANTMENT     (new int[] {320, 720, 40, 40}), /** */
        IMG_INSTANT         (new int[] {360, 720, 40, 40}), /** */
        IMG_LAND            (new int[] {120, 720, 40, 40}), /** */
        IMG_MULTI           (new int[] {80, 720, 40, 40}), /** */
        IMG_PLANESWALKER    (new int[] {200, 720, 40, 40}), /** */
        IMG_PACK            (new int[] {80, 760, 40, 40}), /** */
        IMG_SORCERY         (new int[] {160, 720, 40, 40});

        private int[] coords;
        /** @param xy &emsp; int[] coordinates */
        EditorImages(final int[] xy) { this.coords = xy; }
        /** @return int[] */
        @Override
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
        @Override
        public int[] getCoords() { return coords; }
    }

    /** Properties of various components that make up the skin.
     * This interface allows all enums to be under the same roof.
     * It also enforces a getter for coordinate locations in sprites. */
    public interface SkinProp {
        /** @return int[] */
        int[] getCoords();
    }

    private static Map<Integer, SkinImage> avatars;
    private static Map<Integer, Font> fixedFonts = new HashMap<Integer, Font>();

    /** @return {@link java.awt.font} */
    public static Font getFixedFont(final int size) {
        Font fixedFont = fixedFonts.get(size);
        if (fixedFont == null) {
            fixedFont = new Font("Monospaced", Font.PLAIN, size);
            fixedFonts.put(size, fixedFont);
        }
        return fixedFont;
    }

    /**
     * @return {@link forge.gui.toolbox.FSkin.SkinFont}
     */
    public static SkinFont getFont() {
        return FSkin.getFont(FSkin.defaultFontSize);
    }

    /**
     * @param size - integer, pixel size
     * @return {@link forge.gui.toolbox.FSkin.SkinFont}
     */
    public static SkinFont getFont(final int size) {
        return SkinFont.get(Font.PLAIN, size);
    }

    /**
     * @return {@link forge.gui.toolbox.FSkin.SkinFont}
     */
    public static SkinFont getBoldFont() {
        return FSkin.getBoldFont(FSkin.defaultFontSize);
    }

    /**
     * @param size - integer, pixel size
     * @return {@link forge.gui.toolbox.FSkin.SkinFont}
     */
    public static SkinFont getBoldFont(final int size) {
        return SkinFont.get(Font.BOLD, size);
    }

    /**
     * @return {@link forge.gui.toolbox.FSkin.SkinFont}
     */
    public static SkinFont getItalicFont() {
        return FSkin.getItalicFont(FSkin.defaultFontSize);
    }

    /**
     * @param size - integer, pixel size
     * @return {@link forge.gui.toolbox.FSkin.SkinFont}
     */
    public static SkinFont getItalicFont(final int size) {
        return SkinFont.get(Font.ITALIC, size);
    }

    public static class SkinFont {
        private static Font baseFont;
        private static Map<String, SkinFont> fonts = new HashMap<String, SkinFont>();

        private static SkinFont get(final int style0, final int size0) {
            String key = style0 + "|" + size0;
            SkinFont skinFont = fonts.get(key);
            if (skinFont == null) {
                skinFont = new SkinFont(style0, size0);
                fonts.put(key, skinFont);
            }
            return skinFont;
        }

        private static void setBaseFont(Font baseFont0) {
            baseFont = baseFont0;

            //update all cached skin fonts
            for (SkinFont skinFont : fonts.values()) {
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

        public int getSize() {
            return this.font.getSize();
        }

        private void updateFont() {
            this.font = baseFont.deriveFont(this.style, this.size);
        }
    }

    private final static Map<String, String> encodingSymbolLookup = new LinkedHashMap<String, String>(); //must be LinkedHashMap so iteration order is same as add order
    
    private static void addEncodingSymbol(String key, SkinProp skinProp) {
    	String path = NewConstants.CACHE_SYMBOLS_DIR + "/" + key.replace('/', '_') + ".png";
    	if (!getImage(skinProp).save(path, 13, 13)) { return; }

    	try {
			encodingSymbolLookup.put(key, new File(path).toURI().toURL().toString()); //cache image url as string
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
    }

    public static String encodeSymbols(String in) {
    	String out = in;
    	for (Entry<String, String> e : encodingSymbolLookup.entrySet()) {
    		//replace symbol if followed by space, comma, semi-colon, or if it ends line
    		out = out.replaceAll("(?<![^ \r\n])" + e.getKey() + "(?![^ ,:\r\n])", "<img src='" + e.getValue() + "'>");
    	}
    	if (!out.equals(in)) {
    		out = out.replaceAll("'> +<img", "'><img"); //remove space between consecutive symbols
    		out = "<html>" + out + "</html>"; //need to wrap in HTML tags to ensure text rendered as HTML so symbols appear
    	}
    	return out;
    }

    private static final String
    FILE_SKINS_DIR = "res/skins/",
    FILE_ICON_SPRITE = "sprite_icons.png",
    FILE_FOIL_SPRITE = "sprite_foils.png",
    FILE_OLD_FOIL_SPRITE = "sprite_old_foils.png",
    FILE_AVATAR_SPRITE = "sprite_avatars.png",
    FILE_FONT = "font1.ttf",
    FILE_SPLASH = "bg_splash.png",
    FILE_MATCH_BG = "bg_match.jpg",
    FILE_TEXTURE_BG = "bg_texture.jpg",
    DEFAULT_DIR = FILE_SKINS_DIR + "default/";

    private static String preferredDir;
    private static String preferredName;
    private static BufferedImage bimDefaultSprite, bimPreferredSprite, bimFoils,
    bimOldFoils, bimDefaultAvatars, bimPreferredAvatars;
    private static int x0, y0, w0, h0, newW, newH, preferredW, preferredH;
    private static int[] tempCoords;
    private static int defaultFontSize = 12;
    private static boolean loaded = false;

    public static void changeSkin(final String skinName) {
        final ForgePreferences prefs = Singletons.getModel().getPreferences();
        if (skinName.equals(prefs.getPref(FPref.UI_SKIN))) { return; }

        //save skin preference
        prefs.setPref(FPref.UI_SKIN, skinName);
        prefs.save();

        //load skin
        loaded = false; //reset this temporarily until end of loadFull()
        loadLight(skinName, false);
        loadFull(false);

        //reapply skin to all skinned components
        ComponentSkin.reapplyAll();

        //refresh certain components skinned via look and feel
        Singletons.getControl().getForgeMenu().refresh();
        FComboBoxWrapper.refreshAllSkins();
        FComboBoxPanel.refreshAllSkins();
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
        }

        // Non-default (preferred) skin name and dir.
        FSkin.preferredName = skinName.toLowerCase().replace(' ', '_');
        FSkin.preferredDir = FILE_SKINS_DIR + preferredName + "/";

        if (onInit) {
            final File f = new File(preferredDir + FILE_SPLASH);
            if (!f.exists()) {
                FSkin.loadLight("default", onInit);
            }
            else {
                final BufferedImage img;
                try {
                    img = ImageIO.read(f);

                    final int h = img.getHeight();
                    final int w = img.getWidth();

                    SkinIcon.setIcon(Backgrounds.BG_SPLASH, img.getSubimage(0, 0, w, h - 100));

                    UIManager.put("ProgressBar.background", FSkin.getColorFromPixel(img.getRGB(25, h - 75)));
                    UIManager.put("ProgressBar.selectionBackground", FSkin.getColorFromPixel(img.getRGB(75, h - 75)));
                    UIManager.put("ProgressBar.foreground", FSkin.getColorFromPixel(img.getRGB(25, h - 25)));
                    UIManager.put("ProgressBar.selectionForeground", FSkin.getColorFromPixel(img.getRGB(75, h - 25)));
                    UIManager.put("ProgressBar.border", new LineBorder(Color.BLACK, 0));
                } catch (final IOException e) {
                    e.printStackTrace();
                }
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
            if (FSkin.preferredName.isEmpty()) { FSkin.loadLight("default", onInit); }
        }

        FView.SINGLETON_INSTANCE.setSplashProgessBarMessage("Processing image sprites: ", 5);

        // Grab and test various sprite files.
        final File f1 = new File(DEFAULT_DIR + FILE_ICON_SPRITE);
        final File f2 = new File(preferredDir + FILE_ICON_SPRITE);
        final File f3 = new File(DEFAULT_DIR + FILE_FOIL_SPRITE);
        final File f4 = new File(DEFAULT_DIR + FILE_AVATAR_SPRITE);
        final File f5 = new File(preferredDir + FILE_AVATAR_SPRITE);
        final File f6 = new File(DEFAULT_DIR + FILE_OLD_FOIL_SPRITE);

        try {
            bimDefaultSprite = ImageIO.read(f1);
            FView.SINGLETON_INSTANCE.incrementSplashProgessBar();
            bimPreferredSprite = ImageIO.read(f2);
            FView.SINGLETON_INSTANCE.incrementSplashProgessBar();
            bimFoils = ImageIO.read(f3);
            FView.SINGLETON_INSTANCE.incrementSplashProgessBar();
            bimOldFoils = f6.exists() ? ImageIO.read(f6) : ImageIO.read(f3);
            FView.SINGLETON_INSTANCE.incrementSplashProgessBar();
            bimDefaultAvatars = ImageIO.read(f4);

            if (f5.exists()) { bimPreferredAvatars = ImageIO.read(f5); }

            FView.SINGLETON_INSTANCE.incrementSplashProgessBar();

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
            Font f = UIManager.getDefaults().getFont("Label.font");
            if (f != null) {
                FSkin.defaultFontSize = f.getSize();
            }
        }
        SkinFont.setBaseFont(GuiUtils.newFont(FILE_SKINS_DIR + preferredName + "/" + FILE_FONT));

        // Put various images into map (except sprite and splash).
        // Exceptions handled inside method.
        SkinIcon.setIcon(Backgrounds.BG_TEXTURE, preferredDir + FILE_TEXTURE_BG);
        SkinIcon.setIcon(Backgrounds.BG_MATCH, preferredDir + FILE_MATCH_BG);

        // Run through enums and load their coords.
        Colors.updateAll();
        for (final ZoneImages e : ZoneImages.values())                    { SkinImage.setImage(e); }
        for (final DockIcons e : DockIcons.values())                      { SkinIcon.setIcon(e); }
        for (final InterfaceIcons e : InterfaceIcons.values())            { SkinIcon.setIcon(e); }
        for (final ButtonImages e : ButtonImages.values())                { SkinIcon.setIcon(e); }
        for (final QuestIcons e : QuestIcons.values())                    { SkinIcon.setIcon(e); }

        for (final EditorImages e : EditorImages.values())                { SkinImage.setImage(e); }
        for (final ManaImages e : ManaImages.values())                    { SkinImage.setImage(e); }
        for (final ColorlessManaImages e : ColorlessManaImages.values())  { SkinImage.setImage(e); }
        for (final GameplayImages e : GameplayImages.values())            { SkinImage.setImage(e); }
        for (final LayoutImages e : LayoutImages.values())                { SkinImage.setImage(e); }

        // Foils have a separate sprite, so uses a specific method.
        for (final Foils e : Foils.values()) { FSkin.setFoil(e, false); }
        for (final OldFoils e : OldFoils.values()) { FSkin.setFoil(e, true); }

        // Assemble avatar images
        FSkin.assembleAvatars();

        // Table zebra striping
        UIManager.put("Table.alternateRowColor", new Color(240, 240, 240));

        // Images loaded; can start UI init.
        FView.SINGLETON_INSTANCE.setSplashProgessBarMessage("Creating display components.");
        loaded = true;

        // Clear references to buffered images
        FSkin.bimDefaultSprite.flush();
        FSkin.bimFoils.flush();
        FSkin.bimOldFoils.flush();
        FSkin.bimPreferredSprite.flush();
        FSkin.bimDefaultAvatars.flush();

        if (FSkin.bimPreferredAvatars != null) { FSkin.bimPreferredAvatars.flush(); }

        FSkin.bimDefaultSprite = null;
        FSkin.bimFoils = null;
        FSkin.bimOldFoils = null;
        FSkin.bimPreferredSprite = null;
        FSkin.bimDefaultAvatars = null;
        FSkin.bimPreferredAvatars = null;
        
        //establish encoding symbols
        File dir = new File(NewConstants.CACHE_SYMBOLS_DIR);
        if (!dir.mkdir()) { //ensure symbols directory exists and is empty
        	for (File file : dir.listFiles()) {
        		file.delete();
        	}
        }
        encodingSymbolLookup.clear();

        addEncodingSymbol("W/U", ManaImages.IMG_WHITE_BLUE);
        addEncodingSymbol("U/B", ManaImages.IMG_BLUE_BLACK);
        addEncodingSymbol("B/R", ManaImages.IMG_BLACK_RED);
        addEncodingSymbol("R/G", ManaImages.IMG_RED_GREEN);
        addEncodingSymbol("G/W", ManaImages.IMG_GREEN_WHITE);
        addEncodingSymbol("W/B", ManaImages.IMG_WHITE_BLACK);
        addEncodingSymbol("U/R", ManaImages.IMG_BLUE_RED);
        addEncodingSymbol("B/G", ManaImages.IMG_BLACK_GREEN);
        addEncodingSymbol("R/W", ManaImages.IMG_RED_WHITE);
        addEncodingSymbol("G/U", ManaImages.IMG_GREEN_BLUE);
        addEncodingSymbol("2/W", ManaImages.IMG_2W);
        addEncodingSymbol("2/U", ManaImages.IMG_2U);
        addEncodingSymbol("2/B", ManaImages.IMG_2B);
        addEncodingSymbol("2/R", ManaImages.IMG_2R);
        addEncodingSymbol("2/G", ManaImages.IMG_2G);
        addEncodingSymbol("W/P", ManaImages.IMG_PHRYX_WHITE);
        addEncodingSymbol("U/P", ManaImages.IMG_PHRYX_BLUE);
        addEncodingSymbol("B/P", ManaImages.IMG_PHRYX_BLACK);
        addEncodingSymbol("R/P", ManaImages.IMG_PHRYX_RED);
        addEncodingSymbol("G/P", ManaImages.IMG_PHRYX_GREEN);
        addEncodingSymbol("W", ManaImages.IMG_WHITE); //NOTE: single character symbols must come after multi-character symbols to ensure the correct symbol is used
        addEncodingSymbol("U", ManaImages.IMG_BLUE);
        addEncodingSymbol("B", ManaImages.IMG_BLACK);
        addEncodingSymbol("R", ManaImages.IMG_RED);
        addEncodingSymbol("G", ManaImages.IMG_GREEN);
        for (int i = 0; i <= 20; i++) {
        	try {
        		addEncodingSymbol(String.valueOf(i), ColorlessManaImages.valueOf("IMG_" + i));
        	}
        	catch (Exception e) {} //ignore exception from non-existant mana image
        }
        addEncodingSymbol("X", ColorlessManaImages.IMG_X);
        addEncodingSymbol("Y", ColorlessManaImages.IMG_Y);
        addEncodingSymbol("Z", ColorlessManaImages.IMG_Z);
        addEncodingSymbol("Tap", GameplayImages.IMG_TAP);
        addEncodingSymbol("Untap", GameplayImages.IMG_UNTAP);

        // Set look and feel after skin loaded
        FView.SINGLETON_INSTANCE.setSplashProgessBarMessage("Setting look and feel...");
        ForgeLookAndFeel laf = new ForgeLookAndFeel();
        laf.setForgeLookAndFeel(Singletons.getView().getFrame());
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
     * Gets the skins.
     *
     * @return the skins
     */
    public static ArrayList<String> getSkinDirectoryNames() {
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

    public static String[] getSkinNamesArray(boolean sorted) {
        ArrayList<String> skinDirectoryNames = getSkinDirectoryNames();
        String[] prettySkinNames = new String[skinDirectoryNames.size()];
        for (int i = 0; i < skinDirectoryNames.size(); i++) {
            prettySkinNames[i] = WordUtils.capitalize(skinDirectoryNames.get(i).replace('_', ' '));
        }
        if (sorted) {
            java.util.Arrays.sort(prettySkinNames);
        }
        return prettySkinNames;
    }

    /** @return Map<Integer, Image> */
    public static Map<Integer, SkinImage> getAvatars() {
        return avatars;
    }

    public static boolean isLoaded() { return loaded; }

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
        x = (x0 + w0 / 2);
        y = (y0 + h0 / 2);
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
        FSkin.avatars = new HashMap<Integer, SkinImage>();
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
                    FSkin.avatars.put(counter++, new SkinImage(bimPreferredAvatars.getSubimage(i, j, 100, 100)));
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
                FSkin.avatars.put(counter++, new SkinImage(bimDefaultAvatars.getSubimage(i, j, 100, 100)));
            }
        }
    }

    private static void setFoil(final SkinProp s0, boolean isOldStyle) {
        tempCoords = s0.getCoords();
        x0 = tempCoords[0];
        y0 = tempCoords[1];
        w0 = tempCoords[2];
        h0 = tempCoords[3];

        SkinImage.setImage(s0, isOldStyle ? bimOldFoils.getSubimage(x0, y0, w0, h0) : bimFoils.getSubimage(x0, y0, w0, h0));
    }

    public static boolean isLookAndFeelSet() {
        return ForgeLookAndFeel.isMetalLafSet;
    }

    /**
     * Sets the look and feel of the GUI based on the selected Forge theme.
     *
     * @see <a href="http://tips4java.wordpress.com/2008/10/09/uimanager-defaults/">UIManager Defaults</a>
     */
    private static class ForgeLookAndFeel { //needs to live in FSkin for access to skin colors

        private static boolean onInit = true;
        private static boolean isMetalLafSet = false;

        private final Color FORE_COLOR = FSkin.getColor(FSkin.Colors.CLR_TEXT).color;
        private final Color BACK_COLOR = FSkin.getColor(FSkin.Colors.CLR_THEME2).color;
        private final Color HIGHLIGHT_COLOR = BACK_COLOR.brighter();
        private final Border LINE_BORDER = BorderFactory.createLineBorder(FORE_COLOR.darker(), 1);
        private final Border EMPTY_BORDER = BorderFactory.createEmptyBorder(2, 2, 2, 2);

        /**
         * Sets the look and feel of the GUI based on the selected Forge theme.
         */
        private void setForgeLookAndFeel(final JFrame appFrame) {
            if (isUIManagerEnabled()) {
                if (setMetalLookAndFeel(appFrame)) {
                    setMenusLookAndFeel();
                    setComboBoxLookAndFeel();
                    setTabbedPaneLookAndFeel();
                    setButtonLookAndFeel();
                    setToolTipLookAndFeel();
                }
            }
            onInit = false;
        }

        /**
         * Sets the standard "Java L&F" (also called "Metal") that looks the same on all platforms.
         * <p>
         * If not explicitly set then the Mac uses its native L&F which does
         * not support various settings (eg. combobox background color).
         */
        private boolean setMetalLookAndFeel(JFrame appFrame) {
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
            Color clrTheme = FSkin.getColor(FSkin.Colors.CLR_THEME).color;
            Color backgroundColor = FSkin.stepColor(clrTheme, 0);
            Color menuBarEdgeColor = FSkin.stepColor(clrTheme, -80);
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
            UIManager.put("TabbedPane.contentOpaque", FSkin.getColor(FSkin.Colors.CLR_THEME));
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
            boolean isBright = isColorBright(FORE_COLOR);
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

        /**
         * Determines whether theme styles should be applied to GUI.
         * <p>
         * TODO: Currently is using UI_THEMED_COMBOBOX setting but will
         *       eventually want to rename for clarity.
         */
        private boolean isUIManagerEnabled() {
            return Singletons.getModel().getPreferences().getPrefBoolean(FPref.UI_THEMED_COMBOBOX);
        }

        private Font getDefaultFont(String component) {
            return FSkin.getFont(UIManager.getFont(component).getSize()).font;
        }

        private ArrayList<Object> getColorGradients(Color bottom, Color top) {
            ArrayList<Object> gradients = new ArrayList<>();
            gradients.add(0.0);
            gradients.add(0.0);
            gradients.add(top);
            gradients.add(bottom);
            gradients.add(bottom);
            return gradients;
        }
    }
}
