package forge.adventure.scene;

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
import forge.adventure.stage.GameHUD;
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
    Dialog renameDialog;

    private static DeckSelectScene object;

    public static DeckSelectScene instance() {
        if (object == null)
            object = new DeckSelectScene();
        return object;
    }

    public DeckSelectScene() {
        super(Forge.isLandscapeMode() ? "ui/deck_selector.json" : "ui/deck_selector_portrait.json");

        Window window = ui.findActor("deckSlots");
        Table root = new Table();
        layout = new Table();
        scrollPane = new ScrollPane(layout);
        header = Controls.newTextraLabel(Forge.getLocalizer().getMessage("lblSelectYourDeck"));
        root.row();
        root.add(header).colspan(2);
        root.row();
        root.add(scrollPane).expand().width(window.getWidth() - 20);
        for (int i = 0; i < AdventurePlayer.NUMBER_OF_DECKS; i++)
            addDeckSlot(Forge.getLocalizer().getMessage("lblDeck") + ": " + (i + 1), i);

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
        ui.onButtonPress("copy", DeckSelectScene.this::copy);
        ui.onButtonPress("delete", DeckSelectScene.this::maybeDelete);
        defColor = ui.findActor("return").getColor();
        window.add(root);
    }

    private void copy() {
        if (Current.player().isEmptyDeck(currentSlot)) return;
        int index = Current.player().copyDeck();
        if (index == -1) {
            showDialog(createGenericDialog(Forge.getLocalizer().getMessage("lblCopy"), Forge.getLocalizer().getMessage("lblNoAvailableSlots"),
                Forge.getLocalizer().getMessage("lblOk"),
                null, this::removeDialog, null));
        }
        else {
            updateDeckButton(index);
            select(index);
            scrollPane.scrollTo(buttons.get(index).getX(), buttons.get(index).getY(), 0, 0);
        }
    }

    private void maybeDelete() {
        if (Current.player().isEmptyDeck(currentSlot)) return;
        Dialog deleteDialog = createGenericDialog(Forge.getLocalizer().getMessage("lblDelete"), Forge.getLocalizer().getMessage("lblAreYouSureProceedDelete"),
            Forge.getLocalizer().getMessage("lblOk"),
            Forge.getLocalizer().getMessage("lblAbort"), this::delete, this::removeDialog);

        showDialog(deleteDialog);
    }

    private void delete() {
        Current.player().deleteDeck();
        updateDeckButton(currentSlot);
        removeDialog();
    }

    private void updateDeckButton(int index) {
        buttons.get(index).setText(Current.player().getDeck(index).getName());
        buttons.get(index).getTextraLabel().layout();
        buttons.get(index).layout();
    }

    private void showRenameDialog() {
        if (renameDialog == null) {
            renameDialog = createGenericDialog(Forge.getLocalizer().getMessage("lblRenameDeck"), null,
                    Forge.getLocalizer().getMessage("lblOk"),
                    Forge.getLocalizer().getMessage("lblAbort"), () -> {
                        this.rename();
                        removeDialog();
                    }, this::removeDialog);
            renameDialog.getContentTable().add(Controls.newLabel(Forge.getLocalizer().getMessage("lblNewNameDeck"))).colspan(2);
            renameDialog.getContentTable().row();
            renameDialog.getContentTable().add(Controls.newLabel(Forge.getLocalizer().getMessage("lblName") + ": ")).align(Align.left);
            renameDialog.getContentTable().add(textInput).fillX().expandX();
            renameDialog.getContentTable().row();
        }
        showDialog(renameDialog);
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

        layout.add(Controls.newLabel(name)).pad(2);
        layout.add(button).fill(true, false).expand(true, false).align(Align.left).expandX().pad(2);
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
                buttons.get(i).layout();
            }
        }
        GameHUD.getInstance().switchAudio();
        select(Current.player().getSelectedDeckIndex());
        performTouch(scrollPane); //can use mouse wheel if available to scroll after selection
        super.enter();
    }


    private void rename() {
        String text = textInput.getText();
        Current.player().renameDeck(text);
        buttons.get(currentSlot).setText(Current.player().getDeck(currentSlot).getName());
        buttons.get(currentSlot).getTextraLabel().layout();
        buttons.get(currentSlot).layout();
    }

    private void edit() {
        Forge.switchScene(DeckEditScene.getInstance());
    }
}
