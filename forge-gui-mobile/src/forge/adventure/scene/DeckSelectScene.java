package forge.adventure.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.Timer;
import com.github.tommyettinger.textra.TextraButton;
import com.github.tommyettinger.textra.TextraLabel;
import forge.Forge;
import forge.adventure.player.AdventurePlayer;
import forge.adventure.stage.GameHUD;
import forge.adventure.util.AdventureDialogUtil;
import forge.adventure.util.AdventureFilePicker;
import forge.adventure.util.CardUtil;
import forge.adventure.util.Controls;
import forge.adventure.util.Current;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class DeckSelectScene extends UIScene {
    private final IntMap<TextraButton> buttons = new IntMap<>();
    private final IntMap<Label> labels = new IntMap<>();
    Color defColor;
    TextField textInput;
    Table layout;
    TextraLabel header;
    TextraButton back, edit, rename, add;
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
        this.layoutDeckButtons();

        textInput = Controls.newTextField("");

        root.row();
        Table fileOpsTable = new Table();
        fileOpsTable.defaults().pad(0.5f);
        float opsBtnH = AdventureDialogUtil.deckSelectFileOpsButtonHeight();
        fileOpsTable.add(deckSelectOpsButton(
            Forge.getLocalizer().getMessage("lblAdvImportDeck"), this::importDeck, opsBtnH))
            .height(opsBtnH).maxHeight(opsBtnH).expandX().fillX();
        fileOpsTable.add(deckSelectOpsButton(
            Forge.getLocalizer().getMessage("lblAdvExportDeck"), this::exportDeck, opsBtnH))
            .height(opsBtnH).maxHeight(opsBtnH).expandX().fillX();
        fileOpsTable.row();
        fileOpsTable.add(deckSelectOpsButton(
            Forge.getLocalizer().getMessage("lblAdvExportCollection"), this::exportCollection, opsBtnH))
            .height(opsBtnH).maxHeight(opsBtnH).expandX().fillX();
        fileOpsTable.add(deckSelectOpsButton(
            Forge.getLocalizer().getMessage("lblAdvMarkForSale"), this::markForSale, opsBtnH))
            .height(opsBtnH).maxHeight(opsBtnH).expandX().fillX();
        root.add(fileOpsTable).colspan(2).fillX().padTop(1f);

        back = ui.findActor("return");
        edit = ui.findActor("edit");
        rename = ui.findActor("rename");
        add = ui.findActor("add");
        ui.onButtonPress("return", DeckSelectScene.this::back);
        ui.onButtonPress("edit", DeckSelectScene.this::edit);
        ui.onButtonPress("rename", () -> {
            textInput.setText(Current.player().getSelectedDeck().getName());
            showRenameDialog();
        });
        ui.onButtonPress("copy", DeckSelectScene.this::copy);
        ui.onButtonPress("delete", DeckSelectScene.this::promptDelete);
        ui.onButtonPress("add", DeckSelectScene.this::addDeck);
        defColor = ui.findActor("return").getColor();
        window.add(root);
    }

    private void refreshDeckButtons(){
        clearDeckButtons();
        layoutDeckButtons();
    }

    private void clearDeckButtons(){
        int count = AdventurePlayer.current().getDeckCount();
        for (int i = count; i >= 0; i--){
            clearDeckButton(i);
        }
        layout.clearChildren();
        buttons.clear();
        labels.clear();
    }

    private void layoutDeckButtons() {
        for (int i = 0; i < AdventurePlayer.current().getDeckCount(); i++)
            addDeckButton(i);
    }

    private void addDeck(){
        if (Current.player().getDeckCount() >= Current.player().getMaxDeckCount()){
            showDialog(createGenericDialog(Forge.getLocalizer().getMessage("lblAddDeck"), Forge.getLocalizer().getMessage("lblMaxDeckCountReached"),
                    Forge.getLocalizer().getMessage("lblOK"), null, this::removeDialog, null));
            return;
        }

        Current.player().addDeck();
        refreshDeckButtons();
        select(Current.player().getSelectedDeckIndex());
    }

    private void copy() {
        if (Current.player().isEmptyDeck(currentSlot)) return;
        int index = Current.player().copyDeck();
        if (index == -1) {
            showDialog(createGenericDialog(Forge.getLocalizer().getMessage("lblCopy"), Forge.getLocalizer().getMessage("lblNoAvailableSlots"),
                Forge.getLocalizer().getMessage("lblOK"),
                null, this::removeDialog, null));
        }
        else {
            updateDeckButton(index);
            select(index);
            scrollPane.scrollTo(buttons.get(index).getX(), buttons.get(index).getY(), 0, 0);
        }
    }

    private void promptDelete() {
        Dialog deleteDialog = createGenericDialog(Forge.getLocalizer().getMessage("lblDelete"), Forge.getLocalizer().getMessage("lblAreYouSureProceedDelete"),
            Forge.getLocalizer().getMessage("lblOK"),
            Forge.getLocalizer().getMessage("lblAbort"), this::clearOrDelete, this::removeDialog);

        showDialog(deleteDialog);
    }

    /**
     * Clears or deletes the currently selected deck.
     */
    private void clearOrDelete(){
        if (currentSlot >= AdventurePlayer.MIN_DECK_COUNT){
            Current.player().deleteDeck();
        }
        else {
            Current.player().clearDeck();
        }

        refreshDeckButtons();
        select(0);
        removeDialog();
    }

    private void clear() {
        Current.player().clearDeck();
        updateDeckButton(currentSlot);
        removeDialog();
    }

    private void updateDeckButton(int index) {
        if (!buttons.containsKey(index)) addDeckButton(index);
        buttons.get(index).setText(Current.player().getDeck(index).getName());
        buttons.get(index).getTextraLabel().layout();
        buttons.get(index).layout();
    }

    private void showRenameDialog() {
        if (renameDialog == null) {
            renameDialog = createGenericDialog(Forge.getLocalizer().getMessage("lblRenameDeck"), null,
                    Forge.getLocalizer().getMessage("lblOK"),
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

    /**
     * Not sure if this is strictly necessary in Java but wouldn't want to leak before clearing the layout table.
     */
    private void clearDeckButton(int i){
        if (buttons.containsKey(i)) {
            TextraButton button = buttons.remove(i);
            button.clearListeners();
            button.clearActions();
        }
    }

    /** Compact file-op buttons under the deck scroll list (smaller than dialog option rows). */
    private static TextraButton deckSelectOpsButton(String text, Runnable onClick, float height) {
        TextraButton button = Controls.newTextButton("[%58]" + text, onClick);
        button.getTextraLabel().layout.setTargetWidth(Scene.getIntendedWidth() * 0.2f);
        button.setHeight(height);
        return button;
    }

    private TextraButton addDeckButton(int i) {
        float rowH = AdventureDialogUtil.compactButtonHeight();
        TextraButton button = Controls.newTextButton("-");
        String name = Forge.getLocalizer().getMessage("lblDeck") + ": " + (i + 1);
        button.getTextraLabel().layout.setTargetWidth(Scene.getIntendedWidth() * 0.42f);
        button.setHeight(rowH);
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

        button.setText(Current.player().getDeck(i).getName());
        Label label = Controls.newLabel(name);
        layout.add(label).pad(1);
        layout.add(button).height(rowH).fillX().expandX().align(Align.left).pad(1);
        buttons.put(i, button);
        labels.put(i, label);
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
    protected void onDialogClosed() {
        restoreDeckListScrollFocus();
    }

    private void restoreDeckListScrollFocus() {
        if (scrollPane != null) {
            stage.setScrollFocus(scrollPane);
            stage.setKeyboardFocus(scrollPane);
        }
    }

    @Override
    public void enter() {
        refreshDeckButtons();
        GameHUD.getInstance().updateBGM();
        select(Current.player().getSelectedDeckIndex());
        performTouch(scrollPane); //can use mouse wheel if available to scroll after selection
        restoreDeckListScrollFocus();
        super.enter();
    }


    private void rename() {
        String text = textInput.getText();
        Current.player().renameDeck(text);
        updateDeckButton(currentSlot);
    }

    private void edit() {
        DeckEditScene editScene = DeckEditScene.getInstance();
        editScene.loadEvent(null);
        Forge.switchScene(editScene);
    }

    private void importDeck() {
        var loc = Forge.getLocalizer();
        Dialog modeDialog = AdventureDialogUtil.buildImportDeckDialog(
            loc.getMessage("lblAdvDeckImportIntro"),
            loc.getMessage("lblAdvChooseImportMode"),
            loc.getMessage("lblAdvDeckImportUseOwned"),
            () -> {
                removeDialog();
                pickImportFile(CardUtil.ImportMode.REPORT_ONLY);
            },
            loc.getMessage("lblAdvDeckImportBuyMissing"),
            () -> {
                removeDialog();
                pickImportFile(CardUtil.ImportMode.BUY_MISSING);
            },
            loc.getMessage("lblAdvDeckImportGiveMissing"),
            () -> {
                removeDialog();
                pickImportFile(CardUtil.ImportMode.GIVE_MISSING);
            },
            this::removeDialog);
        showDialog(modeDialog);
    }

    private void pickImportFile(CardUtil.ImportMode mode) {
        var loc = Forge.getLocalizer();
        AdventureFilePicker.chooseOpenFile(loc.getMessage("lblAdvImportDeck"), file -> {
            if (!file.exists()) {
                removeAllDialogs();
                showOkDialog(loc.getMessage("lblAdvImportDeck"),
                    loc.getMessage("lblAdvFileNotFound", file.getAbsolutePath()));
                return;
            }
            if (mode == CardUtil.ImportMode.BUY_MISSING) {
                confirmBuyImport(file);
            } else {
                handleImportResult(CardUtil.importDeckFromFile(file, Current.player(), mode), mode);
            }
        });
    }

    private void confirmBuyImport(File file) {
        var loc = Forge.getLocalizer();
        String title = loc.getMessage("lblAdvImportDeck");
        AdventurePlayer player = Current.player();
        CardUtil.ImportPreview preview = CardUtil.previewImport(file, player);
        if (preview == null) {
            showOkDialog(title, loc.getMessage("lblAdvImportDeckFailed", "Could not parse file"));
            return;
        }
        if (preview.missingCards.isEmpty()) {
            handleImportResult(CardUtil.importDeckFromFile(file, player, CardUtil.ImportMode.BUY_MISSING),
                CardUtil.ImportMode.BUY_MISSING);
            return;
        }
        int gold = player.getGold();
        if (!preview.canAfford(gold)) {
            showOkDialog(title, loc.getMessage("lblAdvBuyNotEnoughGold",
                preview.totalCost, gold));
            return;
        }
        String message = loc.getMessage("lblAdvBuyConfirm",
            preview.missingCopyCount(), preview.missingCards.size(),
            preview.totalCost, gold);
        Dialog confirm = AdventureDialogUtil.buildConfirmDialog(title, message,
            loc.getMessage("lblOK"),
            () -> {
                removeDialog();
                handleImportResult(CardUtil.importDeckFromFile(file, player, CardUtil.ImportMode.BUY_MISSING),
                    CardUtil.ImportMode.BUY_MISSING);
            },
            this::removeDialog);
        showDialog(confirm);
    }

    private void handleImportResult(CardUtil.ImportResult result, CardUtil.ImportMode mode) {
        var loc = Forge.getLocalizer();
        String title = loc.getMessage("lblAdvImportDeck");
        if (!result.success) {
            removeAllDialogs();
            showOkDialog(title, loc.getMessage("lblAdvImportDeckFailed", result.message));
            return;
        }
        if (result.importedDeck()) {
            refreshDeckButtons();
            select(result.slot);
            showBriefToast(loc.getMessage("lblAdvImportDone"));
            return;
        }
        showMissingCardsDialog(result);
    }

    private void exportDeck() {
        String deckName = Current.player().getSelectedDeck().getName();
        String baseName = deckName != null && !deckName.isEmpty() ? deckName : "deck";
        var loc = Forge.getLocalizer();
        chooseExportFormat(loc.getMessage("lblAdvExportDeck"), CardUtil.DeckListExportFormat.values(), format -> {
            String defaultName = CardUtil.defaultSaveFilename(baseName, format.getDefaultExtension());
            AdventureFilePicker.chooseSaveFile(loc.getMessage("lblAdvExportDeck"), defaultName, file -> {
                try {
                    CardUtil.exportDeck(Current.player().getSelectedDeck(), file, format);
                    showSavedThenReturnToDeckList();
                } catch (Exception e) {
                    removeAllDialogs();
                    showOkDialog(loc.getMessage("lblAdvExportDeck"),
                        loc.getMessage("lblAdvMissingListSaveFailed", e.getMessage()));
                }
            });
        });
    }

    private void exportCollection() {
        var loc = Forge.getLocalizer();
        chooseExportFormat(loc.getMessage("lblAdvExportCollection"), CardUtil.CollectionExportFormat.values(), format -> {
            String defaultName = CardUtil.defaultSaveFilename("collection", format.getDefaultExtension());
            AdventureFilePicker.chooseSaveFile(loc.getMessage("lblAdvExportCollection"), defaultName, file -> {
                try {
                    CardUtil.exportCollection(Current.player(), file, format);
                    showSavedThenReturnToDeckList();
                } catch (Exception e) {
                    removeAllDialogs();
                    showOkDialog(loc.getMessage("lblAdvExportCollection"),
                        loc.getMessage("lblAdvMissingListSaveFailed", e.getMessage()));
                }
            });
        });
    }

    private void markForSale() {
        var loc = Forge.getLocalizer();
        AdventureFilePicker.chooseOpenFile(loc.getMessage("lblAdvMarkForSale"),
            loc.getMessage("lblAdvMarkForSaleHelp"), file -> {
            if (!file.exists()) {
                removeAllDialogs();
                showOkDialog(loc.getMessage("lblAdvMarkForSale"),
                    loc.getMessage("lblAdvFileNotFound", file.getAbsolutePath()));
                return;
            }
            CardUtil.MarkSellResult result = CardUtil.markCardsForSale(file, Current.player());
            if (!result.success) {
                removeAllDialogs();
                showOkDialog(loc.getMessage("lblAdvMarkForSale"),
                    loc.getMessage("lblAdvMarkForSaleFailed", result.message));
            } else {
                showMarkForSaleResult(result);
            }
        });
    }

    private void showMarkForSaleResult(CardUtil.MarkSellResult result) {
        var loc = Forge.getLocalizer();
        String title = loc.getMessage("lblAdvMarkForSale");
        String body = result.formatMarkedReport(loc.getMessage("lblAdvLoaded"));
        ScrollPane[] scroll = new ScrollPane[1];
        Runnable onRollback = result.rollbackOps.isEmpty() ? null : () -> {
            result.rollback(Current.player());
            removeDialog();
            showBriefToast(loc.getMessage("lblAdvRolledBack"));
        };
        Dialog dialog = AdventureDialogUtil.buildScrollableOkDialog(title, body,
            this::removeDialog, onRollback, scroll);
        removeAllDialogs();
        showDialog(dialog, scroll[0]);
    }

    private <T extends Enum<T>> void chooseExportFormat(String title, T[] formats, Consumer<T> onChosen) {
        var loc = Forge.getLocalizer();
        List<AdventureDialogUtil.ChoiceOption> options = new ArrayList<>(formats.length);
        for (T format : formats) {
            T chosen = format;
            options.add(new AdventureDialogUtil.ChoiceOption(formatLabel(format), () -> {
                removeDialog();
                onChosen.accept(chosen);
            }));
        }
        Dialog dialog = AdventureDialogUtil.buildChoiceDialog(title,
            loc.getMessage("lblAdvChooseExportFormat"), options, this::removeDialog);
        showDialog(dialog);
    }

    private static String formatLabel(Enum<?> format) {
        if (format instanceof CardUtil.DeckListExportFormat df) {
            return df.getDisplayName();
        }
        if (format instanceof CardUtil.CollectionExportFormat cf) {
            return cf.getDisplayName();
        }
        return format.name();
    }

    private void showResultDialog(String title, String message) {
        showDialog(AdventureDialogUtil.buildMessageDialog(title, message, this::removeDialog));
    }

    private void showOkDialog(String title, String message) {
        var loc = Forge.getLocalizer();
        showDialog(AdventureDialogUtil.buildMessageDialog(title, message,
            loc.getMessage("lblOK"), this::removeDialog));
    }

    private void showSavedThenReturnToDeckList() {
        showBriefToast(Forge.getLocalizer().getMessage("lblAdvSaved"));
    }

    private void showBriefToast(String message) {
        removeAllDialogs();
        showDialog(AdventureDialogUtil.buildTransientMessage(message));
        Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                Gdx.app.postRunnable(DeckSelectScene.this::removeDialog);
            }
        }, 3f);
    }

    private void showMissingCardsDialog(CardUtil.ImportResult result) {
        var loc = Forge.getLocalizer();
        String title = loc.getMessage("lblAdvImportDeck");
        String body = result.formatMissingReport();
        if (result.missingCards == null || result.missingCards.isEmpty()) {
            showResultDialog(title, body);
            return;
        }
        String safeName = result.deckName != null
            ? result.deckName.replaceAll("[^a-zA-Z0-9._-]+", "_") : "deck";
        String defaultFile = "missing_" + safeName + ".txt";
        ScrollPane[] scroll = new ScrollPane[1];
        Dialog dialog = AdventureDialogUtil.buildScrollableReportDialog(title, body,
            loc.getMessage("lblSave"),
            () -> {
                removeDialog();
                AdventureFilePicker.chooseSaveFile(title, defaultFile, file -> {
                    try {
                        forge.util.FileUtil.writeFile(file, body);
                        showSavedThenReturnToDeckList();
                    } catch (Exception e) {
                        removeAllDialogs();
                        showOkDialog(title, loc.getMessage("lblAdvMissingListSaveFailed",
                            e.getMessage()));
                    }
                });
            },
            this::removeDialog, scroll);
        showDialog(dialog, scroll[0]);
    }
}
