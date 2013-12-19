package forge.gui.deckeditor.controllers;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.HashMap;

import javax.swing.JCheckBox;

import forge.Command;
import forge.gui.deckeditor.CDeckEditorUI;
import forge.gui.toolbox.itemmanager.ItemManager;
import forge.gui.toolbox.itemmanager.SItemManagerIO;
import forge.gui.toolbox.itemmanager.SItemManagerIO.EditorPreference;
import forge.gui.toolbox.itemmanager.views.SColumnUtil;
import forge.gui.toolbox.itemmanager.views.TableColumnInfo;
import forge.gui.toolbox.itemmanager.views.SColumnUtil.ColumnName;
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
        SItemManagerIO.loadPreferences();

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
                    final ItemManager<?> itemManager = (col.getEnumValue().substring(0, 4).equals("DECK"))
                        ? CDeckEditorUI.SINGLETON_INSTANCE.getCurrentEditorController().getDeckManager()
                        : CDeckEditorUI.SINGLETON_INSTANCE.getCurrentEditorController().getCatalogManager();
                    SColumnUtil.toggleColumn(itemManager.getTable().getTable(), col);
                    SItemManagerIO.savePreferences(itemManager);
                }
            });
        }

        // Catalog/Deck Stats
        VEditorPreferences.SINGLETON_INSTANCE.getChbCardDisplayUnique().setSelected(
                SItemManagerIO.getPref(EditorPreference.display_unique_only));
        VEditorPreferences.SINGLETON_INSTANCE.getChbElasticColumns().setSelected(
                SItemManagerIO.getPref(EditorPreference.elastic_columns));

        boolean wantElastic = SItemManagerIO.getPref(EditorPreference.elastic_columns);
        boolean wantUnique = SItemManagerIO.getPref(EditorPreference.display_unique_only);
        ACEditorBase<?, ?> curEditor = CDeckEditorUI.SINGLETON_INSTANCE.getCurrentEditorController();
        if (curEditor != null) {
            curEditor.getCatalogManager().getTable().setWantElasticColumns(wantElastic);
            curEditor.getDeckManager().getTable().setWantElasticColumns(wantElastic);
            curEditor.getCatalogManager().setWantUnique(wantUnique);
            curEditor.getCatalogManager().updateView(true);
            curEditor.getDeckManager().setWantUnique(wantUnique);
            curEditor.getDeckManager().updateView(true);
        }

        VEditorPreferences.SINGLETON_INSTANCE.getChbElasticColumns().addItemListener(new ItemListener() {
            @Override public void itemStateChanged(final ItemEvent e) {
                ACEditorBase<?, ?> curEditor = CDeckEditorUI.SINGLETON_INSTANCE.getCurrentEditorController();
                boolean wantElastic = ((JCheckBox) e.getSource()).isSelected();
                if (curEditor != null) {
                    curEditor.getCatalogManager().getTable().setWantElasticColumns(wantElastic);
                    curEditor.getDeckManager().getTable().setWantElasticColumns(wantElastic);
                }
                SItemManagerIO.setPref(EditorPreference.elastic_columns, wantElastic);
                SItemManagerIO.savePreferences(curEditor.getCatalogManager()); } });

        VEditorPreferences.SINGLETON_INSTANCE.getChbCardDisplayUnique().addItemListener(new ItemListener() {
            @Override public void itemStateChanged(final ItemEvent e) {
                ACEditorBase<?, ?> curEditor = CDeckEditorUI.SINGLETON_INSTANCE.getCurrentEditorController();
                boolean wantUnique = ((JCheckBox) e.getSource()).isSelected();
                if (curEditor != null) {
                    curEditor.getCatalogManager().setWantUnique(wantUnique);
                    curEditor.getCatalogManager().updateView(true);
                    curEditor.getDeckManager().setWantUnique(wantUnique);
                    curEditor.getDeckManager().updateView(true);
                }
                SItemManagerIO.setPref(EditorPreference.display_unique_only, wantUnique);
                SItemManagerIO.savePreferences(curEditor.getCatalogManager()); } });
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#update()
     */
    @Override
    public void update() {
    }

    //========== Other methods
}
