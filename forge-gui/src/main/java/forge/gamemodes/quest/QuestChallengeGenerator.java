package forge.gamemodes.quest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.common.base.Predicate;

import forge.deck.DeckgenUtil;
import forge.game.GameFormat;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.util.MyRandom;
import forge.util.storage.IStorage;

public class QuestChallengeGenerator {

    private GameFormat formatMedium=FModel.getFormats().getModern();
    private GameFormat formatHard=FModel.getFormats().get("Legacy");
    private GameFormat formatExpert=FModel.getFormats().get("Vintage");
    private GameFormat baseFormat;

    QuestChallengeGenerator(GameFormat baseFormat){
        this.baseFormat=baseFormat;
        if (baseFormat.getName().equals((FModel.getFormats().getModern().getName()))) {
            formatMedium=FModel.getFormats().get("Legacy");
            formatHard=FModel.getFormats().get("Vintage");
        }
    }

    public QuestEventChallengeList generateChallenges(){
        Map<String,QuestEventChallenge> challenges = new HashMap<>();
        int id = 0;
        for (int i=0;i<5;++i) {
            QuestEventChallenge qc = getFormatChallenge(formatMedium);
            qc.setId(Integer.valueOf(id).toString());
            qc.setCreditsReward(1000);
            qc.setWinsReqd(MyRandom.getRandom().nextInt(5));
            qc.setDifficulty(QuestEventDifficulty.MEDIUM);
            qc.setCardReward("1 multicolor rare");
            challenges.put(Integer.toString(id),qc);
            id++;
        }
        for (int i=0;i<5;++i) {
            QuestEventChallenge qc = getAIHeadstartChallenge(1);
            qc.setId(Integer.valueOf(id).toString());
            qc.setCreditsReward(1000);
            qc.setCardReward("1 multicolor rare");
            qc.setWinsReqd(MyRandom.getRandom().nextInt(5));
            qc.setDifficulty(QuestEventDifficulty.EASY);
            challenges.put(Integer.toString(id),qc);
            id++;
        }
        for (int i=0;i<5;++i) {
            QuestEventChallenge qc = getFormatChallenge(formatHard);
            qc.setId(Integer.valueOf(id).toString());
            qc.setCreditsReward(5000);
            qc.setCardReward("2 multicolor rares");
            qc.setWinsReqd(MyRandom.getRandom().nextInt(25));
            qc.setDifficulty(QuestEventDifficulty.HARD);
            challenges.put(Integer.toString(id),qc);
            id++;
        }
        for (int i=0;i<5;++i) {
            QuestEventChallenge qc = getAIHeadstartChallenge(2);
            qc.setId(Integer.valueOf(id).toString());
            qc.setCreditsReward(5000);
            qc.setCardReward("2 multicolor rares");
            qc.setWinsReqd(MyRandom.getRandom().nextInt(25));
            qc.setDifficulty(QuestEventDifficulty.MEDIUM);
            challenges.put(Integer.toString(id),qc);
            id++;
        }
        for (int i=0;i<5;++i) {
            QuestEventChallenge qc = getFormatChallenge(formatExpert);
            qc.setId(Integer.valueOf(id).toString());
            qc.setCreditsReward(10000);
            qc.setCardReward("3 multicolor rares");
            qc.setWinsReqd(MyRandom.getRandom().nextInt(50));
            qc.setDifficulty(QuestEventDifficulty.EXPERT);
            challenges.put(Integer.toString(id),qc);
            id++;
        }
        for (int i=0;i<5;++i) {
            QuestEventChallenge qc = getAIHeadstartChallenge(3);
            qc.setId(Integer.valueOf(id).toString());
            qc.setCreditsReward(10000);
            qc.setCardReward("3 multicolor rares");
            qc.setWinsReqd(MyRandom.getRandom().nextInt(50));
            qc.setDifficulty(QuestEventDifficulty.HARD);
            challenges.put(Integer.toString(id),qc);
            id++;
        }
        return new QuestEventChallengeList(challenges);
    }

    public QuestEventChallenge getFormatChallenge(GameFormat format){
        QuestEventChallenge qc = new QuestEventChallenge();

        qc.setAiLife(20);
        qc.setEventDeck(DeckgenUtil.buildLDACArchetypeDeck(format,true));
        qc.setTitle(format.getName() + " " + qc.getEventDeck().getName() + " challenge");
        qc.setName(format.getName() + " " + qc.getEventDeck().getName() + " challenge");
        qc.setOpponentName(qc.getEventDeck().getName());
        qc.setDescription("Take on a " + format.getName() + " format deck");
        qc.setOpponentName(qc.getEventDeck().getName());
        qc.setRepeatable(true);
        return qc;
    }

    public QuestEventChallenge getAIHeadstartChallenge(int extras){
        QuestEventChallenge qc = new QuestEventChallenge();

        qc.setAiLife(20);
        qc.setEventDeck(DeckgenUtil.buildLDACArchetypeDeck(baseFormat,true));
        qc.setTitle(qc.getEventDeck().getName() + " headstart challenge");
        qc.setName(qc.getEventDeck().getName() + " headstart  challenge");
        qc.setOpponentName(qc.getEventDeck().getName());
        qc.setDescription("The AI gets a bit of a headstart...");
        ArrayList<String> cards = new ArrayList<>();
        int i = 0;
        while(i < extras) {
            PaperCard card = qc.getEventDeck().getMain().toFlatList().get(
                    MyRandom.getRandom().nextInt(qc.getEventDeck().getMain().toFlatList().size()));
            if(card.getRules().getType().isPermanent()){
                cards.add(card.getName());
                ++i;
            }
        }
        qc.setAiExtraCards(cards);
        qc.setOpponentName(qc.getEventDeck().getName());
        qc.setRepeatable(true);
        return qc;
    }

    public class QuestEventChallengeList implements IStorage<QuestEventChallenge>{

        private Map<String,QuestEventChallenge> challenges;

        public QuestEventChallengeList(Map<String, QuestEventChallenge> list){
            challenges = list;
        }

        @Override
        public String getFullPath() {
            return null;
        }

        @Override
        public QuestEventChallenge get(String id) {
            return challenges.get(id);
        }

        @Override
        public QuestEventChallenge find(Predicate<QuestEventChallenge> condition) {
            for(QuestEventChallenge challenge:challenges.values()){
                if(condition.apply(challenge)){
                    return challenge;
                }
            }
            return null;
        }

        @Override
        public Collection<String> getItemNames() {
            List<String> names = new ArrayList<>();
            for(QuestEventChallenge challenge:challenges.values()){
                names.add(challenge.getName());
            }
            return names;
        }

        @Override
        public boolean contains(String id) {
            return challenges.containsKey(id);
        }

        @Override
        public int size() {
            return challenges.keySet().size();
        }

        @Override
        public void add(QuestEventChallenge item) { }

        @Override
        public void add(String name, QuestEventChallenge item) { }

        @Override
        public void delete(String id) {
            challenges.remove(id);
        }

        @Override
        public IStorage<IStorage<QuestEventChallenge>> getFolders() {
            return null;
        }

        @Override
        public IStorage<QuestEventChallenge> tryGetFolder(String path) {
            return null;
        }

        @Override
        public IStorage<QuestEventChallenge> getFolderOrCreate(String path) {
            return null;
        }

        @Override
        public String getName() {
            return "QuestChallenges";
        }

        @Override
        public Iterator<QuestEventChallenge> iterator() {
            return challenges.values().iterator();
        }
    }
}
