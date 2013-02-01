package forge.gui.deckeditor.controllers;

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;

import forge.Command;
import forge.deck.Deck;
import forge.deck.DeckBase;
import forge.deck.io.DeckSerializer;
import forge.error.ErrorViewer;
import forge.gui.deckeditor.CDeckEditorUI;
import forge.gui.deckeditor.SEditorIO;
import forge.gui.deckeditor.tables.DeckController;
import forge.gui.deckeditor.views.VCurrentDeck;
import forge.gui.framework.ICDoc;
import forge.gui.toolbox.FLabel;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;

/** 
 * Controls the "current deck" panel in the deck editor UI.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
public enum CCurrentDeck implements ICDoc {
    /** */
    SINGLETON_INSTANCE;

    private static File previousDirectory = null;

    private File openStartDir = ForgeProps.getFile(NewConstants.NEW_DECKS);

    private JFileChooser fileChooser = new JFileChooser("");
    
    //========== Overridden methods
    
    private CCurrentDeck() {
        FileFilter[] defaultFilters = fileChooser.getChoosableFileFilters();
        for(FileFilter defFilter : defaultFilters)
        {
            fileChooser.removeChoosableFileFilter(defFilter);
        }
        
        fileChooser.addChoosableFileFilter(DeckSerializer.DCK_FILTER);
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#getCommandOnSelect()
     */
    @Override
    public Command getCommandOnSelect() {
        return null;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#initialize()
     */
    @Override
    @SuppressWarnings("serial")
    public void initialize() {
        ((FLabel) VCurrentDeck.SINGLETON_INSTANCE.getBtnSave())
            .setCommand(new Command() { @Override
                public void execute() { SEditorIO.saveDeck(); } });

        ((FLabel) VCurrentDeck.SINGLETON_INSTANCE.getBtnSaveAs())
            .setCommand(new Command() { @Override
                public void execute() { exportDeck(); } });

        ((FLabel) VCurrentDeck.SINGLETON_INSTANCE.getBtnPrintProxies())
        .setCommand(new Command() { @Override
            public void execute() { printProxies(); } });

        ((FLabel) VCurrentDeck.SINGLETON_INSTANCE.getBtnOpen())
            .setCommand(new Command() { @Override
                public void execute() { openDeck(); } });

        ((FLabel) VCurrentDeck.SINGLETON_INSTANCE.getBtnNew())
            .setCommand(new Command() { @Override
                public void execute() { newDeck(); } });

        VCurrentDeck.SINGLETON_INSTANCE.getTxfTitle().addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(final FocusEvent e) {
                if (((JTextField) e.getSource()).getText().equals("[New Deck]")) {
                    ((JTextField) e.getSource()).setText("");
                }
            }

            @Override
            public void focusLost(final FocusEvent e) {
                if (((JTextField) e.getSource()).getText().isEmpty()) {
                    ((JTextField) e.getSource()).setText("[New Deck]");
                }
            }
        });

        ((FLabel) VCurrentDeck.SINGLETON_INSTANCE.getBtnRemove()).setCommand(new Command() {
            @Override  public void execute() {
                CDeckEditorUI.SINGLETON_INSTANCE.removeSelectedCards(false);
            } });

        ((FLabel) VCurrentDeck.SINGLETON_INSTANCE.getBtnRemove4()).setCommand(new Command() {
            @Override  public void execute() {
                CDeckEditorUI.SINGLETON_INSTANCE.removeSelectedCards(true);
            }
        });
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#update()
     */
    @Override
    public void update() {
    }

    //

    /** */
    @SuppressWarnings("unchecked")
    private void newDeck() {
        if (!SEditorIO.confirmSaveChanges()) { return; }

        try {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    ((DeckController<DeckBase>) CDeckEditorUI.SINGLETON_INSTANCE.getCurrentEditorController().getDeckController()).newModel();
                    VCurrentDeck.SINGLETON_INSTANCE.getTxfTitle().setText("");
                    VCurrentDeck.SINGLETON_INSTANCE.getTxfTitle().requestFocusInWindow();
                }
            });
        } catch (final Exception ex) {
            ErrorViewer.showError(ex);
            throw new RuntimeException("Error creating new deck. " + ex);
        }
    }

    /** */
    @SuppressWarnings("unchecked")
    private void openDeck() {
        if (!SEditorIO.confirmSaveChanges()) { return; }

        final File file = this.getImportFilename();

        if (file != null) {
            try {
                ((DeckController<DeckBase>) CDeckEditorUI.SINGLETON_INSTANCE
                        .getCurrentEditorController().getDeckController())
                        .setModel(Deck.fromFile(file));

            } catch (final Exception ex) {
                ErrorViewer.showError(ex);
                throw new RuntimeException("Error importing deck." + ex);
            }
        }
    }

    /** */
    private File getImportFilename() {
        fileChooser.setDialogTitle("Import Deck");
        
        fileChooser.setCurrentDirectory(openStartDir);
        
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
        final File filename = this.getExportFilename();
        if (filename == null) {
            return;
        }

        try {
            DeckSerializer.writeDeck(
                ((DeckController<Deck>) CDeckEditorUI.SINGLETON_INSTANCE
                .getCurrentEditorController().getDeckController()).getModel(), filename);
        } catch (final Exception ex) {
            ErrorViewer.showError(ex);
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
            DeckSerializer.writeDeckHtml(
                ((DeckController<Deck>) CDeckEditorUI.SINGLETON_INSTANCE
                .getCurrentEditorController().getDeckController()).getModel(), filename);
        } catch (final Exception ex) {
            ErrorViewer.showError(ex);
            throw new RuntimeException("Error exporting deck." + ex);
        }
    }

    private File getExportFilename() {
        fileChooser.setDialogTitle("Export Deck");
        fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
        fileChooser.setCurrentDirectory(previousDirectory);
        
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
        save.setFileFilter(DeckSerializer.HTML_FILTER);

        if (save.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            final File file = save.getSelectedFile();
            final String check = file.getAbsolutePath();

            previousDirectory = file.getParentFile();

            return check.endsWith(".html") ? file : new File(check + ".html");
        }
        return null;
    }

}
