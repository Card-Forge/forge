package forge.deck.io;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;

import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.util.FileSection;
import forge.util.FileSectionManual;
import forge.util.FileUtil;
import forge.util.TextUtil;

public class DeckSerializer {

    public static void writeDeck(final Deck d, final File f) {
        FileUtil.writeFile(f, serializeDeck(d));
    }

    static DeckFileHeader readDeckMetadata(final Map<String, List<String>> map) {
        if (map == null) {
            return null;
        }
        final List<String> metadata = map.get("metadata");
        if (metadata != null) {
            return new DeckFileHeader(FileSection.parse(metadata, FileSection.EQUALS_KV_SEPARATOR));
        }
        final List<String> general = map.get("general");
        if (general != null) {
            final FileSectionManual fs = new FileSectionManual();
            fs.put(DeckFileHeader.NAME, StringUtils.join(map.get(""), " "));
            fs.put(DeckFileHeader.DECK_TYPE, StringUtils.join(general, " "));
            return new DeckFileHeader(fs);
        }

        return null;
    }

    private static List<String> serializeDeck(Deck d) {
        final List<String> out = new ArrayList<>();
        out.add(TextUtil.enclosedBracket("metadata"));
    
        out.add(TextUtil.concatNoSpace(DeckFileHeader.NAME,"=", d.getName().replaceAll("\n", "")));
        // these are optional
        if (d.getComment() != null) {
            out.add(TextUtil.concatNoSpace(DeckFileHeader.COMMENT,"=", d.getComment().replaceAll("\n", "")));
        }
        if (!d.getTags().isEmpty()) {
            out.add(TextUtil.concatNoSpace(DeckFileHeader.TAGS,"=", StringUtils.join(d.getTags(), DeckFileHeader.TAGS_SEPARATOR)));
        }
        if (!d.getAiHints().isEmpty()) {
            out.add(TextUtil.concatNoSpace(DeckFileHeader.AI_HINTS, "=", StringUtils.join(d.getAiHints(), " | ")));
        }
    
        for(Entry<DeckSection, CardPool> s : d) {
            out.add(TextUtil.enclosedBracket(s.getKey().toString()));
            out.add(s.getValue().toCardList(System.getProperty("line.separator")));
        }
        return out;
    }

    public static Deck fromFile(final File deckFile) {
        return fromSections(FileSection.parseSections(FileUtil.readFile(deckFile)));
    }

    public static Deck fromSections(final Map<String, List<String>> sections) {
        if (sections == null || sections.isEmpty()) {
            return null;
        }
    
        final DeckFileHeader dh = readDeckMetadata(sections);
        if (dh == null) {
            return null;
        }

        Deck d = new Deck(dh.getName());
        d.setComment(dh.getComment());
        d.setAiHints(dh.getAiHints());
        d.getTags().addAll(dh.getTags());
        d.setDeferredSections(sections);
        return d;
    }
}