package forge.adventure.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import forge.Forge;
import forge.adventure.data.DifficultyData;
import forge.adventure.data.HeroListData;
import forge.adventure.util.Config;
import forge.adventure.util.Selector;
import forge.adventure.util.UIActor;
import forge.adventure.world.WorldSave;
import forge.card.ColorSet;
import forge.deck.DeckProxy;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;
import forge.player.GamePlayerUtil;
import forge.screens.TransitionScreen;
import forge.util.NameGenerator;

import java.util.Random;

/**
 * NewGame scene that contains the character creation
 */
public class NewGameScene extends UIScene {
    TextField selectedName;
    ColorSet[] colorIds;
    private Image avatarImage;
    private int avatarIndex = 0;
    private Selector race;
    private Selector colorId;
    private Selector gender;
    private Selector mode;
    private Selector difficulty;

    public NewGameScene() {
        super(Forge.isLandscapeMode() ? "ui/new_game.json" : "ui/new_game_portrait.json");
    }

    public boolean start() {
        if (selectedName.getText().isEmpty()) {
            selectedName.setText(NameGenerator.getRandomName("Any", "Any", ""));
        }
        Runnable runnable = () -> {
            FModel.getPreferences().setPref(ForgePreferences.FPref.UI_ENABLE_MUSIC, false);
            WorldSave.generateNewWorld(selectedName.getText(),
                    gender.getCurrentIndex() == 0,
                    race.getCurrentIndex(),
                    avatarIndex,
                    colorIds[colorId.getCurrentIndex()],
                    Config.instance().getConfigData().difficulties[difficulty.getCurrentIndex()],
                    mode.getCurrentIndex()==2, mode.getCurrentIndex()==1,  0);//maybe replace with enum
            GamePlayerUtil.getGuiPlayer().setName(selectedName.getText());
            Forge.clearTransitionScreen();
            Forge.switchScene(SceneType.GameScene.instance);
        };
        Forge.setTransitionScreen(new TransitionScreen(runnable, null, false, true));
        return true;
    }

    public boolean back() {
        Forge.switchScene(SceneType.StartScene.instance);
        return true;
    }

    @Override
    public void resLoaded() {
        super.resLoaded();
            selectedName = ui.findActor("nameField");
            selectedName.setText(NameGenerator.getRandomName("Any", "Any", ""));
            avatarImage = ui.findActor("avatarPreview");
            gender = ui.findActor("gender");
            mode = ui.findActor("mode");
            mode.setTextList(new String[]{"Standard", "Constructed","Chaos"});
            gender.setTextList(new String[]{"Male", "Female"});
            gender.addListener(event -> NewGameScene.this.updateAvatar());
            Random rand = new Random();
            colorId = ui.findActor("colorId");
            String[] colorSet=Config.instance().colorIds();
            String[] colorIdNames=Config.instance().colorIdNames();
            colorIds = new ColorSet[colorSet.length];
            for(int i = 0; i< colorIds.length; i++)
                colorIds[i]=  ColorSet.fromNames(colorSet[i].toCharArray());

            Array<String> stringList = new Array<>(colorIds.length);
            for (String idName : colorIdNames)
                stringList.add(UIActor.localize(idName));
            Array<String> chaos = new Array<>();
            chaos.add("Preconstructed");
            Array<String> easyDecks = new Array<>();
            for (DeckProxy deckProxy : DeckProxy.getAllEasyStarterDecks())
                easyDecks.add(deckProxy.getName());
            colorId.setTextList(stringList);
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
                diffList.add(diff.name);
                i++;
            }
            difficulty.setTextList(diffList);
            difficulty.setCurrentIndex(startingDifficulty);
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
    public void create() {

    }

    @Override
    public void enter() {
        updateAvatar();
        Gdx.input.setInputProcessor(stage); //Start taking input from the ui

        if(Forge.createNewAdventureMap)
        {
            FModel.getPreferences().setPref(ForgePreferences.FPref.UI_ENABLE_MUSIC, false);
            WorldSave.generateNewWorld(selectedName.getText(),
                    gender.getCurrentIndex() == 0,
                    race.getCurrentIndex(),
                    avatarIndex,
                    colorIds[colorId.getCurrentIndex()],
                    Config.instance().getConfigData().difficulties[difficulty.getCurrentIndex()],
                    mode.getCurrentIndex()==2, mode.getCurrentIndex()==1,  0);//maybe replace with enum
            GamePlayerUtil.getGuiPlayer().setName(selectedName.getText());
            Forge.switchScene(SceneType.GameScene.instance);
        }
    }

    @Override
    public boolean keyPressed(int keycode) {
        if (keycode == Input.Keys.ESCAPE || keycode == Input.Keys.BACK) {
            back();
        }
        return true;
    }
}
