package forge.gamemodes.quest.io;

import java.io.File;
import java.io.FilenameFilter;
import java.util.List;
import java.util.Map;

import forge.ImageKeys;
import forge.deck.io.DeckSerializer;
import forge.deck.io.DeckStorage;
import forge.gamemodes.quest.QuestEvent;
import forge.gamemodes.quest.QuestEventDifficulty;
import forge.gamemodes.quest.QuestEventDuel;
import forge.util.FileSection;
import forge.util.FileUtil;
import forge.util.storage.StorageReaderFolder;

public class QuestDuelReader extends StorageReaderFolder<QuestEventDuel> {
    public QuestDuelReader(File deckDir0) {
        super(deckDir0, QuestEvent::getName);
        // TODO Auto-generated constructor stub
    }

    @Override
    protected QuestEventDuel read(File file) {
        final Map<String, List<String>> contents = FileSection.parseSections(FileUtil.readFile(file));
        final QuestEventDuel qc = new QuestEventDuel();

        // Common properties
        FileSection sectionMeta = FileSection.parse(contents.get("metadata"), FileSection.EQUALS_KV_SEPARATOR);
        qc.setTitle(sectionMeta.get("Title"));
        qc.setName(sectionMeta.get("Name")); // Challenges have unique titles
        qc.setDifficulty(QuestEventDifficulty.fromString(sectionMeta.get("Difficulty")));
        qc.setDescription(sectionMeta.get("Description", "").replace("\\n", "\n"));
        qc.setCardReward(sectionMeta.get("Card Reward"));
        qc.setIconImageKey(ImageKeys.ICON_PREFIX + sectionMeta.get("Icon"));
        if (sectionMeta.contains("Profile")) {
            qc.setProfile(sectionMeta.get("Profile"));
        }

        // Deck
        qc.setEventDeck(DeckSerializer.fromSections(contents));
        return qc;
    }

    @Override
    protected FilenameFilter getFileFilter() { 
        return DeckStorage.DCK_FILE_FILTER;
    }
}