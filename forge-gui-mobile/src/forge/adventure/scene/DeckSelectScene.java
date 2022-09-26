package forge.adventure.scene;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
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
    TextField textInput;
    Table layout;
    TextraLabel header;
    TextraButton back, edit, rename;
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

        textInput = Controls.newTextField("");
        back = ui.findActor("return");
        edit = ui.findActor("edit");
        rename = ui.findActor("rename");
        ui.onButtonPress("return", () -> DeckSelectScene.this.back());
        ui.onButtonPress("edit", () -> DeckSelectScene.this.edit());
        ui.onButtonPress("rename", () -> {
            textInput.setText(Current.player().getSelectedDeck().getName());
            showRenameDialog();
        });
        defColor = ui.findActor("return").getColor();

        scrollPane = ui.findActor("deckSlots");
        scrollPane.setActor(layout);
    }

    private void showRenameDialog() {

        Dialog dialog = prepareDialog(Forge.getLocalizer().getMessage("lblRenameDeck"),ButtonOk|ButtonAbort,()->DeckSelectScene.this.rename());
        dialog.getContentTable().add(Controls.newLabel(Forge.getLocalizer().getMessage("lblNameYourSaveFile"))).colspan(2);
        dialog.getContentTable().row();
        dialog.getContentTable().add(Controls.newLabel(Forge.getLocalizer().getMessage("lblName")+": ")).align(Align.left);
        dialog.getContentTable().add(textInput).fillX().expandX();
        dialog.getContentTable().row();
        showDialog(dialog);
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
        addToSelectable(new Selectable(button));
        layout.row();
        return button;
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
    public void enter() {
        for (int i = 0; i < AdventurePlayer.NUMBER_OF_DECKS; i++) {
            if (buttons.containsKey(i)) {
                buttons.get(i).setText(Current.player().getDeck(i).getName());
                buttons.get(i).getTextraLabel().layout();
            }
        }
        select(Current.player().getSelectedDeckIndex());
        super.enter();
    }



    private void rename() {
        String text = textInput.getText();
        Current.player().renameDeck(text);
        buttons.get(currentSlot).setText(Current.player().getDeck(currentSlot).getName());
        buttons.get(currentSlot).getTextraLabel().layout();
    }

    private void edit() {
        Forge.switchScene(DeckEditScene.getInstance());
    }
}
