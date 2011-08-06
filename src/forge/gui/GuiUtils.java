package forge.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Collection;
import java.util.List;

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import forge.AllZone;
import forge.Card;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;



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
    
    //returned Object could be null
    public static <T> T getChoiceOptional(String message, T... choices) {
        if(choices == null || choices.length == 0) return null;
        List<T> choice = getChoices(message, 0, 1, choices);
        return choice.isEmpty()? null:choice.get(0);
    }//getChoiceOptional()
   
    // returned Object will never be null
    public static <T> T getChoice(String message, T... choices) {
        List<T> choice = getChoices(message, 1, 1, choices);
        assert choice.size() == 1;
        return choice.get(0);
    }//getChoice()
   
    // returned Object will never be null
    public static <T> List<T> getChoicesOptional(String message, T... choices) {
        return getChoices(message, 0, choices.length, choices);
    }//getChoice()
   
    // returned Object will never be null
    public static <T> List<T> getChoices(String message, T... choices) {
        return getChoices(message, 1, choices.length, choices);
    }//getChoice()
   
    // returned Object will never be null
    public static <T> List<T> getChoices(String message, int min, int max, T... choices) {
        ListChooser<T> c = new ListChooser<T>(message, min, max, choices);
        final JList list = c.getJList();
        list.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent ev) {
                if(list.getSelectedValue() instanceof Card && AllZone.Display != null) {
                		AllZone.Display.setCard((Card) list.getSelectedValue());
                }
            }
        });
        c.show();
        return c.getSelectedValues();
    }//getChoice()

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
