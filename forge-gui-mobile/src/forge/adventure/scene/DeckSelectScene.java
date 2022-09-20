package forge.adventure.scene;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.IntMap;
import com.github.tommyettinger.textra.TextraButton;
import com.github.tommyettinger.textra.TextraLabel;
import forge.Forge;
import forge.adventure.player.AdventurePlayer;
import forge.adventure.util.Controls;
import forge.adventure.util.Current;

public class DeckSelectScene extends UIScene {
    private final IntMap<TextraButton> buttons = new IntMap<>();
    Color defColor;
    Dialog dialog;
    TextField textInput;
    Table layout;
    TextraLabel header;
    TextraButton back, edit, rename, dialogRenameBtn, dialogAbortBtn;
    int currentSlot = 0;
    ScrollPane scrollPane;

    private static DeckSelectScene object;

    public static DeckSelectScene instance() {
        if(object==null)
            object=new DeckSelectScene();
        return object;
    }
    public DeckSelectScene() {
        super(Forge.isLandscapeMode() ? "ui/deck_selector.json" : "ui/deck_selector_portrait.json");

        layout = new Table();
        stage.addActor(layout);

        header = Controls.newTextraLabel(Forge.getLocalizer().getMessage("lblSelectYourDeck"));
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
        edit = ui.findActor("edit");
        rename = ui.findActor("rename");
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

    private TextraButton addDeckSlot(String name, int i) {
        TextraButton button = Controls.newTextButton("-");
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

        for (IntMap.Entry<TextraButton> butt : new IntMap.Entries<TextraButton>(buttons)) {
            butt.value.setColor(defColor);
        }
        if (buttons.containsKey(slot)) {
            TextraButton button = buttons.get(slot);
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
        clearActorObjects();
        for (int i = 0; i < AdventurePlayer.NUMBER_OF_DECKS; i++) {
            if (buttons.containsKey(i)) {
                buttons.get(i).setText(Current.player().getDeck(i).getName());
                buttons.get(i).getTextraLabel().layout();
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



    private void rename() {
        dialog.hide();
        String text = textInput.getText();
        Current.player().renameDeck(text);
        buttons.get(currentSlot).setText(Current.player().getDeck(currentSlot).getName());
        buttons.get(currentSlot).getTextraLabel().layout();
    }

    private void edit() {
        Forge.switchScene(DeckEditScene.getInstance());
    }
}
