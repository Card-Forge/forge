package forge.gui.deckeditor;

import java.awt.Container;
import java.awt.Dialog.ModalityType;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;
import net.slightlymagic.maxmtg.Predicate;
import forge.AllZone;
import forge.Command;
import forge.Singletons;
import forge.error.ErrorViewer;
import forge.game.GameType;
import forge.item.CardDb;
import forge.item.CardPrinted;
import forge.item.InventoryItem;
import forge.item.ItemPool;
import forge.item.ItemPoolView;

/**
 * <p>
 * Gui_DeckEditor class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public final class DeckEditorCommon extends DeckEditorBase {
    /** Constant <code>serialVersionUID=130339644136746796L</code>. */
    private static final long serialVersionUID = 130339644136746796L;

    /** The custom menu. */
    private DeckEditorCommonMenu customMenu;

    private final JButton removeButton = new JButton();
    private final JButton addButton = new JButton();
    private final JButton importButton = new JButton();

    private final JButton analysisButton = new JButton();
    private final JButton clearFilterButton = new JButton();

    private final JLabel jLabelAnalysisGap = new JLabel("");
    private FilterNameTypeSetPanel filterNameTypeSet;

    /**
     * Show.
     * 
     * @param exitCommand
     *            the exit command
     */
    public void show(final Command exitCommand) {
        final Command exit = new Command() {
            private static final long serialVersionUID = 5210924838133689758L;

            @Override
            public void execute() {
                DeckEditorCommon.this.dispose();
                exitCommand.execute();
            }
        };

        this.setCustomMenu(new DeckEditorCommonMenu(this, AllZone.getDeckManager(), exit));
        this.setJMenuBar(this.getCustomMenu());

        // do not change this!!!!
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent ev) {
                DeckEditorCommon.this.getCustomMenu().close();
            }
        });

        this.setup();

        // show cards, makes this user friendly
        if (!this.getGameType().isLimited()) {
            this.getCustomMenu().newConstructed(false);
        }

        this.getTopTableWithCards().sort(1, true);
        this.getBottomTableWithCards().sort(1, true);

    } // show(Command)

    private void setup() {
        final List<TableColumnInfo<InventoryItem>> columns = new ArrayList<TableColumnInfo<InventoryItem>>();
        columns.add(new TableColumnInfo<InventoryItem>("Qty", 30, PresetColumns.FN_QTY_COMPARE,
                PresetColumns.FN_QTY_GET));
        columns.add(new TableColumnInfo<InventoryItem>("Name", 175, PresetColumns.FN_NAME_COMPARE,
                PresetColumns.FN_NAME_GET));
        columns.add(new TableColumnInfo<InventoryItem>("Cost", 75, PresetColumns.FN_COST_COMPARE,
                PresetColumns.FN_COST_GET));
        columns.add(new TableColumnInfo<InventoryItem>("Color", 60, PresetColumns.FN_COLOR_COMPARE,
                PresetColumns.FN_COLOR_GET));
        columns.add(new TableColumnInfo<InventoryItem>("Type", 100, PresetColumns.FN_TYPE_COMPARE,
                PresetColumns.FN_TYPE_GET));
        columns.add(new TableColumnInfo<InventoryItem>("Stats", 60, PresetColumns.FN_STATS_COMPARE,
                PresetColumns.FN_STATS_GET));
        columns.add(new TableColumnInfo<InventoryItem>("R", 25, PresetColumns.FN_RARITY_COMPARE,
                PresetColumns.FN_RARITY_GET));
        columns.add(new TableColumnInfo<InventoryItem>("Set", 40, PresetColumns.FN_SET_COMPARE,
                PresetColumns.FN_SET_GET));
        columns.add(new TableColumnInfo<InventoryItem>("AI", 30, PresetColumns.FN_AI_STATUS_COMPARE,
                PresetColumns.FN_AI_STATUS_GET));
        columns.get(2).setCellRenderer(new ManaCostRenderer());

        this.getTopTableWithCards().setup(columns, this.getCardView());
        this.getBottomTableWithCards().setup(columns, this.getCardView());

        this.filterNameTypeSet.setListeners(new OnChangeTextUpdateDisplay(), this.getItemListenerUpdatesDisplay());

        this.setSize(1024, 740);
        this.setExtendedState(Frame.MAXIMIZED_BOTH);

    }

    /**
     * Instantiates a new deck editor common.
     * 
     * @param gameType
     *            the game type
     */
    public DeckEditorCommon(final GameType gameType) {
        super(gameType);
        try {
            this.setFilterBoxes(new FilterCheckBoxes(true));
            this.setTopTableWithCards(new TableWithCards("Avaliable Cards", true, true));
            this.setBottomTableWithCards(new TableWithCards("Deck", true));
            this.setCardView(new CardPanelHeavy());
            this.filterNameTypeSet = new FilterNameTypeSetPanel();

            this.jbInit();
        } catch (final Exception ex) {
            ErrorViewer.showError(ex);
        }
    }

    private void jbInit() {

        if (!Singletons.getModel().getPreferences().isLafFonts()) {
            final Font fButtons = new java.awt.Font("Dialog", 0, 13);
            this.removeButton.setFont(fButtons);
            this.addButton.setFont(fButtons);
            this.importButton.setFont(fButtons);
            this.clearFilterButton.setFont(fButtons);
            this.analysisButton.setFont(fButtons);
        }

        this.addButton.setText("Add to Deck");
        this.removeButton.setText("Remove from Deck");
        this.importButton.setText("Import a Deck");
        this.clearFilterButton.setText("Clear Filter");
        this.analysisButton.setText("Deck Analysis");

        this.removeButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                DeckEditorCommon.this.removeButtonClicked(e);
            }
        });
        this.addButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                DeckEditorCommon.this.addButtonActionPerformed(e);
            }
        });
        this.importButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                DeckEditorCommon.this.importButtonActionPerformed(e);
            }
        });
        this.clearFilterButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                DeckEditorCommon.this.clearFilterButtonActionPerformed(e);
            }
        });
        this.analysisButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                DeckEditorCommon.this.analysisButtonActionPerformed(e);
            }
        });

        // Type filtering
        final Font f = new Font("Tahoma", Font.PLAIN, 10);
        for (final JCheckBox box : this.getFilterBoxes().getAllTypes()) {
            if (!Singletons.getModel().getPreferences().isLafFonts()) {
                box.setFont(f);
            }
            box.setOpaque(false);
        }

        // Color filtering
        for (final JCheckBox box : this.getFilterBoxes().getAllColors()) {
            box.setOpaque(false);
        }

        // Do not lower statsLabel any lower, we want this to be visible at 1024
        // x 768 screen size
        this.setTitle("Deck Editor");

        final Container content = this.getContentPane();
        final MigLayout layout = new MigLayout("fill");
        content.setLayout(layout);

        boolean isFirst = true;
        for (final JCheckBox box : this.getFilterBoxes().getAllTypes()) {
            String growParameter = "grow";
            if (isFirst) {
                growParameter = "cell 0 0, egx checkbox, grow, split 14";
                isFirst = false;
            }
            content.add(box, growParameter);
            box.addItemListener(this.getItemListenerUpdatesDisplay());
        }

        for (final JCheckBox box : this.getFilterBoxes().getAllColors()) {
            content.add(box, "grow");
            box.addItemListener(this.getItemListenerUpdatesDisplay());
        }

        content.add(this.clearFilterButton, "wmin 100, hmin 25, wmax 140, hmax 25, grow");

        content.add(this.filterNameTypeSet, "cell 0 1, grow");
        content.add(this.getTopTableWithCards().getTableDecorated(), "cell 0 2 1 2, pushy, grow");
        content.add(this.getTopTableWithCards().getLabel(), "cell 0 4");

        content.add(this.addButton, "w 100, h 49, sg button, cell 0 5, split 5");
        content.add(this.removeButton, "w 100, h 49, sg button");
        content.add(this.importButton, "w 100, h 49, sg button, gapleft 40px");
        // Label is used to push the analysis button to the right to separate
        // analysis button from add/remove card ones
        content.add(this.jLabelAnalysisGap, "wmin 75, growx");
        content.add(this.analysisButton, "w 100, h 49, wrap");

        content.add(this.getBottomTableWithCards().getTableDecorated(), "cell 0 6, grow");
        content.add(this.getBottomTableWithCards().getLabel(), "cell 0 7");

        content.add(this.getCardView(), "cell 1 0 1 8, flowy, grow");

        this.getTopTableWithCards().getTable().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                if (e.getClickCount() == 2) {
                    DeckEditorCommon.this.addCardToDeck();
                }
            }
        });
        this.getTopTableWithCards().getTable().addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(final KeyEvent e) {
                if (e.getKeyChar() == ' ') {
                    DeckEditorCommon.this.addCardToDeck();
                }
            }
        });

        // javax.swing.JRootPane rootPane = this.getRootPane();
        // rootPane.setDefaultButton(filterButton);
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.gui.deckeditor.DeckEditorBase#buildFilter()
     */
    @Override
    protected Predicate<InventoryItem> buildFilter() {
        final Predicate<CardPrinted> cardFilter = Predicate.and(
                Predicate.and(this.getFilterBoxes().buildFilter(), this.filterNameTypeSet.buildFilter()),
                CardPrinted.Predicates.Presets.NON_ALTERNATE);
        return Predicate.instanceOf(cardFilter, CardPrinted.class);
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.gui.deckeditor.DeckEditorBase#setDeck(forge.item.ItemPoolView,
     * forge.item.ItemPoolView, forge.game.GameType)
     */
    @Override
    public void setDeck(final ItemPoolView<CardPrinted> topParam, final ItemPoolView<CardPrinted> bottomParam,
            final GameType gt) {
        final boolean keepRecievedCards = gt.isLimited() || (topParam != null);
        // if constructed, can add the all cards above
        final ItemPoolView<CardPrinted> top = keepRecievedCards ? topParam : ItemPool.createFrom(CardDb.instance()
                .getAllCards(), CardPrinted.class);
        this.importButton.setVisible(!gt.isLimited());
        super.setDeck(top, bottomParam, gt);
    }

    /**
     * Clear filter button_action performed.
     * 
     * @param e
     *            the e
     */
    void clearFilterButtonActionPerformed(final ActionEvent e) {
        // disable automatic update triggered by listeners
        this.setFiltersChangeFiringUpdate(false);

        for (final JCheckBox box : this.getFilterBoxes().getAllTypes()) {
            if (!box.isSelected()) {
                box.doClick();
            }
        }
        for (final JCheckBox box : this.getFilterBoxes().getAllColors()) {
            if (!box.isSelected()) {
                box.doClick();
            }
        }

        this.filterNameTypeSet.clearFilters();

        this.setFiltersChangeFiringUpdate(true);

        this.getTopTableWithCards().setFilter(null);
    }

    /**
     * Adds the button_action performed.
     * 
     * @param e
     *            the e
     */
    void addButtonActionPerformed(final ActionEvent e) {
        this.addCardToDeck();
    }

    /**
     * Adds the card to deck.
     */
    void addCardToDeck() {
        final InventoryItem item = this.getTopTableWithCards().getSelectedCard();
        if ((item == null) || !(item instanceof CardPrinted)) {
            return;
        }

        final CardPrinted card = (CardPrinted) item;
        this.setTitle("Deck Editor : " + this.getCustomMenu().getDeckName() + " : unsaved");

        this.getBottomTableWithCards().addCard(card);
        if (this.getGameType().isLimited()) {
            this.getTopTableWithCards().removeCard(card);
        }

        this.getCustomMenu().notifyDeckChange();
    }

    /**
     * Removes the button clicked.
     * 
     * @param e
     *            the e
     */
    void removeButtonClicked(final ActionEvent e) {
        final InventoryItem item = this.getBottomTableWithCards().getSelectedCard();
        if ((item == null) || !(item instanceof CardPrinted)) {
            return;
        }

        final CardPrinted card = (CardPrinted) item;

        this.setTitle("Deck Editor : " + this.getCustomMenu().getDeckName() + " : unsaved");

        this.getBottomTableWithCards().removeCard(card);
        if (this.getGameType().isLimited()) {
            this.getTopTableWithCards().addCard(card);
        }

        this.getCustomMenu().notifyDeckChange();
    }

    /**
     * Import button_action performed.
     * 
     * @param e
     *            the e
     */
    void importButtonActionPerformed(final ActionEvent e) {
        final DeckEditorBase g = this;
        final DeckImport dImport = new DeckImport(g);
        dImport.setModalityType(ModalityType.APPLICATION_MODAL);
        dImport.setVisible(true);
    }

    /**
     * @return the customMenu
     */
    public DeckEditorCommonMenu getCustomMenu() {
        return customMenu;
    }

    /**
     * @param customMenu
     *            the customMenu to set
     */
    public void setCustomMenu(DeckEditorCommonMenu customMenu) {
        this.customMenu = customMenu; // TODO: Add 0 to parameter's name.
    }

}
