package forge.gui.deckeditor.controllers;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JCheckBox;

import forge.Command;
import forge.gui.deckeditor.SEditorIO;
import forge.gui.deckeditor.SEditorIO.EditorPreference;
import forge.gui.deckeditor.tables.SColumnUtil;
import forge.gui.deckeditor.tables.SColumnUtil.ColumnName;
import forge.gui.deckeditor.views.VCardCatalog;
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

        VEditorPreferences.SINGLETON_INSTANCE.getChbCatalogColor().setSelected(
                SColumnUtil.getColumn(ColumnName.CAT_COLOR).isShowing());
        VEditorPreferences.SINGLETON_INSTANCE.getChbCatalogRarity().setSelected(
                SColumnUtil.getColumn(ColumnName.CAT_RARITY).isShowing());
        VEditorPreferences.SINGLETON_INSTANCE.getChbCatalogCMC().setSelected(
                SColumnUtil.getColumn(ColumnName.CAT_CMC).isShowing());
        VEditorPreferences.SINGLETON_INSTANCE.getChbCatalogSet().setSelected(
                SColumnUtil.getColumn(ColumnName.CAT_SET).isShowing());
        VEditorPreferences.SINGLETON_INSTANCE.getChbCatalogAI().setSelected(
                SColumnUtil.getColumn(ColumnName.CAT_AI).isShowing());
        VEditorPreferences.SINGLETON_INSTANCE.getChbDeckColor().setSelected(
                SColumnUtil.getColumn(ColumnName.DECK_COLOR).isShowing());
        VEditorPreferences.SINGLETON_INSTANCE.getChbDeckRarity().setSelected(
                SColumnUtil.getColumn(ColumnName.DECK_RARITY).isShowing());
        VEditorPreferences.SINGLETON_INSTANCE.getChbDeckCMC().setSelected(
                SColumnUtil.getColumn(ColumnName.DECK_CMC).isShowing());
        VEditorPreferences.SINGLETON_INSTANCE.getChbDeckSet().setSelected(
                SColumnUtil.getColumn(ColumnName.DECK_SET).isShowing());
        VEditorPreferences.SINGLETON_INSTANCE.getChbDeckAI().setSelected(
                SColumnUtil.getColumn(ColumnName.DECK_AI).isShowing());

        VEditorPreferences.SINGLETON_INSTANCE.getChbCatalogStats().setSelected(
                SEditorIO.getPref(EditorPreference.stats_catalog));
        VEditorPreferences.SINGLETON_INSTANCE.getChbDeckStats().setSelected(
                SEditorIO.getPref(EditorPreference.stats_deck));

        if (!SEditorIO.getPref(EditorPreference.stats_deck)) {
            VCurrentDeck.SINGLETON_INSTANCE.getPnlStats().setVisible(false);
        }
        if (!SEditorIO.getPref(EditorPreference.stats_catalog)) {
            VCardCatalog.SINGLETON_INSTANCE.getPnlStats().setVisible(false);
        }

        VEditorPreferences.SINGLETON_INSTANCE.getChbCatalogColor().addItemListener(new ItemListener() {
            @Override public void itemStateChanged(final ItemEvent e) {
                SColumnUtil.toggleColumn(SColumnUtil.getColumn(ColumnName.CAT_COLOR));
                SEditorIO.savePreferences(); } });

        VEditorPreferences.SINGLETON_INSTANCE.getChbCatalogRarity().addItemListener(new ItemListener() {
            @Override public void itemStateChanged(final ItemEvent e) {
                SColumnUtil.toggleColumn(SColumnUtil.getColumn(ColumnName.CAT_RARITY));
                SEditorIO.savePreferences(); } });

        VEditorPreferences.SINGLETON_INSTANCE.getChbCatalogCMC().addItemListener(new ItemListener() {
            @Override public void itemStateChanged(final ItemEvent e) {
                SColumnUtil.toggleColumn(SColumnUtil.getColumn(ColumnName.CAT_CMC));
                SEditorIO.savePreferences(); } });

        VEditorPreferences.SINGLETON_INSTANCE.getChbCatalogSet().addItemListener(new ItemListener() {
            @Override public void itemStateChanged(final ItemEvent e) {
                SColumnUtil.toggleColumn(SColumnUtil.getColumn(ColumnName.CAT_SET));
                SEditorIO.savePreferences(); } });

        VEditorPreferences.SINGLETON_INSTANCE.getChbCatalogAI().addItemListener(new ItemListener() {
            @Override public void itemStateChanged(final ItemEvent e) {
                SColumnUtil.toggleColumn(SColumnUtil.getColumn(ColumnName.CAT_AI));
                SEditorIO.savePreferences(); } });

        VEditorPreferences.SINGLETON_INSTANCE.getChbDeckColor().addItemListener(new ItemListener() {
            @Override public void itemStateChanged(final ItemEvent e) {
                SColumnUtil.toggleColumn(SColumnUtil.getColumn(ColumnName.DECK_COLOR));
                SEditorIO.savePreferences(); } });

        VEditorPreferences.SINGLETON_INSTANCE.getChbDeckRarity().addItemListener(new ItemListener() {
            @Override public void itemStateChanged(final ItemEvent e) {
                SColumnUtil.toggleColumn(SColumnUtil.getColumn(ColumnName.DECK_RARITY));
                SEditorIO.savePreferences(); } });

        VEditorPreferences.SINGLETON_INSTANCE.getChbDeckCMC().addItemListener(new ItemListener() {
            @Override public void itemStateChanged(final ItemEvent e) {
                SColumnUtil.toggleColumn(SColumnUtil.getColumn(ColumnName.DECK_CMC));
                SEditorIO.savePreferences(); } });

        VEditorPreferences.SINGLETON_INSTANCE.getChbDeckSet().addItemListener(new ItemListener() {
            @Override public void itemStateChanged(final ItemEvent e) {
                SColumnUtil.toggleColumn(SColumnUtil.getColumn(ColumnName.DECK_SET));
                SEditorIO.savePreferences(); } });

        VEditorPreferences.SINGLETON_INSTANCE.getChbDeckAI().addItemListener(new ItemListener() {
            @Override public void itemStateChanged(final ItemEvent e) {
                SColumnUtil.toggleColumn(SColumnUtil.getColumn(ColumnName.DECK_AI));
                SEditorIO.savePreferences(); } });

        VEditorPreferences.SINGLETON_INSTANCE.getChbCatalogStats().addItemListener(new ItemListener() {
            @Override public void itemStateChanged(final ItemEvent e) {
                VCardCatalog.SINGLETON_INSTANCE.getPnlStats().setVisible(
                        ((JCheckBox) e.getSource()).isSelected());
                SEditorIO.setPref(EditorPreference.stats_catalog, ((JCheckBox) e.getSource()).isSelected());
                SEditorIO.savePreferences(); } });

        VEditorPreferences.SINGLETON_INSTANCE.getChbDeckStats().addItemListener(new ItemListener() {
            @Override public void itemStateChanged(final ItemEvent e) {
                VCurrentDeck.SINGLETON_INSTANCE.getPnlStats().setVisible(
                        ((JCheckBox) e.getSource()).isSelected());
                SEditorIO.setPref(EditorPreference.stats_deck, ((JCheckBox) e.getSource()).isSelected());
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
