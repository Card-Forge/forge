package forge.deck;

import java.io.File;

import forge.deck.io.DeckSerializer;
import forge.deck.io.DeckGroupSerializer;
import forge.deck.io.OldDeckParser;
import forge.util.FolderMap;
import forge.util.IFolderMap;


/** 
 * Holds editable maps of decks saved to disk. 
 * Adding or removing items to(from) such map turns into immediate file update
 */
public class CardCollections {
    private final IFolderMap<Deck> constructed;
    private final IFolderMap<DeckGroup> draft;
    private final IFolderMap<DeckGroup> sealed;
    private final IFolderMap<Deck> cube;

    /**
     * TODO: Write javadoc for Constructor.
     * @param file
     */
    public CardCollections(File file) {
        constructed = new FolderMap<Deck>(new DeckSerializer(new File(file, "constructed")));
        draft = new FolderMap<DeckGroup>(new DeckGroupSerializer(new File(file, "draft")));
        sealed = new FolderMap<DeckGroup>(new DeckGroupSerializer(new File(file, "sealed")));
        cube = new FolderMap<Deck>(new DeckSerializer(new File(file, "cube")));

        // remove this after most people have been switched to new layout
        OldDeckParser oldParser = new OldDeckParser(file, constructed, draft, sealed, cube);
        oldParser.tryParse();
    }

    public final IFolderMap<Deck> getConstructed() {
        return constructed;
    }

    public final IFolderMap<DeckGroup> getDraft() {
        return draft;
    }

    public final IFolderMap<Deck> getCubes() {
        return cube;
    }

    public IFolderMap<DeckGroup> getSealed() {
        return sealed;
    }



}
