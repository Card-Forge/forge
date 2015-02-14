/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
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
package forge.model;

import forge.deck.Deck;
import forge.deck.DeckGroup;
import forge.deck.io.DeckGroupSerializer;
import forge.deck.io.DeckStorage;
import forge.deck.io.OldDeckParser;
import forge.properties.ForgeConstants;
import forge.util.storage.IStorage;
import forge.util.storage.StorageImmediatelySerialized;

import org.apache.commons.lang3.time.StopWatch;

import java.io.File;

/**
 * Holds editable maps of decks saved to disk. Adding or removing items to(from)
 * such map turns into immediate file update
 */
public class CardCollections {
    private final IStorage<Deck> constructed;
    private final IStorage<DeckGroup> draft;
    private final IStorage<DeckGroup> sealed;
    private final IStorage<DeckGroup> winston;
    private final IStorage<Deck> cube;
    private final IStorage<Deck> scheme;
    private final IStorage<Deck> plane;
    private final IStorage<Deck> commander;
    private final IStorage<Deck> tinyLeaders;

    public CardCollections() {
        StopWatch sw = new StopWatch();
        sw.start();
        constructed = new StorageImmediatelySerialized<Deck>("Constructed decks", new DeckStorage(new File(ForgeConstants.DECK_CONSTRUCTED_DIR), true), true);
        draft = new StorageImmediatelySerialized<DeckGroup>("Draft deck sets", new DeckGroupSerializer(new File(ForgeConstants.DECK_DRAFT_DIR)));
        sealed = new StorageImmediatelySerialized<DeckGroup>("Sealed deck sets", new DeckGroupSerializer(new File(ForgeConstants.DECK_SEALED_DIR)));
        winston = new StorageImmediatelySerialized<DeckGroup>("Winston draft deck sets", new DeckGroupSerializer(new File(ForgeConstants.DECK_WINSTON_DIR)));
        cube = new StorageImmediatelySerialized<Deck>("Cubes", new DeckStorage(new File(ForgeConstants.DECK_CUBE_DIR)));
        scheme = new StorageImmediatelySerialized<Deck>("Archenemy decks", new DeckStorage(new File(ForgeConstants.DECK_SCHEME_DIR)));
        plane = new StorageImmediatelySerialized<Deck>("Planechase decks", new DeckStorage(new File(ForgeConstants.DECK_PLANE_DIR)));
        commander = new StorageImmediatelySerialized<Deck>("Commander decks", new DeckStorage(new File(ForgeConstants.DECK_COMMANDER_DIR)));
        tinyLeaders = new StorageImmediatelySerialized<Deck>("Commander decks", new DeckStorage(new File(ForgeConstants.DECK_TINY_LEADERS_DIR)));
        sw.stop();
        System.out.printf("Read decks (%d ms): %d constructed, %d sealed, %d draft, %d cubes, %d scheme, %d planar, %d commander, %d tiny leaders.%n", sw.getTime(), constructed.size(), sealed.size(), draft.size(), cube.size(), scheme.size(), plane.size(), commander.size(), tinyLeaders.size());
//        int sum = constructed.size() + sealed.size() + draft.size() + cube.size() + scheme.size() + plane.size();
//        FSkin.setProgessBarMessage(String.format("Loaded %d decks in %f sec", sum, sw.getTime() / 1000f ));
        // remove this after most people have been switched to new layout
        final OldDeckParser oldParser = new OldDeckParser(constructed, draft, sealed, cube);
        oldParser.tryParse();
    }

    public final IStorage<Deck> getConstructed() {
        return constructed;
    }

    public final IStorage<DeckGroup> getDraft() {
        return draft;
    }

    public final IStorage<DeckGroup> getWinston() {
        return winston;
    }

    public final IStorage<Deck> getCubes() {
        return cube;
    }

    public IStorage<DeckGroup> getSealed() {
        return sealed;
    }

    public IStorage<Deck> getScheme() {
        return scheme;
    }

    public IStorage<Deck> getPlane() {
        return plane;
    }

    public IStorage<Deck> getCommander() {
        return commander;
    }

    public IStorage<Deck> getTinyLeaders() {
        return tinyLeaders;
    }
}
