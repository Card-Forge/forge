/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Nate
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.deck;

import java.io.File;

import forge.deck.io.DeckGroupSerializer;
import forge.deck.io.DeckSerializer;
import forge.deck.io.OldDeckParser;
import forge.util.FolderMap;
import forge.util.IFolderMap;

/**
 * Holds editable maps of decks saved to disk. Adding or removing items to(from)
 * such map turns into immediate file update
 */
public class CardCollections {
    private final IFolderMap<Deck> constructed;
    private final IFolderMap<DeckGroup> draft;
    private final IFolderMap<DeckGroup> sealed;
    private final IFolderMap<Deck> cube;

    /**
     * TODO: Write javadoc for Constructor.
     *
     * @param file the file
     */
    public CardCollections(final File file) {
        this.constructed = new FolderMap<Deck>(new DeckSerializer(new File(file, "constructed")));
        this.draft = new FolderMap<DeckGroup>(new DeckGroupSerializer(new File(file, "draft")));
        this.sealed = new FolderMap<DeckGroup>(new DeckGroupSerializer(new File(file, "sealed")));
        this.cube = new FolderMap<Deck>(new DeckSerializer(new File(file, "cube")));

        // remove this after most people have been switched to new layout
        final OldDeckParser oldParser = new OldDeckParser(file, this.constructed, this.draft, this.sealed, this.cube);
        oldParser.tryParse();
    }

    /**
     * Gets the constructed.
     *
     * @return the constructed
     */
    public final IFolderMap<Deck> getConstructed() {
        return this.constructed;
    }

    /**
     * Gets the draft.
     *
     * @return the draft
     */
    public final IFolderMap<DeckGroup> getDraft() {
        return this.draft;
    }

    /**
     * Gets the cubes.
     *
     * @return the cubes
     */
    public final IFolderMap<Deck> getCubes() {
        return this.cube;
    }

    /**
     * Gets the sealed.
     *
     * @return the sealed
     */
    public IFolderMap<DeckGroup> getSealed() {
        return this.sealed;
    }

}
