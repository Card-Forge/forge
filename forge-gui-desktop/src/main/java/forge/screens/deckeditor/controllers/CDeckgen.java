package forge.screens.deckeditor.controllers;

import forge.Singletons;
import forge.card.CardDb;
import forge.card.MagicColor;
import forge.deck.Deck;
import forge.deck.DeckBase;
import forge.deck.DeckFormat;
import forge.deck.generation.DeckGenerator2Color;
import forge.deck.generation.DeckGenerator3Color;
import forge.deck.generation.DeckGenerator5Color;
import forge.deck.generation.DeckGeneratorBase;
import forge.deck.generation.DeckGeneratorMonoColor;
import forge.gui.UiCommand;
import forge.gui.framework.ICDoc;
import forge.item.InventoryItem;
import forge.item.PaperCard;
import forge.item.PaperCardPredicates;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.screens.deckeditor.CDeckEditorUI;
import forge.screens.deckeditor.SEditorIO;
import forge.screens.deckeditor.views.VDeckgen;
import forge.util.StreamUtil;

import java.util.List;


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

    @Override
    public void register() {
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.ICDoc#initialize()
     */
    @SuppressWarnings("serial")
    @Override
    public void initialize() {
        VDeckgen.SINGLETON_INSTANCE.getBtnRandCardpool().setCommand((UiCommand) this::newRandomConstructed);
        VDeckgen.SINGLETON_INSTANCE.getBtnRandDeck2().setCommand((UiCommand) () -> newGenerateConstructed(2));
        VDeckgen.SINGLETON_INSTANCE.getBtnRandDeck3().setCommand((UiCommand) () -> newGenerateConstructed(3));
        VDeckgen.SINGLETON_INSTANCE.getBtnRandDeck5().setCommand((UiCommand) () -> newGenerateConstructed(5));
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
        if (!SEditorIO.confirmSaveChanges(Singletons.getControl().getCurrentScreen(), true)) { return; }

        final Deck randomDeck = new Deck();

        List<PaperCard> randomCards = FModel.getMagicDb().getCommonCards().streamUniqueCards()
                .filter(PaperCardPredicates.NOT_BASIC_LAND)
                .collect(StreamUtil.random(15 * 5));
        randomDeck.getMain().addAllFlat(randomCards);

        for(final String landName : MagicColor.Constant.BASIC_LANDS) {
            randomDeck.getMain().add(landName, 1);
        }
        randomDeck.getMain().add("Terramorphic Expanse", 1);

        final ACEditorBase<TItem, TModel> ed = (ACEditorBase<TItem, TModel>)
                CDeckEditorUI.SINGLETON_INSTANCE.getCurrentEditorController();

        ed.getDeckController().setModel((TModel) randomDeck);
    }

    @SuppressWarnings("unchecked")
    private <TItem extends InventoryItem, TModel extends DeckBase> void newGenerateConstructed(final int colorCount0) {
        if (!SEditorIO.confirmSaveChanges(Singletons.getControl().getCurrentScreen(), true)) { return; }

        final Deck genConstructed = new Deck();
        final CardDb cardDb = FModel.getMagicDb().getCommonCards();
        DeckGeneratorBase gen = null;
        switch (colorCount0) {
        case 1: gen = new DeckGeneratorMonoColor(cardDb, DeckFormat.Constructed, null);             break;
        case 2: gen = new DeckGenerator2Color(cardDb, DeckFormat.Constructed, null, null);          break;
        case 3: gen = new DeckGenerator3Color(cardDb, DeckFormat.Constructed, null, null, null);    break;
        case 5: gen = new DeckGenerator5Color(cardDb, DeckFormat.Constructed);                      break;
        }

        if( null != gen ) {
            gen.setSingleton(FModel.getPreferences().getPrefBoolean(FPref.DECKGEN_SINGLETONS));
            gen.setUseArtifacts(!FModel.getPreferences().getPrefBoolean(FPref.DECKGEN_ARTIFACTS));
            genConstructed.getMain().addAll(gen.getDeck(60, false));
        }

        final ACEditorBase<TItem, TModel> ed = (ACEditorBase<TItem, TModel>) CDeckEditorUI.SINGLETON_INSTANCE.getCurrentEditorController();

        ed.getDeckController().setModel((TModel) genConstructed);
    }
}
