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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.apache.commons.lang3.StringUtils;

import forge.Card;
import forge.CardList;
import forge.Command;
import forge.deck.Deck;
import forge.deck.DeckManager;
import forge.deck.generate.GenerateConstructedDeck;
import forge.error.ErrorViewer;
import forge.game.GameType;
import forge.gui.GuiUtils;
import forge.item.CardDb;
import forge.item.CardPrinted;
import forge.item.ItemPool;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;

/**
 * <p>
 * Gui_DeckEditor_Menu class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public final class DeckEditorCommonMenu extends JMenuBar {

    /** Constant <code>serialVersionUID=-4037993759604768755L</code>. */
    private static final long serialVersionUID = -4037993759604768755L;

    /** Constant <code>previousDirectory</code>. */
    private static File previousDirectory = null;

    private final DeckManager deckManager;

    private boolean isDeckSaved = true;

    private String currentDeckName;
    private final DeckDisplay deckDisplay;

    private final Command exitCommand;

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
    public DeckEditorCommonMenu(final DeckDisplay inDisplay, final DeckManager dckManager, final Command exit) {
        this.deckDisplay = inDisplay;
        this.exitCommand = exit;
        this.deckManager = dckManager;

        // this is added just to make save() and saveAs() work ok
        // when first started up, just a silly patch
        this.setDeckData("", true);

        this.setupMenu();
        this.setupSortMenu();
    }

    /**
     * <p>
     * setupSortMenu.
     * </p>
     */
    private void setupSortMenu() {
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

        this.add(menu);

        name.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent ev) {
                ((DeckEditorCommon) DeckEditorCommonMenu.this.deckDisplay).getTopTableModel().sort(1, true);
            }
        });

        // 0 1 2 3 4 5 6
        // private String column[] = {"Qty", "Name", "Cost", "Color", "Type",
        // "Stats", "Rarity"};
        cost.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent ev) {
                ((DeckEditorCommon) DeckEditorCommonMenu.this.deckDisplay).getTopTableModel().sort(4).sort(3).sort(2);
            }
        });

        color.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent ev) {
                ((DeckEditorCommon) DeckEditorCommonMenu.this.deckDisplay).getTopTableModel().sort(4).sort(2).sort(3);
            }
        });

        type.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent ev) {
                ((DeckEditorCommon) DeckEditorCommonMenu.this.deckDisplay).getTopTableModel().sort(2).sort(3).sort(4);
            }
        });

        stats.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent ev) {
                ((DeckEditorCommon) DeckEditorCommonMenu.this.deckDisplay).getTopTableModel().sort(4).sort(2).sort(3)
                        .sort(5);
            }
        });

        rarity.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent ev) {
                // sort by cost, type, color, rarity
                ((DeckEditorCommon) DeckEditorCommonMenu.this.deckDisplay).getTopTableModel().sort(2).sort(4).sort(3)
                        .sort(6);
            }
        });
    } // setupSortMenu()

    /**
     * New constructed.
     * 
     * @param careAboutOldDeck
     *            a boolean
     */
    public void newConstructed(final boolean careAboutOldDeck) {
        if (careAboutOldDeck && !this.canLeaveCurrentDeck()) {
            return;
        }

        this.setDeckData("", true);

        this.deckDisplay.setDeck(null, null, GameType.Constructed);
    }

    private void newRandomConstructed() {
        if (!this.canLeaveCurrentDeck()) {
            return;
        }

        this.setDeckData("", false);

        // The only remaining reference to global variable!
        final CardList random = new CardList(forge.AllZone.getCardFactory().getRandomCombinationWithoutRepetition(
                15 * 5));

        final ItemPool<CardPrinted> cpRandom = new ItemPool<CardPrinted>(CardPrinted.class);
        for (final Card c : random) {
            cpRandom.add(CardDb.instance().getCard(c));
        }
        cpRandom.add(CardDb.instance().getCard("Forest"));
        cpRandom.add(CardDb.instance().getCard("Island"));
        cpRandom.add(CardDb.instance().getCard("Plains"));
        cpRandom.add(CardDb.instance().getCard("Swamp"));
        cpRandom.add(CardDb.instance().getCard("Mountain"));
        cpRandom.add(CardDb.instance().getCard("Terramorphic Expanse"));

        this.deckDisplay.setDeck(cpRandom, null, GameType.Constructed);
    }

    private void newGenerateConstructed() {
        if (!this.canLeaveCurrentDeck()) {
            return;
        }

        this.setDeckData("", false);

        final GenerateConstructedDeck gen = new GenerateConstructedDeck();

        final ItemPool<CardPrinted> generated = new ItemPool<CardPrinted>(CardPrinted.class);
        for (final Card c : gen.generateDeck()) {
            generated.add(CardDb.instance().getCard(c));
        }
        this.deckDisplay.setDeck(null, generated, GameType.Constructed);
    }

    private File getImportFilename() {
        final JFileChooser chooser = new JFileChooser(DeckEditorCommonMenu.previousDirectory);

        chooser.addChoosableFileFilter(DeckManager.DCK_FILTER);
        final int returnVal = chooser.showOpenDialog(null);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            final File file = chooser.getSelectedFile();
            DeckEditorCommonMenu.previousDirectory = file.getParentFile();
            return file;
        }
        return null;
    } // openFileDialog()

    private void importDeck() {
        final File file = this.getImportFilename();

        if (file == null) {
        } else if (file.getName().endsWith(".dck")) {
            try {
                final FileChannel srcChannel = new FileInputStream(file).getChannel();
                final File dst = new File(ForgeProps.getFile(NewConstants.NEW_DECKS).getAbsolutePath(), file.getName());
                if (!dst.createNewFile()) {
                    JOptionPane.showMessageDialog(null, "Cannot import deck " + file.getName()
                            + ", a deck currently has that name.");
                    return;
                }
                final FileChannel dstChannel = new FileOutputStream(dst).getChannel();
                dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
                srcChannel.close();
                dstChannel.close();

                final Deck newDeck = DeckManager.readDeck(file);
                this.deckManager.addDeck(newDeck);
                this.showDeck(newDeck, newDeck.getDeckType());

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
    private void exportDeck() {
        final File filename = this.getExportFilename();
        if (filename == null) {
            return;
        }

        final Deck deck = this.getDeck();
        try {
            DeckManager.writeDeck(deck, filename);
        } catch (final Exception ex) {
            ErrorViewer.showError(ex);
            throw new RuntimeException("Gui_DeckEditor_Menu : exportDeck() error, " + ex);
        }
    }

    private File getExportFilename() {
        final JFileChooser save = new JFileChooser(DeckEditorCommonMenu.previousDirectory);
        save.setDialogTitle("Export Deck Filename");
        save.setDialogType(JFileChooser.SAVE_DIALOG);
        save.setFileFilter(DeckManager.DCK_FILTER);

        if (save.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            final File file = save.getSelectedFile();
            final String check = file.getAbsolutePath();

            DeckEditorCommonMenu.previousDirectory = file.getParentFile();

            return check.endsWith(".dck") ? file : new File(check + ".dck");
        }
        return null;
    }

    /**
     * <p>
     * Generate Proxy for a Deck.
     * </p>
     */
    private void generateProxies() {
        final File filename = this.getProxiesFilename();
        if (filename == null) {
            return;
        }

        final Deck deck = this.getDeck();
        try {
            DeckManager.writeDeckHtml(deck, filename);
        } catch (final Exception ex) {
            ErrorViewer.showError(ex);
            throw new RuntimeException("Gui_DeckEditor_Menu : printProxies() error, " + ex);
        }
    }

    private File getProxiesFilename() {
        final JFileChooser save = new JFileChooser(DeckEditorCommonMenu.previousDirectory);
        save.setDialogTitle("Proxy HTML Filename");
        save.setDialogType(JFileChooser.SAVE_DIALOG);
        save.setFileFilter(DeckManager.HTML_FILTER);

        if (save.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            final File file = save.getSelectedFile();
            final String check = file.getAbsolutePath();

            DeckEditorCommonMenu.previousDirectory = file.getParentFile();

            return check.endsWith(".html") ? file : new File(check + ".html");
        }
        return null;
    }

    private void openDeck(final GameType gameType) {
        if (!this.canLeaveCurrentDeck()) {
            return;
        }

        final String name = this.getUserInputOpenDeck(gameType);

        if (StringUtils.isBlank(name)) {
            return;
        }

        final Deck deck = gameType == GameType.Draft ? this.deckManager.getDraftDeck(name)[0] : this.deckManager
                .getDeck(name);
        this.showDeck(deck, gameType);
    }

    /**
     * 
     * showDeck.
     * 
     * @param deck
     *            a Deck
     * @param gameType
     *            a GameType
     */
    public void showDeck(final Deck deck, final GameType gameType) {
        this.setDeckData(deck.getName(), true);
        if (gameType.isLimited()) {
            this.deckDisplay.setDeck(deck.getSideboard(), deck.getMain(), gameType);
        } else {
            this.deckDisplay.setDeck(null, deck.getMain(), gameType);
        }
    }

    private void save() {

        if (this.currentDeckName.equals("")) {
            this.saveAs();
            return;
        }

        final Deck deck = this.getDeck();
        if (this.deckDisplay.getGameType().equals(GameType.Draft)) {
            this.setDeckData(this.currentDeckName, true);
            // write booster deck
            final Deck[] all = this.deckManager.getDraftDeck(this.currentDeckName);
            all[0] = deck;
            this.deckManager.addDraftDeck(all);
            DeckManager.writeDraftDecks(all);
        } else { // constructed or sealed
            this.setDeckData(this.currentDeckName, true);
            this.deckManager.addDeck(deck);
            DeckManager.writeDeck(deck, DeckManager.makeFileName(deck));
        }
        this.isDeckSaved = true;
    }

    private void saveAs() {
        final String name = this.getDeckNameFromDialog();

        if (name.equals("")) {
            return;
        }
        this.setDeckData(name, true);

        final Deck deck = this.getDeck();
        if (this.deckDisplay.getGameType().equals(GameType.Draft)) {
            // MUST copy array
            final Deck[] read = this.deckManager.getDraftDeck(this.currentDeckName);
            final Deck[] all = new Deck[read.length];

            System.arraycopy(read, 0, all, 0, read.length);

            all[0] = deck;
            this.deckManager.addDraftDeck(all);
            DeckManager.writeDraftDecks(all);
        } else { // constructed and sealed
            this.deckManager.addDeck(deck);
            DeckManager.writeDeck(deck, DeckManager.makeFileName(deck));
        }
        this.isDeckSaved = true;
    }

    private void delete() {
        if (StringUtils.isBlank(this.currentDeckName)) {
            return;
        }

        final int n = JOptionPane.showConfirmDialog(null, "Do you want to delete this deck " + this.currentDeckName
                + " ?", "Delete", JOptionPane.YES_NO_OPTION);

        if (n == JOptionPane.NO_OPTION) {
            return;
        }

        if (this.deckDisplay.getGameType().equals(GameType.Draft)) {
            this.deckManager.deleteDraftDeck(this.currentDeckName);
        } else {
            this.deckManager.deleteDeck(this.currentDeckName);
        }

        this.setDeckData("", true);
        this.deckDisplay.setDeck(null, null, this.deckDisplay.getGameType());
    }

    /**
     * 
     * close window.
     */
    public void close() {
        if (!this.canLeaveCurrentDeck()) {
            return;
        }
        this.exitCommand.execute();
    }

    private boolean canLeaveCurrentDeck() {
        if (this.isSaved()) {
            return true;
        }
        final String message = String.format("Do you wish to save changes you made to your current deck '%s'?",
                this.currentDeckName);
        final int choice = JOptionPane
                .showConfirmDialog((Component) this.deckDisplay, message, "You have unsaved changes in your deck",
                        JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (JOptionPane.CANCEL_OPTION == choice) {
            return false;
        }
        if (JOptionPane.NO_OPTION == choice) {
            return true;
        }

        final Deck deck = this.getDeck();
        deck.setName(this.currentDeckName);
        DeckManager.writeDeck(deck, DeckManager.makeFileName(deck));
        return true;
    }

    private Deck getDeck() {
        final Deck deck = this.deckDisplay.getDeck();
        deck.setName(this.currentDeckName);
        return deck;
    }

    private void setDeckData(final String deckName, final boolean inDeckSaved) {
        this.currentDeckName = deckName;
        this.isDeckSaved = inDeckSaved;

        this.deckDisplay.setTitle("Deck Editor : " + this.currentDeckName);
    }

    /**
     * 
     * Get Deck Name.
     * 
     * @return a String
     */
    public String getDeckName() {
        return this.currentDeckName;
    }

    /**
     * 
     * Is Saved.
     * 
     * @return a boolean
     */
    public boolean isSaved() {
        return this.isDeckSaved;
    }

    /**
     * <p>
     * getUserInput_GetDeckName.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    private String getDeckNameFromDialog() {
        final Object o = JOptionPane.showInputDialog(null, "Save As", "Deck Name", JOptionPane.OK_CANCEL_OPTION);

        if (o == null) {
            return "";
        }

        final String deckName = DeckManager.cleanDeckName(o.toString());
        final boolean isDraft = this.deckDisplay.getGameType() == GameType.Draft;
        final boolean isUniqueName = isDraft ? this.deckManager.isUniqueDraft(deckName) : this.deckManager
                .isUnique(deckName);
        final boolean isGoodName = isUniqueName && StringUtils.isNotBlank(deckName);

        if (isGoodName) {
            return deckName;
        }

        JOptionPane.showMessageDialog(null, "Please pick another deck name, another deck currently has that name.");
        return this.getDeckNameFromDialog();
    }

    private String getUserInputOpenDeck(final GameType deckType) {
        final ArrayList<String> choices = this.deckManager.getDeckNames(deckType);
        if (choices.isEmpty()) {
            JOptionPane.showMessageDialog(null, "No decks found", "Open Deck", JOptionPane.PLAIN_MESSAGE);
            return null;
        }

        final Object o = GuiUtils.getChoiceOptional("Open Deck", choices.toArray());
        return o == null ? null : o.toString();
    }

    // deck.setName(currentDeckName);

    /**
     * 
     * Notify of a Deck Change.
     */
    public void notifyDeckChange() {
        this.isDeckSaved = false;
    }

    private void setupMenu() {
        final JMenuItem newConstructed = new JMenuItem("New Deck - Constructed");

        // JMenuItem newSealed = new JMenuItem("New Deck - Sealed");
        // JMenuItem newDraft = new JMenuItem("New Deck - Draft");

        final JMenuItem newRandomConstructed = new JMenuItem("New Deck - Generate Random Constructed Cardpool");
        final JMenuItem newGenerateConstructed = new JMenuItem("New Deck - Generate Constructed Deck");

        final JMenuItem importDeck = new JMenuItem("Import Deck...");
        final JMenuItem exportDeck = new JMenuItem("Export Deck...");
        // JMenuItem downloadDeck = new JMenuItem("Download Deck");

        final JMenuItem openConstructed = new JMenuItem("Open Deck - Constructed...");
        final JMenuItem openSealed = new JMenuItem("Open Deck - Sealed");
        final JMenuItem openDraft = new JMenuItem("Open Deck - Draft");

        // newDraftItem = newDraft;
        // newDraftItem.setEnabled(false);

        final JMenuItem save = new JMenuItem("Save");
        final JMenuItem saveAs = new JMenuItem("Save As...");
        final JMenuItem delete = new JMenuItem("Delete");
        final JMenuItem close = new JMenuItem("Close");

        final JMenu fileMenu = new JMenu("Deck Actions");
        fileMenu.add(newConstructed);

        // fileMenu.add(newSealed);
        // fileMenu.add(newDraft);
        fileMenu.addSeparator();

        fileMenu.add(openConstructed);
        fileMenu.add(openSealed);
        fileMenu.add(openDraft);
        fileMenu.addSeparator();

        fileMenu.add(importDeck);
        fileMenu.add(exportDeck);

        final JMenuItem generateProxies = new JMenuItem("Generate Proxies...");
        fileMenu.add(generateProxies);

        generateProxies.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent ev) {
                try {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            DeckEditorCommonMenu.this.generateProxies();
                        }
                    });
                } catch (final Exception ex) {
                    ErrorViewer.showError(ex);
                    throw new RuntimeException("Gui_DeckEditor_Menu : generateProxies() error - " + ex);
                }
            }
        });
        // fileMenu.add(downloadDeck);
        fileMenu.addSeparator();

        fileMenu.add(newRandomConstructed);
        fileMenu.add(newGenerateConstructed);
        fileMenu.addSeparator();

        fileMenu.add(save);
        fileMenu.add(saveAs);
        fileMenu.add(delete);
        fileMenu.addSeparator();

        fileMenu.add(close);

        this.add(fileMenu);

        // add listeners
        exportDeck.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent ev) {
                try {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            DeckEditorCommonMenu.this.exportDeck();
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
                            DeckEditorCommonMenu.this.importDeck();
                        }
                    });
                } catch (final Exception ex) {
                    ErrorViewer.showError(ex);
                    throw new RuntimeException("Gui_DeckEditor_Menu : importDeck() error - " + ex);
                }
            }
        });

        /*
         * downloadDeck.addActionListener(new ActionListener() { public void
         * actionPerformed(final ActionEvent ev) { try {
         * SwingUtilities.invokeLater(new Runnable() { public void run() {
         * downloadDeck(); } }); } catch (Exception ex) {
         * ErrorViewer.showError(ex); throw new
         * RuntimeException("Gui_DeckEditor_Menu : downloadDeck() error - " +
         * ex); } } });
         */
        newConstructed.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent ev) {
                try {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            DeckEditorCommonMenu.this.newConstructed(true);
                        }
                    });
                } catch (final Exception ex) {
                    ErrorViewer.showError(ex);
                    throw new RuntimeException("Gui_DeckEditor_Menu : newConstructed() error - " + ex);
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
                            DeckEditorCommonMenu.this.newRandomConstructed();
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
                            DeckEditorCommonMenu.this.newGenerateConstructed();
                        }
                    });
                } catch (final Exception ex) {
                    ErrorViewer.showError(ex);
                    throw new RuntimeException("Gui_DeckEditor_Menu : newRandomConstructed() error - " + ex);
                }
            }
        });

        openConstructed.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent ev) {
                try {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            DeckEditorCommonMenu.this.openDeck(GameType.Constructed);
                        }
                    });
                } catch (final Exception ex) {
                    ErrorViewer.showError(ex);
                    throw new RuntimeException("Gui_DeckEditor_Menu : openConstructed() error - " + ex);
                }
            }
        });

        openSealed.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent ev) {
                try {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            DeckEditorCommonMenu.this.openDeck(GameType.Sealed);
                        }
                    });
                } catch (final Exception ex) {
                    ErrorViewer.showError(ex);
                    throw new RuntimeException("Gui_DeckEditor_Menu : openSealed() error - " + ex);
                }
            }
        });

        openDraft.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent ev) {
                try {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            DeckEditorCommonMenu.this.openDeck(GameType.Draft);
                        }
                    });
                } catch (final Exception ex) {
                    ErrorViewer.showError(ex);
                    throw new RuntimeException("Gui_DeckEditor_Menu : openDraft() error - " + ex);
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
                            DeckEditorCommonMenu.this.save();
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
                            DeckEditorCommonMenu.this.saveAs();
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
                            DeckEditorCommonMenu.this.delete();
                        }
                    });
                } catch (final Exception ex) {
                    ErrorViewer.showError(ex);
                    throw new RuntimeException("Gui_DeckEditor_Menu : delete() error - " + ex);
                }
            }
        });

        close.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent ev) {
                try {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            DeckEditorCommonMenu.this.close();
                        }
                    });
                } catch (final Exception ex) {
                    ErrorViewer.showError(ex);
                    throw new RuntimeException("Gui_DeckEditor_Menu : close() error - " + ex);
                }
            }
        });
    } // setupMenu()
}
