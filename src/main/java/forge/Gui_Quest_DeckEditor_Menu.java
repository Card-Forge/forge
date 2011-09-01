package forge;


import forge.deck.Deck;
import forge.deck.DeckManager;
import forge.error.ErrorViewer;
import forge.gui.GuiUtils;
import forge.quest.data.QuestBattleManager;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


//presumes AllZone.getQuestData() is not null
/**
 * <p>Gui_Quest_DeckEditor_Menu class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class Gui_Quest_DeckEditor_Menu extends JMenuBar {
    /** Constant <code>serialVersionUID=-4052319220021158574L</code> */
    private static final long serialVersionUID = -4052319220021158574L;

    //this should be false in the public version
    //if true, the Quest Deck editor will let you edit the computer's decks
    private final boolean canEditComputerDecks;

    /** Constant <code>deckEditorName="Deck Editor"</code> */
    private static final String deckEditorName = "Deck Editor";

    //used for import and export, try to made the gui user friendly
    /** Constant <code>previousDirectory</code> */
    private static File previousDirectory = null;

    private Command exitCommand;
    private forge.quest.data.QuestData questData;
    private Deck currentDeck;

    //the class DeckDisplay is in the file "Gui_DeckEditor_Menu.java"
    private DeckDisplay deckDisplay;


    /**
     * <p>Constructor for Gui_Quest_DeckEditor_Menu.</p>
     *
     * @param d a {@link forge.DeckDisplay} object.
     * @param exit a {@link forge.Command} object.
     */
    public Gui_Quest_DeckEditor_Menu(DeckDisplay d, Command exit) {
        //is a file named "edit" in this directory
        //lame but it works, I don't like 2 versions of MTG Forge floating around
        //one that lets you edit the AI decks and one that doesn't
        File f = new File("edit");
        if (f.exists()) canEditComputerDecks = true;
        else canEditComputerDecks = false;


        deckDisplay = d;
        d.setTitle(deckEditorName);

        questData = AllZone.getQuestData();

        exitCommand = exit;

        setupMenu();
        setupFilterMenu();

        if (canEditComputerDecks) setupComputerMenu();
    }

    /**
     * <p>setupFilterMenu.</p>
     */
    private void setupFilterMenu() {
        JMenuItem filter = new JMenuItem("New filter");
        JMenuItem clearfilter = new JMenuItem("Clear filter");
        JMenu menu = new JMenu("Filter");
        menu.add(filter);
        menu.add(clearfilter);
        this.add(menu);

        filter.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                Gui_Quest_DeckEditor g = (Gui_Quest_DeckEditor) deckDisplay;
                if (g.stCardList == null) {
                    g.blackCheckBox.setSelected(true);
                    g.blackCheckBox.setEnabled(true);
                    g.blueCheckBox.setSelected(true);
                    g.blueCheckBox.setEnabled(true);
                    g.greenCheckBox.setSelected(true);
                    g.greenCheckBox.setEnabled(true);
                    g.redCheckBox.setSelected(true);
                    g.redCheckBox.setEnabled(true);
                    g.whiteCheckBox.setSelected(true);
                    g.whiteCheckBox.setEnabled(true);
                    g.colorlessCheckBox.setSelected(true);
                    g.colorlessCheckBox.setEnabled(true);
                    g.artifactCheckBox.setSelected(true);
                    g.artifactCheckBox.setEnabled(true);
                    g.creatureCheckBox.setSelected(true);
                    g.creatureCheckBox.setEnabled(true);
                    g.enchantmentCheckBox.setSelected(true);
                    g.enchantmentCheckBox.setEnabled(true);
                    g.instantCheckBox.setSelected(true);
                    g.instantCheckBox.setEnabled(true);
                    g.landCheckBox.setSelected(true);
                    g.landCheckBox.setEnabled(true);
                    g.planeswalkerCheckBox.setSelected(true);
                    g.planeswalkerCheckBox.setEnabled(true);
                    g.sorceryCheckBox.setSelected(true);
                    g.sorceryCheckBox.setEnabled(true);
                    g.stCardList = g.getTop();
                    GuiQuestFilter filt = new GuiQuestFilter(g, deckDisplay);
                    g.setEnabled(false);
                    g.filterUsed = true;
                    filt.setVisible(true);
                } else {
                    g.blackCheckBox.setSelected(true);
                    g.blackCheckBox.setEnabled(true);
                    g.blueCheckBox.setSelected(true);
                    g.blueCheckBox.setEnabled(true);
                    g.greenCheckBox.setSelected(true);
                    g.greenCheckBox.setEnabled(true);
                    g.redCheckBox.setSelected(true);
                    g.redCheckBox.setEnabled(true);
                    g.whiteCheckBox.setSelected(true);
                    g.whiteCheckBox.setEnabled(true);
                    g.colorlessCheckBox.setSelected(true);
                    g.colorlessCheckBox.setEnabled(true);
                    g.artifactCheckBox.setSelected(true);
                    g.artifactCheckBox.setEnabled(true);
                    g.creatureCheckBox.setSelected(true);
                    g.creatureCheckBox.setEnabled(true);
                    g.enchantmentCheckBox.setSelected(true);
                    g.enchantmentCheckBox.setEnabled(true);
                    g.instantCheckBox.setSelected(true);
                    g.instantCheckBox.setEnabled(true);
                    g.landCheckBox.setSelected(true);
                    g.landCheckBox.setEnabled(true);
                    g.planeswalkerCheckBox.setSelected(true);
                    g.planeswalkerCheckBox.setEnabled(true);
                    g.sorceryCheckBox.setSelected(true);
                    g.sorceryCheckBox.setEnabled(true);
                    GuiQuestFilter filt = new GuiQuestFilter(g, deckDisplay);
                    g.filterUsed = true;
                    g.setEnabled(false);
                    filt.setVisible(true);
                }
            }
        });
        clearfilter.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {

                Gui_Quest_DeckEditor g = (Gui_Quest_DeckEditor) deckDisplay;
                if (g.stCardList == null) {
                    g.blackCheckBox.setSelected(true);
                    g.blackCheckBox.setEnabled(true);
                    g.blueCheckBox.setSelected(true);
                    g.blueCheckBox.setEnabled(true);
                    g.greenCheckBox.setSelected(true);
                    g.greenCheckBox.setEnabled(true);
                    g.redCheckBox.setSelected(true);
                    g.redCheckBox.setEnabled(true);
                    g.whiteCheckBox.setSelected(true);
                    g.whiteCheckBox.setEnabled(true);
                    g.colorlessCheckBox.setSelected(true);
                    g.colorlessCheckBox.setEnabled(true);
                    g.artifactCheckBox.setSelected(true);
                    g.artifactCheckBox.setEnabled(true);
                    g.creatureCheckBox.setSelected(true);
                    g.creatureCheckBox.setEnabled(true);
                    g.enchantmentCheckBox.setSelected(true);
                    g.enchantmentCheckBox.setEnabled(true);
                    g.instantCheckBox.setSelected(true);
                    g.instantCheckBox.setEnabled(true);
                    g.landCheckBox.setSelected(true);
                    g.landCheckBox.setEnabled(true);
                    g.planeswalkerCheckBox.setSelected(true);
                    g.planeswalkerCheckBox.setEnabled(true);
                    g.sorceryCheckBox.setSelected(true);
                    g.sorceryCheckBox.setEnabled(true);
                    g.filterUsed = false;
                } else {
                    g.blackCheckBox.setSelected(true);
                    g.blackCheckBox.setEnabled(true);
                    g.blueCheckBox.setSelected(true);
                    g.blueCheckBox.setEnabled(true);
                    g.greenCheckBox.setSelected(true);
                    g.greenCheckBox.setEnabled(true);
                    g.redCheckBox.setSelected(true);
                    g.redCheckBox.setEnabled(true);
                    g.whiteCheckBox.setSelected(true);
                    g.whiteCheckBox.setEnabled(true);
                    g.colorlessCheckBox.setSelected(true);
                    g.colorlessCheckBox.setEnabled(true);
                    g.artifactCheckBox.setSelected(true);
                    g.artifactCheckBox.setEnabled(true);
                    g.creatureCheckBox.setSelected(true);
                    g.creatureCheckBox.setEnabled(true);
                    g.enchantmentCheckBox.setSelected(true);
                    g.enchantmentCheckBox.setEnabled(true);
                    g.instantCheckBox.setSelected(true);
                    g.instantCheckBox.setEnabled(true);
                    g.landCheckBox.setSelected(true);
                    g.landCheckBox.setEnabled(true);
                    g.planeswalkerCheckBox.setSelected(true);
                    g.planeswalkerCheckBox.setEnabled(true);
                    g.sorceryCheckBox.setSelected(true);
                    g.sorceryCheckBox.setEnabled(true);
                    g.filterUsed = false;
                    deckDisplay.updateDisplay(g.stCardList, deckDisplay.getBottom());
                }


            }
        });

    }


    /**
     * <p>addImportExport.</p>
     *
     * @param menu a {@link javax.swing.JMenu} object.
     * @param isHumanMenu a boolean.
     */
    private void addImportExport(JMenu menu, final boolean isHumanMenu) {
        JMenuItem import2 = new JMenuItem("Import");
        JMenuItem export = new JMenuItem("Export");

        import2.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent a) {
                importDeck();//importDeck(isHumanMenu);
            }
        });//import

        export.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent a) {
                exportDeck();
            }
        });//export

        menu.add(import2);
        menu.add(export);

    }//addImportExport()

    /**
     * <p>exportDeck.</p>
     */
    private void exportDeck() {
        File filename = getExportFilename();

        if (filename == null) return;

        //write is an Object variable because you might just
        //write one Deck object
        Deck deck = convertCardListToDeck(deckDisplay.getBottom());
        ;
        Object write = deck;

        deck.setName(filename.getName());


        try {
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(filename));
            out.writeObject(write);
            out.flush();
            out.close();
        } catch (Exception ex) {
            ErrorViewer.showError(ex);
            throw new RuntimeException("Gui_Quest_DeckEditor_Menu : exportDeck() error, " + ex);
        }

        exportDeckText(getExportDeckText(deck), filename.getAbsolutePath());

    }//exportDeck()

    // TableSorter type safety
    /**
     * <p>getExportDeckText.</p>
     *
     * @param aDeck a {@link forge.deck.Deck} object.
     * @return a {@link java.lang.String} object.
     */
    private String getExportDeckText(Deck aDeck) {
        //convert Deck into CardList
        CardList all = new CardList();
        for (int i = 0; i < aDeck.countMain(); i++) {
            String cardName = aDeck.getMain(i);
            Card c = AllZone.getCardFactory().getCard(cardName, null);

            all.add(c);
        }

        //sort by card name
        all.sort(new TableSorter(all, 1, true));

        //remove all copies of cards
        //make a singleton
        CardList noCopies = new CardList();
        for (int i = 0; i < all.size(); i++) {
            Card c = all.get(i);

            if (!noCopies.containsName(c.getName())) noCopies.add(c);
        }

        StringBuffer sb = new StringBuffer();
        String newLine = "\r\n";
        int count = 0;

        sb.append(all.size()).append(" Total Cards").append(newLine).append(newLine);

        //creatures
        sb.append(all.getType("Creature").size()).append(" Creatures").append(newLine);
        sb.append("-------------").append(newLine);

        for (int i = 0; i < noCopies.size(); i++) {
            Card c = noCopies.get(i);
            if (c.isCreature()) {
                count = all.getName(c.getName()).size();
                sb.append(count).append("x ").append(c.getName()).append(newLine);
            }
        }

        //count spells, arg! this is tough
        CardListFilter cf = new CardListFilter() {
            public boolean addCard(Card c) {
                return !(c.isCreature() || c.isLand());
            }
        };//CardListFilter
        count = all.filter(cf).size();

        //spells
        sb.append(newLine).append(count).append(" Spells").append(newLine);
        sb.append("----------").append(newLine);

        for (int i = 0; i < noCopies.size(); i++) {
            Card c = noCopies.get(i);
            if (!(c.isCreature() || c.isLand())) {
                count = all.getName(c.getName()).size();
                sb.append(count).append("x ").append(c.getName()).append(newLine);
            }
        }

        //land
        sb.append(newLine).append(all.getType("Land").size()).append(" Land").append(newLine);
        sb.append("--------").append(newLine);

        for (int i = 0; i < noCopies.size(); i++) {
            Card c = noCopies.get(i);
            if (c.isLand()) {
                count = all.getName(c.getName()).size();
                sb.append(count).append("x ").append(c.getName()).append(newLine);
            }
        }

        sb.append(newLine);

        return sb.toString();
    }//getExportDeckText

    /**
     * <p>exportDeckText.</p>
     *
     * @param deckText a {@link java.lang.String} object.
     * @param filename a {@link java.lang.String} object.
     */
    private void exportDeckText(String deckText, String filename) {

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
    }//exportDeckText()


    /**
     * <p>getFileFilter.</p>
     *
     * @return a {@link javax.swing.filechooser.FileFilter} object.
     */
    private FileFilter getFileFilter() {
        FileFilter filter = new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.getName().endsWith(".dck") || f.isDirectory();
            }

            @Override
            public String getDescription() {
                return "Deck File .dck";
            }
        };

        return filter;
    }//getFileFilter()

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

            if (check.endsWith(".deck")) return file;
            else return new File(check + ".deck");
        }

        return null;
    }//getExportFilename()

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

                CardList cardpool = new CardList();
                CardList decklist = new CardList();
                for (String s : newDeck.getMain()) {
                    Card c = null;
                    if (s.contains("|")) {
                        String split[] = s.split("\\|", 2);
                        c = AllZone.getCardFactory().getCard(split[0], null);
                        decklist.add(c);
                        cardpool.add(c);
                        //setCode = s[1];
                    } else {
                        c = AllZone.getCardFactory().getCard(s, null);
                    }

                    decklist.add(c);
                    cardpool.add(c);
                    questData.addCard(c);
                }
                for (String s : questData.getCardpool()) {
                    cardpool.add(AllZone.getCardFactory().getCard(s, null));
                }

                deckDisplay.updateDisplay(cardpool, decklist);

            } catch (Exception ex) {
                ErrorViewer.showError(ex);
                throw new RuntimeException("Gui_DeckEditor_Menu : importDeck() error, " + ex);
            }
        }

    }//importDeck()
    /*
    private void importDeck(boolean isHumanDeck) {
        File file = getImportFilename();

        if(file == null) return;

        Object check = null;

        try {
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(file));
            check = in.readObject();

            //deck migration - this is a little hard to read, because i can't just plainly reference a class in the
            //default package
            Class<?> deckConverterClass = Class.forName("DeckConverter");
            //invoke public static Object toForgeDeck(Object o) of DeckConverter
            check = deckConverterClass.getDeclaredMethod("toForgeDeck", Object.class).invoke(null, check);

            in.close();
        } catch(Exception ex) {
            ErrorViewer.showError(ex);
            throw new RuntimeException("Gui_Quest_DeckEditor_Menu : importDeck() error, " + ex);
        }

        Deck deck = (Deck) check;

        deckDisplay.setTitle(deckEditorName + " - " + deck.getName());

        CardList cardpool;

        if(isHumanDeck) {
            questData.addDeck(deck);

            //convert ArrayList of card names (Strings), into Card objects
            cardpool = new CardList();
            List<String> list = questData.getCardpool();

            for (String cardName : list) {
                //String setCode = "";
                if (cardName.contains("|")) {
                    String s[] = cardName.split("\\|", 2);
                    cardName = s[0];
                    //setCode = s[1];
                }

                cardpool.add(AllZone.getCardFactory().getCard(cardName, null));
            }
        } else {
            QuestBattleManager.addAIDeck(deck);
            cardpool = AllZone.getCardFactory().getAllCards();
        }

        //convert Deck main to CardList
        CardList deckList = new CardList();
        for(int i = 0; i < deck.countMain(); i++) {
        	String cardName = deck.getMain(i);
        	//String setCode = "";
            if (cardName.contains("|"))
            {
            	String s[] = cardName.split("\\|",2);
            	cardName = s[0];
            	//setCode = s[1];
            }

        	deckList.add(AllZone.getCardFactory().getCard(cardName, null));
        }
        //update gui
        deckDisplay.updateDisplay(cardpool, deckList);

    }//importDeck()
    */

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
    }//openFileDialog()

    //edit the AI decks
    /**
     * <p>setupComputerMenu.</p>
     */
    private void setupComputerMenu() {
        JMenuItem open = new JMenuItem("Open");
        JMenuItem new2 = new JMenuItem("New");
        JMenuItem rename = new JMenuItem("Rename");
        JMenuItem save = new JMenuItem("Save");
        JMenuItem copy = new JMenuItem("Copy");
        JMenuItem delete = new JMenuItem("Delete");
        JMenuItem exit = new JMenuItem("Exit");


        JMenuItem viewAllDecks = new JMenuItem("View All Decks");


        //AI
        viewAllDecks.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent a) {
                List<String> nameList = QuestBattleManager.getAIDeckNames();
                Collections.sort(nameList);

                Deck deck;
                StringBuffer allText = new StringBuffer();

                for (String aNameList : nameList) {
                    deck = QuestBattleManager.getAIDeckFromMap(aNameList);
                    allText.append(deck.getName()).append("\r\n");
                    allText.append(getExportDeckText(deck));
                    allText.append("++++++++++++++++++++++++++++++++++++++++++++++++++++++ \r\n \r\n");
                }

                JTextArea area = new JTextArea(allText.toString(), 30, 30);
                JOptionPane.showMessageDialog(null, new JScrollPane(area));

            }//actionPerformed()
        });//viewAllDecks

        //AI
        open.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent a) {
                String deckName = getUserInput_OpenDeck(QuestBattleManager.getAIDeckNames());

                //check if user selected "cancel"
                if (deckName.equals("")) return;


                setComputerPlayer(deckName);

                Deck d = QuestBattleManager.getAIDeckFromMap(deckName);
                CardList deck = new CardList();

                for (int i = 0; i < d.countMain(); i++) {
                    String cardName = d.getMain(i);
                    //String setCode = "";
                    if (cardName.contains("|")) {
                        String s[] = cardName.split("\\|", 2);
                        cardName = s[0];
                        //setCode = s[1];
                    }

                    deck.add(AllZone.getCardFactory().getCard(cardName, null));
                }
                // This is an expensive heap operation.
                CardList cardpool = new CardList(AllZone.getCardFactory());

                deckDisplay.updateDisplay(cardpool, deck);

            }
        });//open

        //AI
        new2.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent a) {
            	// This is an expensive heap operation.
                CardList allCards = new CardList(AllZone.getCardFactory());
                deckDisplay.updateDisplay(allCards, new CardList());

                setComputerPlayer("");
            }
        });//new


        //AI
        rename.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent a) {
                String name = getUserInput_GetDeckName(QuestBattleManager.getAIDeckNames());

                //check if user cancels
                if (name.equals("")) return;

                //is the current deck already saved and in QuestData?
                if (QuestBattleManager.getAIDeckNames().contains(currentDeck.getName()))
                    QuestBattleManager.removeAIDeck(currentDeck.getName());//remove old deck

                currentDeck.setName(name);

                Deck deck = convertCardListToDeck(deckDisplay.getBottom());
                deck.setName(name);
                QuestBattleManager.addAIDeck(deck);

                setComputerPlayer(name);
            }
        });//rename


        //AI
        save.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent a) {
                String name = currentDeck.getName();

                //check to see if name is set
                if (name.equals("")) {
                    name = getUserInput_GetDeckName(QuestBattleManager.getAIDeckNames());

                    //check if user cancels
                    if (name.equals("")) return;
                }

                setComputerPlayer(name);

                Deck deck = convertCardListToDeck(deckDisplay.getBottom());
                deck.setName(name);

                QuestBattleManager.addAIDeck(deck);
            }
        });//save


        //AI
        copy.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent a) {
                String name = getUserInput_GetDeckName(QuestBattleManager.getAIDeckNames());

                //check if user cancels
                if (name.equals("")) return;

                setComputerPlayer(name);

                Deck deck = convertCardListToDeck(deckDisplay.getBottom());
                deck.setName(name);

                QuestBattleManager.addAIDeck(deck);
            }
        });//copy


        //AI
        delete.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent a) {
                if (currentDeck.getName().equals("")) return;

                int check = JOptionPane.showConfirmDialog(null, "Do you really want to delete this deck?",
                        "Delete", JOptionPane.YES_NO_OPTION);
                if (check == JOptionPane.NO_OPTION) return;//stop here

                QuestBattleManager.removeAIDeck(currentDeck.getName());

                //show card pool
                
            	// This is an expensive heap operation.
                CardList cardpool = new CardList(AllZone.getCardFactory());
                deckDisplay.updateDisplay(cardpool, new CardList());

                setComputerPlayer("");
            }
        });//delete


        //AI
        exit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent a) {
                Gui_Quest_DeckEditor_Menu.this.close();
            }
        });

        JMenu deckMenu = new JMenu("AI Deck");
        deckMenu.add(open);
        deckMenu.add(rename);
        deckMenu.add(new2);
        deckMenu.add(save);
        deckMenu.add(copy);

        deckMenu.addSeparator();
        addImportExport(deckMenu, false);

        deckMenu.add(viewAllDecks);

        deckMenu.addSeparator();
        deckMenu.add(delete);
        deckMenu.addSeparator();
        deckMenu.add(exit);

        this.add(deckMenu);

    }//setupComputerMenu()

    /**
     * <p>openHumanDeck.</p>
     *
     * @param deckName a {@link java.lang.String} object.
     */
    private void openHumanDeck(String deckName) {
        setHumanPlayer(deckName);

        CardList cardpool = covertToCardList(questData.getCardpool());

        //covert Deck main to CardList
        Deck d = questData.getDeck(deckName);
        CardList deck = new CardList();

        for (int i = 0; i < d.countMain(); i++) {
            String cardName = d.getMain(i);
            //String setCode = "";
            if (cardName.contains("|")) {
                String s[] = cardName.split("\\|", 2);
                cardName = s[0];
                //setCode = s[1];
                cardpool.remove(s[0]);
            } else {
                cardpool.remove(d.getMain(i));
            }

            deck.add(AllZone.getCardFactory().getCard(cardName, null));

            //remove any cards that are in the deck from the card pool

        }

        deckDisplay.updateDisplay(cardpool, deck);

    }//openHumanDeck


    //the usual menu options that will be used
    /**
     * <p>setupMenu.</p>
     */
    private void setupMenu() {
        JMenuItem open = new JMenuItem("Open");
        JMenuItem new2 = new JMenuItem("New");
        JMenuItem rename = new JMenuItem("Rename");
        JMenuItem save = new JMenuItem("Save");
        JMenuItem copy = new JMenuItem("Copy");
        JMenuItem delete = new JMenuItem("Delete");
        JMenuItem exit = new JMenuItem("Exit");

        ////////////////////////////////////////////
        //below is new code

        //adds a card to human player's cardpool
        JMenuItem addCard = new JMenuItem("Cheat - Add Card");

        //add card
        addCard.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent a) {
                //sort cards by card name
            	// This is an expensive heap operation.
                CardList cardList = new CardList(AllZone.getCardFactory());
                TableSorter sorter = new TableSorter(cardList, 1, true);
                cardList.sort(sorter);

                //create a new Card object with a different toString() method
                //so that that JList only shows the card's name
                //
                //this is alot of work just to make it a little
                //easier and prettier for the user, gui stuff is very complicated
                class BetterCard extends Card {
                    private Card card;

                    BetterCard(Card c) {
                        card = c;

                        //this line is very important
                        //if you omit this, errors will occur
                        this.setName(c.getName());
                    }

                    public String toString() {
                        return card.getName();
                    }
                }//BetterCard

                Card[] card = cardList.toArray();

                for (int i = 0; i < card.length; i++) {
                    card[i] = new BetterCard(card[i]);
                }

                final JList list = new JList(card);
                list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

                //update the "card detail" on the right with the card info
                list.addListSelectionListener(new ListSelectionListener() {
                    public void valueChanged(ListSelectionEvent e) {

                        /*	I think that the code that was based in CardDetailUtil
                                    has been changed and moved to a new/different class?

                              CardDetail cd = (CardDetail)deckDisplay;
                              cd.updateCardDetail((Card)list.getSelectedValue());

                              */

                    }
                });

                Object[] o = {"Add Card to Your Cardpool", new JScrollPane(list)};
                JOptionPane pane = new JOptionPane(o, JOptionPane.INFORMATION_MESSAGE, JOptionPane.OK_CANCEL_OPTION);

                JDialog dialog = pane.createDialog(null, "Cheat - Add Card");
                dialog.setModal(true);
                dialog.setVisible(true);

                Object choice = pane.getValue();
                boolean cancel = false;

                //there are a ton of ways to cancel
                if (
                        choice == null ||
                                choice.equals(JOptionPane.UNINITIALIZED_VALUE)
                        )
                    cancel = true;
                else {
                    int n = ((Integer) choice).intValue();
                    if (n == JOptionPane.CANCEL_OPTION)
                        cancel = true;
                }

                if (cancel || list.getSelectedValue() == null) {
                    //System.out.println("cancelled");
                } else {
                    //show the choice that the user selected
                    //System.out.println(list.getSelectedValue());

                    Card c = (Card) list.getSelectedValue();

                    Gui_Quest_DeckEditor g = (Gui_Quest_DeckEditor) deckDisplay;
                    TableModel table = g.getTopTableModel();
                    table.addCard(c);
                    table.resort();
                }
            }//actionPerformed()
        });//add card


        //above is new code
        ///////////////////////////////////////

        //human
        open.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent a) {
                String deckName = getUserInput_OpenDeck(questData.getDeckNames());

                //check if user selected "cancel"
                if (deckName.equals("")) return;

                openHumanDeck(deckName);
            }
        });//open

        //human
        new2.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent a) {
                CardList cardpool = covertToCardList(questData.getCardpool());
                deckDisplay.updateDisplay(cardpool, new CardList());

                setHumanPlayer("");
            }
        });//new


        //human
        rename.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent a) {
                String name = getUserInput_GetDeckName(questData.getDeckNames());

                //check if user cancels
                if (name.equals("")) return;

                //is the current deck already saved and in QuestData?
                if (questData.getDeckNames().contains(currentDeck.getName()))
                    questData.removeDeck(currentDeck.getName());//remove old deck

                currentDeck.setName(name);

                Deck deck = convertCardListToDeck(deckDisplay.getBottom());
                deck.setName(name);
                questData.addDeck(deck);

                setHumanPlayer(name);
            }
        });//rename


        //human
        save.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent a) {
                String name = currentDeck.getName();

                //check to see if name is set
                if (name.equals("")) {
                    name = getUserInput_GetDeckName(questData.getDeckNames());

                    //check if user cancels
                    if (name.equals("")) return;
                }

                setHumanPlayer(name);

                Deck deck = convertCardListToDeck(deckDisplay.getBottom());
                deck.setName(name);

                questData.addDeck(deck);
            }
        });//save


        //human
        copy.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent a) {
                String name = getUserInput_GetDeckName(questData.getDeckNames());

                //check if user cancels
                if (name.equals("")) return;

                setHumanPlayer(name);

                Deck deck = convertCardListToDeck(deckDisplay.getBottom());
                deck.setName(name);

                questData.addDeck(deck);
            }
        });//copy


        //human
        delete.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent a) {
                if (currentDeck.getName().equals("")) return;

                int check = JOptionPane.showConfirmDialog(null, "Do you really want to delete this deck?",
                        "Delete", JOptionPane.YES_NO_OPTION);
                if (check == JOptionPane.NO_OPTION) return;//stop here

                questData.removeDeck(currentDeck.getName());

                //show card pool
                CardList cardpool = covertToCardList(questData.getCardpool());
                deckDisplay.updateDisplay(cardpool, new CardList());

                setHumanPlayer("");
            }
        });//delete


        //human
        exit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent a) {
                Gui_Quest_DeckEditor_Menu.this.close();
            }
        });

        JMenu deckMenu = new JMenu("Deck");
        deckMenu.add(open);
        deckMenu.add(new2);
        deckMenu.add(rename);
        deckMenu.add(save);
        deckMenu.add(copy);

        // The "Cheat - Add Card" menu item is buggy.
        // There are other, safer and less buggy ways for people to cheat.

        // deckMenu.addSeparator();//new code
        // deckMenu.add(addCard); //new code

        deckMenu.addSeparator();
        addImportExport(deckMenu, true);

        deckMenu.addSeparator();
        deckMenu.add(delete);
        deckMenu.addSeparator();
        deckMenu.add(exit);

        this.add(deckMenu);

    }//setupMenu()

    /**
     * <p>convertCardListToDeck.</p>
     *
     * @param list a {@link forge.CardList} object.
     * @return a {@link forge.deck.Deck} object.
     */
    private Deck convertCardListToDeck(CardList list) {
        //put CardList into Deck main
        Deck deck = new Deck(Constant.GameType.Sealed);

        for (int i = 0; i < list.size(); i++)
            deck.addMain(list.get(i).getName());

        return deck;
    }

    //needs to be public because Gui_Quest_DeckEditor.show(Command) uses it
    /**
     * <p>setHumanPlayer.</p>
     *
     * @param deckName a {@link java.lang.String} object.
     */
    public void setHumanPlayer(String deckName) {
        //the gui uses this, Gui_Quest_DeckEditor
        currentDeck = new Deck(Constant.GameType.Sealed);
        currentDeck.setName(deckName);

        deckDisplay.setTitle(deckEditorName + " - " + deckName);
    }

    /**
     * <p>setComputerPlayer.</p>
     *
     * @param deckName a {@link java.lang.String} object.
     */
    private void setComputerPlayer(String deckName) {
        //the gui uses this, Gui_Quest_DeckEditor
        currentDeck = new Deck(Constant.GameType.Constructed);
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
    private String cleanString(String in) {
        StringBuffer out = new StringBuffer();
        char[] c = in.toCharArray();

        for (int i = 0; i < c.length && i < 20; i++)
            if (Character.isLetterOrDigit(c[i]) || c[i] == '-' || c[i] == '_' || c[i] == ' ') out.append(c[i]);

        return out.toString();
    }

    //if user cancels, returns ""
    /**
     * <p>getUserInput_GetDeckName.</p>
     *
     * @param nameList a {@link java.util.List} object.
     * @return a {@link java.lang.String} object.
     */
    private String getUserInput_GetDeckName(List<String> nameList) {
        Object o = JOptionPane.showInputDialog(null, "", "Deck Name", JOptionPane.OK_CANCEL_OPTION);

        if (o == null) return "";

        String deckName = cleanString(o.toString());

        if (nameList.contains(deckName) || deckName.equals("")) {
            JOptionPane.showMessageDialog(null, "Please pick another deck name, a deck currently has that name.");
            return getUserInput_GetDeckName(nameList);
        }

        return deckName;
    }//getUserInput_GetDeckName()


    //if user cancels, it will return ""
    /**
     * <p>getUserInput_OpenDeck.</p>
     *
     * @param deckNameList a {@link java.util.List} object.
     * @return a {@link java.lang.String} object.
     */
    private String getUserInput_OpenDeck(List<String> deckNameList) {
        List<String> choices = deckNameList;
        if (choices.size() == 0) {
            JOptionPane.showMessageDialog(null, "No decks found", "Open Deck", JOptionPane.PLAIN_MESSAGE);
            return "";
        }

        //Object o = JOptionPane.showInputDialog(null, "Deck Name", "Open Deck", JOptionPane.OK_CANCEL_OPTION, null,
        //        choices.toArray(), choices.toArray()[0]);
        Object o = GuiUtils.getChoiceOptional("Select Deck", choices.toArray());

        if (o == null) return "";

        return o.toString();
    }//getUserInput_OpenDeck()

    //used by Gui_Quest_DeckEditor
    /**
     * <p>close.</p>
     */
    public void close() {
        exitCommand.execute();
    }

    //used by Gui_Quest_DeckEditor
    /**
     * <p>getDeckName.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getDeckName() {
        return currentDeck.getName();
    }

    //used by Gui_Quest_DeckEditor
    /**
     * <p>getGameType.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getGameType() {
        return currentDeck.getDeckType();
    }


    //returns CardList of Card objects,
    //argument ArrayList holds String card names
    /**
     * <p>covertToCardList.</p>
     *
     * @param list a {@link java.util.List} object.
     * @return a {@link forge.CardList} object.
     */
    public static CardList covertToCardList(List<String> list) {
        CardList c = new CardList();
        Card card;
        for (String aList : list) {

            card = AllZone.getCardFactory().getCard(aList, null);
            c.add(card);
        }

        return c;
    }
}
