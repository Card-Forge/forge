package forge.gui.deckeditor.controllers;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.HashMap;

import javax.swing.JCheckBox;

import forge.Command;
import forge.gui.deckeditor.CDeckEditorUI;
import forge.gui.deckeditor.SEditorIO;
import forge.gui.deckeditor.SEditorIO.EditorPreference;
import forge.gui.deckeditor.tables.SColumnUtil;
import forge.gui.deckeditor.tables.SColumnUtil.ColumnName;
import forge.gui.deckeditor.views.VCurrentDeck;
import forge.gui.deckeditor.views.VEditorPreferences;
import forge.gui.framework.ICDoc;

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
        SEditorIO.loadPreferences();

        HashMap<JCheckBox, ColumnName> prefsDict = new HashMap<JCheckBox, SColumnUtil.ColumnName>();

        // Simplified Column Preferences
        VEditorPreferences prefsInstance = VEditorPreferences.SINGLETON_INSTANCE;

        // Catalog
        prefsDict.put(prefsInstance.getChbCatalogColor(), ColumnName.CAT_COLOR);
        prefsDict.put(prefsInstance.getChbCatalogRarity(), ColumnName.CAT_RARITY);
        prefsDict.put(prefsInstance.getChbCatalogCMC(), ColumnName.CAT_CMC);
        prefsDict.put(prefsInstance.getChbCatalogSet(), ColumnName.CAT_SET);
        prefsDict.put(prefsInstance.getChbCatalogAI(), ColumnName.CAT_AI);
        prefsDict.put(prefsInstance.getChbCatalogPower(), ColumnName.CAT_POWER);
        prefsDict.put(prefsInstance.getChbCatalogToughness(), ColumnName.CAT_TOUGHNESS);
        prefsDict.put(prefsInstance.getChbCatalogOwned(), ColumnName.CAT_OWNED);

        // Deck
        prefsDict.put(prefsInstance.getChbDeckColor(), ColumnName.DECK_COLOR);
        prefsDict.put(prefsInstance.getChbDeckRarity(), ColumnName.DECK_RARITY);
        prefsDict.put(prefsInstance.getChbDeckCMC(), ColumnName.DECK_CMC);
        prefsDict.put(prefsInstance.getChbDeckSet(), ColumnName.DECK_SET);
        prefsDict.put(prefsInstance.getChbDeckAI(), ColumnName.DECK_AI);
        prefsDict.put(prefsInstance.getChbDeckPower(), ColumnName.DECK_POWER);
        prefsDict.put(prefsInstance.getChbDeckToughness(), ColumnName.DECK_TOUGHNESS);

        // Simplified assignments to be less verbose
        for (JCheckBox key : prefsDict.keySet()) {
            final ColumnName name = prefsDict.get(key);
            key.setSelected(SColumnUtil.getColumn(name).isShowing());
            key.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent arg0) {
                    SColumnUtil.toggleColumn(SColumnUtil.getColumn(name));
                    SEditorIO.savePreferences();
                }
            });
        }

        // Catalog/Deck Stats
        VEditorPreferences.SINGLETON_INSTANCE.getChbDeckStats().setSelected(
                SEditorIO.getPref(EditorPreference.stats_deck));
        VEditorPreferences.SINGLETON_INSTANCE.getChbCardDisplayUnique().setSelected(
                SEditorIO.getPref(EditorPreference.display_unique_only));
        VEditorPreferences.SINGLETON_INSTANCE.getChbElasticColumns().setSelected(
                SEditorIO.getPref(EditorPreference.elastic_columns));

        if (!SEditorIO.getPref(EditorPreference.stats_deck)) {
            VCurrentDeck.SINGLETON_INSTANCE.getPnlStats().setVisible(false);
        }

        boolean wantElastic = SEditorIO.getPref(EditorPreference.elastic_columns);
        boolean wantUnique = SEditorIO.getPref(EditorPreference.display_unique_only);
        ACEditorBase<?, ?> curEditor = CDeckEditorUI.SINGLETON_INSTANCE.getCurrentEditorController();
        if (curEditor != null) {
            curEditor.getTableCatalog().setWantElasticColumns(wantElastic);
            curEditor.getTableDeck().setWantElasticColumns(wantElastic);
            curEditor.getTableCatalog().setWantUnique(wantUnique);
            curEditor.getTableCatalog().updateView(true);
            curEditor.getTableDeck().setWantUnique(wantUnique);
            curEditor.getTableDeck().updateView(true);
        }

        VEditorPreferences.SINGLETON_INSTANCE.getChbDeckStats().addItemListener(new ItemListener() {
            @Override public void itemStateChanged(final ItemEvent e) {
                VCurrentDeck.SINGLETON_INSTANCE.getPnlStats().setVisible(
                        ((JCheckBox) e.getSource()).isSelected());
                SEditorIO.setPref(EditorPreference.stats_deck, ((JCheckBox) e.getSource()).isSelected());
                SEditorIO.savePreferences(); } });

        VEditorPreferences.SINGLETON_INSTANCE.getChbElasticColumns().addItemListener(new ItemListener() {
            @Override public void itemStateChanged(final ItemEvent e) {
                ACEditorBase<?, ?> curEditor = CDeckEditorUI.SINGLETON_INSTANCE.getCurrentEditorController();
                boolean wantElastic = ((JCheckBox) e.getSource()).isSelected();
                if (curEditor != null) {
                    curEditor.getTableCatalog().setWantElasticColumns(wantElastic);
                    curEditor.getTableDeck().setWantElasticColumns(wantElastic);
                }
                SEditorIO.setPref(EditorPreference.elastic_columns, wantElastic);
                SEditorIO.savePreferences(); } });

        VEditorPreferences.SINGLETON_INSTANCE.getChbCardDisplayUnique().addItemListener(new ItemListener() {
            @Override public void itemStateChanged(final ItemEvent e) {
                ACEditorBase<?, ?> curEditor = CDeckEditorUI.SINGLETON_INSTANCE.getCurrentEditorController();
                boolean wantUnique = ((JCheckBox) e.getSource()).isSelected();
                if (curEditor != null) {
                    curEditor.getTableCatalog().setWantUnique(wantUnique);
                    curEditor.getTableCatalog().updateView(true);
                    curEditor.getTableDeck().setWantUnique(wantUnique);
                    curEditor.getTableDeck().updateView(true);
                }
                SEditorIO.setPref(EditorPreference.display_unique_only, wantUnique);
                SEditorIO.savePreferences(); } });
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#update()
     */
    @Override
    public void update() {
    }

    //========== Other methods
}
