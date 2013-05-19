package forge.gui.home.gauntlet;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;

import org.apache.commons.lang3.ArrayUtils;

import forge.Command;
import forge.deck.Deck;
import forge.gauntlet.GauntletData;
import forge.gauntlet.GauntletIO;
import forge.gui.framework.ICDoc;
import forge.properties.NewConstants;

/** 
 * Controls the "build gauntlet" submenu in the home UI.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */

@SuppressWarnings("serial")
public enum CSubmenuGauntletBuild implements ICDoc {
    SINGLETON_INSTANCE;

    private final VSubmenuGauntletBuild view = VSubmenuGauntletBuild.SINGLETON_INSTANCE;
    private final List<Deck> workingDecks = new ArrayList<Deck>();
    private File openStartDir = new File(NewConstants.GAUNTLET_DIR.userPrefLoc);

    private final FileFilter filterDAT = new FileFilter() {
        @Override
        public boolean accept(final File f) {
            if (f.isDirectory()) {
                return true;
            }

            String filename = f.getName();
            return (!filename.startsWith(GauntletIO.PREFIX_QUICK) && filename.endsWith(GauntletIO.SUFFIX_DATA));
        }

        @Override
        public String getDescription() {
            return "Forge data file .dat";
        }
    };

    //private final KeyAdapter kadSearch = new KeyAdapter() { @Override
        //public void keyPressed(final KeyEvent e) { search(); } };

    private final Command cmdAddDeck = new Command() { @Override
        public void run() { addDeck(); } };

    private final Command cmdRemoveDeck = new Command() { @Override
        public void run() { removeDeck(); } };

    private final Command cmdDeckUp = new Command() { @Override
        public void run() { deckUp(); } };

    private final Command cmdDeckDown = new Command() { @Override
        public void run() { deckDown(); } };

    private final Command cmdSave = new Command() { @Override
        public void run() { saveGauntlet(); } };

    private final Command cmdNew = new Command() { @Override
        public void run() { newGauntlet(); } };

    private final Command cmdOpen = new Command() { @Override
        public void run() { openGauntlet(); } };

    /* (non-Javadoc)
     * @see forge.gui.home.ICSubmenu#initialize()
     */
    @Override
    public void update() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() { view.focusName(); }
        });
    }

    /* (non-Javadoc)
     * @see forge.gui.home.ICSubmenu#initialize()
     */
    @Override
    public void initialize() {
        view.getLstRight().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        view.getBtnRight().setCommand(cmdAddDeck);
        view.getBtnLeft().setCommand(cmdRemoveDeck);
        view.getBtnUp().setCommand(cmdDeckUp);
        view.getBtnDown().setCommand(cmdDeckDown);

        view.getBtnSave().setCommand(cmdSave);
        view.getBtnOpen().setCommand(cmdOpen);
        view.getBtnNew().setCommand(cmdNew);
        
        view.getLstLeft().initialize();
//        updateDecks();
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#getCommandOnSelect()
     */
    @Override
    public Command getCommandOnSelect() {
        return null;
    }


    private void addDeck() {
        final Deck deckToAdd = view.getLstLeft().getDeck().getOriginalDeck();
        if ( null == deckToAdd ) return;
        workingDecks.add(deckToAdd);
        view.getLblSave().setVisible(false);
        dumpDecksIntoList();
    }

    private void removeDeck() {
        final int selection = view.getLstRight().getSelectedIndex();

        if (selection == -1) { return; }

        workingDecks.remove(selection);

        view.getLblSave().setVisible(false);
        dumpDecksIntoList();
    }

    private void deckUp() {
        final int oldIndex = view.getLstRight().getSelectedIndex();

        if (oldIndex == 0) { return; }

        final Deck movingDeck = workingDecks.remove(oldIndex);
        workingDecks.add(oldIndex - 1, movingDeck);

        view.getLblSave().setVisible(false);
        dumpDecksIntoList();
        view.getLstRight().setSelectedIndex(oldIndex - 1);
    }

    private void deckDown() {
        final int oldIndex = view.getLstRight().getSelectedIndex();

        if (oldIndex == workingDecks.size() - 1) { return; }

        final Deck movingDeck = workingDecks.remove(oldIndex);
        workingDecks.add(oldIndex + 1, movingDeck);

        view.getLblSave().setVisible(false);
        dumpDecksIntoList();
        view.getLstRight().setSelectedIndex(oldIndex + 1);
    }

    private void dumpDecksIntoList() {
        final List<String> names = new ArrayList<String>();

        for (final Deck d : workingDecks) {
            names.add(d.getName());
        }

        view.getLstRight().setListData(names.toArray(ArrayUtils.EMPTY_STRING_ARRAY));
    }

    private boolean saveGauntlet() {
        final String name = view.getTxfFilename().getText();
        final GauntletData gd;

        // Warn if no name
        if (name.equals(GauntletIO.TXF_PROMPT) || name.isEmpty()) {
            JOptionPane.showMessageDialog(null,
                    "Please name your gauntlet using the 'Gauntlet Name' box.",
                    "Save Error!",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }

        final File f = new File(NewConstants.GAUNTLET_DIR.userPrefLoc + name + ".dat");
        // Confirm if overwrite
        if (f.exists()) {
            final int m = JOptionPane.showConfirmDialog(null,
                    "There is already a gauntlet named '" + name + "'.\n"
                    + "All progress and data will be overwritten. Continue?",
                    "Overwrite Gauntlet?",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);

            if (m == JOptionPane.NO_OPTION) { return false; }
            gd = GauntletIO.loadGauntlet(f);
        }
        // Confirm if a new gauntlet will be created
        else {
            final int m = JOptionPane.showConfirmDialog(null,
                    "This will create a new gauntlet named '" + name + "'. Continue?",
                    "Create Gauntlet?",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);

            if (m == JOptionPane.NO_OPTION) { return false; }
            gd = new GauntletData();
        }

        final List<String> names = new ArrayList<String>();
        for (final Deck d : workingDecks) {
            names.add(d.getName());
        }

        gd.setEventNames(names);
        gd.setDecks(workingDecks);
        gd.setName(name);
        gd.reset();

        view.getLblSave().setVisible(false);

        return true;
    }

    private boolean openGauntlet() {
        final File file;
        final JFileChooser open = new JFileChooser(openStartDir);
        open.setDialogTitle("Import Deck");
        open.addChoosableFileFilter(this.filterDAT);
        final int returnVal = open.showOpenDialog(null);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            file = open.getSelectedFile();
        }
        else {
            return false;
        }

        final GauntletData gd = GauntletIO.loadGauntlet(file);

        this.workingDecks.clear();
        this.workingDecks.addAll(gd.getDecks());

        view.getTxfFilename().setText(file.getName().substring(0, file.getName().lastIndexOf('.')));
        dumpDecksIntoList();
        return true;
    }

    private boolean newGauntlet() {
        workingDecks.clear();
        dumpDecksIntoList();
        view.getTxfFilename().setText(GauntletIO.TXF_PROMPT);
        return true;
    }
}
