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
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;

import forge.game.card.Card;
import forge.gui.match.VMatchUI;
import forge.gui.match.nonsingleton.VField;
import forge.view.arcane.CardPanel;

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
    public static void setPanelSelection(final Card c) {
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

    private static final int minItemWidth = 100;
    private static final int itemHeight = 25;

    public static void setMenuItemSize(JMenuItem item) {
        item.setPreferredSize(new Dimension(Math.max(item.getPreferredSize().width, minItemWidth), itemHeight));
    }

    public static JMenuItem createMenuItem(String label, KeyStroke accelerator, final Runnable onClick, boolean enabled, boolean bold) {
        if (label.startsWith("<html>")) { //adjust label if HTML
        	label = "<html>" + "<div style='height: " + itemHeight + "px; margin-top: 6px;'>" + label.substring(6, label.length() - 7) + "</div></html>";
        }
    	JMenuItem item = new JMenuItem(label);
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
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

    public static void addMenuItem(JPopupMenu parent, String label, KeyStroke accelerator, Runnable onClick) {
        parent.add(createMenuItem(label, accelerator, onClick, true, false));
    }
    
    public static void addMenuItem(JMenuItem parent, String label, KeyStroke accelerator, Runnable onClick) {
        parent.add(createMenuItem(label, accelerator, onClick, true, false));
    }
    
    public static void addMenuItem(JPopupMenu parent, String label, KeyStroke accelerator, Runnable onClick, boolean enabled) {
        parent.add(createMenuItem(label, accelerator, onClick, enabled, false));
    }
    
    public static void addMenuItem(JMenuItem parent, String label, KeyStroke accelerator, Runnable onClick, boolean enabled) {
        parent.add(createMenuItem(label, accelerator, onClick, enabled, false));
    }
    
    public static void addMenuItem(JPopupMenu parent, String label, KeyStroke accelerator, Runnable onClick, boolean enabled, boolean bold) {
        parent.add(createMenuItem(label, accelerator, onClick, enabled, bold));
    }
    
    public static void addMenuItem(JMenuItem parent, String label, KeyStroke accelerator, Runnable onClick, boolean enabled, boolean bold) {
        parent.add(createMenuItem(label, accelerator, onClick, enabled, bold));
    }
}
