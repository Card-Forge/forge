package forge.gui.deckeditor;

import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import net.slightlymagic.braids.util.lambda.Lambda1;
import net.slightlymagic.maxmtg.Predicate;

import forge.Command;
import forge.ReadPriceList;
import forge.Singletons;
import forge.deck.Deck;
import forge.error.ErrorViewer;
import forge.game.GameType;
import forge.gui.CardListViewer;
import forge.gui.GuiUtils;
import forge.item.BoosterPack;
import forge.item.CardPrinted;
import forge.item.InventoryItem;
import forge.item.ItemPoolView;
import forge.quest.data.QuestData;

/**
 * <p>
 * Gui_CardShop class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public final class DeckEditorShop extends DeckEditorBase {

    /** Constant <code>serialVersionUID=3988857075791576483L</code> */
    private static final long serialVersionUID = 3988857075791576483L;

    private JButton buyButton = new JButton();
    private JButton sellButton = new JButton();

    private JLabel creditsLabel = new JLabel();
    private JLabel jLabel1 = new JLabel();
    private JLabel sellPercentageLabel = new JLabel();

    private double multiplier;

    private QuestData questData;
    //private CardPoolView newCardsList;
    
    // get pricelist:
    private ReadPriceList r = new ReadPriceList();
    private Map<String, Integer> mapPrices = r.getPriceList();
    private Map<CardPrinted, Integer> decksUsingMyCards;
    


    public void show(final Command exitCommand) {
        final Command exit = new Command() {
            private static final long serialVersionUID = -7428793574300520612L;

            public void execute() {
                DeckEditorShop.this.dispose();
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

        decksUsingMyCards = countDecksForEachCard();

        multiplier = questData.getCards().getSellMutliplier();

        ItemPoolView<InventoryItem> forSale = questData.getCards().getShopList();
        if (forSale.isEmpty()) {
            questData.getCards().generateCardsInShop();
            forSale = questData.getCards().getShopList();
        }
        ItemPoolView<InventoryItem> owned = questData.getCards().getCardpool().getView();
        //newCardsList = questData.getCards().getNewCards();

        setItems(forSale, owned, GameType.Quest);

        double multiPercent = multiplier * 100;
        NumberFormat formatter = new DecimalFormat("#0.00");
        String maxSellingPrice = "";
        int maxSellPrice = questData.getCards().getSellPriceLimit();

        if (maxSellPrice < Integer.MAX_VALUE) { maxSellingPrice = String.format("Max selling price: %d", maxSellPrice); }
        sellPercentageLabel.setText("<html>(You can sell cards at " + formatter.format(multiPercent) + "% of their value)<br>" + maxSellingPrice + "</html>");

        top.sort(1, true);
        bottom.sort(1, true);
    } // show(Command)

    // fills number of decks using each card
    private Map<CardPrinted, Integer> countDecksForEachCard() {
        Map<CardPrinted, Integer> result = new HashMap<CardPrinted, Integer>();
        for (String deckName : questData.getDeckNames()) {
            Deck deck = questData.getDeck(deckName);
            for (Entry<CardPrinted, Integer> e : deck.getMain()) {
                CardPrinted card = e.getKey();
                Integer iValue = result.get(card);
                int cntDecks = iValue == null ? 1 : 1 + iValue.intValue();
                result.put(card, Integer.valueOf(cntDecks));
            }
        }
        return result;
    }
    
    /**
     * <p>
     * setup.
     * </p>
     */
    private void setup() {
        List<TableColumnInfo<InventoryItem>> columns = new ArrayList<TableColumnInfo<InventoryItem>>();
        columns.add(new TableColumnInfo<InventoryItem>("Qty", 30, PresetColumns.fnQtyCompare, PresetColumns.fnQtyGet));
        columns.add(new TableColumnInfo<InventoryItem>("Name", 180, PresetColumns.fnNameCompare, PresetColumns.fnNameGet));
        columns.add(new TableColumnInfo<InventoryItem>("Cost", 70, PresetColumns.fnCostCompare, PresetColumns.fnCostGet));
        columns.add(new TableColumnInfo<InventoryItem>("Color", 50, PresetColumns.fnColorCompare, PresetColumns.fnColorGet));
        columns.add(new TableColumnInfo<InventoryItem>("Type", 100, PresetColumns.fnTypeCompare, PresetColumns.fnTypeGet));
        columns.add(new TableColumnInfo<InventoryItem>("Stats", 40, PresetColumns.fnStatsCompare, PresetColumns.fnStatsGet));
        columns.add(new TableColumnInfo<InventoryItem>("R", 30, PresetColumns.fnRarityCompare, PresetColumns.fnRarityGet));
        columns.add(new TableColumnInfo<InventoryItem>("Set", 35, PresetColumns.fnSetCompare, PresetColumns.fnSetGet));
        columns.get(2).setCellRenderer(new ManaCostRenderer());
        
        List<TableColumnInfo<InventoryItem>> columnsBelow = new ArrayList<TableColumnInfo<InventoryItem>>(columns);
        columns.add(new TableColumnInfo<InventoryItem>("Price", 40, fnPriceCompare, fnPriceGet));
        top.setup(columns, cardView);

        columnsBelow.add(new TableColumnInfo<InventoryItem>("Dks", 30, fnDeckCompare, fnDeckGet));
        columnsBelow.add(new TableColumnInfo<InventoryItem>("New", 35, questData.getCards().fnNewCompare, questData.getCards().fnNewGet));
        columnsBelow.add(new TableColumnInfo<InventoryItem>("Price", 35, fnPriceCompare, fnPriceSellGet));
        bottom.setup(columnsBelow, cardView);

        this.setSize(1024, 768);
        GuiUtils.centerFrame(this);
        this.setResizable(false);
    }

    /**
     * <p>
     * Constructor for Gui_CardShop.
     * </p>
     * 
     * @param qd
     *            a {@link forge.quest.data.QuestData} object.
     */
    public DeckEditorShop(final QuestData qd) {
        super(GameType.Quest);
        questData = qd;
        try {
            filterBoxes = null;
            top = new TableWithCards("Cards for sale", false);
            bottom = new TableWithCards("Owned Cards", false);
            cardView = new CardPanelLite();
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

        this.setLayout(null);
        top.getTableDecorated().setBounds(new Rectangle(19, 20, 726, 346));
        bottom.getTableDecorated().setBounds(new Rectangle(19, 458, 726, 276));

        sellButton.setBounds(new Rectangle(180, 403, 146, 49));
        // removeButton.setIcon(upIcon);
        if (!Singletons.getModel().getPreferences().lafFonts)
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

        if (!Singletons.getModel().getPreferences().lafFonts)
            buyButton.setFont(new java.awt.Font("Dialog", 0, 13));
        buyButton.setBounds(new Rectangle(23, 403, 146, 49));

        cardView.setBounds(new Rectangle(765, 23, 239, 710));
        // Do not lower statsLabel any lower, we want this to be visible at 1024
        // x 768 screen size
        this.setTitle("Card Shop");

        creditsLabel.setBounds(new Rectangle(19, 365, 720, 31));
        creditsLabel.setText("Total credits: " + questData.getCredits());
        if (!Singletons.getModel().getPreferences().lafFonts)
            creditsLabel.setFont(new java.awt.Font("Dialog", 0, 14));
        sellPercentageLabel.setBounds(new Rectangle(350, 403, 450, 31));
        sellPercentageLabel.setText("(Sell percentage: " + multiplier + ")");
        if (!Singletons.getModel().getPreferences().lafFonts)
            sellPercentageLabel.setFont(new java.awt.Font("Dialog", 0, 14));
        jLabel1.setText("Click on the column name (like name or color) to sort the cards");
        jLabel1.setBounds(new Rectangle(20, 1, 400, 19));
        
        this.getContentPane().add(cardView, null);
        this.getContentPane().add(top.getTableDecorated(), null);
        this.getContentPane().add(bottom.getTableDecorated(), null);
        this.getContentPane().add(creditsLabel, null);
        this.getContentPane().add(buyButton, null);
        this.getContentPane().add(sellButton, null);
        this.getContentPane().add(sellPercentageLabel, null);
        this.getContentPane().add(jLabel1, null);
    }

    // TODO: move to cardshop
    private Integer getCardValue(final InventoryItem card) {
        if (mapPrices.containsKey(card.getName())) {
            return mapPrices.get(card.getName());
        } else if (card instanceof CardPrinted)  {
            switch (((CardPrinted) card).getRarity()) {
            case BasicLand: return Integer.valueOf(4);
            case Common: return Integer.valueOf(6);
            case Uncommon: return Integer.valueOf(40);
            case Rare: return Integer.valueOf(120);
            case MythicRare: return Integer.valueOf(600);
            default: return Integer.valueOf(15);
            }
        } else if (card instanceof BoosterPack) {
            return 395;
        }
        return 1337;
    }

    private void buyButton_actionPerformed(ActionEvent e) {
        InventoryItem item = top.getSelectedCard();
        if (item == null ) { return; }

        int value = getCardValue(item);

        if (value <= questData.getCredits()) {
            if (item instanceof CardPrinted) {
                CardPrinted card = (CardPrinted) item;
                bottom.addCard(card);
                top.removeCard(card);

                questData.getCards().buyCard(card, value);
            } else if (item instanceof BoosterPack) {
                top.removeCard(item);
                BoosterPack booster = (BoosterPack) ((BoosterPack) item).clone();
                questData.getCards().buyBooster(booster, value);
                List<CardPrinted> newCards = booster.getCards();
                for (CardPrinted card : newCards) { bottom.addCard(card); }
                CardListViewer c = new CardListViewer(booster.getName(), "You have found the following cards inside:", newCards);
                c.show();
            }

            creditsLabel.setText("Total credits: " + questData.getCredits());
        } else {
            JOptionPane.showMessageDialog(null, "Not enough credits!");
        }
    }

    @Override
    protected Predicate<InventoryItem> buildFilter() {
        return Predicate.getTrue(InventoryItem.class);
    }

    private void sellButton_actionPerformed(ActionEvent e) {
        InventoryItem item = bottom.getSelectedCard();
        if (item == null || !( item instanceof CardPrinted )) { return; }

        CardPrinted card = (CardPrinted) item;
        bottom.removeCard(card);
        top.addCard(card);

        int price = Math.min((int) (multiplier * getCardValue(card)), questData.getCards().getSellPriceLimit());
        questData.getCards().sellCard(card, price);

        creditsLabel.setText("Total credits: " + questData.getCredits());
    }

    @SuppressWarnings("rawtypes")
    private final Lambda1<Comparable, Entry<InventoryItem, Integer>> fnPriceCompare =
        new Lambda1<Comparable, Entry<InventoryItem, Integer>>() { @Override
            public Comparable apply(final Entry<InventoryItem, Integer> from) { return getCardValue(from.getKey()); } };
    private final Lambda1<Object, Entry<InventoryItem, Integer>> fnPriceGet =
        new Lambda1<Object, Entry<InventoryItem, Integer>>() { @Override
            public Object apply(final Entry<InventoryItem, Integer> from) { return getCardValue(from.getKey()); } };
    private final Lambda1<Object, Entry<InventoryItem, Integer>> fnPriceSellGet =
            new Lambda1<Object, Entry<InventoryItem, Integer>>() { @Override
                public Object apply(final Entry<InventoryItem, Integer> from) { return (int) (multiplier * getCardValue(from.getKey())); } };
            
    @SuppressWarnings("rawtypes")
    private final Lambda1<Comparable, Entry<InventoryItem, Integer>> fnDeckCompare =
        new Lambda1<Comparable, Entry<InventoryItem, Integer>>() { @Override
            public Comparable apply(final Entry<InventoryItem, Integer> from) {
                Integer iValue = decksUsingMyCards.get(from.getKey());
                return iValue == null ? Integer.valueOf(0) : iValue;
            } };
    private final Lambda1<Object, Entry<InventoryItem, Integer>> fnDeckGet =
        new Lambda1<Object, Entry<InventoryItem, Integer>>() { @Override
            public Object apply(final Entry<InventoryItem, Integer> from) {
                Integer iValue = decksUsingMyCards.get(from.getKey());
                return iValue == null ? "" : iValue.toString();
            } };


}
