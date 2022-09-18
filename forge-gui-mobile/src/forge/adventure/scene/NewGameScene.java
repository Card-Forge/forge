package forge.adventure.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import com.github.tommyettinger.textra.TextraButton;
import com.github.tommyettinger.textra.TextraLabel;
import forge.Forge;
import forge.adventure.data.DifficultyData;
import forge.adventure.data.HeroListData;
import forge.adventure.util.Config;
import forge.adventure.util.Selector;
import forge.adventure.util.UIActor;
import forge.adventure.world.WorldSave;
import forge.card.ColorSet;
import forge.deck.DeckProxy;
import forge.gui.GuiBase;
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
    private Array<String> stringList, random, custom;
    private TextraLabel colorLabel;
    private int selected = -1;

    private NewGameScene() {

        super(Forge.isLandscapeMode() ? "ui/new_game.json" : "ui/new_game_portrait.json");

        selectedName = ui.findActor("nameField");
        selectedName.setText(NameGenerator.getRandomName("Any", "Any", ""));
        selectedName.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                if (!GuiBase.isAndroid() && showGamepadSelector) {
                    //show onscreen keyboard
                    return true;
                }
                return super.touchDown(event, x, y, pointer, button);
            }
        });
        avatarImage = ui.findActor("avatarPreview");
        gender = ui.findActor("gender");
        mode = ui.findActor("mode");
        colorLabel = ui.findActor("colorIdL");
        String colorIdLabel = colorLabel.storedText;
        custom = new Array<>();
        for (DeckProxy deckProxy : DeckProxy.getAllCustomStarterDecks())
            custom.add(deckProxy.getName());
        mode.setTextList(custom.isEmpty() ? new String[]{"Standard", "Constructed", "Chaos"} : new String[]{"Standard", "Constructed", "Chaos", "Custom"});
        gender.setTextList(new String[]{"Male", "Female"});
        gender.addListener(event -> NewGameScene.this.updateAvatar());
        Random rand = new Random();
        colorId = ui.findActor("colorId");
        String[] colorSet = Config.instance().colorIds();
        String[] colorIdNames = Config.instance().colorIdNames();
        colorIds = new ColorSet[colorSet.length];
        for (int i = 0; i < colorIds.length; i++)
            colorIds[i] = ColorSet.fromNames(colorSet[i].toCharArray());
        stringList = new Array<>(colorIds.length);
        for (String idName : colorIdNames)
            stringList.add(UIActor.localize(idName));
        colorId.setTextList(stringList);
        random = new Array<>();
        random.add(Forge.getLocalizer().getMessage("lblRandomDeck"));
        mode.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent changeEvent, Actor actor) {
                colorLabel.setText(mode.getCurrentIndex() < 2 ? colorIdLabel : Forge.getLocalizer().getMessage("lblDeck")+":");
                if (mode.getCurrentIndex() == 3)
                    colorId.setTextList(custom);
                if (mode.getCurrentIndex() == 2)
                    colorId.setTextList(random);
                if (mode.getCurrentIndex() < 2)
                    colorId.setTextList(stringList);
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

    private static NewGameScene object;

    public static NewGameScene instance() {
        if(object==null)
            object=new NewGameScene();
        return object;
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
                    mode.getCurrentIndex() == 2, mode.getCurrentIndex() == 1, mode.getCurrentIndex() == 3, colorId.getCurrentIndex(), 0);//maybe replace with enum
            GamePlayerUtil.getGuiPlayer().setName(selectedName.getText());
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
    public void create() {

    }

    @Override
    public void enter() {
        updateAvatar();
        Gdx.input.setInputProcessor(stage); //Start taking input from the ui

        if (Forge.createNewAdventureMap) {
            FModel.getPreferences().setPref(ForgePreferences.FPref.UI_ENABLE_MUSIC, false);
            WorldSave.generateNewWorld(selectedName.getText(),
                    gender.getCurrentIndex() == 0,
                    race.getCurrentIndex(),
                    avatarIndex,
                    colorIds[colorId.getCurrentIndex()],
                    Config.instance().getConfigData().difficulties[difficulty.getCurrentIndex()],
                    mode.getCurrentIndex() == 2, mode.getCurrentIndex() == 1, mode.getCurrentIndex() == 3, colorId.getCurrentIndex(), 0);//maybe replace with enum
            GamePlayerUtil.getGuiPlayer().setName(selectedName.getText());
            Forge.switchScene(GameScene.instance());
        }
        clearActorObjects();
        addActorObject(selectedName);
        addActorObject(race);
        addActorObject(gender);
        addActorObject(difficulty);
        addActorObject(colorId);
        addActorObject(mode);
        addActorObject(ui.findActor("back"));
        addActorObject(ui.findActor("start"));
        unselectActors();
    }
    @Override
    public boolean pointerMoved(int screenX, int screenY) {
        ui.screenToLocalCoordinates(pointer.set(screenX,screenY));
        if (showGamepadSelector) {
            unselectActors();
            showGamepadSelector = false;
        }
        if (kbVisible)
            return super.pointerMoved(screenX, screenY);
        updateHovered();
        return super.pointerMoved(screenX, screenY);
    }
    @Override
    public boolean keyPressed(int keycode) {
        if (Forge.hasGamepad())
            showGamepadSelector = true;
        if (keycode == Input.Keys.ESCAPE || keycode == Input.Keys.BACK) {
            if(!kbVisible)
                back();
        }
        if (keycode == Input.Keys.BUTTON_SELECT) {
            if (showGamepadSelector) {
                if(!kbVisible)
                    performTouch(ui.findActor("back"));
            }
        } else if (keycode == Input.Keys.BUTTON_START) {
            if (showGamepadSelector) {
                if(kbVisible)
                    keyOK();
                else
                    performTouch(ui.findActor("start"));
            }
        } else if (keycode == Input.Keys.BUTTON_L2) {
            if(!kbVisible)
                selectedName.setText(NameGenerator.getRandomName("Female", "Any", selectedName.getText()));
        } else if (keycode == Input.Keys.BUTTON_R2) {
            if(!kbVisible)
                selectedName.setText(NameGenerator.getRandomName("Male", "Any", selectedName.getText()));
        } else if (keycode == Input.Keys.BUTTON_L1) {
            if (showGamepadSelector) {
                if (kbVisible)
                    toggleShiftOrBackspace(true);
                else
                    performTouch(ui.findActor("leftAvatar"));
            }
        } else if (keycode == Input.Keys.BUTTON_R1) {
            if (showGamepadSelector) {
                if(kbVisible)
                    toggleShiftOrBackspace(false);
                else
                    performTouch(ui.findActor("rightAvatar"));
            }
        } else if (keycode == Input.Keys.DPAD_DOWN) {
            if (showGamepadSelector) {
                if (kbVisible) {
                    setSelectedKey(keycode);
                } else {
                    if (selectedActor == mode)
                        selectActor(selectedName, false);
                    else if (selectedActor == ui.findActor("back"))
                        selectActor(ui.findActor("start"), false);
                    else if (selectedActor == ui.findActor("start"))
                        selectActor(ui.findActor("back"), false);
                    else
                        selectNextActor(false);
                }
            }
        } else if (keycode == Input.Keys.DPAD_UP) {
            if (showGamepadSelector) {
                if (kbVisible) {
                    setSelectedKey(keycode);
                } else {
                    if (selectedActor == selectedName)
                        selectActor(mode, false);
                    else if (selectedActor == ui.findActor("start"))
                        selectActor(ui.findActor("back"), false);
                    else if (selectedActor == ui.findActor("back"))
                        selectActor(ui.findActor("start"), false);
                    else
                        selectPreviousActor(false);
                }
            }
        } else if (keycode == Input.Keys.DPAD_LEFT) {
            if (showGamepadSelector) {
                if (kbVisible) {
                    setSelectedKey(keycode);
                } else {
                    if (selectedActor == ui.findActor("back") || selectedActor == ui.findActor("start"))
                        selectActor(mode, false);
                }
            }
        } else if (keycode == Input.Keys.DPAD_RIGHT) {
            if (showGamepadSelector) {
                if (kbVisible) {
                    setSelectedKey(keycode);
                } else {
                    if (!(selectedActor == ui.findActor("back") || selectedActor == ui.findActor("start")))
                        selectActor(ui.findActor("start"), false);
                }
            }
        } else if (keycode == Input.Keys.BUTTON_A) {
            if (showGamepadSelector) {
                if (kbVisible) {
                    if (selectedKey != null)
                        performTouch(selectedKey);
                } else {
                    if (selectedActor != null) {
                        if (selectedActor instanceof TextraButton)
                            performTouch(selectedActor);
                        else if (selectedActor instanceof TextField && !kbVisible) {
                            lastInputField = selectedActor;
                            showOnScreenKeyboard("");
                        }
                    }
                }
            }
        } else if (keycode == Input.Keys.BUTTON_B) {
            if (showGamepadSelector) {
                if (kbVisible) {
                    hideOnScreenKeyboard();
                } else {
                    performTouch(ui.findActor("back"));
                }
            }
        } else if (keycode == Input.Keys.BUTTON_X) {
            if (showGamepadSelector) {
                if(!kbVisible)
                    if (selectedActor != null && selectedActor instanceof Selector)
                        performTouch(((Selector) selectedActor).getLeftArrow());
            }
        } else if (keycode == Input.Keys.BUTTON_Y) {
            if (showGamepadSelector) {
                if(!kbVisible)
                    if (selectedActor != null && selectedActor instanceof Selector)
                        performTouch(((Selector) selectedActor).getRightArrow());
            }
        }
        return true;
    }
}
