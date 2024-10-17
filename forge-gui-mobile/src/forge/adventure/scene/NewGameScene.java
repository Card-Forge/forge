package forge.adventure.scene;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import com.github.tommyettinger.textra.TextraLabel;
import forge.Forge;
import forge.adventure.data.DialogData;
import forge.adventure.data.DifficultyData;
import forge.adventure.data.HeroListData;
import forge.adventure.player.AdventurePlayer;
import forge.adventure.stage.WorldStage;
import forge.adventure.util.*;
import forge.adventure.world.WorldSave;
import forge.card.CardEdition;
import forge.card.ColorSet;
import forge.deck.DeckProxy;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;
import forge.player.GamePlayerUtil;
import forge.screens.TransitionScreen;
import forge.sound.SoundSystem;
import forge.util.NameGenerator;

import java.util.Random;

/**
 * NewGame scene that contains the character creation
 */
public class NewGameScene extends MenuScene {
    TextField selectedName;
    ColorSet[] colorIds;
    CardEdition[] editionIds;
    private final Image avatarImage;
    private int avatarIndex = 0;
    private final Selector race;
    private final Selector colorId;
    private final Selector gender;
    private final Selector mode;
    private final Selector difficulty;
    private final Selector starterEdition;
    private final TextraLabel starterEditionLabel;
    private final Array<String> custom;
    private final TextraLabel colorLabel;
    private final ImageButton difficultyHelp;
    private DialogData difficultySummary;
    private final ImageButton modeHelp;
    private DialogData modeSummary;
    private final Random rand = new Random();

    private final Array<AdventureModes> modes = new Array<>();

    private NewGameScene() {

        super(Forge.isLandscapeMode() ? "ui/new_game.json" : "ui/new_game_portrait.json");
        gender = ui.findActor("gender");
        selectedName = ui.findActor("nameField");
        generateName();
        avatarImage = ui.findActor("avatarPreview");
        mode = ui.findActor("mode");
        modeHelp = ui.findActor("modeHelp");
        colorLabel = ui.findActor("colorIdL");
        String colorIdLabel = colorLabel.storedText;
        custom = new Array<>();
        colorId = ui.findActor("colorId");
        String[] colorSet = Config.instance().colorIds();
        String[] colorIdNames = Config.instance().colorIdNames();
        colorIds = new ColorSet[colorSet.length];
        for (int i = 0; i < colorIds.length; i++)
            colorIds[i] = ColorSet.fromNames(colorSet[i].toCharArray());
        Array<String> colorNames = new Array<>(colorIds.length);
        for (String idName : colorIdNames)
            colorNames.add(UIActor.localize(idName));
        colorId.setTextList(colorNames);

        for (DifficultyData diff : Config.instance().getConfigData().difficulties)//check first difficulty if exists
        {
            if (diff.starterDecks != null) {
                modes.add(AdventureModes.Standard);
                AdventureModes.Standard.setSelectionName(colorIdLabel);
                AdventureModes.Standard.setModes(colorNames);
            }

            if (diff.constructedStarterDecks != null) {
                modes.add(AdventureModes.Constructed);
                AdventureModes.Constructed.setSelectionName(colorIdLabel);
                AdventureModes.Constructed.setModes(colorNames);
            }
            if (diff.pileDecks != null) {
                modes.add(AdventureModes.Pile);
                AdventureModes.Pile.setSelectionName(colorIdLabel);
                AdventureModes.Pile.setModes(colorNames);
            }
            break;
        }

        starterEdition = ui.findActor("starterEdition");
        starterEditionLabel = ui.findActor("starterEditionL");
        String[] starterEditions = Config.instance().starterEditions();
        String[] starterEditionNames = Config.instance().starterEditionNames();
        editionIds = new CardEdition[starterEditions.length];
        for (int i = 0; i < editionIds.length; i++)
            editionIds[i] = FModel.getMagicDb().getEditions().get(starterEditions[i]);
        Array<String> editionNames = new Array<>(editionIds.length);
        for (String editionName : starterEditionNames)
            editionNames.add(UIActor.localize(editionName));
        starterEdition.setTextList(editionNames);

        modes.add(AdventureModes.Chaos);
        AdventureModes.Chaos.setSelectionName("[BLACK]" + Forge.getLocalizer().getMessage("lblDeck") + ":");
        AdventureModes.Chaos.setModes(new Array<>(new String[]{Forge.getLocalizer().getMessage("lblRandomDeck")}));
        for (DeckProxy deckProxy : DeckProxy.getAllCustomStarterDecks())
            custom.add(deckProxy.getName());
        if (!custom.isEmpty()) {
            modes.add(AdventureModes.Custom);
            AdventureModes.Custom.setSelectionName("[BLACK]" + Forge.getLocalizer().getMessage("lblDeck") + ":");
            AdventureModes.Custom.setModes(custom);
        }
        String[] modeNames = new String[modes.size];
        int constructedIndex = -1;

        for (int i = 0; i < modes.size; i++) {
            modeNames[i] = modes.get(i).getName();
            if (modes.get(i) == AdventureModes.Constructed) {
                constructedIndex = i;
            }
        }

        mode.setTextList(modeNames);
        mode.setCurrentIndex(constructedIndex != -1 ? constructedIndex : 0);

        AdventureModes initialMode = modes.get(mode.getCurrentIndex());
        starterEdition.setVisible(initialMode == AdventureModes.Standard);
        starterEditionLabel.setVisible(initialMode == AdventureModes.Standard);

        gender.setTextList(new String[]{Forge.getLocalizer().getMessage("lblMale") + "[%120][CYAN] \u2642",
                Forge.getLocalizer().getMessage("lblFemale") + "[%120][MAGENTA] \u2640"});
        gender.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                nameTT = 0.8f;
                super.clicked(event, x, y);
            }
        });
        gender.addListener(event -> NewGameScene.this.updateAvatar());

        mode.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent changeEvent, Actor actor) {
                AdventureModes smode = modes.get(mode.getCurrentIndex());
                colorLabel.setText(smode.getSelectionName());
                colorId.setTextList(smode.getModes());
                starterEdition.setVisible(smode == AdventureModes.Standard);
                starterEditionLabel.setVisible(smode == AdventureModes.Standard);
            }
        });
        race = ui.findActor("race");
        race.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                avatarTT = 0.7f;
                super.clicked(event, x, y);
            }
        });
        race.addListener(event -> NewGameScene.this.updateAvatar());
        race.setTextList(HeroListData.getRaces());
        difficulty = ui.findActor("difficulty");
        difficultyHelp = ui.findActor("difficultyHelp");

        Array<String> diffList = new Array<>(colorIds.length);
        int i = 0;
        int startingDifficulty = 0;
        for (DifficultyData diff : Config.instance().getConfigData().difficulties) {
            if (diff.startingDifficulty)
                startingDifficulty = i;
            diffList.add(Forge.getLocalizer().getMessageorUseDefault("lbl" + diff.name, diff.name));
            i++;
        }
        difficulty.setTextList(diffList);
        difficulty.setCurrentIndex(startingDifficulty);

        generateAvatar();
        gender.setCurrentIndex(rand.nextInt());
        colorId.setCurrentIndex(rand.nextInt());
        race.setCurrentIndex(rand.nextInt());
        ui.onButtonPress("back", NewGameScene.this::back);
        ui.onButtonPress("start", NewGameScene.this::start);
        ui.onButtonPress("leftAvatar", NewGameScene.this::leftAvatar);
        ui.onButtonPress("rightAvatar", NewGameScene.this::rightAvatar);
        difficultyHelp.addListener(new ClickListener() {
            public void clicked(InputEvent e, float x, float y) {
                showDifficultyHelp();
            }
        });
        modeHelp.addListener(new ClickListener() {
            public void clicked(InputEvent e, float x, float y) {
                showModeHelp();
            }
        });
    }

    private static NewGameScene object;

    public static NewGameScene instance() {
        if (object == null)
            object = new NewGameScene();
        return object;
    }

    float avatarT = 1f, avatarTT = 1f;
    float nameT = 1f, nameTT = 1f;

    @Override
    public void act(float delta) {
        super.act(delta);
        if (avatarT > avatarTT) {
            avatarTT += (delta / 0.5f);
            generateAvatar();
        } else {
            avatarTT = avatarT;
        }
        if (nameT > nameTT) {
            nameTT += (delta / 0.5f);
            generateName();
        } else {
            nameTT = nameT;
        }
    }

    private void generateAvatar() {
        avatarIndex = rand.nextInt();
        updateAvatar();
    }

    private void generateName() {
        //gender should be either Male or Female
        String val = gender.getCurrentIndex() > 0 ? "Female" : "Male";
        selectedName.setText(NameGenerator.getRandomName(val, "Any", ""));
    }

    boolean started = false;

    public boolean start() {
        if (started)
            return true;
        started = true;
        if (selectedName.getText().isEmpty()) {
            generateName();
        }
        Runnable runnable = () -> {
            started = false;
            //FModel.getPreferences().setPref(ForgePreferences.FPref.UI_ENABLE_MUSIC, false);
            WorldSave.generateNewWorld(selectedName.getText(),
                    gender.getCurrentIndex() == 0,
                    race.getCurrentIndex(),
                    avatarIndex,
                    colorIds[custom.isEmpty() || !AdventureModes.Custom.equals(modes.get(mode.getCurrentIndex())) ? colorId.getCurrentIndex() : 0],
                    Config.instance().getConfigData().difficulties[difficulty.getCurrentIndex()],
                    modes.get(mode.getCurrentIndex()), colorId.getCurrentIndex(),
                    editionIds[starterEdition.getCurrentIndex()], 0);//maybe replace with enum
            GamePlayerUtil.getGuiPlayer().setName(selectedName.getText());
            SoundSystem.instance.changeBackgroundTrack();
            WorldStage.getInstance().setDirectlyEnterPOI();
            if (AdventurePlayer.current().getQuests().stream().noneMatch(q -> q.getID() == 28)) {
                AdventurePlayer.current().addQuest("28"); //Temporary link to Shandalar main questline
            }
            Forge.switchScene(GameScene.instance());
        };
        Forge.setTransitionScreen(new TransitionScreen(runnable, null, false, true, Forge.getLocalizer().getMessage("lblGeneratingWorld")));
        return true;
    }

    public boolean back() {
        Forge.switchScene(StartScene.instance());
        return true;
    }


    private void rightAvatar() {

        avatarIndex++;
        updateAvatar();
    }

    private void leftAvatar() {
        avatarIndex--;
        updateAvatar();
    }

    private boolean updateAvatar() {
        avatarImage.setDrawable(new TextureRegionDrawable(HeroListData.getAvatar(race.getCurrentIndex(), gender.getCurrentIndex() != 0, avatarIndex)));
        return false;
    }


    @Override
    public void enter() {
        updateAvatar();

        if (Forge.createNewAdventureMap) {
            FModel.getPreferences().setPref(ForgePreferences.FPref.UI_ENABLE_MUSIC, false);
            WorldSave.generateNewWorld(selectedName.getText(),
                    gender.getCurrentIndex() == 0,
                    race.getCurrentIndex(),
                    avatarIndex,
                    colorIds[colorId.getCurrentIndex()],
                    Config.instance().getConfigData().difficulties[difficulty.getCurrentIndex()],
                    modes.get(mode.getCurrentIndex()), colorId.getCurrentIndex(),
                    editionIds[starterEdition.getCurrentIndex()], 0);
            GamePlayerUtil.getGuiPlayer().setName(selectedName.getText());
            Forge.switchScene(GameScene.instance());
        }

        unselectActors();
        super.enter();
    }

    private void showDifficultyHelp() {
        DifficultyData selectedDifficulty = Config.instance().getConfigData().difficulties[difficulty.getCurrentIndex()];

        difficultySummary = new DialogData();
        difficultySummary.name = "Summary";
        switch (selectedDifficulty.name) {
            case "Easy":
                difficultySummary.text = String.format("Difficulty: %s\nFor newer players or those who want a relaxed experience.\nStarter decks are monocolored.\nStarting equipment: Manasight Amulet, Leather Boots", selectedDifficulty.name);
                break;
            case "Normal":
                difficultySummary.text = String.format("Difficulty: %s\nHow Adventure Mode is intended to be played.\nStarter decks will include a second color.\nStarting equipment: Leather Boots", selectedDifficulty.name);
                break;
            case "Hard":
                difficultySummary.text = String.format("Difficulty: %s\nFor players who want a challenge.\nSome enemies will use genetic AI decks.\nStarter decks will include 2-3 colors.\nStarting equipment: None", selectedDifficulty.name);
                break;
            case "Insane":
                difficultySummary.text = String.format("Difficulty: %s\nFor players who don't want to like the game.\nIdentical to Hard difficulty, but with even less forgiving and rewarding results.\nStarter decks will include 2-3 colors.\nStarting equipment: None", selectedDifficulty.name);
                break;
            default:
                difficultySummary.text = "((Custom difficulty settings))";
                break;
        }


        DialogData dismiss = new DialogData();
        //todo: add translation
        dismiss.name = "OK";

        DialogData matchImpacts = new DialogData();
        matchImpacts.text = String.format("Difficulty: %s\nStarting Life: %d\nEnemy Health: %d%%\nGold loss on defeat: %d%%\nLife loss on defeat: %d%%", selectedDifficulty.name, selectedDifficulty.startingLife, (int) (selectedDifficulty.enemyLifeFactor * 100), (int) (selectedDifficulty.goldLoss * 100), (int) (selectedDifficulty.lifeLoss * 100));
        matchImpacts.name = "Duels";

        DialogData economyImpacts = new DialogData();
        economyImpacts.text = String.format("Difficulty: %s\nStarting Gold: %d\nStarting Mana Shards: %d\nCard Sale Price: %d%%\nMana Shard Sale Price: %d%%\nRandom loot rate: %d%%", selectedDifficulty.name, selectedDifficulty.staringMoney, selectedDifficulty.startingShards, (int) (selectedDifficulty.sellFactor * 100), (int) (selectedDifficulty.shardSellRatio * 100), (int) (selectedDifficulty.rewardMaxFactor * 100));
        economyImpacts.name = "Economy";

        difficultySummary.options = new DialogData[3];
        difficultySummary.options[0] = matchImpacts;
        difficultySummary.options[1] = economyImpacts;
        difficultySummary.options[2] = dismiss;
        matchImpacts.options = new DialogData[3];
        matchImpacts.options[0] = difficultySummary;
        matchImpacts.options[1] = economyImpacts;
        matchImpacts.options[2] = dismiss;
        economyImpacts.options = new DialogData[3];
        economyImpacts.options[0] = difficultySummary;
        economyImpacts.options[1] = matchImpacts;
        economyImpacts.options[2] = dismiss;

        loadDialog(difficultySummary);
    }

    private void showModeHelp() {

        AdventureModes selectedMode = modes.get(mode.getCurrentIndex());
        DifficultyData selectedDifficulty = Config.instance().getConfigData().difficulties[difficulty.getCurrentIndex()];

        modeSummary = new DialogData();
        modeSummary.name = "Summary";

        StringBuilder summaryText = new StringBuilder();
        switch (selectedMode) {
            case Standard:
                summaryText.append("Mode: Standard\n\nYour starting deck is built from 2-3 Jumpstart packs of twenty cards each.\n\n");
                switch (selectedDifficulty.name) {
                    case "Easy":
                        summaryText.append("On your currently selected difficulty, Easy, you will receive three jumpstart packs of your chosen color.");
                        break;
                    case "Normal":
                        summaryText.append("On your currently selected difficulty, Normal, you will receive two jumpstart packs of your chosen color and one of an allied color.");
                        break;
                    case "Hard":
                        summaryText.append("On your currently selected difficulty, Hard, you will receive one jumpstart pack of your chosen color and one of an allied color.");
                        break;
                    case "Insane":
                        summaryText.append("On your currently selected difficulty, Insane, you will receive one jumpstart pack of your chosen color and one of an allied color.");
                        break;
                    default:
                        difficultySummary.text = "((Cannot determine starter deck based on custom difficulty settings))";
                        break;
                }
                break;
            case Constructed:
                summaryText.append("Mode: Constructed\n\nYou will receive a specific preconstructed deck based on your chosen color and difficulty.\n\n");
                switch (selectedDifficulty.name) {
                    case "Easy":
                        summaryText.append("On your currently selected difficulty, Easy, your deck will only contain your chosen color.");
                        break;
                    case "Normal":
                        summaryText.append("On your currently selected difficulty, Normal, your deck will contain your chosen color and one allied color.");
                        break;
                    case "Hard":
                        summaryText.append("On your currently selected difficulty, Hard, your deck will contain your chosen color and one opposing color.");
                        break;
                    case "Insane":
                        summaryText.append("On your currently selected difficulty, Insane, your deck will contain your chosen color and one opposing color.");
                        break;
                    default:
                        difficultySummary.text = "((Cannot determine starter deck based on custom difficulty settings))";
                        break;
                }
                break;
            case Pile:
                summaryText.append("Mode: Pile\n\nYou will receive a random pile of cards based on your chosen color and difficulty.\n\n");
                switch (selectedDifficulty.name) {
                    case "Easy":
                        summaryText.append("On your currently selected difficulty, Easy, your deck will only contain your chosen color and one allied color.");
                        break;
                    case "Normal":
                        summaryText.append("On your currently selected difficulty, Normal, your deck will contain your chosen color and two allied colors.");
                        break;
                    case "Hard":
                        summaryText.append("On your currently selected difficulty, Hard, your deck will contain your chosen color and two allied colors.\n\n");
                        summaryText.append("You will receive less uncommon and rare cards than on Normal difficulty.");
                        break;
                    case "Insane":
                        summaryText.append("On your currently selected difficulty, Insane, your deck will contain your chosen color and two allied colors.\n\n");
                        summaryText.append("You will receive less uncommon and rare cards than on Normal difficulty.");
                        break;
                    default:
                        difficultySummary.text = "((Cannot determine starter deck based on custom difficulty settings))";
                        break;
                }
                break;
            case Chaos:
                summaryText.append("Mode: Chaos\n\nYou (and all enemies) will receive a random preconstructed deck.\n\nWarning: This will make encounter difficulty vary wildly from the developers' intent");
                break;
            case Custom:
                summaryText.append("Mode: Custom\n\nChoose your own preconstructed deck. Enemies can receive a random genetic AI deck (difficult).\n\nWarning: This will make encounter difficulty vary wildly from the developers' intent");
                break;
            default:
                summaryText.append("No summary available for your this game mode.");
                break;
        }

        DialogData dismiss = new DialogData();
        dismiss.name = "OK";
        modeSummary.text = summaryText.toString();
        modeSummary.options = new DialogData[1];
        modeSummary.options[0] = dismiss;
        loadDialog(modeSummary);
    }
}
