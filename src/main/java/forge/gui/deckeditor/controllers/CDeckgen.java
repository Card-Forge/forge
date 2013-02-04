package forge.gui.deckeditor.controllers;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;

import forge.Command;
import forge.card.CardRulesPredicates;
import forge.deck.Deck;
import forge.deck.DeckBase;
import forge.deck.generate.Generate2ColorDeck;
import forge.deck.generate.Generate3ColorDeck;
import forge.deck.generate.Generate5ColorDeck;
import forge.game.player.PlayerType;
import forge.gui.deckeditor.CDeckEditorUI;
import forge.gui.deckeditor.SEditorIO;
import forge.gui.deckeditor.views.VDeckgen;
import forge.gui.framework.ICDoc;
import forge.gui.toolbox.FLabel;
import forge.item.CardDb;
import forge.item.CardPrinted;
import forge.item.InventoryItem;
import forge.util.Aggregates;


/** 
 * Controls the "analysis" panel in the deck editor UI.
 * 
 * <br><br><i>(C at beginning of class name denotes a control class.)</i>
 *
 */
public enum CDeckgen implements ICDoc {
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
    @SuppressWarnings("serial")
    @Override
    public void initialize() {
        ((FLabel) VDeckgen.SINGLETON_INSTANCE.getBtnRandCardpool()).setCommand(new Command() {
            @Override
            public void execute() {
                newRandomConstructed();
            }
        });

        ((FLabel) VDeckgen.SINGLETON_INSTANCE.getBtnRandDeck2()).setCommand(new Command() {
            @Override  public void execute() { newGenerateConstructed(2); } });

        ((FLabel) VDeckgen.SINGLETON_INSTANCE.getBtnRandDeck3()).setCommand(new Command() {
            @Override  public void execute() { newGenerateConstructed(3); } });

        ((FLabel) VDeckgen.SINGLETON_INSTANCE.getBtnRandDeck5()).setCommand(new Command() {
            @Override  public void execute() { newGenerateConstructed(5); } });
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#update()
     */
    @Override
    public void update() {
    }

    //========== Other methods
    @SuppressWarnings("unchecked")
    private <TItem extends InventoryItem, TModel extends DeckBase> void newRandomConstructed() {
        if (!SEditorIO.confirmSaveChanges()) { return; }

        final Deck randomDeck = new Deck();

        Predicate<CardPrinted> notBasicLand = Predicates.not(Predicates.compose(CardRulesPredicates.Presets.IS_BASIC_LAND, CardPrinted.FN_GET_RULES));
        Iterable<CardPrinted> source = Iterables.filter(CardDb.instance().getAllUniqueCards(), notBasicLand);
        randomDeck.getMain().addAllFlat(Aggregates.random(source, 15 * 5));

        randomDeck.getMain().add("Plains", 1);
        randomDeck.getMain().add("Island", 1);
        randomDeck.getMain().add("Swamp", 1);
        randomDeck.getMain().add("Mountain", 1);
        randomDeck.getMain().add("Forest", 1);
        randomDeck.getMain().add("Terramorphic Expanse", 1);

        final ACEditorBase<TItem, TModel> ed = (ACEditorBase<TItem, TModel>)
                CDeckEditorUI.SINGLETON_INSTANCE.getCurrentEditorController();

        ed.getDeckController().setModel((TModel) randomDeck);
    }

    @SuppressWarnings("unchecked")
    private <TItem extends InventoryItem, TModel extends DeckBase> void newGenerateConstructed(final int colorCount0) {
        if (!SEditorIO.confirmSaveChanges()) { return; }

        final Deck genConstructed = new Deck();

        switch (colorCount0) {
            case 2:
                genConstructed.getMain().addAll(
                        (new Generate2ColorDeck("AI", "AI")).get2ColorDeck(60, PlayerType.HUMAN));
                break;
            case 3:
                genConstructed.getMain().addAll(
                        (new Generate3ColorDeck("AI", "AI", "AI")).get3ColorDeck(60, PlayerType.HUMAN));
                break;
            case 5:
                genConstructed.getMain().addAll(
                        (new Generate5ColorDeck()).get5ColorDeck(60, PlayerType.HUMAN));
                break;
            default:
        }

        final ACEditorBase<TItem, TModel> ed = (ACEditorBase<TItem, TModel>)
                CDeckEditorUI.SINGLETON_INSTANCE.getCurrentEditorController();

        ed.getDeckController().setModel((TModel) genConstructed);
    }
}
