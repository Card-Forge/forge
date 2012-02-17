package forge.gui.deckeditor;

import java.awt.Component;
import java.util.List;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public interface IDeckManager<T> {

    void newModel();
    void setModel(T model);
    T getModel();
    /** Call this anytime model becomes different from the saved on disk state*/
    void notifyModelChanged();
    Component getOwnerWindow();
    DeckEditorBase<?, T> getView();
    /** Gets names of saved models in folder / questData */
    List<String> getSavedNames();
    void load(String name);
    void save();
    void saveAs(String name0);
    boolean isSaved();
    void delete();
    /** Returns true if no object exists with that name */
    boolean isGoodName(String deckName);
    /** Import in quest adds add cards to pool, unlike constructed */
    void importDeck(T newDeck);
    /** Tells if this deck was already saved to disk / questData */
    boolean isModelInStore();
    /**
     * TODO: Write javadoc for this method.
     * @param deckName
     * @return
     */
    boolean fileExists(String deckName);
    
    /*
     // IMPORT DECK CODE
        this.questData.addDeck(newDeck);
    
        final ItemPool<CardPrinted> cardpool = ItemPool.createFrom(this.questData.getCards().getCardpool(),
                CardPrinted.class);
        final ItemPool<CardPrinted> decklist = new ItemPool<CardPrinted>(CardPrinted.class);
        for (final Entry<CardPrinted, Integer> s : newDeck.getMain()) {
            final CardPrinted cp = s.getKey();
            decklist.add(cp, s.getValue());
            cardpool.add(cp, s.getValue());
            this.questData.getCards().getCardpool().add(cp, s.getValue());
        }
        this.controller.showItems(cardpool, decklist);
     */
}
