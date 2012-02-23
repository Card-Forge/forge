package forge.quest.data;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import forge.deck.Deck;
import forge.util.IFolderMap;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class QuestDeckMap implements IFolderMap<Deck> {

    public QuestDeckMap() {
        map = new HashMap<String, Deck>();
    }

    public QuestDeckMap(Map<String, Deck> inMap) {
        map = inMap;
    }

    private final Map<String, Deck> map;
    /* (non-Javadoc)
     * @see forge.util.IFolderMapView#get(java.lang.String)
     */
    @Override
    public Deck get(String name) {
        return map.get(name);
    }

    /* (non-Javadoc)
     * @see forge.util.IFolderMapView#getNames()
     */
    @Override
    public Collection<String> getNames() {
        return map.keySet();
    }

    /* (non-Javadoc)
     * @see java.lang.Iterable#iterator()
     */
    @Override
    public Iterator<Deck> iterator() {
        return map.values().iterator();
    }

    /* (non-Javadoc)
     * @see forge.util.IFolderMap#add(forge.util.IHasName)
     */
    @Override
    public void add(Deck deck) {
        map.put(deck.getName(), deck);
    }

    /* (non-Javadoc)
     * @see forge.util.IFolderMap#delete(java.lang.String)
     */
    @Override
    public void delete(String deckName) {
        map.remove(deckName);
    }

    /* (non-Javadoc)
     * @see forge.util.IFolderMap#isUnique(java.lang.String)
     */
    @Override
    public boolean isUnique(String name) {
        return !map.containsKey(name);
    }

}
