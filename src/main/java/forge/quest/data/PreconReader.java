package forge.quest.data;

import java.io.File;
import java.io.FilenameFilter;
import forge.deck.io.DeckSerializer;
import forge.item.PreconDeck;
import forge.util.FolderStorageReader;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class PreconReader extends FolderStorageReader<PreconDeck> {

    /**
     * TODO: Write javadoc for Constructor.
     * @param deckDir0
     */
    public PreconReader(File deckDir0) {
        super(deckDir0);
    }

    /* (non-Javadoc)
     * @see forge.deck.io.DeckSerializerBase#read(java.io.File)
     */
    @Override
    protected PreconDeck read(File file) {
        return new PreconDeck(file);
    }

    /* (non-Javadoc)
     * @see forge.deck.io.DeckSerializerBase#getFileFilter()
     */
    @Override
    protected FilenameFilter getFileFilter() {
        return DeckSerializer.DCK_FILE_FILTER;
    }

    /* (non-Javadoc)
     * @see forge.util.IItemReader#readAll()
     */


}
