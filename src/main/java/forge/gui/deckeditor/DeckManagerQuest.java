package forge.gui.deckeditor;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import forge.deck.Deck;
import forge.quest.data.QuestData;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class DeckManagerQuest implements IDeckManager<Deck>{

    private Deck model;
    private boolean saved;
    private boolean modelInStore;
    private final Map<String, Deck> decks;
    private final DeckEditorBase<?, Deck> view;
    
    public DeckManagerQuest(QuestData questData0, DeckEditorBase<?, Deck> view0)
    {
        decks = questData0.getMyDecks();
        view = view0;
    }

    @Override
    public Deck getModel() { return model; }
    
    
    /* (non-Javadoc)
     * @see forge.gui.deckeditor.IDeckManager#newModel()
     */
    @Override
    public void newModel() {
        setModel(new Deck());
    }

    /**
     * @param document0 the document to set
     */
    public void setModel(Deck document) { 
        setModel( document, false );
    }
    
    public void setModel(Deck document, boolean isStored) {
        modelInStore = isStored; 
        this.model = document;
        view.updateView();
        saved = true; // unless set to false in notify
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.IDeckManager#getModel()
     */

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.IDeckManager#notifyModelChanged()
     */
    @Override
    public void notifyModelChanged() {
        saved = false;
        // view.setTitle()
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.IDeckManager#getOwnerWindow()
     */
    @Override
    public Component getOwnerWindow() {
        return view;
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.IDeckManager#getView()
     */
    @Override
    public DeckEditorBase<?, Deck> getView() {
        return view;
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.IDeckManager#getSavedNames()
     */
    @Override
    public List<String> getSavedNames() {
        return new ArrayList<String>(decks.keySet());
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.IDeckManager#load(java.lang.String)
     */
    @Override
    public void load(String name) {
        Deck deck =  decks.get(name);
        if ( null != deck )
            setModel(deck, true);
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.IDeckManager#save()
     */
    @Override
    public void save() {
        decks.put(model.getName(), model);
        // save to disk is done from outside
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.IDeckManager#isSaved()
     */
    @Override
    public boolean isSaved() {
        return saved;
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.IDeckManager#delete()
     */
    @Override
    public void delete() {
        if ( StringUtils.isNotBlank(model.getName())) {
            decks.remove(model.getName());
        }
        modelInStore = false;
        newModel();        
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.IDeckManager#isGoodName(java.lang.String)
     */
    @Override
    public boolean isGoodName(String deckName) {
        return false;
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.IDeckManager#importDeck(java.lang.Object)
     */
    @Override
    public void importDeck(Deck newDeck) {
        
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.IDeckManager#isModelInStore()
     */
    @Override
    public boolean isModelInStore() {
        return modelInStore;
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.IDeckManager#saveAs(java.lang.String)
     */
    @Override
    public void saveAs(String name0) {
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.IDeckManager#fileExists(java.lang.String)
     */
    @Override
    public boolean fileExists(String deckName) {
        return false;
    }

}
