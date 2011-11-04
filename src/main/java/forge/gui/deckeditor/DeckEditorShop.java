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
import forge.quest.data.ReadPriceList;

/**
 * <p>
 * Gui_CardShop class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public final class DeckEditorShop extends DeckEditorBase {

    /** Constant <code>serialVersionUID=3988857075791576483L</code>. */
    private static final long serialVersionUID = 3988857075791576483L;

    private final JButton buyButton = new JButton();
    private final JButton sellButton = new JButton();

    private final JLabel creditsLabel = new JLabel();
    private final JLabel jLabel1 = new JLabel();
    private final JLabel sellPercentageLabel = new JLabel();

    private double multiplier;

    private final QuestData questData;
    
    // get pricelist:
    private final ReadPriceList r = new ReadPriceList();
    private final Map<String, Integer> mapPrices = this.r.getPriceList();
    private Map<CardPrinted, Integer> decksUsingMyCards;
    
    /**
     * Show.
     * 
     * @param exitCommand
     *            the exit command
     */
    public void show(final Command exitCommand) {
        final Command exit = new Command() {
            private static final long serialVersionUID = -7428793574300520612L;

            @Override
            public void execute() {
                DeckEditorShop.this.dispose();
                exitCommand.execute();
            }
        };
        
        // do not change this!!!!
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent ev) {
                exit.execute();
            }
        });

        this.setup();

        this.decksUsingMyCards = this.countDecksForEachCard();

        this.multiplier = this.questData.getCards().getSellMutliplier();

        ItemPoolView<InventoryItem> forSale = this.questData.getCards().getShopList();
        if (forSale.isEmpty()) {
            this.questData.getCards().generateCardsInShop();
            forSale = this.questData.getCards().getShopList();
        }
        final ItemPoolView<InventoryItem> owned = this.questData.getCards().getCardpool().getView();
        //newCardsList = questData.getCards().getNewCards();

        this.setItems(forSale, owned, GameType.Quest);

        final double multiPercent = this.multiplier * 100;
        final NumberFormat formatter = new DecimalFormat("#0.00");
        String maxSellingPrice = "";
        final int maxSellPrice = this.questData.getCards().getSellPriceLimit();

        if (maxSellPrice < Integer.MAX_VALUE) {
            maxSellingPrice = String.format("Max selling price: %d", maxSellPrice);
        }
        this.sellPercentageLabel.setText("<html>(You can sell cards at " + formatter.format(multiPercent)
                + "% of their value)<br>" + maxSellingPrice + "</html>");

        this.getTopTableWithCards().sort(1, true);
        this.getBottomTableWithCards().sort(1, true);
    } // show(Command)

    // fills number of decks using each card
    private Map<CardPrinted, Integer> countDecksForEachCard() {
        final Map<CardPrinted, Integer> result = new HashMap<CardPrinted, Integer>();
        for (final String deckName : this.questData.getDeckNames()) {
            final Deck deck = this.questData.getDeck(deckName);
            for (final Entry<CardPrinted, Integer> e : deck.getMain()) {
                final CardPrinted card = e.getKey();
                final Integer iValue = result.get(card);
                final int cntDecks = iValue == null ? 1 : 1 + iValue.intValue();
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
        final List<TableColumnInfo<InventoryItem>> columns = new ArrayList<TableColumnInfo<InventoryItem>>();
        columns.add(new TableColumnInfo<InventoryItem>("Qty", 30, PresetColumns.FN_QTY_COMPARE,
                PresetColumns.FN_QTY_GET));
        columns.add(new TableColumnInfo<InventoryItem>("Name", 180, PresetColumns.FN_NAME_COMPARE,
                PresetColumns.FN_NAME_GET));
        columns.add(new TableColumnInfo<InventoryItem>("Cost", 70, PresetColumns.FN_COST_COMPARE,
                PresetColumns.FN_COST_GET));
        columns.add(new TableColumnInfo<InventoryItem>("Color", 50, PresetColumns.FN_COLOR_COMPARE,
                PresetColumns.FN_COLOR_GET));
        columns.add(new TableColumnInfo<InventoryItem>("Type", 100, PresetColumns.FN_TYPE_COMPARE,
                PresetColumns.FN_TYPE_GET));
        columns.add(new TableColumnInfo<InventoryItem>("Stats", 40, PresetColumns.FN_STATS_COMPARE,
                PresetColumns.FN_STATS_GET));
        columns.add(new TableColumnInfo<InventoryItem>("R", 30, PresetColumns.FN_RARITY_COMPARE,
                PresetColumns.FN_RARITY_GET));
        columns.add(new TableColumnInfo<InventoryItem>("Set", 35, PresetColumns.FN_SET_COMPARE,
                PresetColumns.FN_SET_GET));
        columns.get(2).setCellRenderer(new ManaCostRenderer());
        
        final List<TableColumnInfo<InventoryItem>> columnsBelow = new ArrayList<TableColumnInfo<InventoryItem>>(columns);
        columns.add(new TableColumnInfo<InventoryItem>("Price", 36, this.fnPriceCompare, this.fnPriceGet));
        this.getTopTableWithCards().setup(columns, this.getCardView());

        columnsBelow.add(new TableColumnInfo<InventoryItem>("Dks", 30, this.fnDeckCompare, this.fnDeckGet));
        columnsBelow.add(new TableColumnInfo<InventoryItem>("New", 35, this.questData.getCards().getFnNewCompare(),
                this.questData.getCards().getFnNewGet()));
        columnsBelow.add(new TableColumnInfo<InventoryItem>("Price", 36, this.fnPriceCompare, this.fnPriceSellGet));
        this.getBottomTableWithCards().setup(columnsBelow, this.getCardView());

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
        this.questData = qd;
        try {
            this.setFilterBoxes(null);
            this.setTopTableWithCards(new TableWithCards("Cards for sale", false));
            this.setBottomTableWithCards(new TableWithCards("Owned Cards", false));
            this.setCardView(new CardPanelLite());
            this.jbInit();
        } catch (final Exception ex) {
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
        this.getTopTableWithCards().getTableDecorated().setBounds(new Rectangle(19, 20, 726, 346));
        this.getBottomTableWithCards().getTableDecorated().setBounds(new Rectangle(19, 458, 726, 276));

        this.sellButton.setBounds(new Rectangle(180, 403, 146, 49));
        // removeButton.setIcon(upIcon);
        if (!Singletons.getModel().getPreferences().isLafFonts()) {
            this.sellButton.setFont(new java.awt.Font("Dialog", 0, 13));
            }
        this.sellButton.setText("Sell Card");
        this.sellButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                DeckEditorShop.this.sellButtonActionPerformed(e);
            }
        });
        this.buyButton.setText("Buy Card");
        this.buyButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                DeckEditorShop.this.buyButtonActionPerformed(e);
            }
        });

        if (!Singletons.getModel().getPreferences().isLafFonts()) {
            this.buyButton.setFont(new java.awt.Font("Dialog", 0, 13));
        }
        this.buyButton.setBounds(new Rectangle(23, 403, 146, 49));

        this.getCardView().setBounds(new Rectangle(765, 23, 239, 710));
        // Do not lower statsLabel any lower, we want this to be visible at 1024
        // x 768 screen size
        this.setTitle("Card Shop");

        this.creditsLabel.setBounds(new Rectangle(19, 365, 720, 31));
        this.creditsLabel.setText("Total credits: " + this.questData.getCredits());
        if (!Singletons.getModel().getPreferences().isLafFonts()) {
            this.creditsLabel.setFont(new java.awt.Font("Dialog", 0, 14));
        }
        this.sellPercentageLabel.setBounds(new Rectangle(350, 403, 450, 31));
        this.sellPercentageLabel.setText("(Sell percentage: " + this.multiplier + ")");
        if (!Singletons.getModel().getPreferences().isLafFonts()) {
            this.sellPercentageLabel.setFont(new java.awt.Font("Dialog", 0, 14));
        }
        this.jLabel1.setText("Click on the column name (like name or color) to sort the cards");
        this.jLabel1.setBounds(new Rectangle(20, 1, 400, 19));
        
        this.getContentPane().add(this.getCardView(), null);
        this.getContentPane().add(this.getTopTableWithCards().getTableDecorated(), null);
        this.getContentPane().add(this.getBottomTableWithCards().getTableDecorated(), null);
        this.getContentPane().add(this.creditsLabel, null);
        this.getContentPane().add(this.buyButton, null);
        this.getContentPane().add(this.sellButton, null);
        this.getContentPane().add(this.sellPercentageLabel, null);
        this.getContentPane().add(this.jLabel1, null);
    }

    // TODO: move to cardshop
    private Integer getCardValue(final InventoryItem card) {
        if (this.mapPrices.containsKey(card.getName())) {
            return this.mapPrices.get(card.getName());
        } else if (card instanceof CardPrinted)  {
            switch (((CardPrinted) card).getRarity()) {
            case BasicLand:
                return Integer.valueOf(4);
            case Common:
                return Integer.valueOf(6);
            case Uncommon:
                return Integer.valueOf(40);
            case Rare:
                return Integer.valueOf(120);
            case MythicRare:
                return Integer.valueOf(600);
            default:
                return Integer.valueOf(15);
            }
        } else if (card instanceof BoosterPack) {
            return 395;
        }
        return 1337;
    }

    private void buyButtonActionPerformed(final ActionEvent e) {
        final InventoryItem item = this.getTopTableWithCards().getSelectedCard();
        if (item == null) {
            return;
        }

        final int value = this.getCardValue(item);

        if (value <= this.questData.getCredits()) {
            if (item instanceof CardPrinted) {
                final CardPrinted card = (CardPrinted) item;
                this.getBottomTableWithCards().addCard(card);
                this.getTopTableWithCards().removeCard(card);

                this.questData.getCards().buyCard(card, value);
            } else if (item instanceof BoosterPack) {
                this.getTopTableWithCards().removeCard(item);
                final BoosterPack booster = (BoosterPack) ((BoosterPack) item).clone();
                this.questData.getCards().buyBooster(booster, value);
                final List<CardPrinted> newCards = booster.getCards();
                for (final CardPrinted card : newCards) {
                    this.getBottomTableWithCards().addCard(card);
                }
                final CardListViewer c = new CardListViewer(booster.getName(),
                        "You have found the following cards inside:", newCards);
                c.show();
            }

            this.creditsLabel.setText("Total credits: " + this.questData.getCredits());
        } else {
            JOptionPane.showMessageDialog(null, "Not enough credits!");
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.gui.deckeditor.DeckEditorBase#buildFilter()
     */
    @Override
    protected Predicate<InventoryItem> buildFilter() {
        return Predicate.getTrue(InventoryItem.class);
    }

    private void sellButtonActionPerformed(final ActionEvent e) {
        final InventoryItem item = this.getBottomTableWithCards().getSelectedCard();
        if ((item == null) || !(item instanceof CardPrinted)) {
            return;
        }

        final CardPrinted card = (CardPrinted) item;
        this.getBottomTableWithCards().removeCard(card);
        this.getTopTableWithCards().addCard(card);

        final int price = Math.min((int) (this.multiplier * this.getCardValue(card)), this.questData.getCards()
                .getSellPriceLimit());
        this.questData.getCards().sellCard(card, price);

        this.creditsLabel.setText("Total credits: " + this.questData.getCredits());
    }

    @SuppressWarnings("rawtypes")
    private final Lambda1<Comparable, Entry<InventoryItem, Integer>> fnPriceCompare = new Lambda1<Comparable, Entry<InventoryItem, Integer>>() {
        @Override
        public Comparable apply(final Entry<InventoryItem, Integer> from) {
            return DeckEditorShop.this.getCardValue(from.getKey());
        }
    };
    private final Lambda1<Object, Entry<InventoryItem, Integer>> fnPriceGet = new Lambda1<Object, Entry<InventoryItem, Integer>>() {
        @Override
        public Object apply(final Entry<InventoryItem, Integer> from) {
            return DeckEditorShop.this.getCardValue(from.getKey());
        }
    };
    private final Lambda1<Object, Entry<InventoryItem, Integer>> fnPriceSellGet = new Lambda1<Object, Entry<InventoryItem, Integer>>() {
        @Override
        public Object apply(final Entry<InventoryItem, Integer> from) {
            return (int) (DeckEditorShop.this.multiplier * DeckEditorShop.this.getCardValue(from.getKey()));
        }
    };
            
    @SuppressWarnings("rawtypes")
    private final Lambda1<Comparable, Entry<InventoryItem, Integer>> fnDeckCompare = new Lambda1<Comparable, Entry<InventoryItem, Integer>>() {
        @Override
            public Comparable apply(final Entry<InventoryItem, Integer> from) {
            final Integer iValue = DeckEditorShop.this.decksUsingMyCards.get(from.getKey());
                return iValue == null ? Integer.valueOf(0) : iValue;
        }
    };
    private final Lambda1<Object, Entry<InventoryItem, Integer>> fnDeckGet = new Lambda1<Object, Entry<InventoryItem, Integer>>() {
        @Override
            public Object apply(final Entry<InventoryItem, Integer> from) {
            final Integer iValue = DeckEditorShop.this.decksUsingMyCards.get(from.getKey());
                return iValue == null ? "" : iValue.toString();
        }
    };

}
