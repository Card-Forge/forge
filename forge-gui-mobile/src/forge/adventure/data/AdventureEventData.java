package forge.adventure.data;

import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.Array;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import forge.Forge;
import forge.adventure.character.EnemySprite;
import forge.adventure.scene.RewardScene;
import forge.adventure.util.AdventureEventController;
import forge.adventure.util.Config;
import forge.adventure.util.Current;
import forge.adventure.util.Reward;
import forge.card.CardEdition;
import forge.deck.Deck;
import forge.game.GameType;
import forge.gamemodes.limited.BoosterDraft;
import forge.gamemodes.limited.LimitedPoolType;
import forge.model.CardBlock;
import forge.model.FModel;
import forge.util.Aggregates;
import forge.util.MyRandom;
import org.apache.commons.lang3.tuple.Pair;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class AdventureEventData implements Serializable {
    private static final long serialVersionUID = 1L;
    public transient BoosterDraft draft;
    public AdventureEventParticipant[] participants;
    public int rounds;
    public int currentRound;
    public AdventureEventRules eventRules = new AdventureEventRules();
    public AdventureEventReward[] rewards;
    public int eventOrigin;
    public String sourceID;
    public long eventSeed;
    public AdventureEventController.EventStyle style;
    public AdventureEventController.EventStatus eventStatus;
    public AdventureEventController.EventFormat format;
    private transient Random random = new Random();
    public Deck registeredDeck;
    public Deck draftedDeck; //Copy of registered before basic lands are added for event reward purposes
    public boolean isDraftComplete = false;
    public String description = "";
    public String[] packConfiguration = new String[0];
    public transient CardBlock cardBlock;
    public String cardBlockName;
    public boolean playerWon;
    public int matchesWon, matchesLost;
    private Deck[] rewardPacks;

    public AdventureEventData(AdventureEventData other) {
        participants = other.participants.clone();
        rounds = other.rounds;
        eventRules = other.eventRules;
        rewards = other.rewards.clone();
        eventOrigin = other.eventOrigin;
        sourceID = other.sourceID;
        eventSeed = other.eventSeed;
        style = other.style;
        random.setSeed(eventSeed);
        eventStatus = other.eventStatus;
        registeredDeck = other.registeredDeck;
        isDraftComplete = other.isDraftComplete;
        description = other.description;
        cardBlockName = other.cardBlockName;
        packConfiguration = other.packConfiguration;
        playerWon = other.playerWon;
        matchesWon = other.matchesWon;
        matchesLost = other.matchesLost;
    }


    public Deck[] getRewardPacks(int count) {
        Deck[] ret = new Deck[count];
        for (int i = 0; i < count; i++) {
            ret[i] = AdventureEventController.instance().generateBooster(Aggregates.random(getCardBlock().getSets()).getCode());
        }
        return ret;
    }

    private Random getEventRandom() {
        if (random == null)
            random = (eventSeed > 0 ? new Random(eventSeed) : new Random());
        return random;
    }

    public AdventureEventData(Long seed) {
        setEventSeed(seed);
        eventStatus = AdventureEventController.EventStatus.Available;
        registeredDeck = new Deck();
        cardBlock = pickWeightedCardBlock();
        if (cardBlock == null)
            return;
        cardBlockName = cardBlock.getName();

        //Below all to be fully generated in later release
        rewardPacks = getRewardPacks(3);
        generateParticipants(7);
        format = AdventureEventController.EventFormat.Draft;
        if (cardBlock != null) {
            packConfiguration = getBoosterConfiguration(cardBlock);

            rewards = new AdventureEventData.AdventureEventReward[4];
            AdventureEventData.AdventureEventReward r0 = new AdventureEventData.AdventureEventReward();
            AdventureEventData.AdventureEventReward r1 = new AdventureEventData.AdventureEventReward();
            AdventureEventData.AdventureEventReward r2 = new AdventureEventData.AdventureEventReward();
            //AdventureEventData.AdventureEventReward r3 = new AdventureEventData.AdventureEventReward();
            r0.minWins = 0;
            r0.maxWins = 0;
            r0.cardRewards = new Deck[]{rewardPacks[0]};
            rewards[0] = r0;
            r1.minWins = 1;
            r1.maxWins = 3;
            r1.cardRewards = new Deck[]{rewardPacks[1], rewardPacks[2]};
            rewards[1] = r1;
            r2.minWins = 2;
            r2.maxWins = 3;
            r2.itemRewards = new String[]{"Challenge Coin"};
            rewards[2] = r2;
        }
    }

    public void setEventSeed(long seed) {
        getEventRandom().setSeed(seed);
    }

    public CardBlock getCardBlock() {
        if (cardBlock == null) {
            cardBlock = FModel.getBlocks().get(cardBlockName);
        }
        return cardBlock;
    }

    public BoosterDraft getDraft() {
        Random placeholder = MyRandom.getRandom();
        MyRandom.setRandom(getEventRandom());
        if (draft == null && (eventStatus == AdventureEventController.EventStatus.Available || eventStatus == AdventureEventController.EventStatus.Entered)) {
            draft = BoosterDraft.createDraft(LimitedPoolType.Block, getCardBlock(), packConfiguration);
        }
        if (packConfiguration == null) {
            packConfiguration = getBoosterConfiguration(getCardBlock());
        }
        MyRandom.setRandom(placeholder);
        return draft;
    }

    private static final Predicate<CardEdition> filterPioneer = FModel.getFormats().getPioneer().editionLegalPredicate;
    private static final Predicate<CardEdition> filterModern = FModel.getFormats().getModern().editionLegalPredicate;
    private static final Predicate<CardEdition> filterVintage = FModel.getFormats().getVintage().editionLegalPredicate;
    private static final Predicate<CardEdition> filterStandard = FModel.getFormats().getStandard().editionLegalPredicate;

    public static Predicate<CardEdition> selectSetPool() {
        final int rollD100 = MyRandom.getRandom().nextInt(100);
        Predicate<CardEdition> rolledFilter;
        if (rollD100 < 40) {
            rolledFilter = filterStandard;
        } else if (rollD100 < 70) {
            rolledFilter = filterPioneer;
        } else if (rollD100 < 90) {
            rolledFilter = filterModern;
        } else {
            rolledFilter = filterVintage;
        }
        return rolledFilter;
    }


    private CardBlock pickWeightedCardBlock() {
        CardEdition.Collection editions = FModel.getMagicDb().getEditions();
        Iterable<CardBlock> src = FModel.getBlocks(); //all blocks
        Predicate<CardEdition> filter = Predicates.and(CardEdition.Predicates.CAN_MAKE_BOOSTER, selectSetPool());
        List<CardEdition> allEditions = new ArrayList<>();
        StreamSupport.stream(editions.spliterator(), false).filter(filter::apply).filter(CardEdition::hasBoosterTemplate).collect(Collectors.toList()).iterator().forEachRemaining(allEditions::add);

        //Temporary restriction until rewards are more diverse - don't want to award restricted cards so these editions need different rewards added.
        List<String> restrictedDrafts = new ArrayList<>();
        restrictedDrafts.add("LEA");
        restrictedDrafts.add("LEB");
        restrictedDrafts.add("2ED");
        restrictedDrafts.add("30A");
        allEditions.removeIf(q -> restrictedDrafts.contains(q.getCode()));

        List<CardBlock> legalBlocks = new ArrayList<>();
        for (CardBlock b : src) { // for each block
            boolean isOkay = !(b.getSets().isEmpty() && b.getCntBoostersDraft() > 0);
            for (CardEdition c : b.getSets()) {
                if (!allEditions.contains(c)) {
                    isOkay = false;
                    break;
                }
                if (!c.hasBoosterTemplate()) {
                    isOkay = false;
                    break;
                } else {
                    final List<Pair<String, Integer>> slots = c.getBoosterTemplate().getSlots();
                    int boosterSize = 0;
                    for (Pair<String, Integer> slot : slots) {
                        boosterSize += slot.getRight();
                    }
                    isOkay = boosterSize == 15;
                }
            }
            if (isOkay)
                legalBlocks.add(b);
        }

        for (String restricted : Config.instance().getConfigData().restrictedEditions) {
            legalBlocks.removeIf(q -> q.getName().equals(restricted));
        }
        return legalBlocks.isEmpty() ? null : Aggregates.random(legalBlocks);
    }

    /**
     * Default filter that only allows actual sets that were printed as 15-card boosters
     */
    private static final Predicate<CardEdition> DEFAULT_FILTER = new Predicate<CardEdition>() {
        @Override
        public boolean apply(final CardEdition cardEdition) {
            if (cardEdition == null)
                return false;
            boolean isExpansion = CardEdition.Type.EXPANSION.equals(cardEdition.getType());
            boolean isCoreSet = CardEdition.Type.CORE.equals(cardEdition.getType());
            boolean isReprintSet = CardEdition.Type.REPRINT.equals(cardEdition.getType());
            if (isExpansion || isCoreSet || isReprintSet) {
                // Only allow sets with 15 cards in booster packs
                if (cardEdition.hasBoosterTemplate()) {
                    final List<Pair<String, Integer>> slots = cardEdition.getBoosterTemplate().getSlots();
                    int boosterSize = 0;
                    for (Pair<String, Integer> slot : slots) {
                        boosterSize += slot.getRight();
                    }
                    return boosterSize == 15;
                }
            }
            return false;
        }
    };

    public String[] getBoosterConfiguration(CardBlock selectedBlock) {
        Random placeholder = MyRandom.getRandom();
        MyRandom.setRandom(getEventRandom());
        String[] ret = new String[selectedBlock.getCntBoostersDraft()];

        for (int i = 0; i < selectedBlock.getCntBoostersDraft(); i++) {
            if (i < selectedBlock.getNumberSets())
                ret[i] = selectedBlock.getSets().get(i).getCode();
            else
                ret[i] = Aggregates.random(selectedBlock.getSets()).getCode();
        }
        MyRandom.setRandom(placeholder);
        return ret;
    }

    public void startEvent() {
        if (eventStatus == AdventureEventController.EventStatus.Ready) {
            currentRound = 1;
            eventStatus = AdventureEventController.EventStatus.Started;
        }
    }

    public void generateParticipants(int numberOfOpponents) {
        participants = new AdventureEventParticipant[numberOfOpponents + 1];

        List<EnemyData> data = Aggregates.random(WorldData.getAllEnemies(), numberOfOpponents);
        for (int i = 0; i < numberOfOpponents; i++) {
            participants[i] = new AdventureEventParticipant().generate(data.get(i));
        }

        participants[numberOfOpponents] = getHumanPlayer();
    }

    private transient AdventureEventHuman humanPlayerInstance;

    public AdventureEventHuman getHumanPlayer() {
        if (humanPlayerInstance == null) {
            humanPlayerInstance = new AdventureEventHuman();
        }
        return humanPlayerInstance;
    }

    public AdventureEventParticipant nextOpponent = null;

    public List<AdventureEventMatch> getMatches(int round) {
        if (matches.containsKey(round)) {
            return matches.get(round);
        }

        List<AdventureEventParticipant> activePlayers = new ArrayList<>();
        if (style == AdventureEventController.EventStyle.Bracket) {
            if (round == 1) {
                activePlayers = Arrays.stream(participants).collect(Collectors.toList());
            } else {
                if (matches.get(round - 1) == null) {
                    return null;
                }
                for (int i = 0; i < matches.get(round - 1).size(); i++) {
                    AdventureEventParticipant w = matches.get(round - 1).get(i).winner;
                    if (w == null)
                        return null;
                    else
                        activePlayers.add(w);
                }
            }
            matches.put(round, new ArrayList<>());
            while (!activePlayers.isEmpty()) {
                AdventureEventMatch match = new AdventureEventMatch();
                match.p1 = activePlayers.remove(MyRandom.getRandom().nextInt(activePlayers.size()));
                if (!activePlayers.isEmpty()) {
                    match.p2 = activePlayers.remove(MyRandom.getRandom().nextInt(activePlayers.size()));
                }
                matches.get(round).add(match);
            }
        }
        return matches.get(currentRound);
    }

    public Map<Integer, List<AdventureEventMatch>> matches = new HashMap<>();

    public void giveRewards() {
        int wins = matchesWon;
        Array<Reward> ret = new Array<>();

        //Todo: this should be automatic... "somehow"
        rewards[3] = new AdventureEventReward();
        rewards[3].minWins = 3;
        rewards[3].maxWins = 3;
        draftedDeck.setName("Drafted Deck");
        draftedDeck.setComment("Prize for placing 1st overall in draft event");
        rewards[3].cardRewards = new Deck[]{draftedDeck};
        //end todo


        for (AdventureEventReward r : rewards) {
            if (r.minWins > wins || r.maxWins < wins) {
                continue;
            }
            for (Deck pack : r.cardRewards) {
                RewardData data = new RewardData();
                data.type = "cardPack";
                data.count = 1;
                data.cardPack = pack;
                ret.addAll(data.generate(false, true));
            }
            for (String item : (r.itemRewards)) {
                RewardData data = new RewardData();
                data.type = "item";
                data.count = 1;
                data.itemName = item;
                ret.addAll(data.generate(false, true));
            }

        }
        if (ret.size > 0) {
            RewardScene.instance().loadRewards(ret, RewardScene.Type.Loot, null);
            Forge.switchScene(RewardScene.instance());
        }

        //todo: more robust logic for event types that can be won without perfect record (Swiss w/cut, round robin)
        playerWon = matchesLost == 0 || matchesWon == rounds;

        eventStatus = AdventureEventController.EventStatus.Awarded;
    }

    public String getPairingDescription() {
        switch (eventRules.pairingStyle) {
            case Swiss:
                return "swiss";
            case SwissWithCut:
                return "swiss (with cut)";
            case RoundRobin:
                return "round robin";
            case SingleElimination:
                return "single elimination";
            case DoubleElimination:
                return "double elimination";
        }
        return "";
    }

    public String getDescription() {
        description = "Event Type: Booster Draft\n";
        description += "Block: " + getCardBlock() + "\n";
        description += "Boosters: " + String.join(", ", packConfiguration) + "\n";
        description += "Competition Style: " + participants.length + " players, matches played as best of " + eventRules.gamesPerMatch + ", " + (getPairingDescription()) + "\n\n";
        description += String.format("Entry Fee (incl. reputation)\nGold %d[][+Gold][BLACK]\nMana Shards %d[][+Shards][BLACK]\n\n", eventRules.goldToEnter, eventRules.shardsToEnter);
        description += String.format("Prizes\nChampion: Keep drafted deck\n2+ round wins: Challenge Coin \n1+ round wins: %s Booster, %s Booster\n0 round wins: %s Booster", rewardPacks[0].getComment(), rewardPacks[1].getComment(), rewardPacks[2].getComment());
        return description;
    }


    public static class AdventureEventParticipant implements Serializable, Comparable<AdventureEventParticipant> {
        private static final long serialVersionUID = 1L;
        private transient EnemySprite sprite;
        String enemyDataName;
        Deck registeredDeck;

        public int wins;
        public int losses;

        public AdventureEventParticipant generate(EnemyData data) {
            AdventureEventParticipant ret = new AdventureEventParticipant();
            ret.enemyDataName = data.getName();
            ret.sprite = new EnemySprite(data);
            return ret;
        }

        public String getRecord() {
            return String.format("%d-%d", wins, losses);
        }

        private AdventureEventParticipant() {
        }

        public Deck getDeck() {
            return registeredDeck;
        }

        public String getName() {
            EnemyData enemyData = WorldData.getEnemy(enemyDataName);
            if (enemyData != null)
                return enemyData.getName();
            return "";
        }

        public Image getAvatar() {
            if (sprite == null) {
                sprite = new EnemySprite(WorldData.getEnemy(enemyDataName));
            }
            return sprite.getAvatar() == null ? new Image() : new Image(sprite.getAvatar());
        }

        public String getAtlasPath() {
            return sprite == null ? "" : sprite.getAtlasPath();
        }

        public EnemySprite getSprite() {
            if (sprite == null) {
                sprite = new EnemySprite(WorldData.getEnemy(enemyDataName));
            }
            return sprite;
        }

        @Override
        public int compareTo(AdventureEventParticipant other) {
            if (this.wins != other.wins)
                return other.wins - this.wins;
            else
                return this.losses - other.losses;
        }

        public void setDeck(Deck deck) {
            registeredDeck = deck;
        }
    }

    public static class AdventureEventHuman extends AdventureEventParticipant {
        @Override
        public Deck getDeck() {
            return registeredDeck == null ? Current.player().getSelectedDeck() : registeredDeck;
        }
        @Override
        public String getName() {
            return Current.player().getName();
        }
        @Override
        public Image getAvatar() {
            return new Image(Current.player().avatar());
        }
    }

    public static class AdventureEventRules implements Serializable {
        private static final long SerialVersionUID = 1L;
        public int goldToEnter;
        public int shardsToEnter;
        public boolean acceptsChallengeCoin;
        public GameType gameType = GameType.AdventureEvent;
        public int startingLife = 20;
        public boolean allowsShards = false;
        public boolean allowsItems = false;
        public boolean allowsBlessings = false;
        public boolean allowsAddBasicLands = true;
        public int gamesPerMatch = 3;
        public PairingStyle pairingStyle = PairingStyle.SingleElimination;
    }

    public static class AdventureEventMatch implements Serializable {
        private static final long SerialVersionUID = 1L;
        public AdventureEventParticipant p1;
        public AdventureEventParticipant p2;
        public AdventureEventParticipant winner;
        public int round;
    }

    public static class AdventureEventReward implements Serializable {
        public int minWins = -1;
        public int maxWins = -1;
        public Deck[] cardRewards = new Deck[0];
        public String[] itemRewards = new String[0];
    }

    enum PairingStyle {
        SingleElimination,
        DoubleElimination,
        Swiss,
        SwissWithCut,
        RoundRobin
    }
}
