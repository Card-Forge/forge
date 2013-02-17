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
package forge.deck.io;

import java.io.File;
import java.io.FilenameFilter;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;

import javax.swing.JOptionPane;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

import forge.deck.Deck;
import forge.deck.DeckGroup;
import forge.game.player.PlayerType;
import forge.util.FileSection;
import forge.util.FileUtil;
import forge.util.storage.IStorage;

/**
 * TODO: Write javadoc for this type.
 * 
 */
public class OldDeckParser {

    /** Constant <code>BDKFileFilter</code>. */
    public static final FilenameFilter BDK_FILE_FILTER = new FilenameFilter() {
        @Override
        public boolean accept(final File dir, final String name) {
            return name.endsWith(".bdk");
        }
    };

    /**
     * TODO: Write javadoc for Constructor.
     *
     * @param file the file
     * @param constructed2 the constructed2
     * @param draft2 the draft2
     * @param sealed2 the sealed2
     * @param cube2 the cube2
     */
    public OldDeckParser(final File file, final IStorage<Deck> constructed2, final IStorage<DeckGroup> draft2,
            final IStorage<DeckGroup> sealed2, final IStorage<Deck> cube2) {
        this.deckDir = file;
        this.sealed = sealed2;
        this.constructed = constructed2;
        this.cube = cube2;
        this.draft = draft2;
    }

    /**
     * Gets the sealed.
     *
     * @return the sealed
     */
    protected final IStorage<DeckGroup> getSealed() {
        return this.sealed;
    }

    /**
     * Gets the constructed.
     *
     * @return the constructed
     */
    protected final IStorage<Deck> getConstructed() {
        return this.constructed;
    }

    /**
     * Gets the draft.
     *
     * @return the draft
     */
    protected final IStorage<DeckGroup> getDraft() {
        return this.draft;
    }

    /**
     * Gets the cube.
     *
     * @return the cube
     */
    protected final IStorage<Deck> getCube() {
        return this.cube;
    }

    /**
     * Gets the deck dir.
     *
     * @return the deck dir
     */
    protected final File getDeckDir() {
        return this.deckDir;
    }

    private final IStorage<DeckGroup> sealed;
    private final IStorage<Deck> constructed;
    private final IStorage<DeckGroup> draft;
    private final IStorage<Deck> cube;
    private final File deckDir;

    /**
     * TODO: Write javadoc for this method.
     */
    public void tryParse() {
        this.convertConstructedAndSealed();
        this.convertDrafts();
    }

    private void convertDrafts() {
        for (final File f : this.deckDir.listFiles(OldDeckParser.BDK_FILE_FILTER)) {
            boolean gotError = false;
            final Deck human = Deck.fromFile(new File(f, "0.dck"));
            final DeckGroup d = new DeckGroup(human.getName());
            d.setHumanDeck(human);

            for (int i = 1; i < DeckGroupSerializer.MAX_DRAFT_PLAYERS; i++) {
                final Deck nextAi = Deck.fromFile(new File(f, i + ".dck"));
                if (nextAi == null) {
                    gotError = true;
                    break;
                }
                d.addAiDeck(nextAi);
            }

            boolean mayDelete = !gotError;
            if (!gotError) {
                this.draft.add(d);
            } else {
                final String msg = String.format("Draft '%s' lacked some decks.%n%nShould it be deleted?");
                mayDelete = JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(null, msg, "Draft loading error",
                        JOptionPane.YES_NO_OPTION);
            }

            if (mayDelete) {
                for (final File f1 : f.listFiles()) {
                    f1.delete();
                }
                f.delete();
            }

        }
    }

    private void convertConstructedAndSealed() {
        boolean allowDeleteUnsupportedConstructed = false;
        final Map<String, Pair<DeckGroup, MutablePair<File, File>>> sealedDecks = new TreeMap<String, Pair<DeckGroup, MutablePair<File, File>>>(
                String.CASE_INSENSITIVE_ORDER);

        for (final File f : this.deckDir.listFiles(DeckSerializer.DCK_FILE_FILTER)) {
            boolean importedOk = false;

            final List<String> fileLines = FileUtil.readFile(f);
            final Map<String, List<String>> sections = FileSection.parseSections(fileLines);
            final DeckFileHeader dh = DeckSerializer.readDeckMetadata(sections, false);
            String name = dh.getName();

            if (dh.isCustomPool()) {
                try {
                    this.cube.add(Deck.fromSections(sections));
                    importedOk = true;
                } catch (final NoSuchElementException ex) {
                    if (!allowDeleteUnsupportedConstructed) {
                        final String msg = String
                                .format("Can not convert deck '%s' for some unsupported cards it contains. %n%s%n%nMay Forge delete all such decks?",
                                        name, ex.getMessage());
                        allowDeleteUnsupportedConstructed = JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(
                                null, msg, "Problem converting decks", JOptionPane.YES_NO_OPTION);
                    }
                }
                if (importedOk || allowDeleteUnsupportedConstructed) {
                    f.delete();
                }
                continue;
            }

            switch (dh.getDeckType()) {
            case Constructed:
                try {
                    this.constructed.add(Deck.fromSections(sections));
                    importedOk = true;
                } catch (final NoSuchElementException ex) {
                    if (!allowDeleteUnsupportedConstructed) {
                        final String msg = String
                                .format("Can not convert deck '%s' for some unsupported cards it contains. %n%s%n%nMay Forge delete all such decks?",
                                        name, ex.getMessage());
                        allowDeleteUnsupportedConstructed = JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(
                                null, msg, "Problem converting decks", JOptionPane.YES_NO_OPTION);
                    }
                }
                if (importedOk || allowDeleteUnsupportedConstructed) {
                    f.delete();
                }
                break;

            case Limited:
                final boolean isAi = dh.getPlayerType() == PlayerType.COMPUTER;
                name = name.startsWith("AI_") ? name.replace("AI_", "") : name;

                Pair<DeckGroup, MutablePair<File, File>> stored = sealedDecks.get(name);
                if (null == stored) {
                    stored = ImmutablePair.of(new DeckGroup(name), MutablePair.of((File) null, (File) null));
                }

                final Deck deck = Deck.fromSections(sections);
                if (isAi) {
                    stored.getLeft().addAiDeck(deck);
                    stored.getRight().setRight(f);
                } else {
                    stored.getLeft().setHumanDeck(deck);
                    stored.getRight().setLeft(f);
                }

                if ((stored.getLeft().getHumanDeck() != null) && !stored.getLeft().getAiDecks().isEmpty()) {
                    // have both parts of sealed deck, may convert
                    this.sealed.add(stored.getLeft());
                    stored.getRight().getLeft().delete();
                    stored.getRight().getRight().delete();

                    // there stay only orphans
                    sealedDecks.remove(name);
                } else {
                    sealedDecks.put(name, stored);
                }
                break;
            default:
                break;
            }
        }

        // advise to kill orphaned decks
        if (!sealedDecks.isEmpty()) {
            final StringBuilder sb = new StringBuilder();
            for (final Pair<DeckGroup, MutablePair<File, File>> s : sealedDecks.values()) {
                final String missingPart = s.getRight().getLeft() == null ? "human" : "computer";
                sb.append(String.format("Sealed deck '%s' has no matching '%s' deck.%n", s.getKey().getName(),
                        missingPart));
            }
            sb.append(System.getProperty("line.separator"));
            sb.append("May Forge delete these decks?");
            final int response = JOptionPane.showConfirmDialog(null, sb.toString(),
                    "Some of your sealed decks are orphaned", JOptionPane.YES_NO_OPTION);
            if (response == JOptionPane.YES_OPTION) {
                for (final Pair<DeckGroup, MutablePair<File, File>> s : sealedDecks.values()) {
                    if (s.getRight().getLeft() != null) {
                        s.getRight().getLeft().delete();
                    }
                    if (s.getRight().getRight() != null) {
                        s.getRight().getRight().delete();
                    }
                }
            }
        }
    }

    /**
     * @return the deckDir
     */

}
