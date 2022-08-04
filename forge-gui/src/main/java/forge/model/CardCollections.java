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

import java.io.File;

import forge.deck.Deck;
import forge.deck.DeckGroup;
import forge.deck.io.DeckGroupSerializer;
import forge.deck.io.DeckStorage;
import forge.localinstance.properties.ForgeConstants;
import forge.util.storage.IStorage;
import forge.util.storage.StorageImmediatelySerialized;

/**
 * Holds editable maps of decks saved to disk. Adding or removing items to(from)
 * such map turns into immediate file update
 */
public class CardCollections {
    // Note: These are loaded lazily.
    private IStorage<Deck> constructed;
    private IStorage<DeckGroup> draft;
    private IStorage<DeckGroup> sealed;
    private IStorage<DeckGroup> winston;
    private IStorage<Deck> cube;
    private IStorage<Deck> scheme;
    private IStorage<Deck> plane;
    private IStorage<Deck> commander;
    private IStorage<Deck> commanderPrecons;
    private IStorage<Deck> oathbreaker;
    private IStorage<Deck> tinyLeaders;
    private IStorage<Deck> brawl;
    private IStorage<Deck> genetic;
    private IStorage<Deck> easy;

    public CardCollections() {
    }

    public final IStorage<Deck> getConstructed() {
        if (constructed == null) {
            constructed = new StorageImmediatelySerialized<>("Constructed decks",
                    new DeckStorage(new File(ForgeConstants.DECK_CONSTRUCTED_DIR), ForgeConstants.DECK_BASE_DIR, true),
                    true);
        }
        return constructed;
    }

    public final IStorage<DeckGroup> getDraft() {
        if (draft == null) {
            draft = new StorageImmediatelySerialized<>("Draft deck sets",
                    new DeckGroupSerializer(new File(ForgeConstants.DECK_DRAFT_DIR), ForgeConstants.DECK_BASE_DIR));
        }
        return draft;
    }

    public IStorage<DeckGroup> getSealed() {
        if (sealed == null) {
            sealed = new StorageImmediatelySerialized<>("Sealed deck sets",
                    new DeckGroupSerializer(new File(ForgeConstants.DECK_SEALED_DIR), ForgeConstants.DECK_BASE_DIR));
        }
        return sealed;
    }

    public final IStorage<DeckGroup> getWinston() {
        if (winston == null) {
            winston = new StorageImmediatelySerialized<>("Winston draft deck sets",
                    new DeckGroupSerializer(new File(ForgeConstants.DECK_WINSTON_DIR), ForgeConstants.DECK_BASE_DIR));
        }
        return winston;
    }

    public final IStorage<Deck> getCubes() {
        if (cube == null) {
            cube = new StorageImmediatelySerialized<>("Cubes",
                    new DeckStorage(new File(ForgeConstants.DECK_CUBE_DIR), ForgeConstants.RES_DIR));
        }
        return cube;
    }

    public IStorage<Deck> getScheme() {
        if (scheme == null) {
            scheme = new StorageImmediatelySerialized<>("Archenemy decks",
                    new DeckStorage(new File(ForgeConstants.DECK_SCHEME_DIR), ForgeConstants.DECK_BASE_DIR));
        }
        return scheme;
    }

    public IStorage<Deck> getPlane() {
        if (plane == null) {
            plane = new StorageImmediatelySerialized<>("Planechase decks",
                    new DeckStorage(new File(ForgeConstants.DECK_PLANE_DIR), ForgeConstants.DECK_BASE_DIR));
        }
        return plane;
    }

    public IStorage<Deck> getCommander() {
        if (commander == null) {
            commander = new StorageImmediatelySerialized<>("Commander decks",
                    new DeckStorage(new File(ForgeConstants.DECK_COMMANDER_DIR), ForgeConstants.DECK_BASE_DIR));
        }
        return commander;
    }

    public IStorage<Deck> getOathbreaker() {
        if (oathbreaker == null) {
            oathbreaker = new StorageImmediatelySerialized<>("Oathbreaker decks",
                    new DeckStorage(new File(ForgeConstants.DECK_OATHBREAKER_DIR), ForgeConstants.DECK_BASE_DIR));
        }
        return oathbreaker;
    }

    public IStorage<Deck> getCommanderPrecons() {
        if (commanderPrecons == null) {
            commanderPrecons = new StorageImmediatelySerialized<Deck>("Commander Precon decks",
                    new DeckStorage(new File(ForgeConstants.COMMANDER_PRECON_DIR), ForgeConstants.QUEST_PRECON_DIR));
        }
        return commanderPrecons;
    }

    public IStorage<Deck> getTinyLeaders() {
        if (tinyLeaders == null) {
            tinyLeaders = new StorageImmediatelySerialized<>("Tiny Leaders decks",
                    new DeckStorage(new File(ForgeConstants.DECK_TINY_LEADERS_DIR), ForgeConstants.DECK_BASE_DIR));
        }
        return tinyLeaders;
    }

    public IStorage<Deck> getBrawl() {
        if (brawl == null) {
            brawl = new StorageImmediatelySerialized<>("Brawl decks",
                    new DeckStorage(new File(ForgeConstants.DECK_BRAWL_DIR), ForgeConstants.DECK_BASE_DIR));
        }
        return brawl;
    }

    public final IStorage<Deck> getGeneticAIDecks() {
        if (genetic == null) {
            genetic = new StorageImmediatelySerialized<>("Genetic AI decks",
                    new DeckStorage(new File(ForgeConstants.GENETIC_AI_DECK_DIR), ForgeConstants.RES_DIR));
        }
        return genetic;
    }

    public final IStorage<Deck> getEasyStarterDecks() {
        if (easy == null) {
            easy = new StorageImmediatelySerialized<>("Easy Starter decks",
                    new DeckStorage(new File(ForgeConstants.EASY_STARTER_DECK_DIR), ForgeConstants.RES_DIR));
        }
        return easy;
    }
}
