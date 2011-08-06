
package forge;


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Proxy;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;

import forge.error.ErrorViewer;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;
import forge.properties.NewConstants.LANG.Gui_DownloadPictures.ERRORS;


interface DeckDisplay {
    public void updateDisplay(CardList top, CardList bottom);
    
    //top shows available card pool
    //if constructed, top shows all cards
    //if sealed, top shows 5 booster packs
    //if draft, top shows cards that were chosen
    public CardList getTop();
    
    //bottom shows cards that the user has chosen for his library
    public CardList getBottom();
    
    public void setTitle(String message);
}

public class Gui_DeckEditor_Menu extends JMenuBar implements NewConstants {
    private static final long    serialVersionUID  = -4037993759604768755L;
    
    //used by importConstructed() and exportConstructected()
    private static File          previousDirectory = null;
    

    private static final boolean debugPrint        = false;
    
//    private final DeckIO      deckIO            = new OldDeckIO(ForgeProps.getFile(DECKS));
//    private final DeckIO      boosterDeckIO     = new OldDeckIO(ForgeProps.getFile(BOOSTER_DECKS));
    private final DeckIO         deckIO            = new NewDeckIO(ForgeProps.getFile(NEW_DECKS));
    //with the new IO, there's no reason to use different instances
    private final DeckIO         boosterDeckIO     = deckIO;
    
    private boolean              isDeckSaved;

    private String               currentDeckName;
    private String               currentGameType;
    
    private JMenuItem            newDraftItem;
    private DeckDisplay          deckDisplay;
    
    private Command              exitCommand;
    
    
    public Gui_DeckEditor_Menu(DeckDisplay in_display, Command exit) {
        deckDisplay = in_display;
        exitCommand = exit;
        
        //this is added just to make save() and saveAs() work ok
        //when first started up, just a silly patch
        currentGameType = Constant.GameType.Constructed;
        setDeckData("", false);
        
        setupMenu();
        setupFilterMenu();
        setupSortMenu();
    }
    
    private void setupFilterMenu() {
        JMenuItem filter = new JMenuItem("New filter");
        JMenuItem clearfilter = new JMenuItem("Clear filter");
        JMenu menu = new JMenu("Filter");
        menu.add(filter);
        menu.add(clearfilter);
        this.add(menu);
        
        filter.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                Gui_DeckEditor g = (Gui_DeckEditor) deckDisplay;
                GUI_Filter filt = new GUI_Filter(g, deckDisplay);
                g.setEnabled(false);
                filt.setVisible(true);
            }
        });
        clearfilter.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                
                CardList all = AllZone.CardFactory.getAllCards();
                deckDisplay.updateDisplay(all, deckDisplay.getBottom());
                Gui_DeckEditor g = (Gui_DeckEditor) deckDisplay;
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
                
            }
        });
        
    }
    
    
    private void setupSortMenu() {
        JMenuItem name = new JMenuItem("Card Name");
        JMenuItem cost = new JMenuItem("Cost");
        JMenuItem color = new JMenuItem("Color");
        JMenuItem type = new JMenuItem("Type");
        JMenuItem stats = new JMenuItem("Power/Toughness");
        JMenuItem rarity = new JMenuItem("Rarity");
        JMenuItem newFirst = new JMenuItem("Most recently added");
        
        JMenu menu = new JMenu("Sort By");
        menu.add(name);
        menu.add(cost);
        menu.add(color);
        menu.add(type);
        menu.add(stats);
        menu.add(rarity);
        
        // menu.add(newFirst);
        /*
         *  The "Most recently added" menu now causes an error exception. This will prevent 
         *  this option from appearing in the "Sort By" menu. There may be a way (or not) 
         *  to sort the indivudual card files by the date created or the date last modified.
         */
        
        this.add(menu);
        
        //add listeners
        

        name.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                //index 1 sorts by card name - for more info see TableSorter
                //                             0       1       2       3        4     5          6
                //private String column[] = {"Qty", "Name", "Cost", "Color", "Type", "Stats", "Rarity"};
                Gui_DeckEditor g = (Gui_DeckEditor) deckDisplay;
                g.getTopTableModel().sort(1, true);
                
            }
        });
        
        cost.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                //                             0       1       2       3        4     5          6
                //private String column[] = {"Qty", "Name", "Cost", "Color", "Type", "Stats", "Rarity"};
                Gui_DeckEditor g = (Gui_DeckEditor) deckDisplay;
                
                //sort by type, color, cost
                g.getTopTableModel().sort(4, true);
                g.getTopTableModel().sort(3, true);
                g.getTopTableModel().sort(2, true);
            }
        });
        
        color.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                //                             0       1       2       3        4     5          6
                //private String column[] = {"Qty", "Name", "Cost", "Color", "Type", "Stats", "Rarity"};
                Gui_DeckEditor g = (Gui_DeckEditor) deckDisplay;
                
                //sort by type, cost, color
                g.getTopTableModel().sort(4, true);
                g.getTopTableModel().sort(2, true);
                g.getTopTableModel().sort(3, true);
            }
        });
        
        type.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                //                             0       1       2       3        4     5          6
                //private String column[] = {"Qty", "Name", "Cost", "Color", "Type", "Stats", "Rarity"};
                Gui_DeckEditor g = (Gui_DeckEditor) deckDisplay;
                
                //sort by cost, color, type
                g.getTopTableModel().sort(2, true);
                g.getTopTableModel().sort(3, true);
                g.getTopTableModel().sort(4, true);
            }
        });
        
        stats.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                //                             0       1       2       3        4     5          6
                //private String column[] = {"Qty", "Name", "Cost", "Color", "Type", "Stats", "Rarity"};
                Gui_DeckEditor g = (Gui_DeckEditor) deckDisplay;
                
                g.getTopTableModel().sort(4, true);
                g.getTopTableModel().sort(2, true);
                g.getTopTableModel().sort(3, true);
                g.getTopTableModel().sort(5, true);
            }
        });
        
        rarity.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                //                             0       1       2       3        4     5          6
                //private String column[] = {"Qty", "Name", "Cost", "Color", "Type", "Stats", "Rarity"};
                Gui_DeckEditor g = (Gui_DeckEditor) deckDisplay;
                
                //sort by cost, type, color, rarity
                g.getTopTableModel().sort(2, true);
                g.getTopTableModel().sort(4, true);
                g.getTopTableModel().sort(3, true);
                g.getTopTableModel().sort(6, true);
            }
        });
        
        newFirst.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                //                             0       1       2       3        4     5          6
                //private String column[] = {"Qty", "Name", "Cost", "Color", "Type", "Stats", "Rarity"};
                Gui_DeckEditor g = (Gui_DeckEditor) deckDisplay;
                
                g.getTopTableModel().sort(99, true);
            }
        });
        
    }//setupSortMenu()
    
    public void populateShop(CardList shop, CardList owned)
    {
    	
    	deckDisplay.updateDisplay(shop, owned);
    }
    
    public void newConstructed() {
        if(debugPrint) System.out.println("New Constructed");
        
//    if(! isDeckSaved)
//      save();
        
        currentGameType = Constant.GameType.Constructed;
        setDeckData("", false);
        
        deckDisplay.updateDisplay(AllZone.CardFactory.getAllCards(), new CardList());
    }//new constructed
    
    private void newRandomConstructed() {
        if(debugPrint) System.out.println("Random Constructed");
        
//    if(! isDeckSaved)
//      save();
        
        currentGameType = Constant.GameType.Constructed;
        setDeckData("", false);
        
        CardList all = AllZone.CardFactory.getAllCards();
        all.shuffle();
        CardList random = new CardList();
        
        for(int i = 0; i < (15 * 5); i++)
            random.add(all.remove(0));
        
        random.add(AllZone.CardFactory.getCard("Forest", AllZone.HumanPlayer));
        random.add(AllZone.CardFactory.getCard("Island", AllZone.HumanPlayer));
        random.add(AllZone.CardFactory.getCard("Plains", AllZone.HumanPlayer));
        random.add(AllZone.CardFactory.getCard("Mountain", AllZone.HumanPlayer));
        random.add(AllZone.CardFactory.getCard("Swamp", AllZone.HumanPlayer));
        
        random.add(AllZone.CardFactory.getCard("Terramorphic Expanse", AllZone.HumanPlayer));
        

        deckDisplay.updateDisplay(random, new CardList());
    }//new sealed
    

    private void newGenerateConstructed() {
        if(debugPrint) System.out.println("Generate Constructed");
        
//    if(! isDeckSaved)
//      save();
        
        currentGameType = Constant.GameType.Constructed;
        setDeckData("", false);
        
        GenerateConstructedDeck gen = new GenerateConstructedDeck();
        
        deckDisplay.updateDisplay(AllZone.CardFactory.getAllCards(), gen.generateDeck());
    }//new sealed
    

    private void newSealed() {
        if(debugPrint) System.out.println("New Sealed");
        
//    if(! isDeckSaved)
//      save();
        
        currentGameType = Constant.GameType.Sealed;
        setDeckData("", false);
        
        deckDisplay.updateDisplay(new ReadBoosterPack().getBoosterPack5(), new CardList());
    }//new sealed
    
    private void newDraft() {
        if(debugPrint) System.out.println("New Draft");
        
//    if(! isDeckSaved)
//      save();
        
        currentGameType = Constant.GameType.Draft;
        
        //move all cards from deck main and sideboard to CardList
        Deck deck = boosterDeckIO.readBoosterDeck(currentDeckName)[0];
        setDeckData("", false);
        
        CardList top = new CardList();
        
        for(int i = 0; i < deck.countMain(); i++)
            top.add(AllZone.CardFactory.getCard(deck.getMain(i), AllZone.HumanPlayer));
        
        for(int i = 0; i < deck.countSideboard(); i++)
            top.add(AllZone.CardFactory.getCard(deck.getSideboard(i), AllZone.HumanPlayer));
        
        deckDisplay.updateDisplay(top, new CardList());
    }//new draft
    
    private FileFilter getFileFilter() {
        FileFilter filter = new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.getName().endsWith(".deck") || f.isDirectory();
            }
            
            @Override
            public String getDescription() {
                return "Deck File .deck";
            }
        };
        
        return filter;
    }//getFileFilter()
    
    private FileFilter dckFilter = new FileFilter() {
                                     @Override
                                     public boolean accept(File f) {
                                         return f.getName().endsWith(".dck") || f.isDirectory();
                                     }
                                     
                                     @Override
                                     public String getDescription() {
                                         return "Simple Deck File .dck";
                                     }
                                 };
    
    
    private File getImportFilename() {
        JFileChooser chooser = new JFileChooser(previousDirectory);
        
        chooser.addChoosableFileFilter(dckFilter);
        chooser.addChoosableFileFilter(getFileFilter());
        int returnVal = chooser.showOpenDialog(null);
        
        if(returnVal == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            previousDirectory = file.getParentFile();
            return file;
        }
        

        return null;
        
    }//openFileDialog()
    
    private void showDeck(Deck deck) {
        String gameType = deck.getDeckType();
        
        if(gameType.equals(Constant.GameType.Constructed)) showConstructedDeck(deck);
        
        if(gameType.equals(Constant.GameType.Draft)) showDraftDeck(deck);
        
        if(gameType.equals(Constant.GameType.Sealed)) showSealedDeck(deck);
    }//showDeck()
    
    private void importDeck() {
        File file = getImportFilename();
        
        if(file == null) return;
        else if(file.getName().endsWith(".dck")) {
            try {
                FileChannel srcChannel = new FileInputStream(file).getChannel();
                File dst = new File(ForgeProps.getFile(NEW_DECKS).getAbsolutePath() + java.io.File.pathSeparator
                        + (file.getName()));
                if(!dst.createNewFile()) {
                    JOptionPane.showMessageDialog(null, "Cannot import deck " + file.getName()
                            + ", a deck currently has that name.");
                    return;
                }
                FileChannel dstChannel = new FileOutputStream(dst).getChannel();
                dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
                srcChannel.close();
                dstChannel.close();
                JOptionPane.showMessageDialog(null, file.getName()
                        + "imported succesfully. Restart the deck editor to see it.");
            } catch(Exception ex) {
                ErrorViewer.showError(ex);
                throw new RuntimeException("Gui_DeckEditor_Menu : importDeck() error, " + ex);
            }
            return;
        }
        
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
            throw new RuntimeException("Gui_DeckEditor_Menu : importDeck() error, " + ex);
        }
        
        Deck deck = null;
        
        //is Draft deck?
        if(check instanceof Deck[]) {
            Deck[] draftDeck = (Deck[]) check;
            
            currentGameType = Constant.GameType.Draft;
            boosterDeckIO.writeBoosterDeck(draftDeck);
            
            //enable the new draft deck menu option
            newDraftItem.setEnabled(true);
            deck = draftDeck[0];
        } else //Sealed or Constructed
        {
            deck = (Deck) check;
            currentGameType = deck.getDeckType();
            deckIO.writeDeck(deck);
            
            //disable the new draft deck menu option
            newDraftItem.setEnabled(false);
        }
        
        //update gui
        showDeck(deck);
        
    }//importDeck()
    
    private void downloadDeck() {
        
        Object o = JOptionPane.showInputDialog(null, "URL(only from http://magic.tcgplayer.com):",
                "Download Deck", JOptionPane.OK_CANCEL_OPTION);
        if(o == null) {
            return;
        }
        String url = o.toString();
        
        if((url.length() < 37)
                || (url.substring(0, 39).equalsIgnoreCase("http://magic.tcgplayer.com/db/deck.asp"))) {
            JOptionPane.showMessageDialog(null, "Bad URL." + "\n"
                    + "Support only deck from http://magic.tcgplayer.com" + "\n"
                    + "Example: http://magic.tcgplayer.com/db/deck.asp?deck_id=474146", "Information",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Proxy p = null;
        p = Proxy.NO_PROXY;
        BufferedInputStream in;
        BufferedOutputStream out;
        try {
            byte[] buf = new byte[1024];
            int len;
            File f = new File("deck_temp.html");
            in = new BufferedInputStream(new URL(url).openConnection(p).getInputStream());
            out = new BufferedOutputStream(new FileOutputStream(f));
            //while - read and write file
            while((len = in.read(buf)) != -1) {
                out.write(buf, 0, len);
                
            }//while - read and write file
            in.close();
            out.flush();
            out.close();
            String fileName = "deck_temp.html";
            FileReader fr = new FileReader(fileName);
            BufferedReader br = new BufferedReader(fr);
            String s = "";
            String z = "";
            StringBuffer sb = new StringBuffer();
            while((z = br.readLine()) != null) {
                sb.append(z);
            }
            s = sb.toString();
            br.close();
            int start = s.indexOf("MAIN DECK");
            int finish = s.indexOf("SIDEBOARD");
            String rStr = "";
            rStr = s.substring(start + 9, finish);
            int first;
            int second;
            while(rStr.indexOf("<") != -1) {
                first = rStr.indexOf("<");
                second = rStr.indexOf(">", first);
                if(first == 0) {
                    rStr = rStr.substring(second + 1);
                } else {
                    rStr = rStr.substring(0, first) + " " + rStr.substring(second + 1);
                }
            }
            first = rStr.indexOf("Creatures [");
            second = rStr.indexOf("]", first);
            if(first != -1) {
                rStr = rStr.substring(0, first) + rStr.substring(second + 1);
            }
            first = rStr.indexOf("Spells [");
            second = rStr.indexOf("]", first);
            if(first != -1) {
                rStr = rStr.substring(0, first) + rStr.substring(second + 1);
            }
            first = rStr.indexOf("Lands [");
            second = rStr.indexOf("]", first);
            if(first != -1) {
                rStr = rStr.substring(0, first) + rStr.substring(second + 1);
            }
            String number[] = new String[59];
            String name[] = new String[59];
            int count = 0;
            DownloadDeck download = new DownloadDeck();
            while(rStr.length() != 0) {
                rStr = download.RemoveSpace(rStr);
                number[count] = download.FoundNumberCard(rStr);
                rStr = download.RemoveFoundNumberCard(rStr, number[count]);
                rStr = download.RemoveSpace(rStr);
                name[count] = download.FoundNameCard(rStr);
                name[count] = download.RemoveSpaceBack(name[count]);
                rStr = download.RemoveFoundNameCard(rStr, name[count]);
                rStr = download.RemoveSpace(rStr);
                count = count + 1;
            }
            String trueName[] = new String[59];
            String trueNumber[] = new String[59];
            String falseName[] = new String[59];
            int trueCount = 0;
            int falseCount = 0;
            for(int i = 0; i < count; i++) {
                if(download.IsCardSupport(name[i]) == true) {
                    trueName[trueCount] = name[i];
                    trueNumber[trueCount] = number[i];
                    trueCount = trueCount + 1;
                } else {
                    falseName[falseCount] = name[i];
                    falseCount = falseCount + 1;
                }
                
            }
            
            Card c = new Card();
            CardList trueList = new CardList();
            for(int i = 0; i < trueCount; i++) {
                for(int k = 0; k < Integer.parseInt(trueNumber[i]); k++) {
                    c = download.GetCardDownload(c, trueName[i]);
                    trueList.add(c);
                }
                
            }
            
            StringBuffer falseCards = new StringBuffer();
            for(int i = 0; i < falseCount; i++) {
                falseCards.append("\n").append(falseName[i]).append(",");
            }
            

            deckDisplay.updateDisplay(deckDisplay.getTop(), trueList);
            
            if(falseCount == 0) {
                JOptionPane.showMessageDialog(null, "Deck downloads.", "Information",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(null, "Sorry, cards:" + falseCards
                        + "\nnot supported in this version MTGForge. \nDeck downloads without this cards.",
                        "Information", JOptionPane.INFORMATION_MESSAGE);
            }
            
            f.delete();
            
        } catch(Exception ex) {
            ErrorViewer.showError(ex, ForgeProps.getLocalized(ERRORS.OTHER), "deck_temp.html", url);
            
        }
        

    }
    
    
    private void exportDeck() {
        File filename = getExportFilename();
        
        if(filename == null) return;
        
        //write is an Object variable because you might just
        //write one Deck object or
        //many Deck objects if it is a draft deck
        Deck deck = getDeck();
        Object write = deck;
        
        deck.setName(filename.getName());
        
        //export Draft decks, this is a little hacky
        //a Draft deck holds 8 decks, [0] is the player's deck
        //and the other 7 are the computer's deck
        if(currentGameType.equals(Constant.GameType.Draft)) {
            //read all draft decks
            Deck d[] = boosterDeckIO.readBoosterDeck(currentDeckName);
            
            //replace your deck
            d[0] = deck;
            write = d;
        }
        
        try {
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(filename));
            out.writeObject(write);
            out.flush();
            out.close();
        } catch(Exception ex) {
            ErrorViewer.showError(ex);
            throw new RuntimeException("Gui_DeckEditor_Menu : exportDeck() error, " + ex);
        }
        
        exportDeckText(deck, filename.getAbsolutePath());
        
    }//exportDeck()
    
    // @SuppressWarnings("unchecked")
    // TableSorter type saftey
    private void exportDeckText(Deck aDeck, String filename) {
        //convert Deck into CardList
        CardList all = new CardList();
        for(int i = 0; i < aDeck.countMain(); i++) {
            String cardName = aDeck.getMain(i);
            Card c = AllZone.CardFactory.getCard(cardName, null);
            
            all.add(c);
        }
        
        //sort by card name
        all.sort(new TableSorter(all, 1, true));
        
        //remove all copies of cards
        //make a singleton
        CardList noCopies = new CardList();
        for(int i = 0; i < all.size(); i++) {
            Card c = all.get(i);
            
            if(!noCopies.containsName(c.getName())) noCopies.add(c);
        }
        

        StringBuffer text = new StringBuffer();
        String newLine = "\r\n";
        int count = 0;
        
        text.append(all.size()).append(" Total Cards").append(newLine).append(newLine);
        
        //creatures
        text.append(all.getType("Creature").size()).append(" Creatures").append(newLine);
        text.append("-------------").append(newLine);
        
        for(int i = 0; i < noCopies.size(); i++) {
            Card c = noCopies.get(i);
            if(c.isCreature()) {
                count = all.getName(c.getName()).size();
                text.append(count).append("x ").append(c.getName()).append(newLine);
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
        text.append(newLine).append(count).append(" Spells").append(newLine);
        text.append("----------").append(newLine);
        
        for(int i = 0; i < noCopies.size(); i++) {
            Card c = noCopies.get(i);
            if(!(c.isCreature() || c.isLand())) {
                count = all.getName(c.getName()).size();
                text.append(count).append("x ").append(c.getName()).append(newLine);
            }
        }
        
        //land
        text.append(newLine).append(all.getType("Land").size()).append(" Land").append(newLine);
        text.append("--------").append(newLine);
        
        for(int i = 0; i < noCopies.size(); i++) {
            Card c = noCopies.get(i);
            if(c.isLand()) {
                count = all.getName(c.getName()).size();
                text.append(count).append("x ").append(c.getName()).append(newLine);
            }
        }
        
        //remove ".deck" extension
        int cut = filename.indexOf(".");
        filename = filename.substring(0, cut);
        
        try {
            FileWriter writer = new FileWriter(filename + ".txt");
            writer.write(text.toString());
            
            writer.flush();
            writer.close();
        } catch(Exception ex) {
            ErrorViewer.showError(ex);
            throw new RuntimeException("Gui_DeckEditor_Menu : exportDeckText() error, " + ex.getMessage() + " : "
                    + Arrays.toString(ex.getStackTrace()));
        }
    }//exportDeckText()
    

    private File getExportFilename() {
        //Object o = null; // unused
        
        JFileChooser save = new JFileChooser(previousDirectory);
        
        save.setDialogTitle("Export Deck Filename");
        save.setDialogType(JFileChooser.SAVE_DIALOG);
        save.addChoosableFileFilter(getFileFilter());
        save.setSelectedFile(new File(currentDeckName + ".deck"));
        
        int returnVal = save.showSaveDialog(null);
        
        if(returnVal == JFileChooser.APPROVE_OPTION) {
            File file = save.getSelectedFile();
            String check = file.getAbsolutePath();
            
            previousDirectory = file.getParentFile();
            
            if(check.endsWith(".deck")) return file;
            else return new File(check + ".deck");
        }
        
        return null;
    }//getExportFilename()
    

    private void openConstructed() {
        if(debugPrint) System.out.println("Open Constructed");
        
//    if(! isDeckSaved)
//      save();
        
        String name = getUserInput_OpenDeck(Constant.GameType.Constructed);
        
        if(name.equals("")) return;
        
        //must be AFTER get user input, since user could cancel
        currentGameType = Constant.GameType.Constructed;
        newDraftItem.setEnabled(false);
        
        Deck deck = deckIO.readDeck(name);
        showConstructedDeck(deck);
    }//open constructed
    
    private void showConstructedDeck(Deck deck) {
        setDeckData(deck.getName(), true);
        
        CardList main = new CardList();
        for(int i = 0; i < deck.countMain(); i++)
            main.add(AllZone.CardFactory.getCard(deck.getMain(i), AllZone.HumanPlayer));
        
        deckDisplay.updateDisplay(AllZone.CardFactory.getAllCards(), main);
    }//showConstructedDeck()
    
    private void openSealed() {
        if(debugPrint) System.out.println("Open Sealed");
        
//    if(! isDeckSaved)
//      save();
        
        String name = getUserInput_OpenDeck(Constant.GameType.Sealed);
        
        if(name.equals("")) return;
        
        //must be AFTER get user input, since user could cancel
        currentGameType = Constant.GameType.Sealed;
        newDraftItem.setEnabled(false);
        
        Deck deck = deckIO.readDeck(name);
        showSealedDeck(deck);
    }//open sealed
    
    private void showSealedDeck(Deck deck) {
        setDeckData(deck.getName(), true);
        
        CardList top = new CardList();
        for(int i = 0; i < deck.countSideboard(); i++)
            top.add(AllZone.CardFactory.getCard(deck.getSideboard(i), AllZone.HumanPlayer));
        
        CardList bottom = new CardList();
        for(int i = 0; i < deck.countMain(); i++)
            bottom.add(AllZone.CardFactory.getCard(deck.getMain(i), AllZone.HumanPlayer));
        
        deckDisplay.updateDisplay(top, bottom);
    }//showSealedDeck()
    
    private void openDraft() {
        if(debugPrint) System.out.println("Open Draft");
        
        String name = getUserInput_OpenDeck(Constant.GameType.Draft);
        
        if(name.equals("")) return;
        
        //must be AFTER get user input, since user could cancel
        currentGameType = Constant.GameType.Draft;
        newDraftItem.setEnabled(true);
        
        Deck deck = boosterDeckIO.readBoosterDeck(name)[0];
        showDraftDeck(deck);
    }//open draft
    
    private void showDraftDeck(Deck deck) {
        setDeckData(deck.getName(), true);
        
        CardList top = new CardList();
        for(int i = 0; i < deck.countSideboard(); i++)
            top.add(AllZone.CardFactory.getCard(deck.getSideboard(i), AllZone.HumanPlayer));
        
        CardList bottom = new CardList();
        for(int i = 0; i < deck.countMain(); i++)
            bottom.add(AllZone.CardFactory.getCard(deck.getMain(i), AllZone.HumanPlayer));
        
        deckDisplay.updateDisplay(top, bottom);
    }//showDraftDeck()
    
    private void save() {
        if(debugPrint) System.out.println("Save");
        
        if(currentDeckName.equals("")) saveAs();
        else if(currentGameType.equals(Constant.GameType.Draft)) {
            setDeckData(currentDeckName, true);
            //write booster deck
            Deck[] all = boosterDeckIO.readBoosterDeck(currentDeckName);
            all[0] = getDeck();
            boosterDeckIO.writeBoosterDeck(all);
        } else//constructed or sealed
        {
            setDeckData(currentDeckName, true);
            deckIO.deleteDeck(currentDeckName);
            deckIO.writeDeck(getDeck());
        }
    }//save
    
    private void saveAs() {
        if(debugPrint) System.out.println("Save As");
        
        String name = getUserInput_GetDeckName();
        
        if(name.equals("")) return;
        else if(currentGameType.equals(Constant.GameType.Draft)) {
            //MUST copy array
            Deck[] read = boosterDeckIO.readBoosterDeck(currentDeckName);
            Deck[] all = new Deck[read.length];
            
            System.arraycopy(read, 0, all, 0, read.length);
            
            setDeckData(name, true);
            
            all[0] = getDeck();
            boosterDeckIO.writeBoosterDeck(all);
        } else//constructed and sealed
        {
            setDeckData(name, true);
            deckIO.writeDeck(getDeck());
        }
    }//save as
    
    private void delete() {
        if(debugPrint) System.out.println("Delete");
        
        if(currentGameType.equals("") || currentDeckName.equals("")) return;
        
        int n = JOptionPane.showConfirmDialog(null, "Do you want to delete this deck " + currentDeckName + " ?",
                "Delete", JOptionPane.YES_NO_OPTION);
        if(n == JOptionPane.NO_OPTION) return;
        
        if(currentGameType.equals(Constant.GameType.Draft)) boosterDeckIO.deleteBoosterDeck(currentDeckName);
        else deckIO.deleteDeck(currentDeckName);
        
        setDeckData("", true);
        deckDisplay.updateDisplay(new CardList(), new CardList());
    }//delete
    
    public void close() {
        if(debugPrint) System.out.println("Close");
        
//    if(! isDeckSaved)
//      save();
        
        deckIO.close();
        boosterDeckIO.close();
        exitCommand.execute();
    }//close
    
    private void setDeckData(String deckName, boolean in_isDeckSaved) {
        currentDeckName = deckName;
        isDeckSaved = in_isDeckSaved;
        
        deckDisplay.setTitle("Deck Editor : " + currentDeckName);
    }
    
    public void setTitle(String s)
    {
    	deckDisplay.setTitle(s);
    }
    
    public String getDeckName() {
        return currentDeckName;
    }
    
    public String getGameType() {
        return currentGameType;
    }
    
    public boolean isDeckSaved() {
        return isDeckSaved;
    }
    
    private String getUserInput_GetDeckName() {
        Object o = JOptionPane.showInputDialog(null, "Save As", "Deck Name", JOptionPane.OK_CANCEL_OPTION);
        
        if(o == null) return "";
        
        String deckName = cleanString(o.toString());
        
        boolean isUniqueName;
        if(currentGameType.equals(Constant.GameType.Draft)) isUniqueName = deckIO.isUniqueDraft(deckName);
        else isUniqueName = deckIO.isUnique(deckName);
        
        if((!isUniqueName) || deckName.equals("")) {
            JOptionPane.showMessageDialog(null, "Please pick another deck name, a deck currently has that name.");
            return getUserInput_GetDeckName();
        }
        
        return deckName;
    }//getUserInput_GetDeckName()
    
    //only accepts numbers, letters or dashes up to 10 characters in length
    private String cleanString(String in) {
        char[] c = in.toCharArray();
        
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < c.length && i < 20; i++)
            if(Character.isLetterOrDigit(c[i]) || c[i] == '-') sb.append(c[i]);
        return sb.toString();
    }
    
    
    private String getUserInput_OpenDeck(String deckType) {
        ArrayList<String> choices = getDeckNames(deckType);
        if(choices.size() == 0) {
            JOptionPane.showMessageDialog(null, "No decks found", "Open Deck", JOptionPane.PLAIN_MESSAGE);
            return "";
        }
        //Object o = JOptionPane.showInputDialog(null, "Deck Name", "Open Deck", JOptionPane.OK_CANCEL_OPTION, null,
        //        choices.toArray(), choices.toArray()[0]);
        Object o = AllZone.Display.getChoiceOptional("Open Deck", choices.toArray());
        
        if(o == null) return "";
        
        return o.toString();
    }//getUserInput_OpenDeck()
    

    private ArrayList<String> getDeckNames(String deckType) {
        ArrayList<String> list = new ArrayList<String>();
        
        //only get decks according to the Gui_NewGame screen option
        if(deckType.equals(Constant.GameType.Draft)) {
            Iterator<?> it = boosterDeckIO.getBoosterDecks().keySet().iterator();
            
            while(it.hasNext())
                list.add(it.next().toString());
        } else {
            Deck[] d = deckIO.getDecks();
            for(int i = 0; i < d.length; i++)
                if(deckType.equals(d[i].getDeckType())) list.add(d[i].toString());
        }
        
        Collections.sort(list);
        return list;
    }//getDecks()
    
    private Deck getDeck() {
        Deck deck = new Deck(currentGameType);
        deck.setName(currentDeckName);
        CardList list;
        String cardName;
        
        //always move "bottom" to main
        list = deckDisplay.getBottom();
        for(int i = 0; i < list.size(); i++) {
            cardName = list.get(i).getName();
            deck.addMain(AllZone.NameChanger.getOriginalName(cardName));
        }
        
        //if sealed or draft, move "top" to sideboard
        if(!currentGameType.equals(Constant.GameType.Constructed)) {
            list = deckDisplay.getTop();
            for(int i = 0; i < list.size(); i++) {
                cardName = list.get(i).getName();
                deck.addSideboard(AllZone.NameChanger.getOriginalName(cardName));
            }
        }
        return deck;
    }//getDeck()
    
    private void setupMenu() {
        JMenuItem newConstructed = new JMenuItem("New Deck - Constructed");
        
        JMenuItem newSealed = new JMenuItem("New Deck - Sealed");
        JMenuItem newDraft = new JMenuItem("New Deck - Draft");
        
        JMenuItem newRandomConstructed = new JMenuItem("New Deck - Generate Random Constructed Cardpool");
        JMenuItem newGenerateConstructed = new JMenuItem("New Deck - Generate Constructed Deck");
        

        JMenuItem importDeck = new JMenuItem("Import Deck");
        JMenuItem exportDeck = new JMenuItem("Export Deck");
        JMenuItem downloadDeck = new JMenuItem("Download Deck");
        

        JMenuItem openConstructed = new JMenuItem("Open Deck - Constructed");
        JMenuItem openSealed = new JMenuItem("Open Deck - Sealed");
        JMenuItem openDraft = new JMenuItem("Open Deck - Draft");
        
        newDraftItem = newDraft;
        newDraftItem.setEnabled(false);
        
        JMenuItem save = new JMenuItem("Save");
        JMenuItem saveAs = new JMenuItem("Save As");
        JMenuItem delete = new JMenuItem("Delete");
        JMenuItem close = new JMenuItem("Close");
        
        JMenu fileMenu = new JMenu("Deck Actions");
        fileMenu.add(newConstructed);
        
        fileMenu.add(newSealed);
        fileMenu.add(newDraft);
        fileMenu.addSeparator();
        
        fileMenu.add(openConstructed);
        fileMenu.add(openSealed);
        fileMenu.add(openDraft);
        fileMenu.addSeparator();
        
        fileMenu.add(importDeck);
        fileMenu.add(exportDeck);
        fileMenu.add(downloadDeck);
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
        
        //add listeners
        exportDeck.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                try {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            exportDeck();
                        }
                    });
                } catch(Exception ex) {
                    ErrorViewer.showError(ex);
                    throw new RuntimeException("Gui_DeckEditor_Menu : exportDeck() error - " + ex);
                }
            }
        });
        

        importDeck.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                try {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            importDeck();
                        }
                    });
                } catch(Exception ex) {
                    ErrorViewer.showError(ex);
                    throw new RuntimeException("Gui_DeckEditor_Menu : importDeck() error - " + ex);
                }
            }
        });
        
        downloadDeck.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                try {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            downloadDeck();
                        }
                    });
                } catch(Exception ex) {
                    ErrorViewer.showError(ex);
                    throw new RuntimeException("Gui_DeckEditor_Menu : downloadDeck() error - " + ex);
                }
            }
        });
        
        newConstructed.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                try {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            newConstructed();
                        }
                    });
                } catch(Exception ex) {
                    ErrorViewer.showError(ex);
                    throw new RuntimeException("Gui_DeckEditor_Menu : newConstructed() error - " + ex);
                }
            }
        });
        

        newRandomConstructed.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                try {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            newRandomConstructed();
                        }
                    });
                } catch(Exception ex) {
                    ErrorViewer.showError(ex);
                    throw new RuntimeException("Gui_DeckEditor_Menu : newRandomConstructed() error - " + ex);
                }
            }
        });
        

        newGenerateConstructed.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                try {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            newGenerateConstructed();
                        }
                    });
                } catch(Exception ex) {
                    ErrorViewer.showError(ex);
                    throw new RuntimeException("Gui_DeckEditor_Menu : newRandomConstructed() error - " + ex);
                }
            }
        });
        

        newSealed.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                try {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            newSealed();
                        }
                    });
                } catch(Exception ex) {
                    ErrorViewer.showError(ex);
                    throw new RuntimeException("Gui_DeckEditor_Menu : newSealed() error - " + ex);
                }
            }
        });
        
        newDraft.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                try {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            newDraft();
                        }
                    });
                } catch(Exception ex) {
                    ErrorViewer.showError(ex);
                    throw new RuntimeException("Gui_DeckEditor_Menu : newDraft() error - " + ex);
                }
            }
        });
        
        openConstructed.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                try {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            openConstructed();
                        }
                    });
                } catch(Exception ex) {
                    ErrorViewer.showError(ex);
                    throw new RuntimeException("Gui_DeckEditor_Menu : openConstructed() error - " + ex);
                }
            }
        });
        
        openSealed.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                try {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            openSealed();
                        }
                    });
                } catch(Exception ex) {
                    ErrorViewer.showError(ex);
                    throw new RuntimeException("Gui_DeckEditor_Menu : openSealed() error - " + ex);
                }
            }
        });
        
        openDraft.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                try {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            openDraft();
                        }
                    });
                } catch(Exception ex) {
                    ErrorViewer.showError(ex);
                    throw new RuntimeException("Gui_DeckEditor_Menu : openDraft() error - " + ex);
                }
            }
        });
        
        save.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                try {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            save();
                        }
                    });
                } catch(Exception ex) {
                    ErrorViewer.showError(ex);
                    throw new RuntimeException("Gui_DeckEditor_Menu : save() error - " + ex);
                }
            }
        });
        
        saveAs.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                try {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            saveAs();
                        }
                    });
                } catch(Exception ex) {
                    ErrorViewer.showError(ex);
                    throw new RuntimeException("Gui_DeckEditor_Menu : saveAs() error - " + ex);
                }
            }
        });
        
        delete.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                try {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            delete();
                        }
                    });
                } catch(Exception ex) {
                    ErrorViewer.showError(ex);
                    throw new RuntimeException("Gui_DeckEditor_Menu : delete() error - " + ex);
                }
            }
        });
        
        close.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                try {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            close();
                        }
                    });
                } catch(Exception ex) {
                    ErrorViewer.showError(ex);
                    throw new RuntimeException("Gui_DeckEditor_Menu : close() error - " + ex);
                }
            }
        });
    }//setupMenu()
}
