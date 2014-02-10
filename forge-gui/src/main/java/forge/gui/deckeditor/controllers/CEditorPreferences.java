package forge.gui.deckeditor.controllers;

import forge.UiCommand;
import forge.gui.deckeditor.views.VEditorPreferences;
import forge.gui.framework.ICDoc;
import forge.gui.toolbox.itemmanager.views.ColumnDef;

import javax.swing.*;
import java.util.HashMap;

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
    public UiCommand getCommandOnSelect() {
        return null;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#initialize()
     */
    @Override
    public void initialize() {
        HashMap<JCheckBox, ColumnDef> prefsDict = new HashMap<JCheckBox, ColumnDef>();

        // Simplified Column Preferences
        VEditorPreferences prefsInstance = VEditorPreferences.SINGLETON_INSTANCE;

        // Catalog
        prefsDict.put(prefsInstance.getChbCatalogColor(), ColumnDef.COLOR);
        prefsDict.put(prefsInstance.getChbCatalogRarity(), ColumnDef.RARITY);
        prefsDict.put(prefsInstance.getChbCatalogCMC(), ColumnDef.CMC);
        prefsDict.put(prefsInstance.getChbCatalogSet(), ColumnDef.SET);
        prefsDict.put(prefsInstance.getChbCatalogAI(), ColumnDef.AI);
        prefsDict.put(prefsInstance.getChbCatalogRanking(), ColumnDef.RANKING);
        prefsDict.put(prefsInstance.getChbCatalogPower(), ColumnDef.POWER);
        prefsDict.put(prefsInstance.getChbCatalogToughness(), ColumnDef.TOUGHNESS);
        prefsDict.put(prefsInstance.getChbCatalogFavorite(), ColumnDef.FAVORITE);
        prefsDict.put(prefsInstance.getChbCatalogOwned(), ColumnDef.OWNED);

        // Deck
        prefsDict.put(prefsInstance.getChbDeckColor(), ColumnDef.COLOR);
        prefsDict.put(prefsInstance.getChbDeckRarity(), ColumnDef.RARITY);
        prefsDict.put(prefsInstance.getChbDeckCMC(), ColumnDef.CMC);
        prefsDict.put(prefsInstance.getChbDeckSet(), ColumnDef.SET);
        prefsDict.put(prefsInstance.getChbDeckAI(), ColumnDef.AI);
        prefsDict.put(prefsInstance.getChbDeckRanking(), ColumnDef.RANKING);
        prefsDict.put(prefsInstance.getChbDeckPower(), ColumnDef.POWER);
        prefsDict.put(prefsInstance.getChbDeckToughness(), ColumnDef.TOUGHNESS);

        // Simplified assignments to be less verbose
        /*for (JCheckBox key : prefsDict.keySet()) {
            final ColumnDef name = prefsDict.get(key);
            key.setSelected(SColumnUtil.getColumn(name).isShowing());
            key.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent arg0) {
                    ItemColumn col = SColumnUtil.getColumn(name);
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
                SItemManagerIO.savePreferences(curEditor.getCatalogManager()); } });*/
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#update()
     */
    @Override
    public void update() {
    }

    //========== Other methods
}
