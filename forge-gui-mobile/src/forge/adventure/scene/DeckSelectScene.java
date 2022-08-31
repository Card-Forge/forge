package forge.adventure.scene;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.IntMap;
import forge.Forge;
import forge.adventure.player.AdventurePlayer;
import forge.adventure.util.Controls;
import forge.adventure.util.Current;

public class DeckSelectScene extends UIScene {
    private final IntMap<TextButton> buttons = new IntMap<>();
    Color defColor;
    Dialog dialog;
    TextField textInput;
    Table layout;
    Label header;
    TextButton back, edit, rename, dialogRenameBtn, dialogAbortBtn;
    int currentSlot = 0;
    ScrollPane scrollPane;

    public DeckSelectScene() {
        super(Forge.isLandscapeMode() ? "ui/deck_selector.json" : "ui/deck_selector_portrait.json");
    }

    private TextButton addDeckSlot(String name, int i) {
        TextButton button = Controls.newTextButton("-");
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                try {
                    if (!button.isDisabled())
                        select(i);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        layout.add(Controls.newLabel(name)).expandX().pad(2);
        layout.add(button).expandX().pad(2);
        buttons.put(i, button);
        layout.row();
        return button;
    }

    public void back() {
        Forge.switchToLast();
    }

    public boolean select(int slot) {
        currentSlot = slot;

        for (IntMap.Entry<TextButton> butt : new IntMap.Entries<TextButton>(buttons)) {
            butt.value.setColor(defColor);
        }
        if (buttons.containsKey(slot)) {
            TextButton button = buttons.get(slot);
            button.setColor(Color.RED);
            selectActor(button, false);
        }
        Current.player().setSelectedDeckSlot(slot);

        return true;
    }


    @Override
    public boolean keyPressed(int keycode) {
        if (keycode == Input.Keys.ESCAPE || keycode == Input.Keys.BACK) {
            back();
        }
        if (kbVisible) {
            if (keycode == Input.Keys.BUTTON_START)
                keyOK();
            else if (keycode == Input.Keys.BUTTON_L1)
                toggleShiftOrBackspace(true);
            else if (keycode == Input.Keys.BUTTON_R1)
                toggleShiftOrBackspace(false);
            else if (keycode == Input.Keys.BUTTON_B)
                hideOnScreenKeyboard();
            else if (keycode == Input.Keys.BUTTON_A) {
                if (selectedKey != null)
                    performTouch(selectedKey);
            } else if (keycode == Input.Keys.DPAD_UP || keycode == Input.Keys.DPAD_DOWN || keycode == Input.Keys.DPAD_LEFT || keycode == Input.Keys.DPAD_RIGHT)
                setSelectedKey(keycode);
        } else if (dialog.getColor().a == 1f) {
            if (keycode == Input.Keys.BUTTON_A) {
                if (selectedActor == textInput) {
                    lastInputField = textInput;
                    showOnScreenKeyboard(textInput.getText());
                } else if (selectedActor == dialogAbortBtn || selectedActor == dialogRenameBtn) {
                    performTouch(selectedActor);
                    select(Current.player().getSelectedDeckIndex());
                }
            } else if (keycode == Input.Keys.BUTTON_B) {
                performTouch(dialogAbortBtn);
                select(Current.player().getSelectedDeckIndex());
            }
            else if (keycode == Input.Keys.DPAD_DOWN) {
                if (selectedActor == null) {
                    selectActor(textInput, false);
                } else if (selectedActor == textInput)
                    selectActor(dialogRenameBtn, false);
            } else if (keycode == Input.Keys.DPAD_UP) {
                if (selectedActor == null)
                    selectActor(dialogRenameBtn, false);
                else if (selectedActor == dialogRenameBtn || selectedActor == dialogAbortBtn) {
                    selectActor(textInput, false);
                }
            } else if (keycode == Input.Keys.DPAD_LEFT) {
                if (selectedActor == dialogAbortBtn)
                    selectActor(dialogRenameBtn, false);
            } else if (keycode == Input.Keys.DPAD_RIGHT) {
                if (selectedActor == dialogRenameBtn)
                    selectActor(dialogAbortBtn, false);
            }
        } else {
            if (keycode == Input.Keys.BUTTON_B)
                performTouch(back);
            else if (keycode == Input.Keys.BUTTON_Y)
                performTouch(rename);
            else if (keycode == Input.Keys.BUTTON_X)
                performTouch(edit);
            else if (keycode == Input.Keys.BUTTON_L1) {
                scrollPane.fling(1f, 0, -300);
            }
            else if (keycode == Input.Keys.BUTTON_R1) {
                scrollPane.fling(1f, 0, +300);
            } else if (keycode == Input.Keys.BUTTON_A)
                performTouch(selectedActor);
            else if (keycode == Input.Keys.DPAD_DOWN) {
                if (selectedActorIndex == 9) {
                    selectActor(actorObjectMap.get(0), false);
                    scrollPane.fling(1f, 0, +300);
                } else {
                    selectNextActor(false);
                }
                if (selectedActorIndex == 6)
                    scrollPane.fling(1f, 0, -300);
            } else if (keycode == Input.Keys.DPAD_UP) {
                if (selectedActorIndex == 0) {
                    selectActor(actorObjectMap.get(9), false);
                    scrollPane.fling(1f, 0, -300);
                } else {
                    selectPreviousActor(false);
                }
                if (selectedActorIndex == 5)
                    scrollPane.fling(1f, 0, +300);
            }
        }
        return true;
    }

    @Override
    public void enter() {
        clearActorObjects();
        for (int i = 0; i < AdventurePlayer.NUMBER_OF_DECKS; i++) {
            if (buttons.containsKey(i)) {
                buttons.get(i).setText(Current.player().getDeck(i).getName());
                addActorObject(buttons.get(i));
            }
        }
        addActorObject(back);
        addActorObject(rename);
        addActorObject(edit);
        addActorObject(textInput);
        addActorObject(dialogRenameBtn);
        addActorObject(dialogAbortBtn);
        select(Current.player().getSelectedDeckIndex());
        super.enter();
    }

    @Override
    public void resLoaded() {
        super.resLoaded();
            layout = new Table();
            stage.addActor(layout);

            header = Controls.newLabel(Forge.getLocalizer().getMessage("lblSelectYourDeck"));
            layout.add(header).colspan(2).align(Align.center).pad(2, 5, 2, 5);
            layout.row();
            for (int i = 0; i < AdventurePlayer.NUMBER_OF_DECKS; i++)
                addDeckSlot(Forge.getLocalizer().getMessage("lblDeck")+": " + (i + 1), i);

            dialog = Controls.newDialog(Forge.getLocalizer().getMessage("lblSave"));
            textInput = Controls.newTextField("");
            dialog.getButtonTable().add(Controls.newLabel(Forge.getLocalizer().getMessage("lblNameYourSaveFile"))).colspan(2);
            dialog.getButtonTable().row();
            dialog.getButtonTable().add(Controls.newLabel(Forge.getLocalizer().getMessage("lblName")+": ")).align(Align.left);
            dialog.getButtonTable().add(textInput).fillX().expandX();
            dialog.getButtonTable().row();
            dialogRenameBtn = Controls.newTextButton(Forge.getLocalizer().getMessage("lblRename"), () -> DeckSelectScene.this.rename());
            dialog.getButtonTable().add(dialogRenameBtn).align(Align.left).padLeft(15);
            dialogAbortBtn = Controls.newTextButton(Forge.getLocalizer().getMessage("lblAbort"), () -> dialog.hide());
            dialog.getButtonTable().add(dialogAbortBtn).align(Align.right).padRight(15);
            dialog.getColor().a = 0f;
            dialog.hide();

            back = ui.findActor("return");
            back.getLabel().setText(Forge.getLocalizer().getMessage("lblBack"));
            edit = ui.findActor("edit");
            edit.getLabel().setText(Forge.getLocalizer().getMessage("lblEdit"));
            rename = ui.findActor("rename");
            rename.getLabel().setText(Forge.getLocalizer().getMessage("lblRename"));
            ui.onButtonPress("return", () -> DeckSelectScene.this.back());
            ui.onButtonPress("edit", () -> DeckSelectScene.this.edit());
            ui.onButtonPress("rename", () -> {
                textInput.setText(Current.player().getSelectedDeck().getName());
                dialog.show(stage);
                selectActor(textInput, false);
            });
            defColor = ui.findActor("return").getColor();

            scrollPane = ui.findActor("deckSlots");
            scrollPane.setActor(layout);

    }

    private void rename() {
        dialog.hide();
        String text = textInput.getText();
        Current.player().renameDeck(text);
        buttons.get(currentSlot).setText(Current.player().getDeck(currentSlot).getName());
    }

    private void edit() {
        Forge.switchScene(SceneType.DeckEditScene.instance);
    }
}
