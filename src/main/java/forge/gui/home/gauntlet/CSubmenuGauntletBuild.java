package forge.gui.home.gauntlet;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.ListSelectionModel;
import javax.swing.filechooser.FileFilter;

import org.apache.commons.lang3.ArrayUtils;

import forge.Command;
import forge.Singletons;
import forge.deck.Deck;
import forge.deck.DeckgenUtil;
import forge.deck.DeckgenUtil.DeckTypes;
import forge.deck.generate.GenerateThemeDeck;
import forge.game.player.PlayerType;
import forge.gauntlet.GauntletData;
import forge.gauntlet.GauntletIO;
import forge.gui.framework.ICDoc;
import forge.quest.QuestController;
import forge.quest.QuestEvent;
import forge.util.storage.IStorage;

/** 
 * Controls the "build gauntlet" submenu in the home UI.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */

@SuppressWarnings("serial")
public enum CSubmenuGauntletBuild implements ICDoc {
    /** */
    SINGLETON_INSTANCE;

    private final VSubmenuGauntletBuild view = VSubmenuGauntletBuild.SINGLETON_INSTANCE;
    private final List<Deck> workingDecks = new ArrayList<Deck>();
    private File previousDirectory = null;
    private File openStartDir = new File(GauntletIO.DIR_GAUNTLETS);

    private final FileFilter filterDAT = new FileFilter() {
        @Override
        public boolean accept(final File f) {
            if (f.isDirectory()) {
                return true;
            }

            if (!f.getName().matches(GauntletIO.REGEX_LOCKED)
                    && !f.getName().matches(GauntletIO.REGEX_QUICK)) {
                return true;
            }

            return false;
        }

        @Override
        public String getDescription() {
            return "Forge data file .dat";
        }
    };

    private final MouseAdapter madDecklist = new MouseAdapter() {
        @Override
        public void mouseClicked(final MouseEvent e) {
            if (e.getClickCount() == 2) {
                if (view.getRadColorDecks().isSelected()) { return; }
                if (view.getRadThemeDecks().isSelected()) { return; }

                DeckgenUtil.showDecklist(((JList) e.getSource())); }
        }
    };

    //private final KeyAdapter kadSearch = new KeyAdapter() { @Override
        //public void keyPressed(final KeyEvent e) { search(); } };

    private final Command cmdAddDeck = new Command() { @Override
        public void execute() { addDeck(); } };

    private final Command cmdRemoveDeck = new Command() { @Override
        public void execute() { removeDeck(); } };

    private final Command cmdDeckUp = new Command() { @Override
        public void execute() { deckUp(); } };

    private final Command cmdDeckDown = new Command() { @Override
        public void execute() { deckDown(); } };

    private final Command cmdSave = new Command() { @Override
        public void execute() { saveGauntlet(); } };

    private final Command cmdNew = new Command() { @Override
        public void execute() { newGauntlet(); } };

    private final Command cmdOpen = new Command() { @Override
        public void execute() { openGauntlet(); } };

    /* (non-Javadoc)
     * @see forge.gui.home.ICSubmenu#initialize()
     */
    @Override
    public void update() {
        // Nothing to see here...
    }

    /* (non-Javadoc)
     * @see forge.gui.home.ICSubmenu#initialize()
     */
    @Override
    public void initialize() {
        final ActionListener deckUpdate = new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent arg0) {
            updateDecks(); }
        };

        view.getLstRight().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        view.getLstLeft().addMouseListener(madDecklist);

        // Deck list and radio button event handling
        view.getRadUserDecks().setSelected(true);

        view.getRadQuestDecks().addActionListener(deckUpdate);
        view.getRadColorDecks().addActionListener(deckUpdate);
        view.getRadThemeDecks().addActionListener(deckUpdate);
        view.getRadUserDecks().addActionListener(deckUpdate);

        view.getBtnRight().setCommand(cmdAddDeck);
        view.getBtnLeft().setCommand(cmdRemoveDeck);
        view.getBtnUp().setCommand(cmdDeckUp);
        view.getBtnDown().setCommand(cmdDeckDown);

        view.getBtnSave().setCommand(cmdSave);
        view.getBtnOpen().setCommand(cmdOpen);
        view.getBtnNew().setCommand(cmdNew);
        updateDecks();
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#getCommandOnSelect()
     */
    @Override
    public Command getCommandOnSelect() {
        return null;
    }

    /** Handles all control for "custom" radio button click. */
    private void updateDecks() {
        if (view.getRadUserDecks().isSelected()) {
            updateUserDecks();
        }
        else if (view.getRadQuestDecks().isSelected()) {
            updateQuestDecks();
        }
        else if (view.getRadThemeDecks().isSelected()) {
            updateThemeDecks();
        }
        else if (view.getRadColorDecks().isSelected()) {
            updateColorDecks();
        }
    }

    private void updateUserDecks() {
        final List<String> customNames = new ArrayList<String>();
        final IStorage<Deck> allDecks = Singletons.getModel().getDecks().getConstructed();
        for (final Deck d : allDecks) { customNames.add(d.getName()); }

        view.getLstLeft().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        view.getLstLeft().setListData(customNames.toArray(ArrayUtils.EMPTY_STRING_ARRAY));
        view.getLstLeft().setName(DeckTypes.CUSTOM.toString());

        // Init first in list
        view.getLstLeft().setSelectedIndex(0);
    }

    /** Handles all control for "quest event" radio button click. */
    private void updateQuestDecks() {
        final List<String> eventNames = new ArrayList<String>();
        QuestController quest = Singletons.getModel().getQuest();

        for (final QuestEvent e : quest.getDuelsManager().getAllDuels()) {
            eventNames.add(e.getEventDeck().getName());
        }

        for (final QuestEvent e : quest.getChallengesManager().getAllChallenges()) {
            eventNames.add(e.getEventDeck().getName());
        }

        view.getLstLeft().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        view.getLstLeft().setListData(eventNames.toArray(ArrayUtils.EMPTY_STRING_ARRAY));
        view.getLstLeft().setName(DeckTypes.QUESTEVENTS.toString());

        // Init first in list
        view.getLstLeft().setSelectedIndex(0);
    }

    /** Handles all control for "themes" radio button click. */
    private void updateThemeDecks() {
        final List<String> themeNames = new ArrayList<String>();
        for (final String s : GenerateThemeDeck.getThemeNames()) { themeNames.add(s); }

        view.getLstLeft().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        view.getLstLeft().setListData(themeNames.toArray(ArrayUtils.EMPTY_STRING_ARRAY));
        view.getLstLeft().setName(DeckTypes.THEMES.toString());

        // Init first in list
        view.getLstLeft().setSelectedIndex(0);
    }

    /** Handles all control for "colors" radio button click. */
    private void updateColorDecks() {
        view.getLstLeft().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        view.getLstLeft().setListData(new String[] {"Random 1", "Random 2", "Random 3",
                "Random 4", "Black", "Blue", "Green", "Red", "White"});
        view.getLstLeft().setName(DeckTypes.COLORS.toString());

        // Init basic two color deck
        view.getLstLeft().setSelectedIndices(new int[]{0, 1});
    }

    private void addDeck() {
        final Deck deckToAdd;
        final String[] selection = Arrays.asList(
                view.getLstLeft().getSelectedValues()).toArray(new String[0]);

        if (selection.length == 0) { return; }

        if (view.getRadColorDecks().isSelected()) {
            if (!DeckgenUtil.colorCheck(selection)) { return; }
            deckToAdd = DeckgenUtil.buildColorDeck(selection, PlayerType.HUMAN);
        }
        else if (view.getRadQuestDecks().isSelected()) {
            deckToAdd = DeckgenUtil.buildQuestDeck(selection);
        }
        else if (view.getRadThemeDecks().isSelected()) {
            deckToAdd = DeckgenUtil.buildThemeDeck(selection);
        }
        else {
            deckToAdd = DeckgenUtil.buildCustomDeck(selection);
        }

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

        final File f = new File(GauntletIO.DIR_GAUNTLETS + name + ".dat");
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
        gd.setActiveFile(f);
        gd.reset();

        view.getLblSave().setVisible(false);

        return true;
    }

    private boolean openGauntlet() {
        /** */
        final File file;
        final JFileChooser open = new JFileChooser(previousDirectory);
        open.setDialogTitle("Import Deck");
        open.addChoosableFileFilter(this.filterDAT);
        open.setCurrentDirectory(openStartDir);
        final int returnVal = open.showOpenDialog(null);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            file = open.getSelectedFile();
            previousDirectory = file.getParentFile();
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
