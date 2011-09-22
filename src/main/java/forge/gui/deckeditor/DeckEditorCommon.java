package forge.gui.deckeditor;

import java.awt.Container;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Dialog.ModalityType;
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
    /** Constant <code>serialVersionUID=130339644136746796L</code> */
    private static final long serialVersionUID = 130339644136746796L;

    public DeckEditorCommonMenu customMenu;

    private JButton removeButton = new JButton();
    private JButton addButton = new JButton();
    private JButton importButton = new JButton();
    
    private JButton analysisButton = new JButton();
    private JButton clearFilterButton = new JButton();
    
    private JLabel jLabelAnalysisGap = new JLabel("");  
    private FilterNameTypeSetPanel filterNameTypeSet;

    public void show(final Command exitCommand) {
        final Command exit = new Command() {
            private static final long serialVersionUID = 5210924838133689758L;

            public void execute() {
                DeckEditorCommon.this.dispose();
                exitCommand.execute();
            }
        };

        customMenu = new DeckEditorCommonMenu(this, AllZone.getDeckManager(), exit);
        this.setJMenuBar(customMenu);

        // do not change this!!!!
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent ev) {
                customMenu.close();
            }
        });

        setup();

        

        // show cards, makes this user friendly
        if (!getGameType().isLimited()) {
            customMenu.newConstructed(false);
        }

        top.sort(1, true);
        bottom.sort(1, true);

    } // show(Command)


    private void setup() {
        List<TableColumnInfo<InventoryItem>> columns = new ArrayList<TableColumnInfo<InventoryItem>>();
        columns.add(new TableColumnInfo<InventoryItem>("Qty", 30, PresetColumns.fnQtyCompare, PresetColumns.fnQtyGet));
        columns.add(new TableColumnInfo<InventoryItem>("Name", 175, PresetColumns.fnNameCompare, PresetColumns.fnNameGet));
        columns.add(new TableColumnInfo<InventoryItem>("Cost", 75, PresetColumns.fnCostCompare, PresetColumns.fnCostGet));
        columns.add(new TableColumnInfo<InventoryItem>("Color", 50, PresetColumns.fnColorCompare, PresetColumns.fnColorGet));
        columns.add(new TableColumnInfo<InventoryItem>("Type", 100, PresetColumns.fnTypeCompare, PresetColumns.fnTypeGet));
        columns.add(new TableColumnInfo<InventoryItem>("Stats", 40, PresetColumns.fnStatsCompare, PresetColumns.fnStatsGet));
        columns.add(new TableColumnInfo<InventoryItem>("R", 35, PresetColumns.fnRarityCompare, PresetColumns.fnRarityGet));
        columns.add(new TableColumnInfo<InventoryItem>("Set", 40, PresetColumns.fnSetCompare, PresetColumns.fnSetGet));
        columns.add(new TableColumnInfo<InventoryItem>("AI", 30, PresetColumns.fnAiStatusCompare, PresetColumns.fnAiStatusGet));
        columns.get(2).setCellRenderer(new ManaCostRenderer());
        
        top.setup(columns, cardView);
        bottom.setup(columns, cardView);
        
        filterNameTypeSet.setListeners(new OnChangeTextUpdateDisplay(), itemListenerUpdatesDisplay);

        setSize(1024, 740);
        setExtendedState(Frame.MAXIMIZED_BOTH);

    }


    public DeckEditorCommon(GameType gameType) {
        super(gameType);
        try {
            filterBoxes = new FilterCheckBoxes(true);
            top = new TableWithCards("Avaliable Cards", true, true);
            bottom = new TableWithCards("Deck", true);
            cardView = new CardPanelHeavy();
            filterNameTypeSet = new FilterNameTypeSetPanel();
            
            jbInit();
        } catch (Exception ex) {
            ErrorViewer.showError(ex);
        }
    }


    private void jbInit() {

        if (!Singletons.getModel().getPreferences().lafFonts) {
            Font fButtons = new java.awt.Font("Dialog", 0, 13);
            removeButton.setFont(fButtons);
            addButton.setFont(fButtons);
            importButton.setFont(fButtons);
            clearFilterButton.setFont(fButtons);
            analysisButton.setFont(fButtons);
        }
        
        addButton.setText("Add to Deck");        
        removeButton.setText("Remove from Deck");
        importButton.setText("Import a Deck");
        clearFilterButton.setText("Clear Filter");
        analysisButton.setText("Deck Analysis");
        
        removeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(final ActionEvent e) { removeButtonClicked(e); } });
        addButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(final ActionEvent e) { addButton_actionPerformed(e); } });
        importButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(final ActionEvent e) { importButton_actionPerformed(e); } });
        clearFilterButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(final ActionEvent e) { clearFilterButton_actionPerformed(e); } });
        analysisButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(final ActionEvent e) { analysisButton_actionPerformed(e); } });

        // Type filtering
        Font f = new Font("Tahoma", Font.PLAIN, 10);
        for (JCheckBox box : filterBoxes.allTypes) {
            if (!Singletons.getModel().getPreferences().lafFonts) { box.setFont(f); }
            box.setOpaque(false);
        }

        // Color filtering
        for (JCheckBox box : filterBoxes.allColors) {
            box.setOpaque(false);
        }

        // Do not lower statsLabel any lower, we want this to be visible at 1024
        // x 768 screen size
        this.setTitle("Deck Editor");

        Container content = this.getContentPane();
        MigLayout layout = new MigLayout("fill");
        content.setLayout(layout);

        boolean isFirst = true;
        for (JCheckBox box : filterBoxes.allTypes) {
            String growParameter = "grow";
            if (isFirst) {
                growParameter = "cell 0 0, egx checkbox, grow, split 14";
                isFirst = false;
            }
            content.add(box, growParameter);
            box.addItemListener(itemListenerUpdatesDisplay);
        }

        for (JCheckBox box : filterBoxes.allColors) {
            content.add(box, "grow");
            box.addItemListener(itemListenerUpdatesDisplay);
        }

        content.add(clearFilterButton, "wmin 100, hmin 25, wmax 140, hmax 25, grow");

        content.add(filterNameTypeSet, "cell 0 1, grow");
        content.add(top.getTableDecorated(), "cell 0 2 1 2, pushy, grow");
        content.add(top.getLabel(), "cell 0 4");

        content.add(addButton, "w 100, h 49, sg button, cell 0 5, split 5");
        content.add(removeButton, "w 100, h 49, sg button");
        content.add(importButton, "w 100, h 49, sg button, gapleft 40px");
        // Label is used to push the analysis button to the right to separate analysis button from add/remove card ones 
        content.add(jLabelAnalysisGap, "wmin 100, growx");
        content.add(analysisButton, "w 100, h 49, wrap");

        content.add(bottom.getTableDecorated(), "cell 0 6, grow");
        content.add(bottom.getLabel(), "cell 0 7");

        content.add(cardView, "cell 1 0 1 8, flowy, grow");

        top.getTable().addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(final MouseEvent e) { if (e.getClickCount() == 2) { addCardToDeck(); } } });
        top.getTable().addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(final KeyEvent e) { if (e.getKeyChar() == ' ') { addCardToDeck(); } } });

        //javax.swing.JRootPane rootPane = this.getRootPane();
        //rootPane.setDefaultButton(filterButton);
    }

    @Override
    protected Predicate<InventoryItem> buildFilter() {
        Predicate<CardPrinted> cardFilter = Predicate.and(filterBoxes.buildFilter(), filterNameTypeSet.buildFilter());
        return Predicate.instanceOf(cardFilter, CardPrinted.class);
    }

    @Override
    public void setDeck(ItemPoolView<CardPrinted> topParam, ItemPoolView<CardPrinted> bottomParam, GameType gt)
    {
        boolean keepRecievedCards = gt.isLimited() || topParam != null;  
        // if constructed, can add the all cards above
        ItemPoolView<CardPrinted> top = keepRecievedCards ? topParam : ItemPool.createFrom(CardDb.instance().getAllCards(), CardPrinted.class);
        importButton.setVisible(!gt.isLimited());
        super.setDeck(top, bottomParam, gt);
    }
    
    void clearFilterButton_actionPerformed(ActionEvent e) {
        // disable automatic update triggered by listeners
        isFiltersChangeFiringUpdate = false;

        for (JCheckBox box : filterBoxes.allTypes) { if (!box.isSelected()) { box.doClick(); } }
        for (JCheckBox box : filterBoxes.allColors) { if (!box.isSelected()) { box.doClick(); } }

        filterNameTypeSet.clearFilters();

        isFiltersChangeFiringUpdate = true;

        top.setFilter(null);
    }

    void addButton_actionPerformed(ActionEvent e) {
        addCardToDeck();
    }

    void addCardToDeck() {
        InventoryItem item = top.getSelectedCard();
        if (item == null || !( item instanceof CardPrinted )) { return; }

        CardPrinted card = (CardPrinted) item;
        setTitle("Deck Editor : " + customMenu.getDeckName() + " : unsaved");

        bottom.addCard(card);
        if (getGameType().isLimited()) {
            top.removeCard(card);
        }
        
        customMenu.notifyDeckChange();
    }

    void removeButtonClicked(ActionEvent e) {
        InventoryItem item = bottom.getSelectedCard();
        if (item == null || !( item instanceof CardPrinted )) { return; }

        CardPrinted card = (CardPrinted) item;

        setTitle("Deck Editor : " + customMenu.getDeckName() + " : unsaved");

        bottom.removeCard(card);
        if (getGameType().isLimited()) {
            top.addCard(card);
        }
        
        customMenu.notifyDeckChange();
    }
    
    void importButton_actionPerformed(ActionEvent e) {
        DeckEditorBase g = this;
        DeckImport dImport = new DeckImport(g);
        dImport.setModalityType(ModalityType.APPLICATION_MODAL);
        dImport.setVisible(true);
    }

}
