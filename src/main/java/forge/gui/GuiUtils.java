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
package forge.gui;

import forge.Card;
import forge.gui.match.VMatchUI;
import forge.gui.match.nonsingleton.VField;
import forge.view.arcane.CardPanel;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Rectangle;
import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import javax.swing.Box;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

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

        for (final Component c : components) {
            if (c.getPreferredSize().getWidth() > maxWidth) {
                maxWidth = (int) c.getPreferredSize().getWidth();
            }
        }

        for (final Component c : components) {
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
        final Font oldFont = component.getFont();
        component.setFont(oldFont.deriveFont((float) newSize));
    }


    /**
     * Centers a frame on the screen based on its current size.
     * 
     * @param frame
     *            a fully laid-out frame
     */
    public static void centerFrame(final Window frame) {
        final Dimension screen = frame.getToolkit().getScreenSize();
        final Rectangle bounds = frame.getBounds();
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
     * @param filename
     *            String
     * @return Font
     */
    public static Font newFont(final String filename) {
        final File file = new File(filename);
        Font ttf = null;

        try {
            ttf = Font.createFont(Font.TRUETYPE_FONT, file);
        } catch (final FontFormatException e) {
            System.err.println("GuiUtils > newFont: bad font format \"" + filename + "\"");
        } catch (final IOException e) {
            System.err.println("GuiUtils > newFont: can't find \"" + filename + "\"");
        }
        return ttf;
    }

    /** Checks if calling method uses event dispatch thread.
     * Exception thrown if method is on "wrong" thread.
     * A boolean is passed to indicate if the method must be EDT or not.
     * 
     * @param methodName &emsp; String, part of the custom exception message.
     * @param mustBeEDT &emsp; boolean: true = exception if not EDT, false = exception if EDT
     */
    public static void checkEDT(String methodName, boolean mustBeEDT) {
        boolean isEDT = SwingUtilities.isEventDispatchThread();

        if (!isEDT && mustBeEDT) {
            throw new IllegalStateException(
                    methodName + " must be accessed from the event dispatch thread.");
        }
        else if (isEDT && !mustBeEDT) {
            throw new IllegalStateException(
                    methodName + " may not be accessed from the event dispatch thread.");
        }
    }

    /**
     * Clear all visually highlighted card panels on the battlefield.
     */
    public static void clearPanelSelections() {
        List<VField> view = VMatchUI.SINGLETON_INSTANCE.getFieldViews();
        for (VField v : view) {
            for (CardPanel p : v.getTabletop().getCardPanels()) {
                p.setSelected(false);
            }
        }
    }

    /**
     * Highlight a card on the playfield.
     * 
     * @param c
     *           a card to be highlighted 
     */
    public static void setPanelSelection(Card c) {
        mainLoop:
        for (VField v : VMatchUI.SINGLETON_INSTANCE.getFieldViews()) {
            List<CardPanel> panels = v.getTabletop().getCardPanels();
            for (CardPanel p : panels) {
                if (p.getCard().equals(c)) {
                    p.setSelected(true);
                    break mainLoop;
                }
            }
        }
    }
}
