package forge.screens.deckeditor.controllers;

import java.awt.Dialog.ModalityType;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;

import forge.Singletons;
import forge.UiCommand;
import forge.deck.Deck;
import forge.deck.DeckBase;
import forge.deck.io.DeckSerializer;
import forge.deck.io.DeckStorage;
import forge.gui.framework.ICDoc;
import forge.item.InventoryItem;
import forge.properties.ForgeConstants;
import forge.screens.deckeditor.CDeckEditorUI;
import forge.screens.deckeditor.DeckImport;
import forge.screens.deckeditor.SEditorIO;
import forge.screens.deckeditor.views.VCurrentDeck;

/**
 * Controls the "current deck" panel in the deck editor UI.
 *
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
public enum CCurrentDeck implements ICDoc {
    SINGLETON_INSTANCE;

    private static File previousDirectory;

    private final JFileChooser fileChooser = new JFileChooser(ForgeConstants.DECK_BASE_DIR);

    //========== Overridden methods

    private CCurrentDeck() {
        final FileFilter[] defaultFilters = fileChooser.getChoosableFileFilters();
        for (final FileFilter defFilter : defaultFilters) {
            fileChooser.removeChoosableFileFilter(defFilter);
        }

        final FileFilter DCK_FILTER = new FileFilter() {
            @Override public final boolean accept(final File f) {
                return f.getName().endsWith(DeckStorage.FILE_EXTENSION) || f.isDirectory();
            }

            @Override public final String getDescription() {
                return "Simple Deck File .dck";
            }
        };

        fileChooser.addChoosableFileFilter(DCK_FILTER);
    }

    @Override
    public void register() {
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#initialize()
     */
    @Override
    @SuppressWarnings("serial")
    public void initialize() {
        VCurrentDeck.SINGLETON_INSTANCE.getBtnSave().setCommand(new UiCommand() {
            @Override
            public void run() {
                SEditorIO.saveDeck();
            }
        });
        VCurrentDeck.SINGLETON_INSTANCE.getBtnSaveAs().setCommand(new UiCommand() {
            @Override
            public void run() {
                exportDeck();
            }
        });
        VCurrentDeck.SINGLETON_INSTANCE.getBtnPrintProxies().setCommand(new UiCommand() {
            @Override
            public void run() {
                printProxies();
            }
        });
        VCurrentDeck.SINGLETON_INSTANCE.getBtnOpen().setCommand(new UiCommand() {
            @Override
            public void run() {
                openDeck();
            }
        });

        VCurrentDeck.SINGLETON_INSTANCE.getBtnNew().setCommand(new UiCommand() {
            @Override
            public void run() {
                newDeck();
            }
        });
        VCurrentDeck.SINGLETON_INSTANCE.getBtnImport().setCommand(new UiCommand() {
            @Override
            public void run() {
                importDeck();
            }
        });
        VCurrentDeck.SINGLETON_INSTANCE.getTxfTitle().addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(final KeyEvent e) {
                if (Character.isLetterOrDigit(e.getKeyChar())) {
                    CDeckEditorUI.SINGLETON_INSTANCE.getCurrentEditorController().getDeckController().notifyModelChanged();
                }
            }
        });
    }

    /**
     * Opens dialog for importing a deck from a different MTG software.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private <TItem extends InventoryItem, TModel extends DeckBase> void importDeck() {
        final ACEditorBase<TItem, TModel> ed = (ACEditorBase<TItem, TModel>)
                CDeckEditorUI.SINGLETON_INSTANCE.getCurrentEditorController();

        final DeckImport dImport = new DeckImport(ed, ed instanceof CEditorCommander);
        dImport.setModalityType(ModalityType.APPLICATION_MODAL);
        dImport.setVisible(true);
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#update()
     */
    @Override
    public void update() {
    }

    @SuppressWarnings("unchecked")
    private void newDeck() {
        if (!SEditorIO.confirmSaveChanges(Singletons.getControl().getCurrentScreen(), true)) { return; }

        try {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    ((DeckController<DeckBase>) CDeckEditorUI.SINGLETON_INSTANCE.getCurrentEditorController().getDeckController()).newModel();
                    VCurrentDeck.SINGLETON_INSTANCE.getTxfTitle().requestFocusInWindow();
                }
            });
        } catch (final Exception ex) {
            //BugReporter.reportException(ex);
            throw new RuntimeException("Error creating new deck. " + ex);
        }
    }

    /** */
    @SuppressWarnings("unchecked")
    private void openDeck() {
        if (!SEditorIO.confirmSaveChanges(Singletons.getControl().getCurrentScreen(), true)) { return; }

        final File file = this.getImportFilename();

        if (file != null) {
            try {
                ((DeckController<DeckBase>) CDeckEditorUI.SINGLETON_INSTANCE
                        .getCurrentEditorController().getDeckController())
                        .setModel(DeckSerializer.fromFile(file));

            } catch (final Exception ex) {
                //BugReporter.reportException(ex);
                throw new RuntimeException("Error importing deck." + ex);
            }
        }
    }

    /** */
    private File getImportFilename() {
        fileChooser.setDialogTitle("Import Deck");
        fileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
        if (previousDirectory != null) {
            fileChooser.setCurrentDirectory(previousDirectory);
        }

        final int returnVal = fileChooser.showOpenDialog(null);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            final File file = fileChooser.getSelectedFile();
            previousDirectory = file.getParentFile();
            return file;
        }
        return null;
    }

    /** */
    @SuppressWarnings("unchecked")
    private void exportDeck() {
        final DeckController<Deck> controller = (DeckController<Deck>)
                CDeckEditorUI.SINGLETON_INSTANCE.getCurrentEditorController().getDeckController();

        final File filename = this.getExportFilename();
        if (filename == null) {
            return;
        }

        //create copy of deck to save under new name
        String name = filename.getName();
        name = name.substring(0, name.lastIndexOf(".")); //remove extension
        final Deck deck = (Deck)controller.getModel().copyTo(name);

        try {
            DeckSerializer.writeDeck(deck, filename);
            controller.setModel(DeckSerializer.fromFile(filename)); //reload deck from file so everything is in sync
        } catch (final Exception ex) {
            //BugReporter.reportException(ex);
            throw new RuntimeException("Error exporting deck." + ex);
        }
    }

    /** */
    @SuppressWarnings("unchecked")
    private void printProxies() {
        final File filename = this.getPrintProxiesFilename();
        if (filename == null) {
            return;
        }

        try {
            DeckHtmlSerializer.writeDeckHtml(
                ((DeckController<Deck>) CDeckEditorUI.SINGLETON_INSTANCE
                .getCurrentEditorController().getDeckController()).getModel(), filename);
        } catch (final Exception ex) {
            //BugReporter.reportException(ex);
            throw new RuntimeException("Error exporting deck." + ex);
        }
    }

    private File getExportFilename() {
        fileChooser.setDialogTitle("Export Deck");
        fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
        if (previousDirectory != null) {
            fileChooser.setCurrentDirectory(previousDirectory);
        }

        if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            final File file = fileChooser.getSelectedFile();
            final String check = file.getAbsolutePath();

            previousDirectory = file.getParentFile();

            return check.endsWith(".dck") ? file : new File(check + ".dck");
        }
        return null;
    }

    private File getPrintProxiesFilename() {
        final JFileChooser save = new JFileChooser(previousDirectory);
        save.setDialogTitle("Print Proxies");
        save.setDialogType(JFileChooser.SAVE_DIALOG);
        save.setFileFilter(HTML_FILTER);

        if (save.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            final File file = save.getSelectedFile();
            final String check = file.getAbsolutePath();

            previousDirectory = file.getParentFile();

            return check.endsWith(".html") ? file : new File(check + ".html");
        }
        return null;
    }

    /** The Constant HTML_FILTER. */
    public static final FileFilter HTML_FILTER = new FileFilter() {
        @Override
        public boolean accept(final File f) {
            return f.getName().endsWith(".html") || f.isDirectory();
        }

        @Override
        public String getDescription() {
            return "Proxy File .html";
        }
    };
}
