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
package forge.gui.deckeditor;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.apache.commons.lang3.StringUtils;

import forge.Command;
import forge.deck.DeckBase;
import forge.deck.io.DeckIOCore;
import forge.error.ErrorViewer;
import forge.gui.GuiUtils;

/**
 * <p>
 * Gui_DeckEditor_Menu class.
 * </p>
 * 
 * @author Forge
 * @version $Id: DeckEditorCommonMenu.java 13590 2012-01-27 20:46:27Z Max mtg $
 */
public class MenuBase<T extends DeckBase> extends JMenuBar {

    private static final long serialVersionUID = -4037993759604768755L;
    private final Command exitCommand;
    private final IDeckManager<T> controller;

    /**
     * 
     * Menu for Deck Editor.
     * 
     * @param inDisplay
     *            a DeckDisplay
     * @param dckManager
     *            a DeckManager
     * @param exit
     *            a Command
     */
    public MenuBase(final IDeckManager<T> ctrl, final Command exit) {
        this.controller = ctrl;
        this.exitCommand = exit;
    
        this.setupMenu();
    }

    protected final IDeckManager<T> getController() {
        return controller;
    }

    protected void setupMenu()
    {
        this.add(getDefaultFileMenu());
        this.add(getSortMenu());
    }
    
    /**
     * New constructed.
     * 
     * @param careAboutOldDeck
     *            a boolean
     */
    protected final void newDocument(final boolean careAboutOldDeck) {
        if (careAboutOldDeck && !this.canLeaveCurrentDeck()) {
            return;
        }

        this.controller.newModel();
    }

    protected final String getUserInputOpenDeck() {
        final List<String> choices = this.controller.getSavedNames();
        if (choices.isEmpty()) {
            JOptionPane.showMessageDialog(null, "No decks found", "Open Deck", JOptionPane.PLAIN_MESSAGE);
            return null;
        }

        final Object o = GuiUtils.getChoiceOptional("Open Deck", choices.toArray());
        return o == null ? null : o.toString();
    }

    // deck.setName(currentDeckName);

    
    protected final void open() {
        if (!this.canLeaveCurrentDeck()) { return; }
        final String name = this.getUserInputOpenDeck();
        if (StringUtils.isBlank(name)) { return; }
        controller.load(name);
    }

    protected final void save() {
        if (StringUtils.isBlank(controller.getModel().getName())) {
            this.saveAs();
            return;
        }

        this.controller.save();
    }

    protected final void saveAs() {
        final String name = this.getDeckNameFromDialog();

        if (StringUtils.isBlank(name)) {
            final int n = JOptionPane.showConfirmDialog(null, "This name is incorrect. Enter another one?", "Cannot save", JOptionPane.YES_NO_OPTION);

            if (n == JOptionPane.NO_OPTION) return;
        }
        
        if (controller.fileExists(name)) {
            final int m = JOptionPane.showConfirmDialog(null, "There is already saved an item named '"+name+"'. Would you like to overwrite it?", "Confirm overwrite", JOptionPane.YES_NO_OPTION);

            if (m == JOptionPane.NO_OPTION) return;
        }
        
        this.controller.saveAs(name);
    }

    protected final void delete() {
        if (!controller.isModelInStore()) {
            return;
        }

        final int n = JOptionPane.showConfirmDialog(null, "Do you want to delete this deck " + controller.getModel().getName()
                + " ?", "Delete", JOptionPane.YES_NO_OPTION);

        if (n == JOptionPane.NO_OPTION) {
            return;
        }

        this.controller.delete();
    }

    /**
     * 
     * close window.
     */
    public final void close() {
        if (!this.canLeaveCurrentDeck()) {
            return;
        }
        this.exitCommand.execute();
    }

    protected final boolean canLeaveCurrentDeck() {
        if (controller.isSaved()) {
            return true;
        }
        final String message = String.format("Do you wish to save changes you made to your current deck '%s'?",
                this.controller.getModel().getName());
        final int choice = JOptionPane
                .showConfirmDialog(this.controller.getOwnerWindow(), message, "You have unsaved changes in your deck",
                        JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (JOptionPane.CANCEL_OPTION == choice) {
            return false;
        }
        if (JOptionPane.NO_OPTION == choice) {
            return true;
        }

        save();
        return true;
    }



    /**
     * <p>
     * getUserInput_GetDeckName.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    protected final String getDeckNameFromDialog() {
        final Object o = JOptionPane.showInputDialog(null, "Save As", "Deck Name", JOptionPane.OK_CANCEL_OPTION);

        if (o == null) {
            return "";
        }

        final String deckName = DeckIOCore.cleanDeckName(o.toString());
        final boolean isGoodName = controller.isGoodName(deckName);

        if (isGoodName) {
            return deckName;
        }

        JOptionPane.showMessageDialog(null, "Please pick another deck name, another deck currently has that name.");
        return this.getDeckNameFromDialog();
    }

    protected JMenu getDefaultFileMenu() {
        final JMenu fileMenu = new JMenu("Deck");
    
        final JMenuItem newDoc = new JMenuItem("New");
        final JMenuItem open = new JMenuItem("Open");
        final JMenuItem save = new JMenuItem("Save");
        final JMenuItem saveAs = new JMenuItem("Save As...");
        final JMenuItem delete = new JMenuItem("Delete");
        
    
        fileMenu.add(newDoc);
        fileMenu.add(open);
        fileMenu.addSeparator();
    
        fileMenu.add(save);
        fileMenu.add(saveAs);
        fileMenu.add(delete);
        
        newDoc.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent ev) {
                try {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            MenuBase.this.newDocument(true);
                        }
                    });
                } catch (final Exception ex) {
                    ErrorViewer.showError(ex);
                    throw new RuntimeException("Gui_DeckEditor_Menu : newConstructed() error - " + ex);
                }
            }
        });
    
    
        open.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent ev) {
                try {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            MenuBase.this.open();
                        }
                    });
                } catch (final Exception ex) {
                    ErrorViewer.showError(ex);
                    throw new RuntimeException("Gui_DeckEditor_Menu : open() error - " + ex);
                }
            }
        });        
        
        save.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent ev) {
                try {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            MenuBase.this.save();
                        }
                    });
                } catch (final Exception ex) {
                    ErrorViewer.showError(ex);
                    throw new RuntimeException("Gui_DeckEditor_Menu : save() error - " + ex);
                }
            }
        });
    
        saveAs.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent ev) {
                try {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            MenuBase.this.saveAs();
                        }
                    });
                } catch (final Exception ex) {
                    ErrorViewer.showError(ex);
                    throw new RuntimeException("Gui_DeckEditor_Menu : saveAs() error - " + ex);
                }
            }
        });
    
        delete.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent ev) {
                try {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            MenuBase.this.delete();
                        }
                    });
                } catch (final Exception ex) {
                    ErrorViewer.showError(ex);
                    throw new RuntimeException("Gui_DeckEditor_Menu : delete() error - " + ex);
                }
            }
        });
        return fileMenu;
    }
    
    protected void appendCloseMenuItemTo(JMenu fileMenu)
    {
        final JMenuItem close = new JMenuItem("Close");
        fileMenu.addSeparator();
        fileMenu.add(close);        
        
        close.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent ev) {
                try {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            MenuBase.this.close();
                        }
                    });
                } catch (final Exception ex) {
                    ErrorViewer.showError(ex);
                    throw new RuntimeException("Gui_DeckEditor_Menu : close() error - " + ex);
                }
            }
        });
    } // setupMenu()

    /**
     * <p>
     * setupSortMenu.
     * </p>
     */
    protected final JMenuItem getSortMenu() {
        final JMenuItem name = new JMenuItem("Card Name");
        final JMenuItem cost = new JMenuItem("Cost");
        final JMenuItem color = new JMenuItem("Color");
        final JMenuItem type = new JMenuItem("Type");
        final JMenuItem stats = new JMenuItem("Power/Toughness");
        final JMenuItem rarity = new JMenuItem("Rarity");
    
        final JMenu menu = new JMenu("Sort By");
        menu.add(name);
        menu.add(cost);
        menu.add(color);
        menu.add(type);
        menu.add(stats);
        menu.add(rarity);
    
        name.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent ev) {
                (MenuBase.this.controller).getView().getTopTableModel().sort(1, true);
            }
        });
    
        // 0 1 2 3 4 5 6
        // private String column[] = {"Qty", "Name", "Cost", "Color", "Type",
        // "Stats", "Rarity"};
        cost.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent ev) {
                (MenuBase.this.controller).getView().getTopTableModel().sort(4).sort(3).sort(2);
            }
        });
    
        color.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent ev) {
                (MenuBase.this.controller).getView().getTopTableModel().sort(4).sort(2).sort(3);
            }
        });
    
        type.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent ev) {
                (MenuBase.this.controller).getView().getTopTableModel().sort(2).sort(3).sort(4);
            }
        });
    
        stats.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent ev) {
                (MenuBase.this.controller).getView().getTopTableModel().sort(4).sort(2).sort(3).sort(5);
            }
        });
    
        rarity.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent ev) {
                // sort by cost, type, color, rarity
                (MenuBase.this.controller).getView().getTopTableModel().sort(2).sort(4).sort(3).sort(6);
            }
        });
        
        return menu;
    } // setupSortMenu()
}
