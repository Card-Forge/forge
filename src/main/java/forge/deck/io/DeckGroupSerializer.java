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
package forge.deck.io;

import java.io.File;
import java.io.FilenameFilter;
import java.util.List;
import forge.deck.Deck;
import forge.deck.DeckGroup;
import forge.util.FileUtil;
import forge.util.IItemSerializer;

/**
 * TODO: Write javadoc for this type.
 * 
 */
public class DeckGroupSerializer extends DeckReaderBase<DeckGroup> implements IItemSerializer<DeckGroup> {
    /**
     * TODO: Write javadoc for Constructor.
     * @param deckDir0
     */
    public DeckGroupSerializer(File deckDir0) {
        super(deckDir0);
    }

    public final static int MAX_DRAFT_PLAYERS = 8;

    /**
     * 
     * Write draft Decks.
     * 
     * @param drafts
     *            a Deck[]
     */
    @Override
    public void save(DeckGroup unit) {
        final File f = makeFileFor(unit);
        f.mkdir();
        FileUtil.writeFile(new File(f, "human.dck"), unit.getHumanDeck().save());
        List<Deck> aiDecks = unit.getAiDecks();
        for (int i = 1; i <= aiDecks.size(); i++) {
            FileUtil.writeFile(new File(f, "ai-" + i + ".dck"), aiDecks.get(i - 1).save());
        }
    }

    protected final DeckGroup read(File file) {

        Deck human = Deck.fromFile(new File(file, "human.dck"));
        if (null == human) {
            return null;
        }

        final DeckGroup d = new DeckGroup(human.getName());
        d.setHumanDeck(human);
        for (int i = 1; i < MAX_DRAFT_PLAYERS; i++) {
            File theFile = new File(file, "ai-" + i + ".dck");
            if (!theFile.exists()) {
                break;
            }

            d.addAiDeck(Deck.fromFile(theFile));
        }
        return d;
    }


    /* (non-Javadoc)
     * @see forge.deck.IDeckSerializer#erase(forge.item.CardCollectionBase, java.io.File)
     */
    @Override
    public void erase(DeckGroup unit) {
        File dir = makeFileFor(unit);
        final File[] files = dir.listFiles();
        for (File f : files) {
            f.delete();
        }
        dir.delete();
    }

    public File makeFileFor(final DeckGroup decks) {
        return new File(getDirectory(), deriveFileName(cleanDeckName(decks.getName())));
    }

    /* (non-Javadoc)
     * @see forge.deck.io.DeckSerializerBase#getFileFilter()
     */
    @Override
    protected FilenameFilter getFileFilter() {
        return new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                return dir.isDirectory() && !dir.isHidden();
            }
        };
    }

}
