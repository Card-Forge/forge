package forge.adventure.scene;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
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
    TextButton back, edit, rename;
    int currentSlot = 0;

    public DeckSelectScene() {
        super(Forge.isLandscapeMode() ? "ui/deck_selector_mobile.json" : "ui/deck_selector.json");
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
        }
        Current.player().setSelectedDeckSlot(slot);

        return true;
    }


    @Override
    public boolean keyPressed(int keycode) {
        if (keycode == Input.Keys.ESCAPE || keycode == Input.Keys.BACK) {
            back();
        }
        return true;
    }

    @Override
    public void enter() {
        select(Current.player().getSelectedDeckIndex());
        for (int i = 0; i < AdventurePlayer.NUMBER_OF_DECKS; i++) {
            if (buttons.containsKey(i)) {
                buttons.get(i).setText(Current.player().getDeck(i).getName());
            }
        }
        super.enter();
    }

    @Override
    public void resLoaded() {
        super.resLoaded();
        layout = new Table();
        stage.addActor(layout);

        header = Controls.newLabel("Select your Deck");
        layout.add(header).colspan(2).align(Align.center).pad(2, 5, 2, 5);
        layout.row();
        for (int i = 0; i < AdventurePlayer.NUMBER_OF_DECKS; i++)
            addDeckSlot("Deck:" + (i + 1), i);

        dialog = Controls.newDialog("Save");
        textInput = Controls.newTextField("");
        if (!Forge.isLandscapeMode()) {
            dialog.getButtonTable().add(Controls.newLabel("Name your new save file.")).colspan(2).pad(2, 15, 2, 15);
            dialog.getButtonTable().row();
            dialog.getButtonTable().add(Controls.newLabel("Name:")).align(Align.left).pad(2, 15, 2, 2);
            dialog.getButtonTable().add(textInput).fillX().expandX().padRight(15);
            dialog.getButtonTable().row();
            dialog.getButtonTable().add(Controls.newTextButton("Rename", new Runnable() {
                @Override
                public void run() {
                    DeckSelectScene.this.rename();
                }
            })).align(Align.left).padLeft(15);
            dialog.getButtonTable().add(Controls.newTextButton("Abort", new Runnable() {
                @Override
                public void run() {
                    dialog.hide();
                }
            })).align(Align.right).padRight(15);
        } else {
            dialog.getButtonTable().add(Controls.newLabel("Name your new save file.")).colspan(2);
            dialog.getButtonTable().row();
            dialog.getButtonTable().add(Controls.newLabel("Name:")).align(Align.left);
            dialog.getButtonTable().add(textInput).fillX().expandX();
            dialog.getButtonTable().row();
            dialog.getButtonTable().add(Controls.newTextButton("Rename", new Runnable() {
                @Override
                public void run() {
                    DeckSelectScene.this.rename();
                }
            })).align(Align.left);
            dialog.getButtonTable().add(Controls.newTextButton("Abort", new Runnable() {
                @Override
                public void run() {
                    dialog.hide();
                }
            })).align(Align.left);
        }
        back = ui.findActor("return");
        edit = ui.findActor("edit");
        rename = ui.findActor("rename");
        ui.onButtonPress("return", new Runnable() {
            @Override
            public void run() {
                DeckSelectScene.this.back();
            }
        });
        ui.onButtonPress("edit", new Runnable() {
            @Override
            public void run() {
                DeckSelectScene.this.edit();
            }
        });
        ui.onButtonPress("rename", new Runnable() {
            @Override
            public void run() {
                textInput.setText(Current.player().getSelectedDeck().getName());
                dialog.show(stage);
                stage.setKeyboardFocus(textInput);
            }
        });
        defColor = ui.findActor("return").getColor();

        ScrollPane scrollPane = ui.findActor("deckSlots");
        scrollPane.setActor(layout);
        if (!Forge.isLandscapeMode()) {
            float w = Scene.GetIntendedWidth();
            float sW = w - 20;
            float oX = w/2 - sW/2;
            float h = Scene.GetIntendedHeight();
            float sH = (h - 10)/12;
            scrollPane.setWidth(sW);
            scrollPane.setHeight(sH*11);
            scrollPane.setX(oX);
            float rW = (w - 20)/3;
            float rX = w/2 - rW/2;
            rename.setWidth(rW);
            rename.setHeight(20);
            rename.setX(rX);
            back.setWidth(rW);
            back.setHeight(20);
            back.setX(rename.getX()-rW);
            edit.setWidth(rW);
            edit.setHeight(20);
            edit.setX(rename.getRight());
        }

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
