package forge.screens.deckeditor.controllers;

import java.util.*;
import java.util.stream.Collectors;

import forge.deck.DeckBase;
import forge.gui.UiCommand;
import forge.gui.framework.ICDoc;
import forge.item.InventoryItem;
import forge.item.PaperCard;
import forge.screens.deckeditor.CDeckEditorUI;
import forge.screens.deckeditor.views.VProbabilities;
import forge.util.ItemPool;
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

    @Override
    public void register() {
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#initialize()
     */
    @Override
    @SuppressWarnings("serial")
    public void initialize() {
        VProbabilities.SINGLETON_INSTANCE.getLblReshuffle().setCommand((UiCommand) this::update);
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

        if (ed == null) { return new ArrayList<>(); }

        final ItemPool<PaperCard> deck = ItemPool.createFrom(ed.getDeckManager().getPool(), PaperCard.class);

        final List<String> cardProbabilities = new ArrayList<>();

        final List<PaperCard> shuffled = deck.toFlatList();
        Collections.shuffle(shuffled, MyRandom.getRandom());

        // Log totals of each card for decrementing
        final Map<PaperCard, Long> cardTotals = shuffled.stream().collect(Collectors.groupingBy(pc -> pc, Collectors.counting()));

        // Run through shuffled deck and calculate probabilities.
        // Formulas is (remaining instances of this card / total cards remaining)
        final Iterator<PaperCard> itr = shuffled.iterator();
        PaperCard tmp;
        while (itr.hasNext()) {
            tmp = itr.next();

            // int prob = SEditorUtil.calculatePercentage(
            //       cardTotals.get(tmp), shuffled.size());

            cardTotals.merge(tmp, -1l, Long::sum);
            cardProbabilities.add(tmp.getName()); // + " (" + prob + "%)");
            itr.remove();
        }

        return cardProbabilities;
    }
}
