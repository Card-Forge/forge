package forge.gui.deckeditor.controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import forge.Command;
import forge.deck.DeckBase;
import forge.gui.deckeditor.CDeckEditorUI;
import forge.gui.deckeditor.views.VProbabilities;
import forge.gui.framework.ICDoc;
import forge.gui.toolbox.FLabel;
import forge.item.PaperCard;
import forge.item.InventoryItem;
import forge.item.ItemPool;
import forge.item.ItemPoolView;
import forge.util.MyRandom;

/** 
 * Controls the "analysis" panel in the deck editor UI.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
public enum CProbabilities implements ICDoc {
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
    @SuppressWarnings("serial")
    public void initialize() {
        ((FLabel) VProbabilities.SINGLETON_INSTANCE.getLblReshuffle()).setCommand(
            new Command() { @Override  public void run() { update(); } });
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#update()
     */
    @Override
    public void update() {
        VProbabilities.SINGLETON_INSTANCE.rebuildLabels(analyze());
    }

    //========== Other methods
    @SuppressWarnings("unchecked")
    private <T extends InventoryItem, TModel extends DeckBase> List<String> analyze() {
        final ACEditorBase<T, TModel> ed = (ACEditorBase<T, TModel>)
                CDeckEditorUI.SINGLETON_INSTANCE.getCurrentEditorController();

        final ItemPoolView<PaperCard> deck = ItemPool.createFrom(ed.getTableDeck().getCards(), PaperCard.class);

        final List<String> cardProbabilities = new ArrayList<String>();

        final List<PaperCard> shuffled = deck.toFlatList();
        Collections.shuffle(shuffled, MyRandom.getRandom());

        // Log totals of each card for decrementing
        final Map<PaperCard, Integer> cardTotals = new HashMap<PaperCard, Integer>();
        for (final PaperCard c : shuffled) {
            if (cardTotals.containsKey(c)) { cardTotals.put(c, cardTotals.get(c) + 1); }
            else { cardTotals.put(c, 1); }
        }

        // Run through shuffled deck and calculate probabilities.
        // Formulas is (remaining instances of this card / total cards remaining)
        final Iterator<PaperCard> itr = shuffled.iterator();
        PaperCard tmp;
       // int prob;
        while (itr.hasNext()) {
            tmp = itr.next();

           // prob = SEditorUtil.calculatePercentage(
             //       cardTotals.get(tmp), shuffled.size());

            cardTotals.put(tmp, cardTotals.get(tmp) - 1);
            cardProbabilities.add(tmp.getName()); // + " (" + prob + "%)");
            itr.remove();
        }

        return cardProbabilities;
    }
}
