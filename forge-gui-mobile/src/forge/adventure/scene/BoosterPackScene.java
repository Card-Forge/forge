package forge.adventure.scene;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.github.tommyettinger.textra.TextraButton;
import com.github.tommyettinger.textra.TextraLabel;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import forge.Forge;
import forge.StaticData;
import forge.adventure.data.ConfigData;
import forge.adventure.data.RewardData;
import forge.adventure.player.AdventurePlayer;
import forge.adventure.util.*;
import forge.card.CardEdition;
import forge.card.ColorSet;
import forge.deck.Deck;
import forge.item.PaperCard;
import forge.model.CardBlock;
import forge.model.FModel;
import forge.util.Aggregates;
import forge.util.MyRandom;
import forge.card.CardEdition.CardInSet;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;


public class BoosterPackScene extends UIScene {

    private static BoosterPackScene object;

    // Overloaded method without PointOfInterestChanges
    public static BoosterPackScene instance() {
        if (object == null)
            object = new BoosterPackScene();
        return object;
    }

    private List<PaperCard> cardPool = new ArrayList<>();
    private TextraLabel playerGold, playerShards, poolSize;
    private final TextraButton pullUsingGold, pullUsingShards, acceptReward, declineReward, exitSmith;
    private final ScrollPane rewardDummy;
    private RewardActor rewardActor;
    SelectBox<CardEdition> editionList;
    //Button containers.
    final private HashMap<String, TextraButton> rarityButtons = new HashMap<>();
    final private HashMap<String, TextraButton> costButtons = new HashMap<>();
    final private HashMap<String, TextraButton> colorButtons = new HashMap<>();

    //Filter variables.
    private String edition = "";
    private String rarity = "";
    private int cost_low = -1;
    private int cost_high = 9999;
    //Other
    private final float basePrice = 0f;
    private int currentPrice = 0;
    private int currentShardPrice = 0;
    private List<CardEdition> editions = null;
    private Reward currentReward = null;

    private BoosterPackScene() {
        super(Forge.isLandscapeMode() ? "ui/boosterpack.json" : "ui/boosterpack_portrait.json");

        editionList = ui.findActor("BSelectPlane");
        rewardDummy = ui.findActor("RewardDummy");
        rewardDummy.setVisible(false);

        pullUsingGold = ui.findActor("pullUsingGold");
        pullUsingGold.setDisabled(true);

        pullUsingShards = ui.findActor("pullUsingShards");
        pullUsingShards.setDisabled(true);

        exitSmith = ui.findActor("done");

        acceptReward = ui.findActor("accept");
        acceptReward.setVisible(false);
        declineReward = ui.findActor("decline");
        declineReward.setVisible(false);

        playerGold = Controls.newAccountingLabel(ui.findActor("playerGold"), false);
        playerShards = Controls.newAccountingLabel(ui.findActor("playerShards"), true);
        poolSize = ui.findActor("poolSize");
        for (String i : new String[]{"BBlack", "BBlue", "BGreen", "BRed", "BWhite", "BColorless"}) {
            TextraButton button = ui.findActor(i);
            if (button != null) {
                colorButtons.put(i, button);
                button.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        selectColor(i);
                        filterResults();
                    }
                });
            }
        }

        ui.onButtonPress("done", BoosterPackScene.this::done);
        ui.onButtonPress("pullUsingGold", () -> BoosterPackScene.this.pullPack(false));
        ui.onButtonPress("pullUsingShards", () -> BoosterPackScene.this.pullPack(true));
        ui.onButtonPress("BReset", () -> {
            reset();
            filterResults();
        });
    }

    private void reset() {
        edition = "";
        cost_low = -1;
        cost_high = 9999;
        rarity = "";
        currentPrice = 0;
        for (Map.Entry<String, TextraButton> B : colorButtons.entrySet()) B.getValue().setColor(Color.WHITE);
        for (Map.Entry<String, TextraButton> B : costButtons.entrySet()) B.getValue().setColor(Color.WHITE);
        for (Map.Entry<String, TextraButton> B : rarityButtons.entrySet()) B.getValue().setColor(Color.WHITE);
        editionList.setColor(Color.WHITE);
        editionList.setUserObject(edition);
    }

    public void loadEditions() {
        if (editions != null)
            return;
        Predicate<CardEdition> filter = Predicates.and(CardEdition.Predicates.CAN_MAKE_BOOSTER, Predicates.notNull());
        Iterable<CardEdition> possibleEditions = Iterables.filter(FModel.getMagicDb().getEditions(), filter);
        List<CardEdition> possibleEditionsList = Lists.newArrayList(possibleEditions);
        System.out.println("Editions after CAN_MAKE_BOOSTER and notNull filter: " + possibleEditionsList.size());
        editions = StaticData.instance().getSortedEditions().stream().filter(input -> {
            if (input == null)
                return false;
            if (input.getType() != CardEdition.Type.CORE && input.getType() != CardEdition.Type.EXPANSION && input.getType() != CardEdition.Type.BOXED_SET && input.getType() != CardEdition.Type.DRAFT)
                return false;
            if (input.getDate() != null) {
                Instant now = Instant.now(); //this should filter upcoming sets from release date + 1 day..
                if (input.getDate().after(Date.from(now.minus(1, ChronoUnit.DAYS))))
                    return false;
            }
            List<PaperCard> it = StreamSupport.stream(RewardData.getAllCards().spliterator(), false)
                    .filter(input2 -> input2.getEdition().equals(input.getCode())).collect(Collectors.toList());
            if (it.isEmpty())
                return false;
            ConfigData configData = Config.instance().getConfigData();
            if (configData.allowedEditions != null)
                return Arrays.asList(configData.allowedEditions).contains(input.getCode());
            return (!Arrays.asList(configData.restrictedEditions).contains(input.getCode()));
        }).sorted(Comparator.comparing(CardEdition::getName)).collect(Collectors.toList());

        // Log the number of editions after all filtering
        System.out.println("Editions after all filters: " + editions.size());

        editionList.setUserObject(editions.get(0).getCode());
        String selectedEditionCode = editions.get(0).getCode();
        System.out.println("User object set to edition code: " + selectedEditionCode);
    }

    public void setSelectedByEditionCode(String editionCode) {
        // Find the CardEdition that matches the given edition code
        for (CardEdition edition : editionList.getItems()) {
            if (edition.getCode().equals(editionCode)) {
                // Set the matching edition as the selected item in the SelectBox
                editionList.setSelected(edition);
                return;
            }
        }
        System.out.println("Edition with code " + editionCode + " not found.");
    }

    public boolean done() {
        if (rewardActor != null) rewardActor.remove();
        cardPool.clear(); //Get rid of cardPool, filtering is fast enough to justify keeping it cached.
        Forge.switchToLast();
        return true;
    }

    private void selectColor(String what) {
        TextraButton B = colorButtons.get(what);
        switch (what) {
            case "BColorless":
                if (B.getColor().equals(Color.RED)) B.setColor(Color.WHITE);
                else {
                    for (Map.Entry<String, TextraButton> BT : colorButtons.entrySet())
                        BT.getValue().setColor(Color.WHITE);
                    B.setColor(Color.RED);
                }
                break;
            case "BBlack":
            case "BBlue":
            case "BGreen":
            case "BRed":
            case "BWhite":
                if (B.getColor().equals(Color.RED)) B.setColor(Color.WHITE);
                else B.setColor(Color.RED);
                break;

        }
    }

    private CardEdition previousEdition;  // Store the previously selected edition

    @Override
    public void enter() {
        // Only reload editions if there is no previous selection or no editions have been loaded yet
        if (previousEdition == null || editions == null) {
            loadEditions();  // Reload editions only if necessary
        }

        editionList.clearListeners();
        editionList.clearItems();
        editionList.setItems(editions.toArray(new CardEdition[0]));  // Populate the SelectBox

        // Restore the previous selection if available
        if (previousEdition != null) {
            editionList.setSelected(previousEdition);  // Restore the previous selection in the SelectBox
            edition = previousEdition.getCode();  // Set the edition code based on the restored selection
            System.out.println("Restored previously selected edition: " + edition);
        }

        // Add listeners for the SelectBox
        editionList.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                CardEdition E = editionList.getSelected();  // Get the selected edition
                edition = E.getCode();  // Store the code of the selected edition
                previousEdition = E;  // Save the current selection for future re-entries
                filterResults();  // Apply filters based on the selected edition
            }
        });

        // Listener to show the scroll pane when clicked
        editionList.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                editionList.showScrollPane();  // Show scrollable list of editions
            }
        });

        // Reset the color to white on re-entry (optional, depending on your UI needs)
        editionList.setColor(Color.WHITE);

        // Filter results based on the selected edition or any other filters
        filterResults();

        super.enter();  // Call the superclass' enter method
    }

    public void filterResults() {
        Iterable<PaperCard> P = RewardData.getAllCards();
        float totalCost = basePrice * Current.player().goldModifier();
        final List<String> colorFilter = new ArrayList<>();
        for (Map.Entry<String, TextraButton> B : colorButtons.entrySet())
            switch (B.getKey()) {
                case "BColorless":
                    if (B.getValue().getColor().equals(Color.RED)) colorFilter.add("Colorless");
                    continue;
                case "BBlack":
                    if (B.getValue().getColor().equals(Color.RED)) colorFilter.add("Black");
                    break;
                case "BBlue":
                    if (B.getValue().getColor().equals(Color.RED)) colorFilter.add("Blue");
                    break;
                case "BGreen":
                    if (B.getValue().getColor().equals(Color.RED)) colorFilter.add("Green");
                    break;
                case "BRed":
                    if (B.getValue().getColor().equals(Color.RED)) colorFilter.add("Red");
                    break;
                case "BWhite":
                    if (B.getValue().getColor().equals(Color.RED)) colorFilter.add("White");
                    break;
            }
        P = StreamSupport.stream(P.spliterator(), false).filter(input -> {
            //L|Basic Land, C|Common, U|Uncommon, R|Rare, M|Mythic Rare, S|Special, N|None
            if (input == null) return false;
            final CardEdition cardEdition = FModel.getMagicDb().getEditions().get(edition);

            if (cardEdition != null && cardEdition.getCardInSet(input.getName()).size() == 0) return false;
            if (colorFilter.size() > 0)
                if (input.getRules().getColor() != ColorSet.fromNames(colorFilter)) return false;
            if (!rarity.isEmpty()) if (!input.getRarity().toString().equals(rarity)) return false;
            return true;
        }).collect(Collectors.toList());
        //Stream method is very fast, might not be necessary to precache anything.
        if (colorFilter.size() > 0)
            totalCost *= Math.min(colorFilter.size() * 2.0f, 4.0f); //Color filter cost multiplier.

        cardPool = StreamSupport.stream(P.spliterator(), false).collect(Collectors.toList());
        poolSize.setText(((cardPool.size() > 0 ? "[/][FOREST]" : "[/][RED]")) + cardPool.size() + " possible card" + (cardPool.size() != 1 ? "s" : ""));
        currentPrice = (int) totalCost;
        currentShardPrice = (int) (totalCost * 0.1f); //Intentionally rounding up via the cast to int
        pullUsingGold.setText("[+Pull][+goldcoin] "+ currentPrice);
        pullUsingShards.setText("[+Pull][+shards]" + currentShardPrice);
        pullUsingGold.setDisabled(!(cardPool.size() > 0) || Current.player().getGold() < totalCost || edition.isEmpty());
        pullUsingShards.setDisabled(!(cardPool.size() > 0) || Current.player().getShards() < currentShardPrice || edition.isEmpty());
        editionList.setUserObject(edition);
    }

    public void pullPack(boolean usingShards) {
        Array<Reward> rewardPack = new Array<>();
        int p = rewardPack.size;
        // Separate card pool by rarity and type
        List<PaperCard> allCards = new ArrayList<>(cardPool);
        List<PaperCard> foilCards = new ArrayList<>();
        List<PaperCard> basicLands = new ArrayList<>();
        List<PaperCard> commonCards = new ArrayList<>();
        List<PaperCard> uncommonCards = new ArrayList<>();
        List<PaperCard> rareCards = new ArrayList<>();
        List<PaperCard> mythicRareCards = new ArrayList<>();

        // Log the quantities
        System.out.println("Card quantities in " + edition + ":");
        System.out.println("All cards: " + allCards.size());

        for (PaperCard card : allCards) {
            switch (card.getRarity()) {
                case Common:
                    commonCards.add(card);
                    break;
                case Uncommon:
                    uncommonCards.add(card);
                    break;
                case Rare:
                    rareCards.add(card);
                    break;
                case MythicRare:
                    mythicRareCards.add(card);
                    break;
                default:
                    break;
            }
            if (card.isFoil()) {
                foilCards.add(card);
            }
            if (card.isVeryBasicLand()) {
                basicLands.add(card);
            }
        }

        // Pull 1 basic land
        if (!basicLands.isEmpty()) {
            PaperCard P = basicLands.get(MyRandom.getRandom().nextInt(basicLands.size()));
            Reward reward = createReward(P);
            rewardPack.add(reward);
        } else {
            PaperCard P = allCards.get(MyRandom.getRandom().nextInt(allCards.size()));
            Reward reward = createReward(P);
            rewardPack.add(reward);
        }

        // Pull 10 commons
        for (int i = 0; i < 10 && !commonCards.isEmpty(); i++) {
            PaperCard P = commonCards.get(MyRandom.getRandom().nextInt(commonCards.size()));
            Reward reward = createReward(P);
            rewardPack.add(reward);
        }

        // Pull 2 uncommons
        for (int i = 0; i < 2 && !uncommonCards.isEmpty(); i++) {
            PaperCard P = uncommonCards.get(MyRandom.getRandom().nextInt(uncommonCards.size()));
            Reward reward = createReward(P);
            rewardPack.add(reward);
        }

        // Pull 1 uncommon or any
        boolean hasFoil = MyRandom.getRandom().nextInt(1) == 0;  // 1 in 3 chance for a foil card
        PaperCard foilCard = null;

        if (hasFoil && !foilCards.isEmpty()) {
            // Determine rarity of the foil card
            int foilRarityRoll = MyRandom.getRandom().nextInt(100);
            if (foilRarityRoll < 70 && !commonCards.isEmpty()) {
                foilCard = commonCards.get(MyRandom.getRandom().nextInt(commonCards.size()));
            } else if (foilRarityRoll < 90 && !uncommonCards.isEmpty()) {
                foilCard = uncommonCards.get(MyRandom.getRandom().nextInt(uncommonCards.size()));
            } else if (foilRarityRoll < 97 && !rareCards.isEmpty()) {
                foilCard = rareCards.get(MyRandom.getRandom().nextInt(rareCards.size()));
            } else if (!mythicRareCards.isEmpty()) {
                foilCard = mythicRareCards.get(MyRandom.getRandom().nextInt(mythicRareCards.size()));
            }
            // Add the foil card to the pack
            if (foilCard != null) {
                Reward foilReward = createReward(foilCard);
                rewardPack.add(foilReward);
            }
        } else if (!hasFoil && !uncommonCards.isEmpty()) {
            PaperCard P = uncommonCards.get(MyRandom.getRandom().nextInt(uncommonCards.size()));
            Reward reward = createReward(P);
            rewardPack.add(reward);
        }

        // Pull 1 rare with a 1/8th chance of being a mythic rare
        if (!rareCards.isEmpty()) {
            // 1/8th chance of pulling a mythic rare
            if (MyRandom.getRandom().nextInt(8) == 0 && !mythicRareCards.isEmpty()) {
                PaperCard P = mythicRareCards.get(MyRandom.getRandom().nextInt(mythicRareCards.size()));
                Reward reward = createReward(P);
                rewardPack.add(reward);
            } else {
                PaperCard P = rareCards.get(MyRandom.getRandom().nextInt(rareCards.size()));
                Reward reward = createReward(P);
                rewardPack.add(reward);
            }
        }

        // Display the rewards using the reward scene
        showRewardScene(rewardPack);
        if (usingShards) {
            Current.player().takeShards(currentShardPrice);
        } else {
            Current.player().takeGold(currentPrice);
        }
//        // Below all to be fully generated in later release
//        // Generate reward packs
//
//        System.out.println("Generating reward packs...");
//        Deck[] rewardPacks;
//        rewardPacks = getRewardPacks(1);
//        System.out.println("Reward packs generated: " + rewardPacks.length);
//
//        Deck[] cardRewards = new Deck[0];
//        // Assign a new deck to one of the elements in the array
//        // Ensure you use a valid index, such as 0, 1, or 2
//        cardRewards = new Deck[]{rewardPacks[0]};
//        System.out.println("Card rewards assigned: " + cardRewards.length);
//
//        Array<Reward> ret = new Array<>();
//        System.out.println("Initialized reward array.");
//
//        for (Deck pack : cardRewards) {
//            RewardData data = new RewardData();
//            data.type = "cardPack";
//            data.count = 1;
//            data.cardPack = pack;
//            System.out.println("Creating reward data for pack: " + pack.getName());
//            ret.addAll(data.generate(false, true));
//            System.out.println("Reward data generated and added to the array.");
//        }
//        System.out.println("Loading rewards into the RewardScene.");
//        RewardScene.instance().loadRewards(ret, RewardScene.Type.Loot, null);
//        Forge.switchScene(RewardScene.instance());

        // Clear the current reward and update the UI
        //clearReward();
        //updatePullButtons();
    }

    public Deck[] getRewardPacks(int count) {
        Deck[] ret = new Deck[count];
        for (int i = 0; i < count; i++) {
            ret[i] = AdventureEventController.instance().generateBooster(edition);
        }
        return ret;
    }

    private Reward createReward(PaperCard P) {
        if (Config.instance().getSettingData().useAllCardVariants) {
            if (!edition.isEmpty()) {
                return new Reward(CardUtil.getCardByNameAndEdition(P.getCardName(), edition));
            } else {
                return new Reward(CardUtil.getCardByName(P.getCardName()));
            }
        } else {
            return new Reward(P);
        }
    }

    private void showRewardScene(Array<Reward> rewards) {
        RewardScene.instance().loadRewards(rewards, RewardScene.Type.Loot, null);
        Forge.switchScene(RewardScene.instance());
    }


    private void clearReward() {
        if (rewardActor != null) rewardActor.remove();
        currentReward = null;
    }

    private void updatePullButtons() {
        String selectedEdition = editionList.getSelected().getCode();
        pullUsingGold.setDisabled(Current.player().getGold() < currentPrice || selectedEdition.isEmpty());
        pullUsingShards.setDisabled(Current.player().getShards() < currentShardPrice || selectedEdition.isEmpty());

        acceptReward.setVisible(false);
        declineReward.setVisible(false);

        exitSmith.setDisabled(false);
    }

    private void disablePullButtons() {
        pullUsingGold.setDisabled(true);
        pullUsingShards.setDisabled(true);
    }
}
