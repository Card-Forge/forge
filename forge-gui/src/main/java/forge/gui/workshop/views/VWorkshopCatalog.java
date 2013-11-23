package forge.gui.workshop.views;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.miginfocom.swing.MigLayout;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Iterables;

import forge.Command;
import forge.Singletons;
import forge.gui.WrapLayout;
import forge.gui.deckeditor.views.VCardCatalog.RangeTypes;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.IVDoc;
import forge.gui.match.controllers.CDetail;
import forge.gui.match.controllers.CPicture;
import forge.gui.toolbox.FComboBoxWrapper;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.FSkin;
import forge.gui.toolbox.FSpinner;
import forge.gui.toolbox.FTextField;
import forge.gui.toolbox.itemmanager.CardManager;
import forge.gui.toolbox.itemmanager.ItemManagerContainer;
import forge.gui.toolbox.itemmanager.SItemManagerUtil;
import forge.gui.workshop.controllers.CCardScript;
import forge.gui.workshop.controllers.CWorkshopCatalog;
import forge.item.PaperCard;
import forge.util.ItemPool;

/** 
 * Assembles Swing components of card catalog in workshop.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 * 
 */
public enum VWorkshopCatalog implements IVDoc<CWorkshopCatalog> {
    /** */
    SINGLETON_INSTANCE;

    public static final int SEARCH_MODE_INVERSE_INDEX = 1;

    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab("Card Catalog");

    // Total and color count labels/filter toggles
    private final Dimension labelSize = new Dimension(60, 24);
    private final JPanel pnlStats = new JPanel(new WrapLayout(FlowLayout.LEFT));
    private final Map<SItemManagerUtil.StatTypes, FLabel> statLabels =
            new HashMap<SItemManagerUtil.StatTypes, FLabel>();

    // restriction button and search widgets
    private final JPanel pnlSearch = new JPanel(new MigLayout("insets 0, gap 5px, center"));
    private final FLabel btnAddRestriction = new FLabel.ButtonBuilder()
            .text("Add filter")
            .tooltip("Click to add custom filters to the card list")
            .reactOnMouseDown().build();
    private final FComboBoxWrapper<String> cbSearchMode = new FComboBoxWrapper<String>();
    private final JTextField txfSearch = new FTextField.Builder().ghostText("Search").build();
    private final FLabel lblName = new FLabel.Builder().text("Name").hoverable().selectable().selected().build();
    private final FLabel lblType = new FLabel.Builder().text("Type").hoverable().selectable().selected().build();
    private final FLabel lblText = new FLabel.Builder().text("Text").hoverable().selectable().selected().build();
    private final JPanel pnlRestrictions = new JPanel(new WrapLayout(FlowLayout.LEFT, 10, 5));

    private final ItemManagerContainer cardManagerContainer = new ItemManagerContainer();
    private final CardManager cardManager;

    private final Map<RangeTypes, Pair<FSpinner, FSpinner>> spinners = new HashMap<RangeTypes, Pair<FSpinner, FSpinner>>();

    //========== Constructor
    /** */
    private VWorkshopCatalog() {
        pnlStats.setOpaque(false);

        for (SItemManagerUtil.StatTypes s : SItemManagerUtil.StatTypes.values()) {
            FLabel label = buildToggleLabel(s, SItemManagerUtil.StatTypes.TOTAL != s);
            statLabels.put(s, label);
            JComponent component = label;
            if (SItemManagerUtil.StatTypes.TOTAL == s) {
                label.setToolTipText("Total cards (click to toggle all filters)");
            } else if (SItemManagerUtil.StatTypes.PACK == s) {
                // wrap in a constant-size panel so we can change its visibility without affecting layout
                component = new JPanel(new MigLayout("insets 0, gap 0"));
                component.setPreferredSize(labelSize);
                component.setMinimumSize(labelSize);
                component.setOpaque(false);
                label.setVisible(false);
                component.add(label);
            }
            pnlStats.add(component);
        }

        pnlSearch.setOpaque(false);
        pnlSearch.add(btnAddRestriction, "center, w pref+8, h pref+8");
        pnlSearch.add(txfSearch, "pushx, growx");
        cbSearchMode.addItem("in");
        cbSearchMode.addItem("not in");
        cbSearchMode.addTo(pnlSearch, "center");
        pnlSearch.add(lblName, "w pref+8, h pref+8");
        pnlSearch.add(lblType, "w pref+8, h pref+8");
        pnlSearch.add(lblText, "w pref+8, h pref+8");

        pnlRestrictions.setOpaque(false);

        // fill spinner map
        for (RangeTypes t : RangeTypes.values()) {
            FSpinner lowerBound = new FSpinner.Builder().maxValue(10).build();
            FSpinner upperBound = new FSpinner.Builder().maxValue(10).build();
            _setupSpinner(lowerBound);
            _setupSpinner(upperBound);
            spinners.put(t, Pair.of(lowerBound, upperBound));
        }

        this.cardManager = new CardManager(this.statLabels, true);
        Iterable<PaperCard> allCards = Iterables.concat(Singletons.getMagicDb().getCommonCards(), Singletons.getMagicDb().getVariantCards());
        this.cardManager.setPool(ItemPool.createFrom(allCards, PaperCard.class), true);
        this.cardManagerContainer.setItemManager(this.cardManager);

        this.cardManager.addSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                PaperCard card = cardManager.getSelectedItem();
                CDetail.SINGLETON_INSTANCE.showCard(card);
                CPicture.SINGLETON_INSTANCE.showImage(card);
                CCardScript.SINGLETON_INSTANCE.showCard(card);
            }
        });
    }

    private void _setupSpinner (JSpinner spinner) {
        spinner.setFocusable(false); // only the spinner text field should be focusable, not the up/down widget
    }

    //========== Overridden from IVDoc

    @Override
    public EDocID getDocumentID() {
        return EDocID.WORKSHOP_CATALOG;
    }

    @Override
    public DragTab getTabLabel() {
        return tab;
    }

    @Override
    public CWorkshopCatalog getLayoutControl() {
        return CWorkshopCatalog.SINGLETON_INSTANCE;
    }

    @Override
    public void setParentCell(DragCell cell0) {
        this.parentCell = cell0;
    }

    @Override
    public DragCell getParentCell() {
        return this.parentCell;
    }

    @Override
    public void populate() {
        JPanel parentBody = parentCell.getBody();
        parentBody.setLayout(new MigLayout("insets 0, gap 0, wrap, hidemode 3"));
        parentBody.add(pnlStats, "w 100:520:520, center");
        parentBody.add(pnlSearch, "w 96%, gap 1% 1%");
        parentBody.add(pnlRestrictions, "w 96%, gapleft 1%, gapright push");
        parentBody.add(cardManagerContainer, "w 98%!, h 100% - 35, gap 1% 0 0 1%");
    }

    //========== Accessor/mutator methods
    public FLabel getLblName()       { return lblName;       }
    public FLabel getLblType()       { return lblType;       }
    public FLabel getLblText()       { return lblText;       }

    public FLabel getBtnAddRestriction()       { return btnAddRestriction; }
    public FComboBoxWrapper<String> getCbSearchMode() { return cbSearchMode; }
    public JTextField getTxfSearch()           { return txfSearch;         }

    public CardManager getCardManager() {
        return cardManager;
    }
    public Map<SItemManagerUtil.StatTypes, FLabel> getStatLabels() {
        return statLabels;
    }
    public Map<RangeTypes, Pair<FSpinner, FSpinner>> getSpinners() {
        return spinners;
    }

    //========== Other methods
    private FLabel buildToggleLabel(SItemManagerUtil.StatTypes s, boolean selectable) {
        String tooltip;
        if (selectable) { //construct tooltip for selectable toggle labels, indicating click and right-click behavior
            String labelString = s.toLabelString();
            tooltip = labelString + " (click to toggle the filter, right-click to show only " + labelString.toLowerCase() + ")";
        }
        else { tooltip = ""; }

        FLabel label = new FLabel.Builder()
                .icon(s.img).iconScaleAuto(false)
                .fontSize(11)
                .tooltip(tooltip)
                .hoverable().selectable(selectable).selected(selectable)
                .build();

        label.setPreferredSize(labelSize);
        label.setMinimumSize(labelSize);

        return label;
    }

    @SuppressWarnings("serial")
    public void addRestrictionWidget(JComponent component, final Command onRemove) {
        final JPanel pnl = new JPanel(new MigLayout("insets 2, gap 2, h 30!"));

        pnl.setOpaque(false);
        FSkin.get(pnl).setMatteBorder(1, 2, 1, 2, FSkin.getColor(FSkin.Colors.CLR_TEXT));

        pnl.add(component, "h 30!, center");
        pnl.add(new FLabel.Builder().text("X").fontSize(10).hoverable(true)
                .tooltip("Remove filter").cmdClick(new Command() {
                    @Override
                    public void run() {
                        pnlRestrictions.remove(pnl);
                        refreshRestrictionWidgets();
                        onRemove.run();
                    }
                }).build(), "top");

        pnlRestrictions.add(pnl, "h 30!");
        refreshRestrictionWidgets();
    }

    public void refreshRestrictionWidgets() {
        Container parent = pnlRestrictions.getParent();
        pnlRestrictions.validate();
        parent.validate();
        parent.repaint();
    }

    public JPanel buildRangeRestrictionWidget(RangeTypes t) {
        JPanel pnl = new JPanel(new MigLayout("insets 0, gap 2"));
        pnl.setOpaque(false);

        Pair<FSpinner, FSpinner> s = spinners.get(t);
        pnl.add(s.getLeft(), "w 45!, h 26!, center");
        pnl.add(new FLabel.Builder().text("<=").fontSize(11).build(), "h 26!, center");
        pnl.add(new FLabel.Builder().text(t.toLabelString()).fontSize(11).build(), "h 26!, center");
        pnl.add(new FLabel.Builder().text("<=").fontSize(11).build(), "h 26!, center");
        pnl.add(s.getRight(), "w 45!, h 26!, center");

        return pnl;
    }

    public FLabel buildPlainRestrictionWidget(String label, String tooltip) {
        return new FLabel.Builder().text(label).tooltip(tooltip).fontSize(11).build();
    }
}
