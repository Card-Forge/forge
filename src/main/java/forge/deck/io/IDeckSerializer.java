package forge.deck.io;

import forge.item.IHasName;


/** 
 * TODO: Write javadoc for this type.
 *
 */
public interface IDeckSerializer<T extends IHasName> extends IDeckReader<T> {
    void save(T unit);
    void erase(T unit);
}


