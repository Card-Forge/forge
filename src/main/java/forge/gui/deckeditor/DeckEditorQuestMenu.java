package forge.gui.deckeditor;


import forge.AllZone;
import forge.Command;
import forge.Constant;
import forge.card.CardRules;
import forge.card.CardDb;
import forge.card.CardPool;
import forge.card.CardPoolView;
import forge.card.CardPrinted;
import forge.deck.Deck;
import forge.deck.DeckManager;
import forge.error.ErrorViewer;
import forge.gui.GuiUtils;
import forge.gui.ListChooser;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;

import org.apache.commons.lang3.StringUtils;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;


//presumes AllZone.getQuestData() is not null
/**
 * <p>Gui_Quest_DeckEditor_Menu class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class DeckEditorQuestMenu extends JMenuBar {
    /** Constant <code>serialVersionUID=-4052319220021158574L</code>. */
    private static final long serialVersionUID = -4052319220021158574L;

    //this should be false in the public version
    //if true, the Quest Deck editor will let you edit the computer's decks
    private final boolean canEditComputerDecks;

    /** Constant <code>deckEditorName="Deck Editor"</code>. */
    private static final String deckEditorName = "Deck Editor";

    //used for import and export, try to made the gui user friendly
    /** Constant <code>previousDirectory</code>. */
    private static File previousDirectory = null;

    private Command exitCommand;
    private forge.quest.data.QuestData questData;
    private Deck currentDeck;

    //the class DeckDisplay is in the file "Gui_DeckEditor_Menu.java"
    private DeckDisplay deckDisplay;


    /**
     * <p>Constructor for Gui_Quest_DeckEditor_Menu.</p>
     *
     * @param d a {@link forge.gui.deckeditor.DeckDisplay} object.
     * @param exit a {@link forge.Command} object.
     */
    public DeckEditorQuestMenu(final DeckDisplay d, final Command exit) {
        //is a file named "edit" in this directory
        //lame but it works, I don't like 2 versions of Forge floating around
        //one that lets you edit the AI decks and one that doesn't
        File f = new File("edit");
        if (f.exists()) {
            canEditComputerDecks = true;
        } else {
            canEditComputerDecks = false;
        }

        deckDisplay = d;
        d.setTitle(deckEditorName);

        questData = AllZone.getQuestData();

        exitCommand = exit;

        setupMenu();
    }



    /**
     * <p>addImportExport.</p>
     *
     * @param menu a {@link javax.swing.JMenu} object.
     * @param isHumanMenu a boolean.
     */
    private void addImportExport(final JMenu menu, final boolean isHumanMenu) {
        JMenuItem import2 = new JMenuItem("Import");
        JMenuItem export = new JMenuItem("Export");

        import2.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent a) {
                importDeck(); //importDeck(isHumanMenu);
            }
        }); //import

        export.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent a) {
                exportDeck();
            }
        }); //export

        menu.add(import2);
        menu.add(export);

    } //addImportExport()

    /**
     * <p>exportDeck.</p>
     */
    private void exportDeck() {
        File filename = getExportFilename();

        if (filename == null) {
            return;
        }

        //write is an Object variable because you might just
        //write one Deck object
        Deck deck = cardPoolToDeck(deckDisplay.getBottom());

        deck.setName(filename.getName());

        try {
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(filename));
            out.writeObject(deck);
            out.flush();
            out.close();
        } catch (Exception ex) {
            ErrorViewer.showError(ex);
            throw new RuntimeException("Gui_Quest_DeckEditor_Menu : exportDeck() error, " + ex);
        }

        exportDeckText(getExportDeckText(deck), filename.getAbsolutePath());

    } //exportDeck()

    /**
     * <p>exportDeckText.</p>
     *
     * @param deckText a {@link java.lang.String} object.
     * @param filename a {@link java.lang.String} object.
     */
    private void exportDeckText(final String deckText, String filename) {

        //remove ".deck" extension
        int cut = filename.indexOf(".");
        filename = filename.substring(0, cut);

        try {
            FileWriter writer = new FileWriter(filename + ".txt");
            writer.write(deckText);

            writer.flush();
            writer.close();
        } catch (Exception ex) {
            ErrorViewer.showError(ex);
            throw new RuntimeException("Gui_Quest_DeckEditor_Menu : exportDeckText() error, " + ex.getMessage()
                    + " : " + Arrays.toString(ex.getStackTrace()));
        }
    } //exportDeckText()

    /**
     * <p>getExportDeckText.</p>
     *
     * @param aDeck a {@link forge.deck.Deck} object.
     * @return a {@link java.lang.String} object.
     */
    private String getExportDeckText(final Deck aDeck) {
        //convert Deck into CardList
        CardPoolView all = aDeck.getMain();
        //sort by card name
        Collections.sort(all.getOrderedList(), TableSorter.byNameThenSet);

        StringBuffer sb = new StringBuffer();
        String newLine = "\r\n";

        sb.append(String.format("%d Total Cards%n%n", all.countAll()));

        //creatures
        sb.append(String.format("%d Creatures%n-------------%n", CardRules.Predicates.Presets.isCreature.aggregate(all, CardPoolView.fnToCard, CardPoolView.fnToCount)));
        for (Entry<CardPrinted, Integer> e : CardRules.Predicates.Presets.isCreature.select(all, CardPoolView.fnToCard)) {
            sb.append(String.format("%d x %s%n", e.getValue(), e.getKey().getName()));
        }

        //spells
        sb.append(String.format("%d Spells%n----------%n", CardRules.Predicates.Presets.isNonCreatureSpell.aggregate(all, CardPoolView.fnToCard, CardPoolView.fnToCount)));
        for (Entry<CardPrinted, Integer> e : CardRules.Predicates.Presets.isNonCreatureSpell.select(all, CardPoolView.fnToCard)) {
            sb.append(String.format("%d x %s%n", e.getValue(), e.getKey().getName()));
        }

        //lands
        sb.append(String.format("%d Land%n--------%n", CardRules.Predicates.Presets.isLand.aggregate(all, CardPoolView.fnToCard, CardPoolView.fnToCount)));
        for (Entry<CardPrinted, Integer> e : CardRules.Predicates.Presets.isLand.select(all, CardPoolView.fnToCard)) {
            sb.append(String.format("%d x %s%n", e.getValue(), e.getKey().getName()));
        }

        sb.append(newLine);

        return sb.toString();
    } //getExportDeckText

    /**
     * <p>getFileFilter.</p>
     *
     * @return a {@link javax.swing.filechooser.FileFilter} object.
     */
    private FileFilter getFileFilter() {
        FileFilter filter = new FileFilter() {
            @Override
            public boolean accept(final File f) {
                return f.getName().endsWith(".dck") || f.isDirectory();
            }

            @Override
            public String getDescription() {
                return "Deck File .dck";
            }
        };

        return filter;
    } //getFileFilter()

    /**
     * <p>getExportFilename.</p>
     *
     * @return a {@link java.io.File} object.
     */
    private File getExportFilename() {
        //Object o = null; // unused

        JFileChooser save = new JFileChooser(previousDirectory);

        save.setDialogTitle("Export Deck Filename");
        save.setDialogType(JFileChooser.SAVE_DIALOG);
        save.addChoosableFileFilter(getFileFilter());
        save.setSelectedFile(new File(currentDeck.getName() + ".deck"));

        int returnVal = save.showSaveDialog(null);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = save.getSelectedFile();
            String check = file.getAbsolutePath();

            previousDirectory = file.getParentFile();

            if (check.endsWith(".deck")) {
                return file;
            } else {
                return new File(check + ".deck");
            }
        }

        return null;
    } //getExportFilename()

    /**
     * <p>importDeck.</p>
     */
    private void importDeck() {
        File file = getImportFilename();

        if (file == null) {
        } else if (file.getName().endsWith(".dck")) {
            try {
                Deck newDeck = DeckManager.readDeck(file);
                questData.addDeck(newDeck);

                CardPool cardpool = new CardPool(questData.getCardpool());
                CardPool decklist = new CardPool();
                for (Entry<CardPrinted, Integer> s : newDeck.getMain()) {
                    CardPrinted cp = s.getKey();
                    decklist.add(cp, s.getValue());
                    cardpool.add(cp, s.getValue());
                    questData.getCardpool().add(cp, s.getValue());
                }
                deckDisplay.setDecks(cardpool, decklist);

            } catch (Exception ex) {
                ErrorViewer.showError(ex);
                throw new RuntimeException("Gui_DeckEditor_Menu : importDeck() error, " + ex);
            }
        }

    } //importDeck()

    /**
     * <p>getImportFilename.</p>
     *
     * @return a {@link java.io.File} object.
     */
    private File getImportFilename() {
        JFileChooser chooser = new JFileChooser(previousDirectory);

        chooser.addChoosableFileFilter(getFileFilter());
        int returnVal = chooser.showOpenDialog(null);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            previousDirectory = file.getParentFile();
            return file;
        }

        return null;
    } //openFileDialog()



    private final ActionListener addCardActionListener = new ActionListener() {
        public void actionPerformed(final ActionEvent a) {

            // Provide a model here: all unique cards to be displayed by only name (unlike default toString)
            Iterable<CardPrinted> uniqueCards = CardDb.instance().getAllUniqueCards();
            List<String> cards = new ArrayList<String>();
            for (CardPrinted c : uniqueCards) { cards.add(c.getName()); }
            Collections.sort(cards);

            // use standard forge's list selection dialog
            ListChooser<String> c = new ListChooser<String>("Cheat - Add Card to Your Cardpool", 0, 1, cards);
            if (c.show()) {
                String cardName = c.getSelectedValue();
                DeckEditorQuest g = (DeckEditorQuest) deckDisplay;
                g.addCheatCard(CardDb.instance().getCard(cardName));
            }
        }
    };

    private final ActionListener openDeckActionListener = new ActionListener() {
        public void actionPerformed(final ActionEvent a) {
            String deckName = getUserInput_OpenDeck(questData.getDeckNames());

            //check if user selected "cancel"
            if (StringUtils.isBlank(deckName)) { return; }

            setPlayerDeckName(deckName);
            CardPool cards = new CardPool(questData.getCardpool().getView());
            CardPoolView deck = questData.getDeck(deckName).getMain();

            // show in pool all cards except ones used in deck
            cards.removeAll(deck);
            deckDisplay.setDecks(cards,  deck);
        }
    };

    private final ActionListener newDeckActionListener = new ActionListener() {
        public void actionPerformed(final ActionEvent a) {
            deckDisplay.setDecks(questData.getCardpool().getView(), new CardPool());
            setPlayerDeckName("");
        }
    };

    private final ActionListener renameDeckActionListener = new ActionListener() {
        public void actionPerformed(final ActionEvent a) {
            String deckName = getUserInput_GetDeckName(questData.getDeckNames());

            //check if user cancels
            if (StringUtils.isBlank(deckName)) { return; }

            //is the current deck already saved and in QuestData?
            if (questData.getDeckNames().contains(currentDeck.getName())) {
                questData.removeDeck(currentDeck.getName());
            }

            currentDeck.setName(deckName);

            Deck deck = cardPoolToDeck(deckDisplay.getBottom());
            deck.setName(deckName);
            questData.addDeck(deck);

            setPlayerDeckName(deckName);
        }
    };

    private final ActionListener saveDeckActionListener = new ActionListener() {
        public void actionPerformed(final ActionEvent a) {
            String name = currentDeck.getName();

            //check to see if name is set
            if (name.equals("")) {
                name = getUserInput_GetDeckName(questData.getDeckNames());

                //check if user cancels
                if (name.equals("")) {
                    return;
                }
            }

            setPlayerDeckName(name);

            Deck deck = cardPoolToDeck(deckDisplay.getBottom());
            deck.setName(name);

            questData.addDeck(deck);
        }
    };

    private final ActionListener copyDeckActionListener = new ActionListener() {
        public void actionPerformed(final ActionEvent a) {
            String name = getUserInput_GetDeckName(questData.getDeckNames());

            //check if user cancels
            if (name.equals("")) {
                return;
            }

            setPlayerDeckName(name);

            Deck deck = cardPoolToDeck(deckDisplay.getBottom());
            deck.setName(name);

            questData.addDeck(deck);
        }
    };

    private final ActionListener deleteDeckActionListener = new ActionListener() {
        public void actionPerformed(final ActionEvent a) {
            if (currentDeck.getName().equals("")) {
                return;
            }

            int check = JOptionPane.showConfirmDialog(null, "Do you really want to delete this deck?",
                    "Delete", JOptionPane.YES_NO_OPTION);
            if (check == JOptionPane.NO_OPTION) {
                return; //stop here
            }

            questData.removeDeck(currentDeck.getName());

            //show card pool
            deckDisplay.setDecks(questData.getCardpool().getView(), new CardPool());

            setPlayerDeckName("");
        }
    };

    //the usual menu options that will be used
    /**
     * <p>setupMenu.</p>
     */
    private void setupMenu() {
        JMenuItem openDeck = new JMenuItem("Open");
        JMenuItem newDeck = new JMenuItem("New");
        JMenuItem rename = new JMenuItem("Rename");
        JMenuItem save = new JMenuItem("Save");
        JMenuItem copy = new JMenuItem("Copy");
        JMenuItem delete = new JMenuItem("Delete");
        JMenuItem exit = new JMenuItem("Exit");

        JMenuItem addCard = new JMenuItem("Cheat - Add Card");


        addCard.addActionListener(addCardActionListener);
        openDeck.addActionListener(openDeckActionListener);
        newDeck.addActionListener(newDeckActionListener);
        rename.addActionListener(renameDeckActionListener);
        save.addActionListener(saveDeckActionListener);
        copy.addActionListener(copyDeckActionListener);
        delete.addActionListener(deleteDeckActionListener);


        //human
        exit.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent a) {
                DeckEditorQuestMenu.this.close();
            }
        });

        JMenu deckMenu = new JMenu("Deck");
        deckMenu.add(openDeck);
        deckMenu.add(newDeck);
        deckMenu.add(rename);
        deckMenu.add(save);
        deckMenu.add(copy);

        if (Constant.Runtime.DevMode[0]) {
            deckMenu.addSeparator();
            deckMenu.add(addCard);
        }

        deckMenu.addSeparator();
        addImportExport(deckMenu, true);

        deckMenu.addSeparator();
        deckMenu.add(delete);
        deckMenu.addSeparator();
        deckMenu.add(exit);

        this.add(deckMenu);

    }

    /**
     * <p>convertCardPoolToDeck.</p>
     *
     * @param list a {@link forge.CardPool} object.
     * @return a {@link forge.deck.Deck} object.
     */
    private Deck cardPoolToDeck(final CardPoolView list) {
        //put CardPool into Deck main
        Deck deck = new Deck(Constant.GameType.Sealed);
        deck.addMain(list);
        return deck;
    }

    //needs to be public because Gui_Quest_DeckEditor.show(Command) uses it
    /**
     * <p>setHumanPlayer.</p>
     *
     * @param deckName a {@link java.lang.String} object.
     */
    public final void setPlayerDeckName(final String deckName) {
        //the gui uses this, Gui_Quest_DeckEditor
        currentDeck = new Deck(Constant.GameType.Sealed);
        currentDeck.setName(deckName);

        deckDisplay.setTitle(deckEditorName + " - " + deckName);
    }

    //only accepts numbers, letters or dashes up to 20 characters in length
    /**
     * <p>cleanString.</p>
     *
     * @param in a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    private String cleanString(final String in) {
        StringBuffer out = new StringBuffer();
        char[] c = in.toCharArray();

        for (int i = 0; i < c.length && i < 20; i++) {
            if (Character.isLetterOrDigit(c[i]) || c[i] == '-' || c[i] == '_' || c[i] == ' ') { out.append(c[i]); }
        }

        return out.toString();
    }

    //if user cancels, returns ""
    /**
     * <p>getUserInput_GetDeckName.</p>
     *
     * @param nameList a {@link java.util.List} object.
     * @return a {@link java.lang.String} object.
     */
    private String getUserInput_GetDeckName(final List<String> nameList) {
        Object o = JOptionPane.showInputDialog(null, "", "Deck Name", JOptionPane.OK_CANCEL_OPTION);

        if (o == null) {
            return "";
        }

        String deckName = cleanString(o.toString());

        if (nameList.contains(deckName) || deckName.equals("")) {
            JOptionPane.showMessageDialog(null, "Please pick another deck name, a deck currently has that name.");
            return getUserInput_GetDeckName(nameList);
        }

        return deckName;
    } //getUserInput_GetDeckName()


    //if user cancels, it will return ""
    /**
     * <p>getUserInput_OpenDeck.</p>
     *
     * @param deckNameList a {@link java.util.List} object.
     * @return a {@link java.lang.String} object.
     */
    private String getUserInput_OpenDeck(final List<String> deckNameList) {
        List<String> choices = deckNameList;
        if (choices.size() == 0) {
            JOptionPane.showMessageDialog(null, "No decks found", "Open Deck", JOptionPane.PLAIN_MESSAGE);
            return "";
        }

        //Object o = JOptionPane.showInputDialog(null, "Deck Name", "Open Deck", JOptionPane.OK_CANCEL_OPTION, null,
        //        choices.toArray(), choices.toArray()[0]);
        Object o = GuiUtils.getChoiceOptional("Select Deck", choices.toArray());

        if (o == null) {
            return "";
        }

        return o.toString();
    } //getUserInput_OpenDeck()

    //used by Gui_Quest_DeckEditor
    /**
     * <p>close.</p>
     */
    public final void close() {
        exitCommand.execute();
    }

    //used by Gui_Quest_DeckEditor
    /**
     * <p>getDeckName.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public final String getDeckName() {
        return currentDeck.getName();
    }

}
