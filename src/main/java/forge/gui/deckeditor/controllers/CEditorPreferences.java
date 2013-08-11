package forge.gui.deckeditor.controllers;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.HashMap;

import javax.swing.JCheckBox;
import javax.swing.JTable;

import forge.Command;
import forge.gui.deckeditor.CDeckEditorUI;
import forge.gui.listview.SListViewIO;
import forge.gui.listview.SListViewIO.EditorPreference;
import forge.gui.listview.SColumnUtil;
import forge.gui.listview.SColumnUtil.ColumnName;
import forge.gui.listview.TableColumnInfo;
import forge.gui.deckeditor.views.VCurrentDeck;
import forge.gui.deckeditor.views.VEditorPreferences;
import forge.gui.framework.ICDoc;
import forge.item.InventoryItem;

/** 
 * Controls the "analysis" panel in the deck editor UI.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
public enum CEditorPreferences implements ICDoc {
    /** */
    SINGLETON_INSTANCE;

    //========== Overridden methods

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#getCommandOnSelect()
     */
    @Override
    public Command getCommandOnSelect() {
        return null;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#initialize()
     */
    @Override
    public void initialize() {
        SListViewIO.loadPreferences();

        HashMap<JCheckBox, ColumnName> prefsDict = new HashMap<JCheckBox, SColumnUtil.ColumnName>();

        // Simplified Column Preferences
        VEditorPreferences prefsInstance = VEditorPreferences.SINGLETON_INSTANCE;

        // Catalog
        prefsDict.put(prefsInstance.getChbCatalogColor(), ColumnName.CAT_COLOR);
        prefsDict.put(prefsInstance.getChbCatalogRarity(), ColumnName.CAT_RARITY);
        prefsDict.put(prefsInstance.getChbCatalogCMC(), ColumnName.CAT_CMC);
        prefsDict.put(prefsInstance.getChbCatalogSet(), ColumnName.CAT_SET);
        prefsDict.put(prefsInstance.getChbCatalogAI(), ColumnName.CAT_AI);
        prefsDict.put(prefsInstance.getChbCatalogRanking(), ColumnName.CAT_RANKING);
        prefsDict.put(prefsInstance.getChbCatalogPower(), ColumnName.CAT_POWER);
        prefsDict.put(prefsInstance.getChbCatalogToughness(), ColumnName.CAT_TOUGHNESS);
        prefsDict.put(prefsInstance.getChbCatalogOwned(), ColumnName.CAT_OWNED);

        // Deck
        prefsDict.put(prefsInstance.getChbDeckColor(), ColumnName.DECK_COLOR);
        prefsDict.put(prefsInstance.getChbDeckRarity(), ColumnName.DECK_RARITY);
        prefsDict.put(prefsInstance.getChbDeckCMC(), ColumnName.DECK_CMC);
        prefsDict.put(prefsInstance.getChbDeckSet(), ColumnName.DECK_SET);
        prefsDict.put(prefsInstance.getChbDeckAI(), ColumnName.DECK_AI);
        prefsDict.put(prefsInstance.getChbDeckRanking(), ColumnName.DECK_RANKING);
        prefsDict.put(prefsInstance.getChbDeckPower(), ColumnName.DECK_POWER);
        prefsDict.put(prefsInstance.getChbDeckToughness(), ColumnName.DECK_TOUGHNESS);

        // Simplified assignments to be less verbose
        for (JCheckBox key : prefsDict.keySet()) {
            final ColumnName name = prefsDict.get(key);
            key.setSelected(SColumnUtil.getColumn(name).isShowing());
            key.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent arg0) {
                    TableColumnInfo<InventoryItem> col = SColumnUtil.getColumn(name);
                    final JTable table = (col.getEnumValue().substring(0, 4).equals("DECK"))
                        ? CDeckEditorUI.SINGLETON_INSTANCE.getCurrentEditorController().getDeckListView().getTable()
                        : CDeckEditorUI.SINGLETON_INSTANCE.getCurrentEditorController().getCatalogListView().getTable();
                    SColumnUtil.toggleColumn(table, col);
                    SListViewIO.savePreferences(table);
                }
            });
        }

        // Catalog/Deck Stats
        VEditorPreferences.SINGLETON_INSTANCE.getChbDeckStats().setSelected(
                SListViewIO.getPref(EditorPreference.stats_deck));
        VEditorPreferences.SINGLETON_INSTANCE.getChbCardDisplayUnique().setSelected(
                SListViewIO.getPref(EditorPreference.display_unique_only));
        VEditorPreferences.SINGLETON_INSTANCE.getChbElasticColumns().setSelected(
                SListViewIO.getPref(EditorPreference.elastic_columns));

        if (!SListViewIO.getPref(EditorPreference.stats_deck)) {
            VCurrentDeck.SINGLETON_INSTANCE.setStatsVisible(false);
        }

        boolean wantElastic = SListViewIO.getPref(EditorPreference.elastic_columns);
        boolean wantUnique = SListViewIO.getPref(EditorPreference.display_unique_only);
        ACEditorBase<?, ?> curEditor = CDeckEditorUI.SINGLETON_INSTANCE.getCurrentEditorController();
        if (curEditor != null) {
            curEditor.getCatalogListView().setWantElasticColumns(wantElastic);
            curEditor.getDeckListView().setWantElasticColumns(wantElastic);
            curEditor.getCatalogListView().setWantUnique(wantUnique);
            curEditor.getCatalogListView().updateView(true);
            curEditor.getDeckListView().setWantUnique(wantUnique);
            curEditor.getDeckListView().updateView(true);
        }

        VEditorPreferences.SINGLETON_INSTANCE.getChbDeckStats().addItemListener(new ItemListener() {
            @Override public void itemStateChanged(final ItemEvent e) {
                VCurrentDeck.SINGLETON_INSTANCE.setStatsVisible(
                        ((JCheckBox) e.getSource()).isSelected());
                SListViewIO.setPref(EditorPreference.stats_deck, ((JCheckBox) e.getSource()).isSelected());
                SListViewIO.savePreferences(CDeckEditorUI.SINGLETON_INSTANCE.getCurrentEditorController().getCatalogListView().getTable()); } });

        VEditorPreferences.SINGLETON_INSTANCE.getChbElasticColumns().addItemListener(new ItemListener() {
            @Override public void itemStateChanged(final ItemEvent e) {
                ACEditorBase<?, ?> curEditor = CDeckEditorUI.SINGLETON_INSTANCE.getCurrentEditorController();
                boolean wantElastic = ((JCheckBox) e.getSource()).isSelected();
                if (curEditor != null) {
                    curEditor.getCatalogListView().setWantElasticColumns(wantElastic);
                    curEditor.getDeckListView().setWantElasticColumns(wantElastic);
                }
                SListViewIO.setPref(EditorPreference.elastic_columns, wantElastic);
                SListViewIO.savePreferences(curEditor.getCatalogListView().getTable()); } });

        VEditorPreferences.SINGLETON_INSTANCE.getChbCardDisplayUnique().addItemListener(new ItemListener() {
            @Override public void itemStateChanged(final ItemEvent e) {
                ACEditorBase<?, ?> curEditor = CDeckEditorUI.SINGLETON_INSTANCE.getCurrentEditorController();
                boolean wantUnique = ((JCheckBox) e.getSource()).isSelected();
                if (curEditor != null) {
                    curEditor.getCatalogListView().setWantUnique(wantUnique);
                    curEditor.getCatalogListView().updateView(true);
                    curEditor.getDeckListView().setWantUnique(wantUnique);
                    curEditor.getDeckListView().updateView(true);
                }
                SListViewIO.setPref(EditorPreference.display_unique_only, wantUnique);
                SListViewIO.savePreferences(curEditor.getCatalogListView().getTable()); } });
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#update()
     */
    @Override
    public void update() {
    }

    //========== Other methods
}
