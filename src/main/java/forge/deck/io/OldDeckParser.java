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

import forge.PlayerType;
import forge.deck.Deck;
import forge.deck.DeckGroup;
import forge.util.FileUtil;
import forge.util.IFolderMap;
import forge.util.SectionUtil;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public class OldDeckParser {

    /** Constant <code>BDKFileFilter</code>. */
    public final static FilenameFilter bdkFileFilter = new FilenameFilter() {
        @Override
        public boolean accept(final File dir, final String name) {
            return name.endsWith(".bdk");
        }
    };

    /**
     * TODO: Write javadoc for Constructor.
     * @param file
     * @param constructed2
     * @param draft2
     * @param sealed2
     * @param cube2
     */
    public OldDeckParser(File file, IFolderMap<Deck> constructed2, IFolderMap<DeckGroup> draft2,
            IFolderMap<DeckGroup> sealed2, IFolderMap<Deck> cube2) {
        deckDir = file;
        sealed = sealed2;
        constructed = constructed2;
        cube = cube2;
        draft = draft2;
    }

    protected final IFolderMap<DeckGroup> getSealed() {
        return sealed;
    }
    protected final IFolderMap<Deck> getConstructed() {
        return constructed;
    }
    protected final IFolderMap<DeckGroup> getDraft() {
        return draft;
    }
    protected final IFolderMap<Deck> getCube() {
        return cube;
    }
    protected final File getDeckDir() {
        return deckDir;
    }

    private final IFolderMap<DeckGroup> sealed;
    private final IFolderMap<Deck> constructed;
    private final IFolderMap<DeckGroup> draft;
    private final IFolderMap<Deck> cube;
    private final File deckDir;
    /**
     * TODO: Write javadoc for this method.
     */
    public void tryParse() {
        convertConstructedAndSealed();
        convertDrafts();
    }

    private void convertDrafts() {
        for (File f : deckDir.listFiles(bdkFileFilter)) {
            boolean gotError = false;
            Deck human = Deck.fromFile(new File(f, "0.dck"));
            final DeckGroup d = new DeckGroup(human.getName());
            d.setHumanDeck(human);

            for (int i = 1; i < DeckGroupSerializer.MAX_DRAFT_PLAYERS; i++) {
                Deck nextAi = Deck.fromFile(new File(f, i + ".dck"));
                if (nextAi == null) {
                    gotError = true;
                    break;
                }
                d.addAiDeck(nextAi);
            }

            boolean mayDelete = !gotError;
            if (!gotError) {
                draft.add(d);
            } else {
                String msg = String.format("Draft '%s' lacked some decks.%n%nShould it be deleted?");
                mayDelete = JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(null, msg, "Draft loading error", JOptionPane.YES_NO_OPTION);
            }

            if (mayDelete) {
                for (File f1 : f.listFiles()) {
                    f1.delete();
                }
                f.delete();
            }

        }
    }

    private void convertConstructedAndSealed() {
       boolean allowDeleteUnsupportedConstructed = false;
       Map<String, Pair<DeckGroup, MutablePair<File, File>>> sealedDecks = new TreeMap<String, Pair<DeckGroup, MutablePair<File, File>>>(String.CASE_INSENSITIVE_ORDER);

       for (File f : deckDir.listFiles(DeckSerializer.DCK_FILE_FILTER)) {
           boolean importedOk = false;

           List<String> fileLines = FileUtil.readFile(f);
           Map<String, List<String>> sections = SectionUtil.parseSections(fileLines);
           DeckFileHeader dh = DeckSerializer.readDeckMetadata(sections, false);
           String name = dh.getName();

           if (dh.isCustomPool()) {
               try {
                   cube.add(Deck.fromSections(sections));
                   importedOk = true;
               } catch (NoSuchElementException ex) {
                   if (!allowDeleteUnsupportedConstructed) {
                       String msg = String.format("Can not convert deck '%s' for some unsupported cards it contains. %n%s%n%nMay Forge delete all such decks?", name, ex.getMessage());
                       allowDeleteUnsupportedConstructed = JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(null, msg, "Problem converting decks", JOptionPane.YES_NO_OPTION);
                   }
               }
               if (importedOk || allowDeleteUnsupportedConstructed) {
                   f.delete();
               }
               continue;
           }

           switch(dh.getDeckType()) {
           case Constructed:
               try {
                   constructed.add(Deck.fromSections(sections));
                   importedOk = true;
               } catch (NoSuchElementException ex) {
                   if (!allowDeleteUnsupportedConstructed) {
                       String msg = String.format("Can not convert deck '%s' for some unsupported cards it contains. %n%s%n%nMay Forge delete all such decks?", name, ex.getMessage());
                       allowDeleteUnsupportedConstructed = JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(null, msg, "Problem converting decks", JOptionPane.YES_NO_OPTION);
                   }
               }
               if (importedOk || allowDeleteUnsupportedConstructed) {
                   f.delete();
               }
               break;

           case Sealed:
               boolean isAi = dh.getPlayerType() == PlayerType.COMPUTER;
               name = name.startsWith("AI_") ? name.replace("AI_", "") : name;

               Pair<DeckGroup, MutablePair<File, File>> stored = sealedDecks.get(name);
               if (null == stored) {
                   stored = ImmutablePair.of(new DeckGroup(name), MutablePair.of((File) null, (File) null));
               }

               Deck deck = Deck.fromSections(sections);
               if (isAi) {
                   stored.getLeft().addAiDeck(deck);
                   stored.getRight().setRight(f);
               } else {
                   stored.getLeft().setHumanDeck(deck);
                   stored.getRight().setLeft(f);
               }

               if (stored.getLeft().getHumanDeck() != null && !stored.getLeft().getAiDecks().isEmpty()) {
                   // have both parts of sealed deck, may convert
                   sealed.add(stored.getLeft());
                   stored.getRight().getLeft().delete();
                   stored.getRight().getRight().delete();

                   // there stay only orphans
                   sealedDecks.remove(name);
               } else {
                   sealedDecks.put(name, stored);
               }
               break;
           }
       }

       // advise to kill orphaned decks
       if (!sealedDecks.isEmpty()) {
           StringBuilder sb = new StringBuilder();
           for (Pair<DeckGroup, MutablePair<File, File>> s : sealedDecks.values()) {
               String missingPart = s.getRight().getLeft() == null ? "human" : "computer";
               sb.append(String.format("Sealed deck '%s' has no matching '%s' deck.%n", s.getKey().getName(), missingPart));
           }
           sb.append(System.getProperty("line.separator"));
           sb.append("May Forge delete these decks?");
           int response = JOptionPane.showConfirmDialog(null, sb.toString(), "Some of your sealed decks are orphaned", JOptionPane.YES_NO_OPTION);
           if (response == JOptionPane.YES_OPTION) {
               for (Pair<DeckGroup, MutablePair<File, File>> s : sealedDecks.values()) {
                   if (s.getRight().getLeft() != null) { s.getRight().getLeft().delete(); }
                   if (s.getRight().getRight() != null) { s.getRight().getRight().delete(); }
               }
           }
       }
    }


    /**
     * @return the deckDir
     */

}
