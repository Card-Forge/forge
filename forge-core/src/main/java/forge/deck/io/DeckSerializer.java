package forge.deck.io;

import forge.card.CardDb;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckSection;
import forge.item.IPaperCard;
import forge.util.FileSection;
import forge.util.FileSectionManual;
import forge.util.FileUtil;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
            return new DeckFileHeader(FileSection.parse(metadata, "="));
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
        final List<String> out = new ArrayList<String>();
        out.add(String.format("[metadata]"));
    
        out.add(String.format("%s=%s", DeckFileHeader.NAME, d.getName().replaceAll("\n", "")));
        // these are optional
        if (d.getComment() != null) {
            out.add(String.format("%s=%s", DeckFileHeader.COMMENT, d.getComment().replaceAll("\n", "")));
        }
        if (!d.getTags().isEmpty()) {
            out.add(String.format("%s=%s", DeckFileHeader.TAGS, StringUtils.join(d.getTags(), DeckFileHeader.TAGS_SEPARATOR)));
        }
    
        for(Entry<DeckSection, CardPool> s : d) {
            out.add(String.format("[%s]", s.getKey().toString()));
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
    
        final Deck d = new Deck(dh.getName());
        d.setComment(dh.getComment());
        d.getTags().addAll(dh.getTags());
    
        boolean hasExplicitlySpecifiedSet = false;
        
        for (Entry<String, List<String>> s : sections.entrySet()) {
            DeckSection sec = DeckSection.smartValueOf(s.getKey());
            if (sec == null) {
                continue;
            }
            
            for(String k : s.getValue()) 
                if ( k.indexOf(CardDb.NameSetSeparator) > 0 )
                    hasExplicitlySpecifiedSet = true;
    
            CardPool pool = CardPool.fromCardList(s.getValue());
            // I used to store planes and schemes under sideboard header, so this will assign them to a correct section
            IPaperCard sample = pool.get(0);
            if (sample != null && ( sample.getRules().getType().isPlane() || sample.getRules().getType().isPhenomenon())) {
                sec = DeckSection.Planes;
            }
            if (sample != null && sample.getRules().getType().isScheme()) {
                sec = DeckSection.Schemes;
            }
    
            d.putSection(sec, pool);
        }
        
        if (!hasExplicitlySpecifiedSet) {
            d.convertByXitaxMethod();
        }
            
        return d;
    }
}