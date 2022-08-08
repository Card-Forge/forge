package forge.adventure.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import forge.Forge;
import forge.adventure.data.DifficultyData;
import forge.adventure.data.HeroListData;
import forge.adventure.util.Config;
import forge.adventure.util.Controls;
import forge.adventure.util.Selector;
import forge.adventure.world.WorldSave;
import forge.deck.Deck;
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
    Deck[] starterDeck;
    private Image avatarImage;
    private int avatarIndex = 0;
    private Selector race;
    private Selector deck;
    private Selector gender;
    private Selector difficulty;
    private ScrollPane scrollPane;
    private Label titleL, avatarL, nameL, raceL, genderL, difficultyL, deckL;
    private ImageButton leftArrow, rightArrow;
    private TextButton backButton, startButton;
    boolean fantasyMode = false;
    boolean easyMode = false;
    private CheckBox box, box2;

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
                    deck.getCurrentIndex(),
                    Config.instance().getConfigData().difficulties[difficulty.getCurrentIndex()],
                    fantasyMode, easyMode, deck.getText(), 0);
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
            gender.setTextList(new String[]{"Male", "Female"});
            gender.addListener(event -> NewGameScene.this.updateAvatar());
            Random rand = new Random();
            deck = ui.findActor("deck");
            starterDeck = Config.instance().starterDecks();
            Array<String> stringList = new Array<>(starterDeck.length);
            for (Deck deck : starterDeck)
                stringList.add(deck.getName());
            Array<String> chaos = new Array<>();
            chaos.add("Preconstructed");
            Array<String> easyDecks = new Array<>();
            for (DeckProxy deckProxy : DeckProxy.getAllEasyStarterDecks())
                easyDecks.add(deckProxy.getName());
            deck.setTextList(stringList);
            race = ui.findActor("race");
            race.addListener(event -> NewGameScene.this.updateAvatar());
            race.setTextList(HeroListData.getRaces());
            difficulty = ui.findActor("difficulty");

            Array<String> diffList = new Array<>(starterDeck.length);
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
            gender.setCurrentIndex(rand.nextInt());
            deck.setCurrentIndex(rand.nextInt());
            race.setCurrentIndex(rand.nextInt());
            ui.onButtonPress("back", () -> NewGameScene.this.back());
            ui.onButtonPress("start", () -> NewGameScene.this.start());
            ui.onButtonPress("leftAvatar", () -> NewGameScene.this.leftAvatar());
            ui.onButtonPress("rightAvatar", () -> NewGameScene.this.rightAvatar());

            scrollPane = ui.findActor("scroll");
            titleL = ui.findActor("titleL");
            titleL.setScale(2, 2);
            titleL.setText(Forge.getLocalizer().getMessage("lblCreateACharacter"));
            titleL.setX(scrollPane.getX() + 20);
            avatarL = ui.findActor("avatarL");
            avatarL.setText(Forge.getLocalizer().getMessage("lblAvatar"));
            nameL = ui.findActor("nameL");
            nameL.setText(Forge.getLocalizer().getMessage("lblName"));
            raceL = ui.findActor("raceL");
            raceL.setText(Forge.getLocalizer().getMessage("lblRace"));
            genderL = ui.findActor("genderL");
            genderL.setText(Forge.getLocalizer().getMessage("lblGender"));
            difficultyL = ui.findActor("difficultyL");
            difficultyL.setText(Forge.getLocalizer().getMessage("lblDifficulty"));
            deckL = ui.findActor("deckL");
            deckL.setText(Forge.getLocalizer().getMessage("lblDeck"));
            box2 = Controls.newCheckBox("");
            box2.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent changeEvent, Actor actor) {
                    if (((CheckBox) actor).isChecked()) {
                        box.setChecked(false);
                        easyMode = true;
                        fantasyMode = false;
                        deck.setTextList(easyDecks);
                    } else {
                        easyMode = false;
                        deck.setTextList(stringList);
                    }
                }
            });

            box = Controls.newCheckBox("");
            box.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                if (((CheckBox) actor).isChecked()) {
                    box2.setChecked(false);
                    fantasyMode = true;
                    easyMode = false;
                    deck.setTextList(chaos);
                } else {
                    fantasyMode = false;
                    deck.setTextList(stringList);
                }
                }
            });
            //easy mode
            box2.setBounds(deckL.getX()-box2.getHeight(), deckL.getY()-box2.getHeight(), deckL.getHeight(), deckL.getHeight());
            Label label2 = Controls.newLabel("Starter");
            label2.setColor(Color.BLACK);
            label2.setBounds(box2.getX()+22, box2.getY(), box2.getWidth(), box2.getHeight());
            ui.addActor(box2);
            ui.addActor(label2);
            //chaos mode
            box.setBounds(label2.getX()+25, label2.getY(), box2.getWidth(), box2.getHeight());
            Label label = Controls.newLabel("Chaos");
            label.setColor(Color.BLACK);
            label.setBounds(box.getX()+22, box.getY(), box.getWidth(), box.getHeight());
            ui.addActor(box);
            ui.addActor(label);
            if (easyDecks.isEmpty()) {
                box2.setDisabled(true);
                box2.getColor().a = 0.5f;
                label2.getColor().a = 0.5f;
            }
            leftArrow = ui.findActor("leftAvatar");
            rightArrow = ui.findActor("rightAvatar");
            backButton = ui.findActor("back");
            backButton.getLabel().setText(Forge.getLocalizer().getMessage("lblBack"));
            startButton = ui.findActor("start");
            startButton.getLabel().setText(Forge.getLocalizer().getMessage("lblStart"));

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
            start();
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
