package forge.gui;

import forge.AllZone;
import forge.Card;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.slightlymagic.braids.util.UtilFunctions;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * <p>
 * GuiUtils class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public final class GuiUtils {

    private GuiUtils() {
        throw new AssertionError();
    }

    /**
     * This method takes a collection of components and sets the width of each
     * component to the maximum of the collection.
     * 
     * @param components
     *            a {@link java.util.Collection} object.
     */
    public static void setWidthToMax(final Collection<Component> components) {
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
     * Adds a Horizontal Glue to panel.
     * 
     * @param panel
     *            a {@link javax.swing.JPanel} object.
     */
    public static void addExpandingHorizontalSpace(final JPanel panel) {
        panel.add(Box.createHorizontalGlue());
    }

    /**
     * Adds a Vertical Glue to panel.
     * 
     * @param panel
     *            a {@link javax.swing.JPanel} object.
     */
    public static void addExpandingVerticalSpace(final JPanel panel) {
        panel.add(Box.createHorizontalGlue());
    }

    /**
     * Adds a rigid area of size strutSize to panel.
     * 
     * @param panel
     *            a {@link javax.swing.JPanel} object.
     * @param strutSize
     *            a int.
     */
    public static void addGap(final JPanel panel, final int strutSize) {
        panel.add(Box.createRigidArea(new Dimension(strutSize, strutSize)));
    }

    /**
     * Adds a rigid area of size 5 to panel.
     * 
     * @param panel
     *            a {@link javax.swing.JPanel} object.
     */
    public static void addGap(final JPanel panel) {
        panel.add(Box.createRigidArea(new Dimension(5, 5)));
    }

    /**
     * Sets the font size of a component.
     * 
     * @param component
     *            a {@link java.awt.Component} object.
     * @param newSize
     *            a int.
     */
    public static void setFontSize(final Component component, final int newSize) {
        Font oldFont = component.getFont();
        component.setFont(oldFont.deriveFont((float) newSize));
    }

    /**
     * <p>
     * getIconFromFile.
     * </p>
     * 
     * @param filename
     *            a {@link java.lang.String} object.
     * @return a {@link javax.swing.ImageIcon} object.
     */
    public static ImageIcon getIconFromFile(final String filename) {
        File base = ForgeProps.getFile(NewConstants.IMAGE_ICON);
        File file = new File(base, filename);
        if (filename.equals("") || !file.exists()) {
            return null;
        } else {
            return new ImageIcon(file.toString());
        }
    }

    /**
     * <p>
     * getResizedIcon.
     * </p>
     * 
     * @param filename String.
     * @param scale Double.
     * @return {@link javax.swing.ImageIcon} object
     */
    public static ImageIcon getResizedIcon(final String filename, final double scale) {
        ImageIcon icon = getIconFromFile(filename);

        int w = (int) (icon.getIconWidth() * scale);
        int h = (int) (icon.getIconHeight() * scale);

        return new ImageIcon(icon.getImage().getScaledInstance(w, h, Image.SCALE_SMOOTH));
    }

    /**
     * <p>
     * getResizedIcon.
     * </p>
     * 
     * @param icon ImageIcon
     * @param scale Double
     * @return {@link javax.swing.ImageIcon} object
     */
    public static ImageIcon getResizedIcon(final ImageIcon icon, final double scale) {
        int w = (int) (icon.getIconWidth() * scale);
        int h = (int) (icon.getIconHeight() * scale);

        return new ImageIcon(icon.getImage().getScaledInstance(w, h, Image.SCALE_SMOOTH));
    }

    /**
     * <p>
     * getResizedIcon.
     * </p>
     * 
     * @param icon
     *            a {@link javax.swing.ImageIcon} object.
     * @param width
     *            a int.
     * @param height
     *            a int.
     * @return a {@link javax.swing.ImageIcon} object.
     */
    public static ImageIcon getResizedIcon(final ImageIcon icon, final int width, final int height) {
        return new ImageIcon(icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH));
    }

    /**
     * <p>
     * getEmptyIcon.
     * </p>
     * 
     * @param width
     *            a int.
     * @param height
     *            a int.
     * @return a {@link javax.swing.ImageIcon} object.
     */
    public static ImageIcon getEmptyIcon(final int width, final int height) {
        return new ImageIcon(new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB));
    }

    /**
     * Convenience for getChoices(message, 0, 1, choices).
     * 
     * @see #getChoices(String, int, int, Object...)
     * 
     * @param message
     *            a {@link java.lang.String} object.
     * @param choices
     *            a T object.
     * @param <T>
     *            is automatically inferred.
     * 
     * @return null if choices is missing, empty, or if the users' choices are
     *         empty; otherwise, returns the first item in the List returned by
     *         getChoices.
     */
    public static <T> T getChoiceOptional(final String message, final T... choices) {
        if (choices == null || choices.length == 0) {
            return null;
        }
        List<T> choice = getChoices(message, 0, 1, choices);
        return choice.isEmpty() ? null : choice.get(0);
    } // getChoiceOptional(String,T...)

    /**
     * Like getChoiceOptional, but this takes an Iterator instead of a variable
     * number of arguments.
     * 
     * 
     * @param message
     *            a {@link java.lang.String} object.
     * @param choices
     *            an Iterator over T objects.
     * @param <T>
     *            is automatically inferred.
     * 
     * @return null if choices is missing, empty, or if the users' choices are
     *         empty; otherwise, returns the first item in the List returned by
     *         getChoices.
     */
    public static <T> T getChoiceOptional(final String message, final Iterator<T> choices) {
        if (choices == null | !choices.hasNext()) {
            return null;
        }

        // TODO this is an expensive operation; it would be better to
        // update getChoices to accept an Iterator.
        T[] choicesArray = UtilFunctions.iteratorToArray(choices);

        List<T> choice = getChoices(message, 0, 1, choicesArray);
        return choice.isEmpty() ? null : choice.get(0);
    } // getChoiceOptional(String,Iterator<T>)

    // returned Object will never be null
    /**
     * <p>
     * getChoice.
     * </p>
     * 
     * @param message
     *            a {@link java.lang.String} object.
     * @param choices
     *            a T object.
     * @param <T>
     *            a T object.
     * @return a T object.
     */
    public static <T> T getChoice(final String message, final T... choices) {
        List<T> choice = getChoices(message, 1, 1, choices);
        assert choice.size() == 1;
        return choice.get(0);
    } // getChoice()

    // returned Object will never be null
    /**
     * <p>
     * getChoicesOptional.
     * </p>
     * 
     * @param message
     *            a {@link java.lang.String} object.
     * @param choices
     *            a T object.
     * @param <T>
     *            a T object.
     * @return a {@link java.util.List} object.
     */
    public static <T> List<T> getChoicesOptional(final String message, final T... choices) {
        return getChoices(message, 0, choices.length, choices);
    } // getChoice()

    // returned Object will never be null
    /**
     * <p>
     * getChoices.
     * </p>
     * 
     * @param message
     *            a {@link java.lang.String} object.
     * @param choices
     *            a T object.
     * @param <T>
     *            a T object.
     * @return a {@link java.util.List} object.
     */
    public static <T> List<T> getChoices(final String message, final T... choices) {
        return getChoices(message, 1, choices.length, choices);
    } // getChoice()

    // returned Object will never be null
    /**
     * <p>
     * getChoices.
     * </p>
     * 
     * @param message
     *            a {@link java.lang.String} object.
     * @param min
     *            a int.
     * @param max
     *            a int.
     * @param choices
     *            a T object.
     * @param <T>
     *            a T object.
     * @return a {@link java.util.List} object.
     */
    public static <T> List<T> getChoices(final String message, final int min, final int max, final T... choices) {
        ListChooser<T> c = new ListChooser<T>(message, min, max, choices);
        final JList list = c.getJList();
        list.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(final ListSelectionEvent ev) {
                if (list.getSelectedValue() instanceof Card && AllZone.getDisplay() != null) {
                    AllZone.getDisplay().setCard((Card) list.getSelectedValue());
                }
            }
        });
        c.show();
        return c.getSelectedValues();
    } // getChoice()

    /**
     * Centers a frame on the screen based on its current size.
     * 
     * @param frame
     *            a fully laid-out frame
     */
    public static void centerFrame(final Window frame) {
        Dimension screen = frame.getToolkit().getScreenSize();
        Rectangle bounds = frame.getBounds();
        bounds.width = frame.getWidth();
        bounds.height = frame.getHeight();
        bounds.x = (screen.width - bounds.width) / 2;
        bounds.y = (screen.height - bounds.height) / 2;
        frame.setBounds(bounds);
    }

    /**
     * Attempts to create a font from a filename. Concise error reported if
     * exceptions found.
     * 
     * @param filename String
     * @return Font
     */
    public static Font newFont(final String filename) {
        File file = new File(filename);
        Font ttf = null;

        try {
            ttf = Font.createFont(Font.TRUETYPE_FONT, file);
        } catch (FontFormatException e) {
            System.err.println("GuiUtils > newFont: bad font format \"" + filename + "\"");
        } catch (IOException e) {
            System.err.println("GuiUtils > newFont: can't find \"" + filename + "\"");
        }
        return ttf;
    }
}
