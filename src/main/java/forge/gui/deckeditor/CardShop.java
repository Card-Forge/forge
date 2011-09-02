package forge.gui.deckeditor;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import net.slightlymagic.braids.util.lambda.Lambda1;

import forge.Command;
import forge.ReadPriceList;
import forge.card.CardPool;
import forge.card.CardPoolView;
import forge.card.CardPrinted;
import forge.deck.Deck;
import forge.error.ErrorViewer;
import forge.view.swing.OldGuiNewGame;

/**
 * <p>
 * Gui_CardShop class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class CardShop extends DeckEditorBase {

    /** Constant <code>serialVersionUID=3988857075791576483L</code> */
    private static final long serialVersionUID = 3988857075791576483L;

    private JButton buyButton = new JButton();
    private JButton sellButton = new JButton();

    private JScrollPane jScrollPane3 = new JScrollPane();
    private JPanel jPanel3 = new JPanel();
    private GridLayout gridLayout1 = new GridLayout();
    private JLabel creditsLabel = new JLabel();
    private JLabel jLabel1 = new JLabel();
    private JLabel sellPercentageLabel = new JLabel();

    private double multiplier;

    private forge.quest.data.QuestData questData;

    // get pricelist:
    private ReadPriceList r = new ReadPriceList();
    private Map<String, Integer> mapPrices = r.getPriceList();


    /** {@inheritDoc} */
    public void setDecks(CardPoolView topParam, CardPoolView bottomParam) {
        this.top = new CardPool(topParam);
        this.bottom = bottomParam;

        topModel.clear();
        bottomModel.clear();

        // update top
        topModel.addCards(topParam);
        bottomModel.addCards(bottomParam);

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
                CardShop.this.dispose();
                exitCommand.execute();
            }
        };

        // do not change this!!!!
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent ev) {
                exit.execute();
            }
        });

        setup();

        multiplier = questData.getSellMutliplier();

        CardPoolView forSale = questData.getShopList();
        if (forSale.isEmpty()) {
            questData.generateCardsInShop();
            forSale = questData.getShopList();
        }
        CardPoolView owned = questData.getCardpool().getView();

        setDecks(forSale, owned);

        double multiPercent = multiplier * 100;
        NumberFormat formatter = new DecimalFormat("#0.00");
        String maxSellingPrice = "";
        int maxSellPrice = questData.getSellPriceLimit();

        if (maxSellPrice < Integer.MAX_VALUE) { maxSellingPrice = String.format("     Max selling price: %d", maxSellPrice); }
        sellPercentageLabel.setText("(Sell percentage: " + formatter.format(multiPercent) + "% of value)" + maxSellingPrice);

        topModel.sort(1, true);
        bottomModel.sort(1, true);
    }// show(Command)

    /**
     * <p>
     * setup.
     * </p>
     */
    private void setup() {
        List<TableColumnInfo<CardPrinted>> columns = new ArrayList<TableColumnInfo<CardPrinted>>();
        columns.add(new TableColumnInfo<CardPrinted>("Qty", 30, CardColumnPresets.fnQtyCompare, CardColumnPresets.fnQtyGet));
        columns.add(new TableColumnInfo<CardPrinted>("Name", 180, CardColumnPresets.fnNameCompare, CardColumnPresets.fnNameGet));
        columns.add(new TableColumnInfo<CardPrinted>("Cost", 70, CardColumnPresets.fnCostCompare, CardColumnPresets.fnCostGet));
        columns.add(new TableColumnInfo<CardPrinted>("Color", 50, CardColumnPresets.fnColorCompare, CardColumnPresets.fnColorGet));
        columns.add(new TableColumnInfo<CardPrinted>("Type", 100, CardColumnPresets.fnTypeCompare, CardColumnPresets.fnTypeGet));
        columns.add(new TableColumnInfo<CardPrinted>("Stats", 40, CardColumnPresets.fnStatsCompare, CardColumnPresets.fnStatsGet));
        columns.add(new TableColumnInfo<CardPrinted>("R", 35, CardColumnPresets.fnRarityCompare, CardColumnPresets.fnRarityGet));
        columns.add(new TableColumnInfo<CardPrinted>("Set", 40, CardColumnPresets.fnSetCompare, CardColumnPresets.fnSetGet));
        columns.add(new TableColumnInfo<CardPrinted>("Price", 40, fnPriceCompare, fnPriceGet));

        setupTables(columns, false);

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

    /**
     * <p>
     * Constructor for Gui_CardShop.
     * </p>
     * 
     * @param qd
     *            a {@link forge.quest.data.QuestData} object.
     */
    public CardShop(forge.quest.data.QuestData qd) {
        super(false, false);
        questData = qd;
        try {
            jbInit();
        } catch (Exception ex) {
            ErrorViewer.showError(ex);
        }
    }


    /**
     * <p>
     * jbInit.
     * </p>
     * 
     * @throws java.lang.Exception
     *             if any.
     */
    private void jbInit() throws Exception {

        jbInitTables("Cards for sale", "Owned Cards");

        jScrollPane1.setBounds(new Rectangle(19, 20, 726, 346));
        jScrollPane2.setBounds(new Rectangle(19, 458, 726, 276));

        sellButton.setBounds(new Rectangle(180, 403, 146, 49));
        // removeButton.setIcon(upIcon);
        if (!OldGuiNewGame.useLAFFonts.isSelected())
            sellButton.setFont(new java.awt.Font("Dialog", 0, 13));
        sellButton.setText("Sell Card");
        sellButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sellButton_actionPerformed(e);
            }
        });
        buyButton.setText("Buy Card");
        buyButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent e) {
                buyButton_actionPerformed(e);
            }
        });

        if (!OldGuiNewGame.useLAFFonts.isSelected())
            buyButton.setFont(new java.awt.Font("Dialog", 0, 13));
        buyButton.setBounds(new Rectangle(23, 403, 146, 49));

        cardView.jbInit();
        cardView.setBounds(new Rectangle(765, 23, 239, 710));
        // Do not lower statsLabel any lower, we want this to be visible at 1024
        // x 768 screen size
        this.setTitle("Card Shop");
        jScrollPane3.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        jScrollPane3.setBounds(new Rectangle(6, 168, 225, 143));
        jPanel3.setBounds(new Rectangle(7, 21, 224, 141));
        jPanel3.setLayout(gridLayout1);
        gridLayout1.setColumns(1);
        gridLayout1.setRows(0);
        creditsLabel.setBounds(new Rectangle(19, 365, 720, 31));
        creditsLabel.setText("Total credits: " + questData.getCredits());
        if (!OldGuiNewGame.useLAFFonts.isSelected())
            creditsLabel.setFont(new java.awt.Font("Dialog", 0, 14));
        sellPercentageLabel.setBounds(new Rectangle(350, 403, 450, 31));
        sellPercentageLabel.setText("(Sell percentage: " + multiplier + ")");
        if (!OldGuiNewGame.useLAFFonts.isSelected())
            sellPercentageLabel.setFont(new java.awt.Font("Dialog", 0, 14));
        jLabel1.setText("Click on the column name (like name or color) to sort the cards");
        jLabel1.setBounds(new Rectangle(20, 1, 400, 19));
        this.getContentPane().add(cardView, null);
        this.getContentPane().add(jScrollPane1, null);
        this.getContentPane().add(jScrollPane2, null);
        this.getContentPane().add(creditsLabel, null);
        this.getContentPane().add(buyButton, null);
        this.getContentPane().add(sellButton, null);
        this.getContentPane().add(sellPercentageLabel, null);
        this.getContentPane().add(jLabel1, null);
    }

    // TODO: move to cardshop
    private Integer getCardValue(final CardPrinted card) {
        if (mapPrices.containsKey(card.getName())) {
            return mapPrices.get(card.getName());
        } else {
            switch (card.getRarity()) {
            case BasicLand: return Integer.valueOf(4);
            case Common: return Integer.valueOf(6);
            case Uncommon: return Integer.valueOf(40);
            case Rare: return Integer.valueOf(120);
            case MythicRare: return Integer.valueOf(600);
            default: return Integer.valueOf(15);
            }
        }
    }

    private void buyButton_actionPerformed(ActionEvent e) {
        int n = topTable.getSelectedRow();
        if (n != -1) {
            CardPrinted c = topModel.rowToCard(n).getKey();
            int value = getCardValue(c);

            if (value <= questData.getCredits()) {
                bottomModel.addCard(c);
                bottomModel.resort();

                topModel.removeCard(c);

                questData.buyCard(c, value);

                creditsLabel.setText("Total credits: " + questData.getCredits());
                fixSelection(topModel, topTable, n);

            } else {
                JOptionPane.showMessageDialog(null, "Not enough credits!");
            }
        }// if(valid row)
    }// buyButton_actionPerformed

    /**
     * <p>
     * sellButton_actionPerformed.
     * </p>
     * 
     * @param e
     *            a {@link java.awt.event.ActionEvent} object.
     */
    void sellButton_actionPerformed(ActionEvent e) {

        int n = bottomTable.getSelectedRow();
        if (n != -1) {
            CardPrinted c = bottomModel.rowToCard(n).getKey();
            bottomModel.removeCard(c);

            topModel.addCard(c);
            topModel.resort();

            // bottomModel.removeCard(c);
            questData.addCardToShopList(c);

            int price = Math.min((int) (multiplier * getCardValue(c)), questData.getSellPriceLimit());

            questData.sellCard(c, price);
            creditsLabel.setText("Total credits: " + questData.getCredits());

            int leftInPool = questData.getCardpool().count(c);
            // remove sold cards from all decks:
            for (String deckName : questData.getDeckNames()) {
                Deck deck = questData.getDeck(deckName);
                for (int cntInDeck = deck.getMain().count(c); cntInDeck > leftInPool; cntInDeck--) {
                    deck.removeMain(c);
                }
            }

            fixSelection(bottomModel, bottomTable, n);

        }// if(valid row)
    }// sellButton_actionPerformed

    @SuppressWarnings("rawtypes")
    protected final Lambda1<Comparable, Entry<CardPrinted, Integer>> fnPriceCompare =
        new Lambda1<Comparable, Entry<CardPrinted, Integer>>() { @Override
            public Comparable apply(final Entry<CardPrinted, Integer> from) { return getCardValue(from.getKey()); } };
    protected final Lambda1<Object, Entry<CardPrinted, Integer>> fnPriceGet =
        new Lambda1<Object, Entry<CardPrinted, Integer>>() { @Override
            public Object apply(final Entry<CardPrinted, Integer> from) { return getCardValue(from.getKey()); } };
            

}
