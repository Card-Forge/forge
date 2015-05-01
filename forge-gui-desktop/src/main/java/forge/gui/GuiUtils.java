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

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;

/**
 * <p>
 * GuiUtils class.
 * </p>
 *
 * @author Forge
 * @version $Id: GuiUtils.java 24769 2014-02-09 13:56:04Z Hellfish $
 */
public final class GuiUtils {

    /**
     * Private constructor to prevent instantiation.
     */
    private GuiUtils() {
        throw new AssertionError();
    }

    /**
     * Attempts to create a font from a filename. Concise error reported if
     * exceptions found.
     *
     * @param filename
     *            the name of the font file.
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

    private static final int minItemWidth = 100;
    private static final int itemHeight = 25;

    public static void setMenuItemSize(final JMenuItem item) {
        item.setPreferredSize(new Dimension(Math.max(item.getPreferredSize().width, minItemWidth), itemHeight));
    }

    public static JMenu createMenu(String label) {
        if (label.startsWith("<html>")) { //adjust label if HTML
            label = "<html>" + "<div style='height: " + itemHeight + "px; margin-top: 6px;'>" + label.substring(6, label.length() - 7) + "</div></html>";
        }
        final JMenu menu = new JMenu(label);
        setMenuItemSize(menu);
        return menu;
    }

    public static JMenuItem createMenuItem(String label, final KeyStroke accelerator, final Runnable onClick, final boolean enabled, final boolean bold) {
        if (label.startsWith("<html>")) { //adjust label if HTML
            label = "<html>" + "<div style='height: " + itemHeight + "px; margin-top: 6px;'>" + label.substring(6, label.length() - 7) + "</div></html>";
        }
        final JMenuItem item = new JMenuItem(label);
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
                if (null != onClick) {
                    onClick.run();
                }
            }
        });
        item.setEnabled(enabled);
        item.setAccelerator(accelerator);
        if (bold) {
            item.setFont(item.getFont().deriveFont(Font.BOLD));
        }
        setMenuItemSize(item);
        return item;
    }

    public static void addMenuItem(final JPopupMenu parent, final String label, final KeyStroke accelerator, final Runnable onClick) {
        parent.add(createMenuItem(label, accelerator, onClick, true, false));
    }

    public static void addMenuItem(final JMenuItem parent, final String label, final KeyStroke accelerator, final Runnable onClick) {
        parent.add(createMenuItem(label, accelerator, onClick, true, false));
    }

    public static void addMenuItem(final JPopupMenu parent, final String label, final KeyStroke accelerator, final Runnable onClick, final boolean enabled) {
        parent.add(createMenuItem(label, accelerator, onClick, enabled, false));
    }

    public static void addMenuItem(final JMenuItem parent, final String label, final KeyStroke accelerator, final Runnable onClick, final boolean enabled) {
        parent.add(createMenuItem(label, accelerator, onClick, enabled, false));
    }

    public static void addMenuItem(final JPopupMenu parent, final String label, final KeyStroke accelerator, final Runnable onClick, final boolean enabled, final boolean bold) {
        parent.add(createMenuItem(label, accelerator, onClick, enabled, bold));
    }

    public static void addMenuItem(final JMenuItem parent, final String label, final KeyStroke accelerator, final Runnable onClick, final boolean enabled, final boolean bold) {
        parent.add(createMenuItem(label, accelerator, onClick, enabled, bold));
    }

    public static void addSeparator(final JPopupMenu parent) {
        parent.add(new JSeparator());
    }

    public static void addSeparator(final JMenuItem parent) {
        parent.add(new JSeparator());
    }
}
