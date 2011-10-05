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
public final class DeckEditorCommonMenu extends JMenuBar implements NewConstants {

    /** Constant <code>serialVersionUID=-4037993759604768755L</code>. */
    private static final long serialVersionUID = -4037993759604768755L;

    /** Constant <code>previousDirectory</code>. */
    private static File previousDirectory = null;

    private DeckManager deckManager;

    private boolean isDeckSaved = true;

    private String currentDeckName;
    private DeckDisplay deckDisplay;

    private Command exitCommand;

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
        deckDisplay = inDisplay;
        exitCommand = exit;
        deckManager = dckManager;

        // this is added just to make save() and saveAs() work ok
        // when first started up, just a silly patch
        setDeckData("", true);

        setupMenu();
        setupSortMenu();
    }

    /**
     * <p>
     * setupSortMenu.
     * </p>
     */
    private void setupSortMenu() {
        JMenuItem name = new JMenuItem("Card Name");
        JMenuItem cost = new JMenuItem("Cost");
        JMenuItem color = new JMenuItem("Color");
        JMenuItem type = new JMenuItem("Type");
        JMenuItem stats = new JMenuItem("Power/Toughness");
        JMenuItem rarity = new JMenuItem("Rarity");

        JMenu menu = new JMenu("Sort By");
        menu.add(name);
        menu.add(cost);
        menu.add(color);
        menu.add(type);
        menu.add(stats);
        menu.add(rarity);

        this.add(menu);

        name.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent ev) {
                ((DeckEditorCommon) deckDisplay).getTopTableModel().sort(1, true);
            }
        });

        // 0 1 2 3 4 5 6
        // private String column[] = {"Qty", "Name", "Cost", "Color", "Type",
        // "Stats", "Rarity"};
        cost.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent ev) {
                ((DeckEditorCommon) deckDisplay).getTopTableModel().sort(4).sort(3).sort(2);
            }
        });

        color.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent ev) {
                ((DeckEditorCommon) deckDisplay).getTopTableModel().sort(4).sort(2).sort(3);
            }
        });

        type.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent ev) {
                ((DeckEditorCommon) deckDisplay).getTopTableModel().sort(2).sort(3).sort(4);
            }
        });

        stats.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent ev) {
                ((DeckEditorCommon) deckDisplay).getTopTableModel().sort(4).sort(2).sort(3).sort(5);
            }
        });

        rarity.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent ev) {
                // sort by cost, type, color, rarity
                ((DeckEditorCommon) deckDisplay).getTopTableModel().sort(2).sort(4).sort(3).sort(6);
            }
        });
    } // setupSortMenu()

    /**
     * 
     * 
     * @param careAboutOldDeck
     *            a boolean
     */
    public void newConstructed(final boolean careAboutOldDeck) {
        if (careAboutOldDeck && !canLeaveCurrentDeck()) {
            return;
        }

        setDeckData("", true);

        deckDisplay.setDeck(null, null, GameType.Constructed);
    }

    private void newRandomConstructed() {
        if (!canLeaveCurrentDeck()) {
            return;
        }

        setDeckData("", false);

        // The only remaining reference to global variable!
        CardList random = new CardList(forge.AllZone.getCardFactory().getRandomCombinationWithoutRepetition(15 * 5));

        ItemPool<CardPrinted> cpRandom = new ItemPool<CardPrinted>(CardPrinted.class);
        for (Card c : random) {
            cpRandom.add(CardDb.instance().getCard(c));
        }
        cpRandom.add(CardDb.instance().getCard("Forest"));
        cpRandom.add(CardDb.instance().getCard("Island"));
        cpRandom.add(CardDb.instance().getCard("Plains"));
        cpRandom.add(CardDb.instance().getCard("Swamp"));
        cpRandom.add(CardDb.instance().getCard("Mountain"));
        cpRandom.add(CardDb.instance().getCard("Terramorphic Expanse"));

        deckDisplay.setDeck(cpRandom, null, GameType.Constructed);
    }

    private void newGenerateConstructed() {
        if (!canLeaveCurrentDeck()) {
            return;
        }

        setDeckData("", false);

        GenerateConstructedDeck gen = new GenerateConstructedDeck();

        ItemPool<CardPrinted> generated = new ItemPool<CardPrinted>(CardPrinted.class);
        for (Card c : gen.generateDeck()) {
            generated.add(CardDb.instance().getCard(c));
        }
        deckDisplay.setDeck(null, generated, GameType.Constructed);
    }

    private File getImportFilename() {
        JFileChooser chooser = new JFileChooser(previousDirectory);

        chooser.addChoosableFileFilter(DeckManager.DCK_FILTER);
        int returnVal = chooser.showOpenDialog(null);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            previousDirectory = file.getParentFile();
            return file;
        }
        return null;
    } // openFileDialog()

    private void importDeck() {
        File file = getImportFilename();

        if (file == null) {
        } else if (file.getName().endsWith(".dck")) {
            try {
                FileChannel srcChannel = new FileInputStream(file).getChannel();
                File dst = new File(ForgeProps.getFile(NEW_DECKS).getAbsolutePath(), file.getName());
                if (!dst.createNewFile()) {
                    JOptionPane.showMessageDialog(null, "Cannot import deck " + file.getName()
                            + ", a deck currently has that name.");
                    return;
                }
                FileChannel dstChannel = new FileOutputStream(dst).getChannel();
                dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
                srcChannel.close();
                dstChannel.close();

                Deck newDeck = DeckManager.readDeck(file);
                deckManager.addDeck(newDeck);
                showDeck(newDeck, newDeck.getDeckType());

            } catch (Exception ex) {
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
        File filename = getExportFilename();
        if (filename == null) {
            return;
        }

        Deck deck = getDeck();
        try {
            DeckManager.writeDeck(deck, filename);
        } catch (Exception ex) {
            ErrorViewer.showError(ex);
            throw new RuntimeException("Gui_DeckEditor_Menu : exportDeck() error, " + ex);
        }
    }

    private File getExportFilename() {
        JFileChooser save = new JFileChooser(previousDirectory);
        save.setDialogTitle("Export Deck Filename");
        save.setDialogType(JFileChooser.SAVE_DIALOG);
        save.setFileFilter(DeckManager.DCK_FILTER);

        if (save.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            File file = save.getSelectedFile();
            String check = file.getAbsolutePath();

            previousDirectory = file.getParentFile();

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
        File filename = getProxiesFilename();
        if (filename == null) {
            return;
        }

        Deck deck = getDeck();
        try {
            DeckManager.writeDeckHtml(deck, filename);
        } catch (Exception ex) {
            ErrorViewer.showError(ex);
            throw new RuntimeException("Gui_DeckEditor_Menu : printProxies() error, " + ex);
        }
    }

    private File getProxiesFilename() {
        JFileChooser save = new JFileChooser(previousDirectory);
        save.setDialogTitle("Proxy HTML Filename");
        save.setDialogType(JFileChooser.SAVE_DIALOG);
        save.setFileFilter(DeckManager.HTML_FILTER);

        if (save.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            File file = save.getSelectedFile();
            String check = file.getAbsolutePath();

            previousDirectory = file.getParentFile();

            return check.endsWith(".html") ? file : new File(check + ".html");
        }
        return null;
    }

    private void openDeck(final GameType gameType) {
        if (!canLeaveCurrentDeck()) {
            return;
        }

        String name = getUserInputOpenDeck(gameType);

        if (StringUtils.isBlank(name)) {
            return;
        }

        Deck deck = gameType == GameType.Draft ? deckManager.getDraftDeck(name)[0] : deckManager.getDeck(name);
        showDeck(deck, gameType);
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
        setDeckData(deck.getName(), true);
        if (gameType.isLimited()) {
            deckDisplay.setDeck(deck.getSideboard(), deck.getMain(), gameType);
        } else {
            deckDisplay.setDeck(null, deck.getMain(), gameType);
        }
    }

    private void save() {

        if (currentDeckName.equals("")) {
            saveAs();
            return;
        }

        Deck deck = getDeck();
        if (deckDisplay.getGameType().equals(GameType.Draft)) {
            setDeckData(currentDeckName, true);
            // write booster deck
            Deck[] all = deckManager.getDraftDeck(currentDeckName);
            all[0] = deck;
            deckManager.addDraftDeck(all);
            DeckManager.writeDraftDecks(all);
        } else { // constructed or sealed
            setDeckData(currentDeckName, true);
            deckManager.addDeck(deck);
            DeckManager.writeDeck(deck, DeckManager.makeFileName(deck));
        }
        isDeckSaved = true;
    }

    private void saveAs() {
        String name = getDeckNameFromDialog();

        if (name.equals("")) {
            return;
        }
        setDeckData(name, true);

        Deck deck = getDeck();
        if (deckDisplay.getGameType().equals(GameType.Draft)) {
            // MUST copy array
            Deck[] read = deckManager.getDraftDeck(currentDeckName);
            Deck[] all = new Deck[read.length];

            System.arraycopy(read, 0, all, 0, read.length);

            all[0] = deck;
            deckManager.addDraftDeck(all);
            DeckManager.writeDraftDecks(all);
        } else { // constructed and sealed
            deckManager.addDeck(deck);
            DeckManager.writeDeck(deck, DeckManager.makeFileName(deck));
        }
        isDeckSaved = true;
    }

    private void delete() {
        if (StringUtils.isBlank(currentDeckName)) {
            return;
        }

        int n = JOptionPane.showConfirmDialog(null, "Do you want to delete this deck " + currentDeckName + " ?",
                "Delete", JOptionPane.YES_NO_OPTION);

        if (n == JOptionPane.NO_OPTION) {
            return;
        }

        if (deckDisplay.getGameType().equals(GameType.Draft)) {
            deckManager.deleteDraftDeck(currentDeckName);
        } else {
            deckManager.deleteDeck(currentDeckName);
        }

        setDeckData("", true);
        deckDisplay.setDeck(null, null, deckDisplay.getGameType());
    }

    /**
     * 
     * close window.
     */
    public void close() {
        if (!canLeaveCurrentDeck()) {
            return;
        }
        exitCommand.execute();
    }

    private boolean canLeaveCurrentDeck() {
        if (isSaved()) {
            return true;
        }
        String message = String.format("Do you wish to save changes you made to your current deck '%s'?",
                currentDeckName);
        int choice = JOptionPane
                .showConfirmDialog((Component) deckDisplay, message, "You have unsaved changes in your deck",
                        JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (JOptionPane.CANCEL_OPTION == choice) {
            return false;
        }
        if (JOptionPane.NO_OPTION == choice) {
            return true;
        }

        Deck deck = getDeck();
        deck.setName(currentDeckName);
        DeckManager.writeDeck(deck, DeckManager.makeFileName(deck));
        return true;
    }

    private Deck getDeck() {
        Deck deck = deckDisplay.getDeck();
        deck.setName(currentDeckName);
        return deck;
    }

    private void setDeckData(final String deckName, final boolean inDeckSaved) {
        currentDeckName = deckName;
        isDeckSaved = inDeckSaved;

        deckDisplay.setTitle("Deck Editor : " + currentDeckName);
    }

    /**
     * 
     * Get Deck Name.
     * 
     * @return a String
     */
    public String getDeckName() {
        return currentDeckName;
    }

    /**
     * 
     * Is Saved.
     * 
     * @return a boolean
     */
    public boolean isSaved() {
        return isDeckSaved;
    }

    /**
     * <p>
     * getUserInput_GetDeckName.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    private String getDeckNameFromDialog() {
        Object o = JOptionPane.showInputDialog(null, "Save As", "Deck Name", JOptionPane.OK_CANCEL_OPTION);

        if (o == null) {
            return "";
        }

        String deckName = DeckManager.cleanDeckName(o.toString());
        boolean isDraft = deckDisplay.getGameType() == GameType.Draft;
        boolean isUniqueName = isDraft ? deckManager.isUniqueDraft(deckName) : deckManager.isUnique(deckName);
        boolean isGoodName = isUniqueName && StringUtils.isNotBlank(deckName);

        if (isGoodName) {
            return deckName;
        }

        JOptionPane.showMessageDialog(null, "Please pick another deck name, another deck currently has that name.");
        return getDeckNameFromDialog();
    }

    private String getUserInputOpenDeck(final GameType deckType) {
        ArrayList<String> choices = deckManager.getDeckNames(deckType);
        if (choices.isEmpty()) {
            JOptionPane.showMessageDialog(null, "No decks found", "Open Deck", JOptionPane.PLAIN_MESSAGE);
            return null;
        }

        Object o = GuiUtils.getChoiceOptional("Open Deck", choices.toArray());
        return o == null ? null : o.toString();
    }

    // deck.setName(currentDeckName);

    /**
     * 
     * Notify of a Deck Change.
     */
    public void notifyDeckChange() {
        isDeckSaved = false;
    }

    private void setupMenu() {
        JMenuItem newConstructed = new JMenuItem("New Deck - Constructed");

        // JMenuItem newSealed = new JMenuItem("New Deck - Sealed");
        // JMenuItem newDraft = new JMenuItem("New Deck - Draft");

        JMenuItem newRandomConstructed = new JMenuItem("New Deck - Generate Random Constructed Cardpool");
        JMenuItem newGenerateConstructed = new JMenuItem("New Deck - Generate Constructed Deck");

        JMenuItem importDeck = new JMenuItem("Import Deck");
        JMenuItem exportDeck = new JMenuItem("Export Deck");
        // JMenuItem downloadDeck = new JMenuItem("Download Deck");

        JMenuItem openConstructed = new JMenuItem("Open Deck - Constructed");
        JMenuItem openSealed = new JMenuItem("Open Deck - Sealed");
        JMenuItem openDraft = new JMenuItem("Open Deck - Draft");

        // newDraftItem = newDraft;
        // newDraftItem.setEnabled(false);

        JMenuItem save = new JMenuItem("Save");
        JMenuItem saveAs = new JMenuItem("Save As");
        JMenuItem delete = new JMenuItem("Delete");
        JMenuItem close = new JMenuItem("Close");

        JMenu fileMenu = new JMenu("Deck Actions");
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

        JMenuItem generateProxies = new JMenuItem("Generate Proxies...");
        fileMenu.add(generateProxies);

        generateProxies.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent ev) {
                try {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            generateProxies();
                        }
                    });
                } catch (Exception ex) {
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
            public void actionPerformed(final ActionEvent ev) {
                try {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            exportDeck();
                        }
                    });
                } catch (Exception ex) {
                    ErrorViewer.showError(ex);
                    throw new RuntimeException("Gui_DeckEditor_Menu : exportDeck() error - " + ex);
                }
            }
        });

        importDeck.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent ev) {
                try {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            importDeck();
                        }
                    });
                } catch (Exception ex) {
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
            public void actionPerformed(final ActionEvent ev) {
                try {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            newConstructed(true);
                        }
                    });
                } catch (Exception ex) {
                    ErrorViewer.showError(ex);
                    throw new RuntimeException("Gui_DeckEditor_Menu : newConstructed() error - " + ex);
                }
            }
        });

        newRandomConstructed.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent ev) {
                try {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            newRandomConstructed();
                        }
                    });
                } catch (Exception ex) {
                    ErrorViewer.showError(ex);
                    throw new RuntimeException("Gui_DeckEditor_Menu : newRandomConstructed() error - " + ex);
                }
            }
        });

        newGenerateConstructed.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent ev) {
                try {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            newGenerateConstructed();
                        }
                    });
                } catch (Exception ex) {
                    ErrorViewer.showError(ex);
                    throw new RuntimeException("Gui_DeckEditor_Menu : newRandomConstructed() error - " + ex);
                }
            }
        });

        openConstructed.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent ev) {
                try {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            openDeck(GameType.Constructed);
                        }
                    });
                } catch (Exception ex) {
                    ErrorViewer.showError(ex);
                    throw new RuntimeException("Gui_DeckEditor_Menu : openConstructed() error - " + ex);
                }
            }
        });

        openSealed.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent ev) {
                try {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            openDeck(GameType.Sealed);
                        }
                    });
                } catch (Exception ex) {
                    ErrorViewer.showError(ex);
                    throw new RuntimeException("Gui_DeckEditor_Menu : openSealed() error - " + ex);
                }
            }
        });

        openDraft.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent ev) {
                try {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            openDeck(GameType.Draft);
                        }
                    });
                } catch (Exception ex) {
                    ErrorViewer.showError(ex);
                    throw new RuntimeException("Gui_DeckEditor_Menu : openDraft() error - " + ex);
                }
            }
        });

        save.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent ev) {
                try {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            save();
                        }
                    });
                } catch (Exception ex) {
                    ErrorViewer.showError(ex);
                    throw new RuntimeException("Gui_DeckEditor_Menu : save() error - " + ex);
                }
            }
        });

        saveAs.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent ev) {
                try {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            saveAs();
                        }
                    });
                } catch (Exception ex) {
                    ErrorViewer.showError(ex);
                    throw new RuntimeException("Gui_DeckEditor_Menu : saveAs() error - " + ex);
                }
            }
        });

        delete.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent ev) {
                try {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            delete();
                        }
                    });
                } catch (Exception ex) {
                    ErrorViewer.showError(ex);
                    throw new RuntimeException("Gui_DeckEditor_Menu : delete() error - " + ex);
                }
            }
        });

        close.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent ev) {
                try {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            close();
                        }
                    });
                } catch (Exception ex) {
                    ErrorViewer.showError(ex);
                    throw new RuntimeException("Gui_DeckEditor_Menu : close() error - " + ex);
                }
            }
        });
    } // setupMenu()
}
