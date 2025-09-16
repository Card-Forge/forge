package forge.adventure.data;

import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.Array;
import forge.Forge;
import forge.adventure.character.EnemySprite;
import forge.adventure.pointofintrest.PointOfInterestChanges;
import forge.adventure.scene.RewardScene;
import forge.adventure.util.AdventureEventController;
import forge.adventure.util.Config;
import forge.adventure.util.Current;
import forge.adventure.util.Reward;
import forge.card.CardEdition;
import forge.card.PrintSheet;
import forge.deck.Deck;
import forge.game.GameType;
import forge.gamemodes.limited.BoosterDraft;
import forge.gamemodes.limited.LimitedPlayer;
import forge.gamemodes.limited.LimitedPoolType;
import forge.model.CardBlock;
import forge.model.FModel;
import forge.util.Aggregates;
import forge.util.IterableUtil;
import forge.util.MyRandom;
import forge.util.StreamUtil;
import org.apache.commons.lang3.tuple.Pair;

import java.io.Serializable;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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
    public List<Deck> jumpstartBoosters = new ArrayList<>();
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

    public AdventureEventData(Long seed, AdventureEventController.EventFormat selectedFormat) {
        setEventSeed(seed);
        eventStatus = AdventureEventController.EventStatus.Available;
        registeredDeck = new Deck();
        format = selectedFormat;
        if (format == AdventureEventController.EventFormat.Draft) {
            cardBlock = pickWeightedCardBlock();
            if (cardBlock == null)
                return;
            cardBlockName = cardBlock.getName();

            //Below all to be fully generated in later release
            rewardPacks = getRewardPacks(3);
            generateParticipants(7);
            if (cardBlock != null) {
                packConfiguration = getBoosterConfiguration(cardBlock);

                rewards = new AdventureEventData.AdventureEventReward[4];
                AdventureEventData.AdventureEventReward r0 = new AdventureEventData.AdventureEventReward();
                AdventureEventData.AdventureEventReward r1 = new AdventureEventData.AdventureEventReward();
                AdventureEventData.AdventureEventReward r2 = new AdventureEventData.AdventureEventReward();
                AdventureEventData.AdventureEventReward r3 = new AdventureEventData.AdventureEventReward();
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
        } else if (format == AdventureEventController.EventFormat.Jumpstart) {
            int numPacksToPickFrom = 6;
            generateParticipants(7);

            cardBlock = pickJumpstartCardBlock();
            if (cardBlock == null)
                return;
            cardBlockName = cardBlock.getName();

            jumpstartBoosters = AdventureEventController.instance().getJumpstartBoosters(cardBlock, numPacksToPickFrom);

            packConfiguration = new String[]{cardBlock.getLandSet().getCode(), cardBlock.getLandSet().getCode(), cardBlock.getLandSet().getCode()};

            for (AdventureEventParticipant participant : participants) {
                List<Deck> availableOptions = AdventureEventController.instance().getJumpstartBoosters(cardBlock, numPacksToPickFrom);
                List<Deck> chosenPacks = new ArrayList<>();

                Map<String, List<Deck>> themeMap = new HashMap<>();

                //1. Search for matching themes from deck names, fill deck with them if possible
                for (Deck option : availableOptions) {
                    // This matches up theme for all except DMU - with only 2 per color the next part will handle that
                    String theme = option.getName().replaceAll("\\d$", "").trim();
                    if (!themeMap.containsKey(theme)) {
                        themeMap.put(theme, new ArrayList<>());
                    }
                    themeMap.get(theme).add(option);
                }

                String themeAdded = "";
                boolean done = false;
                while (!done) {
                    for (int i = packConfiguration.length - chosenPacks.size(); i > 1; i--) {
                        if (themeAdded.isEmpty()) {
                            for (String theme : themeMap.keySet()) {
                                if (themeMap.get(theme).size() >= i) {
                                    themeAdded = theme;
                                    break;
                                }
                            }
                        }
                    }
                    if (themeAdded.isEmpty()) {
                        done = true;
                    } else {
                        chosenPacks.addAll(themeMap.get(themeAdded).subList(0, Math.min(themeMap.get(themeAdded).size(), packConfiguration.length - chosenPacks.size())));
                        availableOptions.removeAll(themeMap.get(themeAdded));
                        themeMap.remove(themeAdded);
                        themeAdded = "";
                    }
                }

                //2. Fill remaining slots with colors already picked whenever possible
                Map<String, List<Deck>> colorMap = new HashMap<>();
                for (Deck option : availableOptions) {
                    if (option.getTags().contains("black"))
                        colorMap.computeIfAbsent("black", (k) -> new ArrayList<>()).add(option);
                    if (option.getTags().contains("blue"))
                        colorMap.computeIfAbsent("blue", (k) -> new ArrayList<>()).add(option);
                    if (option.getTags().contains("green"))
                        colorMap.computeIfAbsent("green", (k) -> new ArrayList<>()).add(option);
                    if (option.getTags().contains("red"))
                        colorMap.computeIfAbsent("red", (k) -> new ArrayList<>()).add(option);
                    if (option.getTags().contains("white"))
                        colorMap.computeIfAbsent("white", (k) -> new ArrayList<>()).add(option);
                    if (option.getTags().contains("multicolor"))
                        colorMap.computeIfAbsent("multicolor", (k) -> new ArrayList<>()).add(option);
                    if (option.getTags().contains("colorless"))
                        colorMap.computeIfAbsent("colorless", (k) -> new ArrayList<>()).add(option);
                }

                done = false;
                String colorAdded = "";
                while (!done) {
                    List<String> colorsAlreadyPicked = new ArrayList<>();
                    for (Deck picked : chosenPacks) {
                        if (picked.getTags().contains("black")) colorsAlreadyPicked.add("black");
                        if (picked.getTags().contains("blue")) colorsAlreadyPicked.add("blue");
                        if (picked.getTags().contains("green")) colorsAlreadyPicked.add("green");
                        if (picked.getTags().contains("red")) colorsAlreadyPicked.add("red");
                        if (picked.getTags().contains("white")) colorsAlreadyPicked.add("white");
                        if (picked.getTags().contains("multicolor")) colorsAlreadyPicked.add("multicolor");
                        if (picked.getTags().contains("colorless")) colorsAlreadyPicked.add("colorless");
                    }

                    while (colorAdded.isEmpty() && !colorsAlreadyPicked.isEmpty()) {
                        String colorToTry = Aggregates.removeRandom(colorsAlreadyPicked);
                        for (Deck toCheck : availableOptions) {
                            if (toCheck.getTags().contains(colorToTry)) {
                                colorAdded = colorToTry;
                                chosenPacks.add(toCheck);
                                availableOptions.remove(toCheck);
                                break;
                            }
                        }
                    }
                    //3. If no matching color found and need more packs, add any available at random.
                    if (packConfiguration.length > chosenPacks.size() && colorAdded.isEmpty() && !availableOptions.isEmpty()) {
                        chosenPacks.add(Aggregates.removeRandom(availableOptions));
                        colorAdded = "";
                    } else {
                        done = colorAdded.isEmpty() || packConfiguration.length <= chosenPacks.size();
                        colorAdded = "";
                    }

                }
                participant.registeredDeck = new Deck();
                for (Deck chosen : chosenPacks) {
                    participant.registeredDeck.getMain().addAllFlat(chosen.getMain().toFlatList());
                }
            }

            rewards = new AdventureEventData.AdventureEventReward[4];
            AdventureEventData.AdventureEventReward r0 = new AdventureEventData.AdventureEventReward();
            AdventureEventData.AdventureEventReward r1 = new AdventureEventData.AdventureEventReward();
            AdventureEventData.AdventureEventReward r2 = new AdventureEventData.AdventureEventReward();
            AdventureEventData.AdventureEventReward r3 = new AdventureEventData.AdventureEventReward();

            RewardData r0gold = new RewardData();
            r0gold.count = 100;
            r0gold.type = "gold";
            r0.rewards = new RewardData[]{r0gold};
            r0.minWins = 1;
            r0.maxWins = 1;
            rewards[0] = r0;
            RewardData r1gold = new RewardData();
            r1gold.count = 200;
            r1gold.type = "gold";
            r1.rewards = new RewardData[]{r1gold};
            r1.minWins = 2;
            r1.maxWins = 2;
            rewards[1] = r1;
            r2.minWins = 3;
            r2.maxWins = 3;
            RewardData r2gold = new RewardData();
            r2gold.count = 500;
            r2gold.type = "gold";
            r2.rewards = new RewardData[]{r2gold};
            rewards[2] = r2;
            r3.minWins = 0;
            r3.maxWins = 3;
            rewards[3] = r3;
            //r3 will be the selected card packs
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
        if (format != AdventureEventController.EventFormat.Draft)
            return null;

        Random placeholder = MyRandom.getRandom();
        MyRandom.setRandom(getEventRandom());
        if (draft == null && (eventStatus == AdventureEventController.EventStatus.Available || eventStatus == AdventureEventController.EventStatus.Entered)) {
            draft = BoosterDraft.createDraft(LimitedPoolType.Block, getCardBlock(), packConfiguration, 8);
            registeredDeck = draft.getHumanPlayer().getDeck();
            assignPlayerNames(draft);
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
        if (rollD100 < 30) {
            rolledFilter = filterStandard;
        } else if (rollD100 < 60) {
            rolledFilter = filterPioneer;
        } else if (rollD100 < 80) {
            rolledFilter = filterModern;
        } else {
            rolledFilter = filterVintage;
        }
        return rolledFilter;
    }


    private CardBlock pickWeightedCardBlock() {
        CardEdition.Collection editions = FModel.getMagicDb().getEditions();
        ConfigData configData = Config.instance().getConfigData();
        Iterable<CardBlock> src = FModel.getBlocks(); //all blocks
        Predicate<CardEdition> filter = CardEdition.Predicates.CAN_MAKE_BOOSTER.and(selectSetPool());

        if(configData.restrictedEvents != null) {
            //Temporary restriction until rewards are more diverse - don't want to award restricted cards so these editions need different rewards added.
            //Also includes sets that use conspiracy or commander drafts.
            Set<String> restrictedEvents = Set.of(configData.restrictedEvents);
            filter = filter.and((q) -> !restrictedEvents.contains(q.getCode()));
        }
        if (configData.allowedEditions != null) {
            Set<String> allowed = Set.of(configData.allowedEditions);
            filter = filter.and(q -> allowed.contains(q.getCode()));
        } else {
            List<String> restrictedList = Arrays.asList(configData.restrictedEditions);
            Set<String> restricted = new HashSet<>(restrictedList); //Would use Set.of but that throws an error if there's any duplicates, and users edit these lists all the time.
            filter = filter.and(q -> !restricted.contains(q.getCode()));
        }

        List<CardEdition> allEditions = new ArrayList<>();
        StreamUtil.stream(editions)
                .filter(filter)
                .filter(CardEdition::hasBoosterTemplate)
                .forEach(allEditions::add);

        List<CardBlock> legalBlocks = new ArrayList<>();
        for (CardBlock b : src) { // for each block
            if (b.getSets().isEmpty() || (b.getCntBoostersDraft() < 1))
                continue;
            boolean isOkay = true;
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
                    isOkay = boosterSize > 11;
                }
                for (PrintSheet ps : c.getPrintSheetsBySection()) {
                    //exclude block with sets containing P9 cards..
                    if (ps.containsCardNamed("Black Lotus", 1)
                                || ps.containsCardNamed("Mox Emerald", 1)
                                || ps.containsCardNamed("Mox Pearl", 1)
                                || ps.containsCardNamed("Mox Ruby", 1)
                                || ps.containsCardNamed("Mox Sapphire", 1)
                                || ps.containsCardNamed("Mox Jet", 1)
                                || ps.containsCardNamed("Ancestral Recall", 1)
                                || ps.containsCardNamed("Timetwister", 1)
                                || ps.containsCardNamed("Time Walk", 1)) {
                            isOkay = false;
                        break;
                    }
                }
            }
            if (isOkay) {
                legalBlocks.add(b);
            }
        }

        return legalBlocks.isEmpty() ? null : Aggregates.random(legalBlocks);
    }

    private CardBlock pickJumpstartCardBlock() {
        Iterable<CardBlock> src = FModel.getBlocks(); //all blocks
        List<CardBlock> legalBlocks = new ArrayList<>();
        ConfigData configData = Config.instance().getConfigData();
        if (configData.allowedJumpstart != null) {
            Set<String> allowed = Set.of(configData.allowedJumpstart);
            for (CardBlock b : src) { // for each block
                if (allowed.contains(b.getName())) {
                    legalBlocks.add(b);
                }
            }
            for (CardBlock b : FModel.getFantasyBlocks()) {
                if (allowed.contains(b.getName())) {
                    legalBlocks.add(b);
                }
            }
        }
        else {
            for (CardBlock b : src) { // for each block
                //I hate doing this, but it seems like the simplest way to reliably filter out prereleases
                if (b.getName().toUpperCase().contains("JUMPSTART")) {
                    legalBlocks.add(b);
                }
            }
            if (configData.allowedEditions != null) {
                Set<String> allowed = Set.of(configData.allowedEditions);
                legalBlocks.removeIf(q -> !allowed.contains(q.getName()));
            } else {
                for (String restricted : configData.restrictedEditions) {
                    legalBlocks.removeIf(q -> q.getName().equals(restricted));
                }
            }
        }
        return legalBlocks.isEmpty() ? null : Aggregates.random(legalBlocks);
    }


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

        Iterable<EnemyData> validParticipants = IterableUtil.filter(WorldData.getAllEnemies(), q -> q.nextEnemy == null);
        List<EnemyData> data = Aggregates.random(validParticipants, numberOfOpponents);
        for (int i = 0; i < numberOfOpponents; i++) {
            participants[i] = new AdventureEventParticipant().generate(data.get(i));
        }

        participants[numberOfOpponents] = getHumanPlayer();
    }

    private void assignPlayerNames(BoosterDraft draft) {
        if(participants == null)
            return;
        draft.getHumanPlayer().setName(getHumanPlayer().getName());
        LimitedPlayer[] opponents = draft.getOpposingPlayers();
        for(int i = 0; i < Math.min(participants.length, opponents.length); i++)
            opponents[i].setName(participants[i].getName());
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

        if (format == AdventureEventController.EventFormat.Draft) {

            rewards[3] = new AdventureEventReward();
            rewards[3].minWins = 3;
            rewards[3].maxWins = 3;
            draftedDeck.setName("Drafted Deck");
            draftedDeck.setComment("Prize for placing 1st overall in draft event");
            rewards[3].cardRewards = new Deck[]{draftedDeck};

        } else if (format == AdventureEventController.EventFormat.Jumpstart) {

            rewards[3] = new AdventureEventReward();
            rewards[3].minWins = 0;
            rewards[3].maxWins = 3;
            registeredDeck.setName("Jumpstart Event Packs");
            rewards[3].cardRewards = new Deck[]{registeredDeck};
            rewards[3].isNoSell = true;

        }

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
                ret.addAll(data.generate(false, true, r.isNoSell));
            }
            for (String item : (r.itemRewards)) {
                RewardData data = new RewardData();
                data.type = "item";
                data.count = 1;
                data.itemName = item;
                ret.addAll(data.generate(false, true));
            }
            for (RewardData data : r.rewards) {
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

    public String getDescription(PointOfInterestChanges changes) {
        float townPriceModifier = changes == null ? 1f : changes.getTownPriceModifier();
        if (format.equals(AdventureEventController.EventFormat.Draft)) {
            description = "Event Type: Booster Draft\n";
            description += "Block: " + getCardBlock() + "\n";
            description += "Boosters: " + String.join(", ", packConfiguration) + "\n";
            description += "Competition Style: " + participants.length + " players, matches played as best of " + eventRules.gamesPerMatch + ", " + (getPairingDescription()) + "\n\n";

            if (eventStatus == AdventureEventController.EventStatus.Available) {
                description += String.format("Pay 1 Entry Fee\n- Gold %d[][+Gold][BLACK]\n- Mana Shards %d[][+Shards][BLACK]\n", Math.round(eventRules.goldToEnter * townPriceModifier), Math.round(eventRules.shardsToEnter * townPriceModifier));
                if (eventRules.acceptsBronzeChallengeCoin) {
                    description += "- Bronze Challenge Coin [][+BronzeChallengeCoin][BLACK]\n\n";
                } else if (eventRules.acceptsSilverChallengeCoin) {
                    description += "- Silver Challenge Coin [][+SilverChallengeCoin][BLACK]\n\n";
                } else if (eventRules.acceptsChallengeCoin) {
                    description += "- Gold Challenge Coin [][+ChallengeCoin][BLACK]\n\n";
                } else {
                    description += "\n";
                }
            }
            description += String.format("Prizes\nChampion: Keep drafted deck\n2+ round wins: Challenge Coin \n1+ round wins: %s Booster, %s Booster\n0 round wins: %s Booster", rewardPacks[0].getComment(), rewardPacks[1].getComment(), rewardPacks[2].getComment());
        } else if (format.equals(AdventureEventController.EventFormat.Jumpstart)) {
            description = "Event Type: Jumpstart\n";
            description += "Block: " + getCardBlock() + "\n";
            description += "Competition Style: " + participants.length + " players, matches played as best of " + eventRules.gamesPerMatch + ", " + (getPairingDescription()) + "\n\n";
            description += String.format("Pay 1 Entry Fee\n- Gold %d[][+Gold][BLACK]\n- Mana Shards %d[][+Shards][BLACK]\n", Math.round(eventRules.goldToEnter * townPriceModifier), Math.round(eventRules.shardsToEnter * townPriceModifier));
            if (eventRules.acceptsBronzeChallengeCoin) {
                description += "- Bronze Challenge Coin [][+BronzeChallengeCoin][BLACK]\n\n";
            } else if (eventRules.acceptsSilverChallengeCoin) {
                description += "- Silver Challenge Coin [][+SilverChallengeCoin][BLACK]\n\n";
            } else if (eventRules.acceptsChallengeCoin) {
                description += "- Gold Challenge Coin [][+ChallengeCoin][BLACK]\n\n";
            } else {
                description += "\n";
            }
            description += "Prizes\n3 round wins: 500 gold\n2 round wins: 200 gold\n1 round win: 100 gold\n";
            description += "Participating in this event will award a valueless copy of each card in your Jumpstart deck.";
        }
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
                EnemyData data = WorldData.getEnemy(enemyDataName);
                if (data == null) {
                    //enemyDataName was not found, replace with something valid.
                    enemyDataName = Aggregates.random(WorldData.getAllEnemies()).getName();
                }
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
        private static final long serialVersionUID = -2902188278147984885L;
        public int goldToEnter;
        public int shardsToEnter;
        public boolean acceptsChallengeCoin = false;
        public boolean acceptsSilverChallengeCoin = false;
        public boolean acceptsBronzeChallengeCoin = false;
        public GameType gameType = GameType.AdventureEvent;
        public int startingLife = 20;
        public boolean allowsShards = false;
        public boolean allowsItems = false;
        public boolean allowsBlessings = false;
        public boolean allowsAddBasicLands = true;
        public int gamesPerMatch = 3;
        public PairingStyle pairingStyle = PairingStyle.SingleElimination;

        public AdventureEventRules() {
            this(AdventureEventController.EventFormat.Constructed, PairingStyle.SingleElimination, 1.0f);
        }

        public AdventureEventRules(AdventureEventController.EventFormat format, float localPriceModifier) {
            this(format, PairingStyle.SingleElimination, localPriceModifier);
        }

        public AdventureEventRules(AdventureEventController.EventFormat format, PairingStyle pairingStyle, float localPriceModifier) {
            int baseGoldEntry = 99999;
            int baseShardEntry = 9999;
            this.pairingStyle = pairingStyle;

            switch (format) {
                case Constructed:
                    acceptsSilverChallengeCoin = true;
                    acceptsChallengeCoin = false;
                    acceptsBronzeChallengeCoin = false;
                    baseGoldEntry = 1500;
                    baseShardEntry = 25;
                    allowsAddBasicLands = false;
                    break;
                case Draft:
                    acceptsChallengeCoin = true;
                    acceptsSilverChallengeCoin = false;
                    acceptsBronzeChallengeCoin = false;
                    baseGoldEntry = 3000;
                    baseShardEntry = 50;
                    startingLife = 20;
                    allowsAddBasicLands = true;
                    break;
                case Jumpstart:
                    acceptsChallengeCoin = false;
                    acceptsSilverChallengeCoin = false;
                    acceptsBronzeChallengeCoin = true;
                    baseGoldEntry = 200;
                    baseShardEntry = 5;
                    startingLife = 15;
                    allowsAddBasicLands = false;
                    break;
            }
            goldToEnter = baseGoldEntry;
            shardsToEnter = baseShardEntry;
        }
    }

    public static class AdventureEventMatch implements Serializable {
        private static final long serialVersionUID = 1L;
        public AdventureEventParticipant p1;
        public AdventureEventParticipant p2;
        public AdventureEventParticipant winner;
        public int round;
    }

    public static class AdventureEventReward implements Serializable {
        private final static long serialVersionUID = -2605375040895115477L;
        public int minWins = -1;
        public int maxWins = -1;
        public Deck[] cardRewards = new Deck[0];
        public String[] itemRewards = new String[0];
        public RewardData[] rewards = new RewardData[0];
        public boolean isNoSell = false;
    }

    enum PairingStyle {
        SingleElimination,
        DoubleElimination,
        Swiss,
        SwissWithCut,
        RoundRobin
    }
}
