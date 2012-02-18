package forge.deck;

import java.io.File;

import forge.deck.io.CubeSerializer;
import forge.deck.io.DeckSerializer;
import forge.deck.io.DeckSetSerializer;
import forge.deck.io.OldDeckParser;
import forge.util.FolderMap;
import forge.util.IFolderMap;


/** 
 * TODO: Write javadoc for this type.
 *
 */
public class CardCollections {
    private final IFolderMap<Deck> constructed;
    private final IFolderMap<DeckSet> draft;
    private final IFolderMap<DeckSet> sealed;
    private final IFolderMap<CustomLimited> cube;

    /**
     * TODO: Write javadoc for Constructor.
     * @param file
     */
    public CardCollections(File file) {
        constructed = new FolderMap<Deck>(new DeckSerializer(new File(file, "constructed")));
        draft = new FolderMap<DeckSet>(new DeckSetSerializer(new File(file, "draft")));
        sealed = new FolderMap<DeckSet>(new DeckSetSerializer(new File(file, "sealed")));
        cube = new FolderMap<CustomLimited>(new CubeSerializer(new File(file, "cube")));

        // remove this after most people have been switched to new layout
        OldDeckParser oldParser = new OldDeckParser(file, constructed, draft, sealed, cube);
        oldParser.tryParse();
    }

    public final IFolderMap<Deck> getConstructed() {
        return constructed;
    }

    public final IFolderMap<DeckSet> getDraft() {
        return draft;
    }

    public final IFolderMap<CustomLimited> getCubes() {
        return cube;
    }

    public IFolderMap<DeckSet> getSealed() {
        return sealed;
    }



}
