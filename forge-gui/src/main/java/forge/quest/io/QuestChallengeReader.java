package forge.quest.io;

import forge.ImageKeys;
import forge.deck.io.DeckSerializer;
import forge.deck.io.DeckStorage;
import forge.quest.QuestEventChallenge;
import forge.quest.QuestEventDifficulty;
import forge.util.FileSection;
import forge.util.FileUtil;
import forge.util.TextUtil;
import forge.util.storage.StorageReaderFolder;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class QuestChallengeReader extends StorageReaderFolder<QuestEventChallenge> {
    public QuestChallengeReader(File deckDir0) {
        super(deckDir0, QuestEventChallenge.FN_GET_ID);
        // TODO Auto-generated constructor stub
    }

    @Override
    protected QuestEventChallenge read(File file) {
        final Map<String, List<String>> contents = FileSection.parseSections(FileUtil.readFile(file));
        final QuestEventChallenge qc = new QuestEventChallenge();

        // Unique properties
        FileSection sectionQuest = FileSection.parse(contents.get("quest"), "=");
        qc.setId(sectionQuest.get("ID", "-1"));
        qc.setOpponent(sectionQuest.get("OpponentName"));
        qc.setRepeatable(sectionQuest.getBoolean("Repeat", false));
        qc.setAiLife(sectionQuest.getInt("AILife", 25));
        qc.setWinsReqd(sectionQuest.getInt("Wins", 20));
        qc.setCreditsReward(sectionQuest.getInt("Credit Reward", 100));
        qc.setCardReward(sectionQuest.get("Card Reward"));
        qc.setHumanExtraCards(Arrays.asList(TextUtil.split(sectionQuest.get("HumanExtras", ""), '|')));
        qc.setAiExtraCards(Arrays.asList(TextUtil.split(sectionQuest.get("AIExtras", ""), '|')));
        // Less common properties
        int humanLife = sectionQuest.getInt("HumanLife", 0);
        if (humanLife != 0) {
            qc.setHumanLife(humanLife);
        }
        qc.setUseBazaar(sectionQuest.getBoolean("UseBazaar", true));
        qc.setForceAnte(sectionQuest.contains("ForceAnte") ? sectionQuest.getBoolean("ForceAnte") : null);

        String humanDeck = sectionQuest.get("HumanDeck", null);
        if (humanDeck != null) {
            // Defined human decks must live in the same directory as each other
            try {
                File humanFile = new File(file.getParent(), humanDeck);
                qc.setHumanDeck(DeckSerializer.fromFile(humanFile));
            } catch (Exception e) {
                System.out.println("Defined human deck couldn't be loaded from " + file.getParent());
            }
        }

        // Common properties
        FileSection sectionMeta = FileSection.parse(contents.get("metadata"), "=");
        qc.setTitle(sectionMeta.get("Title"));
        qc.setName(qc.getTitle()); // Challenges have unique titles
        qc.setDifficulty(QuestEventDifficulty.fromString(sectionMeta.get("Difficulty")));
        qc.setDescription(sectionMeta.get("Description"));
        qc.setIconImageKey(ImageKeys.ICON_PREFIX + sectionMeta.get("Icon"));

        // Deck
        qc.setEventDeck(DeckSerializer.fromSections(contents));
        return qc;
    }

    @Override
    protected FilenameFilter getFileFilter() { 
        return DeckStorage.DCK_FILE_FILTER;
    }
}