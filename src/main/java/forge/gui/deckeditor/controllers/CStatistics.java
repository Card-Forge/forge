package forge.gui.deckeditor.controllers;

import java.util.Map.Entry;

import javax.swing.JLabel;

import com.google.common.collect.Iterables;

import forge.Command;
import forge.card.CardColor;
import forge.card.CardRules;
import forge.deck.DeckBase;
import forge.gui.deckeditor.CDeckEditorUI;
import forge.gui.deckeditor.SEditorUtil;
import forge.gui.deckeditor.views.VStatistics;
import forge.gui.framework.ICDoc;
import forge.item.CardPrinted;
import forge.item.InventoryItem;
import forge.item.ItemPool;
import forge.item.ItemPoolView;
import forge.util.Aggregates;
import forge.util.closures.Predicate;

/** 
 * Controls the "analysis" panel in the deck editor UI.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
public enum CStatistics implements ICDoc {
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
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#update()
     */
    @Override
    public void update() {
        analyze();
    }

    private void setLabelValue(JLabel label, ItemPoolView<CardPrinted> deck, Predicate<CardRules> predicate, int total) {
        int tmp = Aggregates.sum(Iterables.filter(deck, predicate.bridge(deck.getFnToCard())), deck.getFnToCount());
        label.setText( tmp + " (" + SEditorUtil.calculatePercentage(tmp, total) + "%)");

    }
    
    //========== Other methods
    @SuppressWarnings("unchecked")
    private <T extends InventoryItem, TModel extends DeckBase> void analyze() {
        final ACEditorBase<T, TModel> ed = (ACEditorBase<T, TModel>)
                CDeckEditorUI.SINGLETON_INSTANCE.getCurrentEditorController();

        if (ed == null) { return; }

        final ItemPoolView<CardPrinted> deck = ItemPool.createFrom(
                ed.getTableDeck().getCards(), CardPrinted.class);

        int total = deck.countAll();

        // Hack-ish: avoid /0 cases, but still populate labels :)
        if (total == 0) { total = 1; }

 
        setLabelValue( VStatistics.SINGLETON_INSTANCE.getLblCreature(), deck, CardRules.Predicates.Presets.IS_CREATURE, total );
        setLabelValue( VStatistics.SINGLETON_INSTANCE.getLblLand(), deck, CardRules.Predicates.Presets.IS_LAND, total );
        setLabelValue( VStatistics.SINGLETON_INSTANCE.getLblEnchantment(), deck, CardRules.Predicates.Presets.IS_ENCHANTMENT, total );
        setLabelValue( VStatistics.SINGLETON_INSTANCE.getLblArtifact(), deck, CardRules.Predicates.Presets.IS_ARTIFACT, total );
        setLabelValue( VStatistics.SINGLETON_INSTANCE.getLblInstant(), deck, CardRules.Predicates.Presets.IS_INSTANT, total );
        setLabelValue( VStatistics.SINGLETON_INSTANCE.getLblSorcery(), deck, CardRules.Predicates.Presets.IS_SORCERY, total );
        setLabelValue( VStatistics.SINGLETON_INSTANCE.getLblPlaneswalker(), deck, CardRules.Predicates.Presets.IS_PLANESWALKER, total );
        
        setLabelValue( VStatistics.SINGLETON_INSTANCE.getLblMulti(), deck, CardRules.Predicates.Presets.IS_MULTICOLOR, total );
        setLabelValue( VStatistics.SINGLETON_INSTANCE.getLblColorless(), deck, CardRules.Predicates.Presets.IS_COLORLESS, total );
        setLabelValue( VStatistics.SINGLETON_INSTANCE.getLblBlack(), deck, CardRules.Predicates.isMonoColor(CardColor.BLACK), total );
        setLabelValue( VStatistics.SINGLETON_INSTANCE.getLblBlue(), deck, CardRules.Predicates.isMonoColor(CardColor.BLUE), total );
        setLabelValue( VStatistics.SINGLETON_INSTANCE.getLblGreen(), deck, CardRules.Predicates.isMonoColor(CardColor.GREEN), total );
        setLabelValue( VStatistics.SINGLETON_INSTANCE.getLblRed(), deck, CardRules.Predicates.isMonoColor(CardColor.RED), total );
        setLabelValue( VStatistics.SINGLETON_INSTANCE.getLblWhite(), deck, CardRules.Predicates.isMonoColor(CardColor.WHITE), total );
        
        int cmc0 = 0, cmc1 = 0, cmc2 = 0, cmc3 = 0, cmc4 = 0, cmc5 = 0, cmc6 = 0;
        int tmc = 0;

        for (final Entry<CardPrinted, Integer> e : deck) {
            final CardRules cardRules = e.getKey().getCard();
            final int count = e.getValue();
            final int cmc = cardRules.getManaCost().getCMC();

            if (cmc == 0)       { cmc0 += count; }
            else if (cmc == 1)  { cmc1 += count; }
            else if (cmc == 2)  { cmc2 += count; }
            else if (cmc == 3)  { cmc3 += count; }
            else if (cmc == 4)  { cmc4 += count; }
            else if (cmc == 5)  { cmc5 += count; }
            else if (cmc >= 6)  { cmc6 += count; }

            tmc += (cmc * count);
        }

        VStatistics.SINGLETON_INSTANCE.getLblCMC0().setText(
                cmc0 + " (" + SEditorUtil.calculatePercentage(cmc0, total) + "%)");
        VStatistics.SINGLETON_INSTANCE.getLblCMC1().setText(
                cmc1 + " (" + SEditorUtil.calculatePercentage(cmc1, total) + "%)");
        VStatistics.SINGLETON_INSTANCE.getLblCMC2().setText(
                cmc2 + " (" + SEditorUtil.calculatePercentage(cmc2, total) + "%)");
        VStatistics.SINGLETON_INSTANCE.getLblCMC3().setText(
                cmc3 + " (" + SEditorUtil.calculatePercentage(cmc3, total) + "%)");
        VStatistics.SINGLETON_INSTANCE.getLblCMC4().setText(
                cmc4 + " (" + SEditorUtil.calculatePercentage(cmc4, total) + "%)");
        VStatistics.SINGLETON_INSTANCE.getLblCMC5().setText(
                cmc5 + " (" + SEditorUtil.calculatePercentage(cmc5, total) + "%)");
        VStatistics.SINGLETON_INSTANCE.getLblCMC6().setText(
                cmc6 + " (" + SEditorUtil.calculatePercentage(cmc6, total) + "%)");

        double amc = (double) Math.round((double) tmc / (double) total * 100) / 100.0d;

        VStatistics.SINGLETON_INSTANCE.getLblTotal().setText("TOTAL CARDS: " + deck.countAll());
        VStatistics.SINGLETON_INSTANCE.getLblTMC().setText("TOTAL MANA COST: " + tmc);
        VStatistics.SINGLETON_INSTANCE.getLblAMC().setText("AVERAGE MANA COST: " + amc);
    }
}
