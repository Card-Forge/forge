package forge.gui;

import forge.properties.ForgeProps;
import forge.properties.NewConstants;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Collection;

public class GuiUtils {

    /**
     * This method takes a collection of components and sets the width of each component
     * to the maximum of the collection
     */
    public static void setWidthToMax(Collection<Component> components) {
        int maxWidth = 0;

        for (Component c : components) {
            if (c.getPreferredSize().getWidth() > maxWidth) {
                maxWidth = (int) c.getPreferredSize().getWidth();
            }
        }

        for (Component c : components) {
            c.setMinimumSize(new Dimension(maxWidth, (int) c.getPreferredSize().getHeight()));
            c.setMaximumSize(new Dimension(maxWidth, (int) c.getPreferredSize().getHeight()));
            c.setPreferredSize(new Dimension(maxWidth, (int) c.getPreferredSize().getHeight()));
        }

    }

    /**
     * Adds a Horizontal Glue to panel
     */
    public static void addExpandingHorizontalSpace(JPanel panel) {
        panel.add(Box.createHorizontalGlue());
    }

    /**
     * Adds a Vertical Glue to panel
     */
    public static void addExpandingVerticalSpace(JPanel panel) {
        panel.add(Box.createHorizontalGlue());
    }

    /**
     * Adds a rigid area of size strutSize to panel
     */
    public static void addGap(JPanel panel, int strutSize) {
        panel.add(Box.createRigidArea(new Dimension(strutSize, strutSize)));
    }

    /**
     * Adds a rigid area of size 5 to panel
     */
    public static void addGap(JPanel panel) {
        panel.add(Box.createRigidArea(new Dimension(5, 5)));
    }

    /**
     * Sets the font size of a component
     */
    public static void setFontSize(Component component, int newSize) {
        Font oldFont = component.getFont();
        component.setFont(oldFont.deriveFont((float) newSize));
    }

    public static ImageIcon getIconFromFile(String iconName) {
        File base = ForgeProps.getFile(NewConstants.IMAGE_ICON);
        File file = new File(base, iconName);
        if (file.exists()) {
            return new ImageIcon(file.toString());
        }
        else {
            return null;
        }
    }

    public static ImageIcon getResizedIcon(ImageIcon icon, int width, int height) {
        return new ImageIcon(icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH));
    }

    public static ImageIcon getEmptyIcon(int width, int height) {
        return new ImageIcon(new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB));
    }

    /**
     * Centers a frame on the screen based on its current size
     * @param frame a fully laid-out frame
     */
    public static void centerFrame(Frame frame) {
        Dimension screen = frame.getToolkit().getScreenSize();
        Rectangle bounds = frame.getBounds();
        bounds.width = frame.getWidth();
        bounds.height = frame.getHeight();
        bounds.x = (screen.width - bounds.width) / 2;
        bounds.y = (screen.height - bounds.height) / 2;
        frame.setBounds(bounds);
    }
}
