package forge.gui.deckeditor;

import forge.AllZone;
import forge.Card;
import forge.CardList;
import forge.Command;
import forge.Constant;
import forge.GUI_DeckAnalysis;
import forge.GuiDisplayUtil;
import forge.ImageCache;
import forge.ImagePreviewPanel;
import forge.MyRandom;
import forge.Constant.GameType;
import forge.Constant.Runtime;
import forge.card.CardRules;
import forge.card.CardDb;
import forge.card.CardPool;
import forge.card.CardPoolView;
import forge.card.CardPrinted;
import forge.deck.Deck;
import forge.error.ErrorViewer;
import forge.gui.game.CardDetailPanel;
import forge.gui.game.CardPicturePanel;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;
import forge.view.swing.OldGuiNewGame;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.MouseInputListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileFilter;

import net.slightlymagic.maxmtg.Predicate;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.Random;

//import forge.quest.data.QuestBoosterPack;

/**
 * <p>
 * Gui_Quest_DeckEditor class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class DeckEditorQuest extends DeckEditorBase implements NewConstants {
    /** Constant <code>serialVersionUID=152061168634545L</code> */
    private static final long serialVersionUID = 152061168634545L;

    DeckEditorQuestMenu customMenu;

    // private ImageIcon upIcon = Constant.IO.upIcon;
    // private ImageIcon downIcon = Constant.IO.downIcon;

    private JButton addButton = new JButton();
    private JButton removeButton = new JButton();
    private JButton analysisButton = new JButton();

    private GridLayout gridLayout1 = new GridLayout();

    private JLabel jLabel1 = new JLabel();

    /** {@inheritDoc} */
    @Override
    public void setTitle(String message) {
        super.setTitle(message);
    }

    /** {@inheritDoc} */
    public void setDecks(CardPoolView top, CardPoolView bottom) {

        this.top = new CardPool( top );
        this.bottom = bottom;

        topModel.clear();
        topModel.addCards(top);

        bottomModel.clear();
        bottomModel.addCards(bottom);

        topModel.resort();
        bottomModel.resort();
    }// updateDisplay


    /**
     * <p>
     * show.
     * </p>
     * 
     * @param exitCommand
     *            a {@link forge.Command} object.
     */
    public void show(final Command exitCommand) {
        final Command exit = new Command() {
            private static final long serialVersionUID = -7428793574300520612L;

            public void execute() {
                DeckEditorQuest.this.dispose();
                exitCommand.execute();
            }
        };

        // do not change this!!!!
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent ev) {
                customMenu.close();
            }
        });

        setup();

        customMenu = new DeckEditorQuestMenu(this, exit);
        this.setJMenuBar(customMenu);

        forge.quest.data.QuestData questData = AllZone.getQuestData();
        Deck deck = null;

        // open deck that the player used if QuestData has it
        if (Constant.Runtime.HumanDeck[0] != null
                && questData.getDeckNames().contains(Constant.Runtime.HumanDeck[0].getName())) {
            deck = questData.getDeck(Constant.Runtime.HumanDeck[0].getName());
        } else {
            deck = new Deck(Constant.GameType.Sealed);
            deck.setName("");
        }

        // tell Gui_Quest_DeckEditor the name of the deck
        customMenu.setPlayerDeckName(deck.getName());

        CardPoolView bottomPool = deck.getMain();        
        CardPool cardpool = new CardPool();
        cardpool.addAll(AllZone.getQuestData().getCardpool());

        // remove bottom cards that are in the deck from the card pool
        cardpool.removeAll(bottomPool);

        // show cards, makes this user friendly, lol, well may, ha
        setDecks(cardpool, bottomPool);

        // this affects the card pool
        topModel.sort(4, true);// sort by type
        topModel.sort(3, true);// then sort by color

        bottomModel.sort(1, true);
    }// show(Command)


    /**
     * <p>
     * setup.
     * </p>
     */
    public void setup() {
        List<TableColumnInfo<CardPrinted>> columns = new ArrayList<TableColumnInfo<CardPrinted>>();
        columns.add(new TableColumnInfo<CardPrinted>("Qty", 30, CardColumnPresets.fnQtyCompare, CardColumnPresets.fnQtyGet));
        columns.add(new TableColumnInfo<CardPrinted>("Name", 180, CardColumnPresets.fnNameCompare, CardColumnPresets.fnNameGet));
        columns.add(new TableColumnInfo<CardPrinted>("Cost", 70, CardColumnPresets.fnCostCompare, CardColumnPresets.fnCostGet));
        columns.add(new TableColumnInfo<CardPrinted>("Color", 50, CardColumnPresets.fnColorCompare, CardColumnPresets.fnColorGet));
        columns.add(new TableColumnInfo<CardPrinted>("Type", 100, CardColumnPresets.fnTypeCompare, CardColumnPresets.fnTypeGet));
        columns.add(new TableColumnInfo<CardPrinted>("Stats", 40, CardColumnPresets.fnStatsCompare, CardColumnPresets.fnStatsGet));
        columns.add(new TableColumnInfo<CardPrinted>("R", 35, CardColumnPresets.fnRarityCompare, CardColumnPresets.fnRarityGet));
        columns.add(new TableColumnInfo<CardPrinted>("Set", 40, CardColumnPresets.fnSetCompare, CardColumnPresets.fnSetGet));
        // Add NEW column here
        setupTables(columns, true);


        setSize(1024, 768);
        this.setResizable(false);
        Dimension screen = getToolkit().getScreenSize();
        Rectangle bounds = getBounds();
        bounds.width = 1024;
        bounds.height = 768;
        bounds.x = (screen.width - bounds.width) / 2;
        bounds.y = (screen.height - bounds.height) / 2;
        setBounds(bounds);

        // TODO use this as soon the deck editor has resizable GUI
        // //Use both so that when "un"maximizing, the frame isn't tiny
        // setSize(1024, 740);
        // setExtendedState(Frame.MAXIMIZED_BOTH);
    }// setupAndDisplay()

    public DeckEditorQuest() {
        super(true, false);
        try {
            jbInit();
        } catch (Exception ex) {
            ErrorViewer.showError(ex);
        }
    }


    private void jbInit() throws Exception {

        jbInitTables("All Cards", "Your deck");

        jScrollPane1.setBounds(new Rectangle(19, 20, 726, 346));
        jScrollPane2.setBounds(new Rectangle(19, 458, 726, 218));

        removeButton.setBounds(new Rectangle(180, 403, 146, 49));
        // removeButton.setIcon(upIcon);
        if (!OldGuiNewGame.useLAFFonts.isSelected())
            removeButton.setFont(new java.awt.Font("Dialog", 0, 13));
        removeButton.setText("Remove Card");
        removeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                removeButtonActionPerformed(e);
            }
        });
        addButton.setText("Add Card");
        addButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                addButtonActionPerformed(e);
            }
        });
        // addButton.setIcon(downIcon);
        if (!OldGuiNewGame.useLAFFonts.isSelected())
            addButton.setFont(new java.awt.Font("Dialog", 0, 13));
        addButton.setBounds(new Rectangle(23, 403, 146, 49));

        analysisButton.setText("Deck Analysis");
        analysisButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                analysisButton_actionPerformed(e);
            }
        });
        if (!OldGuiNewGame.useLAFFonts.isSelected())
            analysisButton.setFont(new java.awt.Font("Dialog", 0, 13));
        analysisButton.setBounds(new Rectangle(578, 426, 166, 25));

        cardView.jbInit();
       

        /**
         * Type filtering
         */
        
        filterBoxes.land.setBounds(340, 400, 48, 20);
        filterBoxes.creature.setBounds(385, 400, 65, 20);
        filterBoxes.sorcery.setBounds(447, 400, 62, 20);
        filterBoxes.instant.setBounds(505, 400, 60, 20);
        filterBoxes.planeswalker.setBounds(558, 400, 85, 20);
        filterBoxes.artifact.setBounds(638, 400, 58, 20);
        filterBoxes.enchantment.setBounds(692, 400, 80, 20);
        
        Font f = new Font("Tahoma", Font.PLAIN, 10);
        for (JCheckBox box : filterBoxes.allTypes) {
            if (!OldGuiNewGame.useLAFFonts.isSelected()) { box.setFont(f); }
            box.setOpaque(false);
            box.addItemListener(itemListenerUpdatesDisplay);
        }

        /**
         * Color filtering
         */
        filterBoxes.white.setBounds(340, 430, 40, 20);
        filterBoxes.blue.setBounds(380, 430, 40, 20);
        filterBoxes.black.setBounds(420, 430, 40, 20);
        filterBoxes.red.setBounds(460, 430, 40, 20);
        filterBoxes.green.setBounds(500, 430, 40, 20);
        filterBoxes.colorless.setBounds(540, 430, 40, 20);
        
        for (JCheckBox box : filterBoxes.allColors) {
            box.setOpaque(false);
            box.addItemListener(itemListenerUpdatesDisplay);
        }
        /**
         * Other
         */
        cardView.setBounds(new Rectangle(765, 23, 239, 687));

        if (!OldGuiNewGame.useLAFFonts.isSelected())
            statsLabel.setFont(new java.awt.Font("Dialog", 0, 14));
        statsLabel.setText("Total - 0, Creatures - 0 Land - 0");
        statsLabel.setBounds(new Rectangle(19, 672, 720, 31));
        // Do not lower statsLabel any lower, we want this to be visible at 1024
        // x 768 screen size
        this.setTitle("Deck Editor");
        gridLayout1.setColumns(1);
        gridLayout1.setRows(0);
        statsLabel2.setBounds(new Rectangle(19, 365, 720, 31));
        statsLabel2.setText("Total - 0, Creatures - 0 Land - 0");
        if (!OldGuiNewGame.useLAFFonts.isSelected())
            statsLabel2.setFont(new java.awt.Font("Dialog", 0, 14));
        jLabel1.setText("Click on the column name (like name or color) to sort the cards");
        jLabel1.setBounds(new Rectangle(20, 1, 400, 19));

        this.getContentPane().add(jScrollPane1, null);
        this.getContentPane().add(jScrollPane2, null);
        this.getContentPane().add(addButton, null);
        this.getContentPane().add(removeButton, null);
        this.getContentPane().add(analysisButton, null);
        this.getContentPane().add(statsLabel2, null);
        this.getContentPane().add(statsLabel, null);
        this.getContentPane().add(jLabel1, null);
        this.getContentPane().add(cardView, null);

        for (JCheckBox box : filterBoxes.allTypes) {
            this.getContentPane().add(box, null);
        }

        for (JCheckBox box : filterBoxes.allColors) {
            this.getContentPane().add(box, null);
        }
    }


    final void addButtonActionPerformed(final ActionEvent e) {
        setTitle("Deck Editor : " + customMenu.getDeckName() + " : unsaved");

        int n = topTable.getSelectedRow();
        if (n == -1) { return; }

        CardPrinted c = topModel.rowToCard(n).getKey();
        bottomModel.addCard(c);
        bottomModel.resort();

        // remove from cardpool
        top.remove(c);

        // redraw top after deletion
        updateDisplay();
        fixSelection(topModel, topTable, n);
    }


    final void removeButtonActionPerformed(final ActionEvent e) {
        setTitle("Deck Editor : " + customMenu.getDeckName() + " : unsaved");

        int n = bottomTable.getSelectedRow();
        if (n == -1) { return; }

        CardPrinted c = bottomModel.rowToCard(n).getKey();
        bottomModel.removeCard(c);
        fixSelection(bottomModel, bottomTable, n);

        top.add(c);
        updateDisplay();
    }

}
