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
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

import forge.CardList;
import forge.Command;
import forge.deck.Deck;
import forge.deck.generate.GenerateConstructedDeck;
import forge.deck.io.DeckIOCore;
import forge.deck.io.DeckSerializer;
import forge.error.ErrorViewer;

/**
 * <p>
 * Gui_DeckEditor_Menu class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public final class MenuCommon extends MenuBase<Deck> {

    /** Constant <code>serialVersionUID=-4037993759604768755L</code>. */
    private static final long serialVersionUID = -4037993759604768755L;
    /** Constant <code>previousDirectory</code>. */
    private static File previousDirectory = null;

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
    public MenuCommon(final IDeckManager<Deck> ctrl, final Command exit) {
        super(ctrl, exit);

        // this is added just to make save() and saveAs() work ok
        // when first started up, just a silly patch

    }

    
    private final void newRandomConstructed() {
        if (!this.canLeaveCurrentDeck()) {
            return;
        }

        Deck randomDeck = new Deck();

        // The only remaining reference to global variable!
        final CardList random = new CardList(forge.AllZone.getCardFactory().getRandomCombinationWithoutRepetition(
                15 * 5));

        randomDeck.getMain().add(random);
        randomDeck.getMain().add("Plains");
        randomDeck.getMain().add("Island");
        randomDeck.getMain().add("Swamp");
        randomDeck.getMain().add("Mountain");
        randomDeck.getMain().add("Forest");
        randomDeck.getMain().add("Terramorphic Expanse");
        
        getController().setModel(randomDeck);
    }

    private final void newGenerateConstructed() {
        if (!this.canLeaveCurrentDeck()) {
            return;
        }

        Deck genConstructed = new Deck();
        genConstructed.getMain().add((new GenerateConstructedDeck()).generateDeck());
        getController().setModel(genConstructed);
    }

    private final File getImportFilename() {
        final JFileChooser chooser = new JFileChooser(MenuCommon.previousDirectory);

        chooser.addChoosableFileFilter(DeckSerializer.DCK_FILTER);
        final int returnVal = chooser.showOpenDialog(null);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            final File file = chooser.getSelectedFile();
            MenuCommon.previousDirectory = file.getParentFile();
            return file;
        }
        return null;
    } // openFileDialog()

    private final void importDeck() {
        final File file = this.getImportFilename();
        if (file == null) {
        } else if (file.getName().endsWith(".dck")) {
            try {
                getController().setModel(DeckIOCore.readDeck(file));

            } catch (final Exception ex) {
                ErrorViewer.showError(ex);
                throw new RuntimeException("Gui_DeckEditor_Menu : importDeck() error, " + ex);
            }
        }

    }

    /**
     * <p>
     * exportDeck.
     * </p>
     */
    private final void exportDeck() {
        final File filename = this.getExportFilename();
        if (filename == null) {
            return;
        }

        try {
            DeckSerializer.writeDeck(getController().getModel(), filename);
        } catch (final Exception ex) {
            ErrorViewer.showError(ex);
            throw new RuntimeException("Gui_DeckEditor_Menu : exportDeck() error, " + ex);
        }
    }

    private final File getExportFilename() {
        final JFileChooser save = new JFileChooser(MenuCommon.previousDirectory);
        save.setDialogTitle("Export Deck Filename");
        save.setDialogType(JFileChooser.SAVE_DIALOG);
        save.setFileFilter(DeckSerializer.DCK_FILTER);

        if (save.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            final File file = save.getSelectedFile();
            final String check = file.getAbsolutePath();

            MenuCommon.previousDirectory = file.getParentFile();

            return check.endsWith(".dck") ? file : new File(check + ".dck");
        }
        return null;
    }

    
    /**
     * <p>
     * Generate Proxy for a Deck.
     * </p>
     */
    private final void generateProxies() {
        final File filename = this.getProxiesFilename();
        if (filename == null) {
            return;
        }

        try {
            DeckSerializer.writeDeckHtml(getController().getModel(), filename);
        } catch (final Exception ex) {
            ErrorViewer.showError(ex);
            throw new RuntimeException("Gui_DeckEditor_Menu : printProxies() error, " + ex);
        }
    }

    private final File getProxiesFilename() {
        final JFileChooser save = new JFileChooser(MenuCommon.previousDirectory);
        save.setDialogTitle("Proxy HTML Filename");
        save.setDialogType(JFileChooser.SAVE_DIALOG);
        save.setFileFilter(DeckSerializer.HTML_FILTER);

        if (save.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            final File file = save.getSelectedFile();
            final String check = file.getAbsolutePath();

            MenuCommon.previousDirectory = file.getParentFile();

            return check.endsWith(".html") ? file : new File(check + ".html");
        }
        return null;
    }

    

    // deck.setName(currentDeckName);

    
    protected JMenu getDefaultFileMenu() {
        final JMenu fileMenu = super.getDefaultFileMenu();
        

        final JMenuItem newRandomConstructed = new JMenuItem("New Deck - Generate Random Constructed Cardpool");
        final JMenuItem newGenerateConstructed = new JMenuItem("New Deck - Generate Constructed Deck");

        final JMenuItem importDeck = new JMenuItem("Import Deck...");
        final JMenuItem exportDeck = new JMenuItem("Export Deck...");
        // JMenuItem downloadDeck = new JMenuItem("Download Deck");


        // newDraftItem = newDraft;
        // newDraftItem.setEnabled(false);



        // fileMenu.add(newSealed);
        // fileMenu.add(newDraft);
        fileMenu.addSeparator();

        fileMenu.add(importDeck);
        fileMenu.add(exportDeck);

        final JMenuItem generateProxies = new JMenuItem("Generate Proxies...");
        fileMenu.add(generateProxies);


        // fileMenu.add(downloadDeck);
        fileMenu.addSeparator();

        fileMenu.add(newRandomConstructed);
        fileMenu.add(newGenerateConstructed);
        
        appendCloseMenuItemTo(fileMenu);

        generateProxies.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent ev) {
                try {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            MenuCommon.this.generateProxies();
                        }
                    });
                } catch (final Exception ex) {
                    ErrorViewer.showError(ex);
                    throw new RuntimeException("Gui_DeckEditor_Menu : generateProxies() error - " + ex);
                }
            }
        });        
        
        // add listeners
        exportDeck.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent ev) {
                try {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            MenuCommon.this.exportDeck();
                        }
                    });
                } catch (final Exception ex) {
                    ErrorViewer.showError(ex);
                    throw new RuntimeException("Gui_DeckEditor_Menu : exportDeck() error - " + ex);
                }
            }
        });

        importDeck.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent ev) {
                try {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            MenuCommon.this.importDeck();
                        }
                    });
                } catch (final Exception ex) {
                    ErrorViewer.showError(ex);
                    throw new RuntimeException("Gui_DeckEditor_Menu : importDeck() error - " + ex);
                }
            }
        });

        newRandomConstructed.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent ev) {
                try {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            MenuCommon.this.newRandomConstructed();
                        }
                    });
                } catch (final Exception ex) {
                    ErrorViewer.showError(ex);
                    throw new RuntimeException("Gui_DeckEditor_Menu : newRandomConstructed() error - " + ex);
                }
            }
        });

        newGenerateConstructed.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent ev) {
                try {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            MenuCommon.this.newGenerateConstructed();
                        }
                    });
                } catch (final Exception ex) {
                    ErrorViewer.showError(ex);
                    throw new RuntimeException("Gui_DeckEditor_Menu : newRandomConstructed() error - " + ex);
                }
            }
        });

        return fileMenu;
    } // setupMenu()
}
