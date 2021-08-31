package forge.adventure.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import forge.adventure.AdventureApplicationAdapter;
import forge.adventure.data.HeroListData;
import forge.adventure.util.Res;
import forge.adventure.util.Selector;
import forge.adventure.world.WorldSave;
import forge.deck.Deck;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;
import forge.player.GamePlayerUtil;
import forge.util.NameGenerator;


public class NewGameScene extends UIScene {
    TextField selectedName;
    WorldSave.Difficulty selectedDiff = WorldSave.Difficulty.Medium;
    Deck[] starterDeck;
    private Image avatarImage;
    private int avatarIndex = 0;
    private Selector race;
    private Selector deck;
    private Selector gender;
    private Selector difficulty;

    public NewGameScene() {
        super("ui/newgame.json");
    }

    public boolean start() {
        FModel.getPreferences().setPref(ForgePreferences.FPref.UI_ENABLE_MUSIC, false);
        WorldSave.generateNewWorld(selectedName.getText(),
                gender.getCurrentIndex() == 0,
                race.getCurrentIndex(),
                avatarIndex,
                starterDeck[deck.getCurrentIndex()],
                WorldSave.Difficulty.values()[difficulty.getCurrentIndex()],0);
        GamePlayerUtil.getGuiPlayer().setName(selectedName.getText());
        //image = new Texture(img);

        AdventureApplicationAdapter.instance.switchScene(SceneType.GameScene.instance);
        return true;
    }

    public boolean Back() {
        AdventureApplicationAdapter.instance.switchScene(SceneType.StartScene.instance);
        return true;
    }

    @Override
    public void resLoaded() {
        super.resLoaded();
        selectedName = ui.findActor("nameField");
        selectedName.setText(NameGenerator.getRandomName("Any", "Any", ""));
        avatarImage = ui.findActor("avatarPreview");
        gender = ui.findActor("gender");
        gender.setTextList(new String[]{"Male", "Female"});
        gender.addListener(event -> updateAvatar());

        deck = ui.findActor("deck");

        starterDeck = Res.CurrentRes.starterDecks();
        Array<String> stringList = new Array<>(starterDeck.length);
        for (Deck deckit : starterDeck)
            stringList.add(deckit.getName());

        deck.setTextList(stringList);

        race = ui.findActor("race");
        race.addListener(event -> updateAvatar());
        race.setTextList(HeroListData.getRaces());
        difficulty = ui.findActor("difficulty");

        stringList = new Array<>(WorldSave.Difficulty.values().length);
        for (WorldSave.Difficulty diff : WorldSave.Difficulty.values())
            stringList.add(diff.toString());
        difficulty.setTextList(stringList);
        difficulty.setCurrentIndex(1);

        ui.onButtonPress("back", () -> Back());
        ui.onButtonPress("start", () -> start());
        ui.onButtonPress("leftAvatar", () -> leftAvatar());
        ui.onButtonPress("rightAvatar", () -> rightAvatar());

        updateAvatar();
    }

    private boolean rightAvatar() {

        avatarIndex++;
        updateAvatar();
        return false;
    }

    private boolean leftAvatar() {
        avatarIndex--;
        updateAvatar();
        return false;
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
        Gdx.input.setInputProcessor(stage); //Start taking input from the ui


    }
}
