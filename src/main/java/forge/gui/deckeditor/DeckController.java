package forge.gui.deckeditor;

import java.awt.Component;
import java.util.ArrayList;
import net.slightlymagic.braids.util.lambda.Lambda0;

import org.apache.commons.lang3.StringUtils;

import forge.deck.DeckBase;
import forge.util.IFolderMap;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class DeckController<T extends DeckBase> implements IDeckController<T> {
    
    private T model;
    private boolean saved;
    private boolean modelInStore;
    private final IFolderMap<T> folder;
    private final DeckEditorBase<?, T> view;
    private final Lambda0<T> newModelCreator;
    
    public DeckController(IFolderMap<T> folder0, DeckEditorBase<?, T> view0, Lambda0<T> newModelCreator0)
    {
        folder = folder0;
        view = view0;
        model = null;
        saved = true;
        modelInStore = false;
        newModelCreator = newModelCreator0;
    }
    
    /**
     * @return the document
     */
    public T getModel() {
        return model;
    }


    /**
     * @param document0 the document to set
     */
    public void setModel(T document) { 
        setModel( document, false );
    }
    
    public void setModel(T document, boolean isStored) {
        modelInStore = isStored; 
        this.model = document;
        view.updateView();
        saved = true; // unless set to false in notify
        if ( !isModelInSyncWithFolder() ) {
            notifyModelChanged();
        }
    }

    private boolean isModelInSyncWithFolder() {
        T modelStored = folder.get(model.getName());
        // checks presence in dictionary only.
        if (modelStored == model) return true;
        if (null == modelStored) return false;
        
        return modelStored.equals(model);
    }
    

    /**
     * @return the view
     */
    public DeckEditorBase<?, T> getView() {
        return view;
    }

    /* (non-Javadoc)
     * @see forge.gui.deckeditor.IDeckController#notifyModelChanged()
     */
    @Override
    public void notifyModelChanged() { 
        saved = false;
        //view.setTitle();
    }


    /* (non-Javadoc)
     * @see forge.gui.deckeditor.IDeckController#getOwnerWindow()
     */
    @Override
    public Component getOwnerWindow() {
        return getView();
    }


    /* (non-Javadoc)
     * @see forge.gui.deckeditor.IDeckController#getSavedModelNames()
     */
    @Override
    public ArrayList<String> getSavedNames() {
        return new ArrayList<String>(folder.getNames());
    }


    /* (non-Javadoc)
     * @see forge.gui.deckeditor.IDeckController#load(java.lang.String)
     */
    @Override
    public void load(String name) {
        setModel(folder.get(name), true);
    }


    /* (non-Javadoc)
     * @see forge.gui.deckeditor.IDeckController#save()
     */
    @Override
    public void save() {
        folder.add(model);
        saved = true;
        modelInStore = true;
    }


    /* (non-Javadoc)
     * @see forge.gui.deckeditor.IDeckController#rename(java.lang.String)
     */
    @SuppressWarnings("unchecked")
    @Override
    public void saveAs(String name0) {
        setModel((T)model.copyTo(name0), false);
        save();
    }


    /* (non-Javadoc)
     * @see forge.gui.deckeditor.IDeckController#isSaved()
     */
    @Override
    public boolean isSaved() {
        return saved;
    }


    /* (non-Javadoc)
     * @see forge.gui.deckeditor.IDeckController#delete()
     */
    @Override
    public void delete() {
        if ( StringUtils.isNotBlank(model.getName())) {
            folder.delete(model.getName());
        }
        modelInStore = false;
        newModel();
    }


    /* (non-Javadoc)
     * @see forge.gui.deckeditor.IDeckController#isGoodName(java.lang.String)
     */
    
    @Override
    public boolean fileExists(String deckName) {
        return !folder.isUnique(deckName);
    }
    
    
    @Override
    public boolean isGoodName(String deckName) {
        return StringUtils.isNotBlank(deckName) && folder.isUnique(deckName);
    }


    /* (non-Javadoc)
     * @see forge.gui.deckeditor.IDeckController#importDeck(forge.deck.Deck)
     */
    @Override
    public void importDeck(T newDeck) {
        setModel(newDeck);
    }


    /* (non-Javadoc)
     * @see forge.gui.deckeditor.IDeckController#isModelInStore()
     */
    @Override
    public boolean isModelInStore() {
        return modelInStore;
    }


    /* (non-Javadoc)
     * @see forge.gui.deckeditor.IDeckController#newModel()
     */
    @Override
    public void newModel() {
        model = newModelCreator.apply();
        saved = true;
        view.updateView();
    }
}
