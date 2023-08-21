package forge.adventure.scene;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import com.github.tommyettinger.textra.TextraLabel;
import forge.Forge;
import forge.adventure.data.DifficultyData;
import forge.adventure.data.HeroListData;
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
public class NewGameScene extends UIScene {
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

    private final Array<AdventureModes> modes = new Array<>();

    private NewGameScene() {

        super(Forge.isLandscapeMode() ? "ui/new_game.json" : "ui/new_game_portrait.json");

        selectedName = ui.findActor("nameField");
        selectedName.setText(NameGenerator.getRandomName("Any", "Any", ""));
        avatarImage = ui.findActor("avatarPreview");
        gender = ui.findActor("gender");
        mode = ui.findActor("mode");
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
        for (int i = 0; i < modes.size; i++)
            modeNames[i] = modes.get(i).getName();
        mode.setTextList(modeNames);

        gender.setTextList(new String[]{Forge.getLocalizer().getInstance().getMessage("lblMale"), Forge.getLocalizer().getInstance().getMessage("lblFemale")});
        gender.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                //gender should be either Male or Female
                String val = gender.getCurrentIndex() > 0 ? "Female" : "Male";
                selectedName.setText(NameGenerator.getRandomName(val, "Any", ""));
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
        race.addListener(event -> NewGameScene.this.updateAvatar());
        race.setTextList(HeroListData.getRaces());
        difficulty = ui.findActor("difficulty");

        Array<String> diffList = new Array<>(colorIds.length);
        int i = 0;
        int startingDifficulty = 0;
        for (DifficultyData diff : Config.instance().getConfigData().difficulties) {
            if (diff.startingDifficulty)
                startingDifficulty = i;
            diffList.add(Forge.getLocalizer().getInstance().getMessageorUseDefault("lbl" + diff.name, diff.name));
            i++;
        }
        difficulty.setTextList(diffList);
        difficulty.setCurrentIndex(startingDifficulty);

        Random rand = new Random();
        avatarIndex = rand.nextInt();
        updateAvatar();
        gender.setCurrentIndex(rand.nextInt());
        colorId.setCurrentIndex(rand.nextInt());
        race.setCurrentIndex(rand.nextInt());
        ui.onButtonPress("back", () -> NewGameScene.this.back());
        ui.onButtonPress("start", () -> NewGameScene.this.start());
        ui.onButtonPress("leftAvatar", () -> NewGameScene.this.leftAvatar());
        ui.onButtonPress("rightAvatar", () -> NewGameScene.this.rightAvatar());
    }

    private static NewGameScene object;

    public static NewGameScene instance() {
        if (object == null)
            object = new NewGameScene();
        return object;
    }

    boolean started = false;

    public boolean start() {
        if (started)
            return true;
        started = true;
        if (selectedName.getText().isEmpty()) {
            selectedName.setText(NameGenerator.getRandomName("Any", "Any", ""));
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
            //AdventurePlayer.current().addQuest("28"); //Temporary link to Shandalar main questline
            Forge.switchScene(GameScene.instance());
        };
        Forge.setTransitionScreen(new TransitionScreen(runnable, null, false, true, "Generating World..."));
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
}
