package forge.adventure.scene;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.github.tommyettinger.textra.TextraButton;
import com.github.tommyettinger.textra.TextraLabel;
import forge.Forge;
import forge.adventure.data.ItemData;
import forge.adventure.stage.ConsoleCommandInterpreter;
import forge.adventure.stage.GameHUD;
import forge.adventure.stage.MapStage;
import forge.adventure.util.*;
import forge.deck.Deck;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class InventoryScene extends UIScene {
    TextraButton leave;
    Button equipButton;
    TextraButton useButton;
    TextraLabel itemDescription;
    private final Table inventory;
    private final Array<Button> inventoryButtons = new Array<>();
    private final HashMap<String, Button> equipmentSlots = new HashMap<>();
    HashMap<Button, Pair<String, ItemData>> itemLocation = new HashMap<>();
    HashMap<Button, Deck> deckLocation = new HashMap<>();
    Button selected;
    Button deleteButton;
    TextraButton repairButton;
    Texture equipOverlay, unusableOverlay;
    Dialog useDialog, deleteDialog;
    int columns = 0;
    private String selectedSlot = null;
    private NinePatchDrawable slotBorderDrawable = null;
    private static final String SLOT_BORDER_NAME = "slotBorder";
    private static final String SLOT_ITEM_NAME = "slotItem";

    private NinePatchDrawable getSlotBorderDrawable() {
        if (slotBorderDrawable == null) {
            int border = 4;
            int size = border * 2 + 2; // 10px total; center is 2x2 transparent
            Pixmap pm = new Pixmap(size, size, Pixmap.Format.RGBA8888);
            pm.setColor(new Color(1f, 0.9f, 0.05f, 1f)); // bright yellow
            pm.fill();
            pm.setBlending(Pixmap.Blending.None); // write transparent pixels directly, no alpha blending
            pm.setColor(0f, 0f, 0f, 0f);
            pm.fillRectangle(border, border, size - border * 2, size - border * 2);
            Texture tex = new Texture(pm);
            pm.dispose();
            slotBorderDrawable = new NinePatchDrawable(new NinePatch(tex, border, border, border, border));
        }
        return slotBorderDrawable;
    }

    private void addSlotBorder(Button button) {
        Image border = new Image(getSlotBorderDrawable());
        border.setName(SLOT_BORDER_NAME);
        border.setSize(button.getWidth(), button.getHeight());
        button.addActor(border);
    }

    private void removeSlotBorder(Button button) {
        Actor border = button.findActor(SLOT_BORDER_NAME);
        if (border != null) border.remove();
    }

    public InventoryScene() {
        super(Forge.isLandscapeMode() ? "ui/inventory.json" : "ui/inventory_portrait.json");
        equipOverlay = Forge.getAssets().getTexture(Config.instance().getFile(Paths.ITEMS_EQUIP));
        unusableOverlay = Forge.getAssets().getTexture(Config.instance().getFile(Paths.ITEMS_UNUSABLE));
        ui.onButtonPress("return", this::done);
        leave = ui.findActor("return");
        repairButton = ui.findActor("repair");
        ui.onButtonPress("repair", this::repair);
        ui.onButtonPress("delete", this::showConfirm);
        ui.onButtonPress("equip", this::equip);
        ui.onButtonPress("use", this::use);
        equipButton = ui.findActor("equip");
        useButton = ui.findActor("use");
        useButton.setDisabled(true);
        deleteButton = ui.findActor("delete");
        itemDescription = ui.findActor("item_description");
        itemDescription.setAlignment(Align.topLeft);
        itemDescription.setWrap(true);
        ScrollPane pane = new ScrollPane(itemDescription);
        pane.setBounds(itemDescription.getX(), itemDescription.getY(), itemDescription.getWidth() - 5, itemDescription.getHeight() - 8);
        ui.addActor(pane);

        Array<Actor> children = ui.getChildren();
        for (int i = 0, n = children.size; i < n; i++) {
            if (children.get(i).getName() != null && children.get(i).getName().startsWith("Equipment")) {
                String slotName = children.get(i).getName().split("_")[1];
                equipmentSlots.put(slotName, (Button) children.get(i));
                Actor slot = children.get(i);
                slot.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        Button button = ((Button) actor);
                        if (button.isChecked()) {
                            for (Button otherButton : equipmentSlots.values()) {
                                if (button != otherButton && otherButton.isChecked()) {
                                    otherButton.setChecked(false);
                                }
                            }
                            selectedSlot = slotName;
                            updateInventory();
                            Long id = Current.player().itemInSlot(slotName);
                            if (id != null) {
                                Button changeButton = null;
                                for (Button invButton : inventoryButtons) {
                                    if (itemLocation.get(invButton) == null)
                                        continue;
                                    ItemData data = itemLocation.get(invButton).getRight();
                                    if (data != null && id.equals(data.longID)) {
                                        changeButton = invButton;
                                        break;
                                    }
                                }
                                if (changeButton != null)
                                    changeButton.setChecked(true);
                            } else {
                                setSelected(null);
                            }
                        } else {
                            removeSlotBorder(button);
                            boolean anyChecked = false;
                            for (Button otherButton : equipmentSlots.values()) {
                                if (otherButton.isChecked()) {
                                    anyChecked = true;
                                    break;
                                }
                            }
                            if (!anyChecked) {
                                selectedSlot = null;
                                updateInventory();
                            }
                        }
                    }
                });
            }
        }
        inventory = new Table(Controls.getSkin());
        ScrollPane scrollPane = ui.findActor("inventory");
        scrollPane.setScrollingDisabled(true, false);
        scrollPane.setActor(inventory);
        columns = (int) (scrollPane.getWidth() / createInventorySlot().getWidth());
        columns -= 1;
        if (columns <= 0) columns = 1;
        scrollPane.setActor(inventory);
    }

    private void showConfirm() {
        if (deleteDialog == null) {
            deleteDialog = createGenericDialog("", Forge.getLocalizer().getMessage("lblDelete"),
                Forge.getLocalizer().getMessage("lblYes"),
                Forge.getLocalizer().getMessage("lblNo"), () -> {
                    this.delete();
                    removeDialog();
                }, this::removeDialog);
        }
        showDialog(deleteDialog);
    }

    private void repair() {
        if (selected == null)
            return;
        if (itemLocation.get(selected) == null)
            return;
        ItemData data = itemLocation.get(selected).getRight();
        if (data == null)
            return;
        int initialCost;
        try {
            //TODO apply modifiers from reputation..
            initialCost = (int) (data.cost * 0.4f);
        } catch (Exception e) {
            initialCost = 500;
        }
        if (Current.player().getGold() < initialCost) {
            showDialog(createGenericDialog("", Forge.getLocalizer().getMessage("lblNotEnoughCredits") + "\n[+GoldCoin] " + initialCost,
                Forge.getLocalizer().getMessage("lblOK"), null, this::removeDialog, null));
            return;
        }
        final int cost = initialCost;
        showDialog(createGenericDialog("", "[+" + data.iconName + "] " + data.name + "\n" +
            Forge.getLocalizer().getMessage("lblRepairCost", "[+GoldCoin] " + cost),
            Forge.getLocalizer().getMessage("lblYes"),
            Forge.getLocalizer().getMessage("lblNo"), () -> {
                if (data.isCracked) {
                    data.isCracked = false;
                    updateInventory();
                    setSelected(selected);
                    Current.player().takeGold(cost);
                }
                removeDialog();
            }, this::removeDialog)
        );
    }

    private static InventoryScene object;

    public static InventoryScene instance() {
        if (object == null)
            object = new InventoryScene();
        return object;
    }


    public void done() {
        selectedSlot = null;
        for (Button slot : equipmentSlots.values()) {
            removeSlotBorder(slot);
            slot.setChecked(false);
        }
        GameHUD.getInstance().getTouchpad().setVisible(false);
        Forge.switchToLast();
    }

    public void delete() {
        if (selected == null)
            return;
        if (itemLocation.get(selected) == null)
            return;
        ItemData data = itemLocation.get(selected).getRight();
        if (data != null) {
            data.isEquipped = false;
            Current.player().removeItem(data);
        }
        updateInventory();

    }

    public void equip() {
        if (selected == null)
            return;
        if (itemLocation.get(selected) == null)
            return;
        ItemData data = itemLocation.get(selected).getRight();
        if (data == null) return;
        Current.player().equip(data);
        updateInventory();
    }

    @Override
    public void act(float delta) {
        stage.act(delta);
    }

    private void triggerUse() {
        if (selected == null)
            return;
        if (itemLocation.get(selected) == null)
            return;
        ItemData data = itemLocation.get(selected).getRight();
        if (data == null) return;
        Current.player().addShards(-data.shardsNeeded);
        done();
        if (data.commandOnUse != null && !data.commandOnUse.isEmpty())
            ConsoleCommandInterpreter.getInstance().command(data.commandOnUse);
        if (data.dialogOnUse != null && data.dialogOnUse.text != null && !data.dialogOnUse.text.isEmpty()) {
            MapDialog dialog = new MapDialog(data.dialogOnUse, MapStage.getInstance(),0,null);
            MapStage.getInstance().showDialog();
            dialog.activate();
            ChangeListener listen = new ChangeListener() {
                @Override
                public void changed(ChangeEvent changeEvent, Actor actor) {
                    AdventureQuestController.instance().showQuestDialogs(MapStage.getInstance());
                }
            };
            dialog.addDialogCompleteListener(listen);
        }
        AdventureQuestController.instance().updateItemUsed(data);
    }

    private void openBooster() {
        if (selected == null) return;

        Deck data = (deckLocation.get(selected));
        if (data == null) return;

        //done();
        setSelected(null);
        RewardScene.instance().loadRewards(data, RewardScene.Type.Loot, null, data.getTags().contains("noSell"));
        Forge.switchScene(RewardScene.instance());
        Current.player().getBoostersOwned().removeValue(data, true);
    }

    private void use() {
        if (itemLocation.containsKey(selected)) {
            ItemData data = itemLocation.get(selected).getRight();
            if (data == null)
                return;
            if (useDialog == null) {
                useDialog = createGenericDialog("", null, Forge.getLocalizer().getMessage("lblYes"),
                        Forge.getLocalizer().getMessage("lblNo"), () -> {
                            this.triggerUse();
                            removeDialog();
                        }, this::removeDialog);
                useDialog.getContentTable().add(Controls.newTextraLabel("Use " + data.name + "?\n" + data.getDescription()));
            }
            showDialog(useDialog);
        }
        if (deckLocation.containsKey(selected)){
            Deck data = deckLocation.get(selected);
            if (data == null)
                return;
            this.openBooster();
        }
    }

    public void clearItemDescription() {
        itemDescription.setText("");
    }
    private void setSelected(Button actor) {
        selected = actor;
        if (actor == null) {
            clearItemDescription();
            deleteButton.setDisabled(true);
            equipButton.setDisabled(true);
            useButton.setDisabled(true);
            repairButton.setVisible(false);
            for (Button button : inventoryButtons) {
                button.setChecked(false);
            }
            return;
        }
        if (itemLocation.containsKey(actor)) {
            ItemData data = itemLocation.get(actor).getRight();
            if (data == null) return;

            deleteButton.setDisabled(data.questItem);

            boolean isInPoi = MapStage.getInstance().isInMap();
            useButton.setDisabled(!(isInPoi && data.usableInPoi || !isInPoi && data.usableOnWorldMap));
            if (data.shardsNeeded == 0)
                useButton.setText("Use");
            else
                useButton.setText("Use " + data.shardsNeeded + "[+Shards]");
            useButton.layout();
            if (Current.player().getShards() < data.shardsNeeded)
                useButton.setDisabled(true);

            if (data.equipmentSlot == null || data.equipmentSlot.isEmpty() || data.isCracked) {
                equipButton.setDisabled(true);
            } else {
                equipButton.setDisabled(false);
                if (equipButton instanceof TextraButton) {
                    TextraButton button = (TextraButton) equipButton;
                    Long id = Current.player().itemInSlot(data.equipmentSlot);
                    if (id != null && id.equals(data.longID) && data.isEquipped) {
                        button.setText("Unequip");
                    } else {
                        button.setText("Equip");
                    }
                    button.layout();
                }
            }
            repairButton.setVisible(data.isCracked);
            String status = data.isCracked ? " (" + Forge.getLocalizer().getMessage("lblCracked") + ")" : "";
            itemDescription.setText(data.name + status + "\n[%98]" + data.getDescription());
        }
        else if (deckLocation.containsKey(actor)){
            Deck data = (deckLocation.get(actor));
            if (data == null) return;

            deleteButton.setDisabled(true);
            useButton.setDisabled(false);
            useButton.setText("Open");
            useButton.layout();
            equipButton.setDisabled(true);
            repairButton.setVisible(false);

            itemDescription.setText(data.getName() + "\n[%98]" + (data.getComment() == null?"":data.getComment()+" - ") + data.getAllCardsInASinglePool(true, true).countAll() + " cards");
        }


        for (Button button : inventoryButtons) {
            if (actor != button && button.isChecked()) {
                button.setChecked(false);
            }
        }

        performTouch(scrollPaneOfActor(itemDescription)); //can use mouse wheel if available to scroll after selection
    }

    private void updateInventory() {
        clearSelectable();
        inventoryButtons.clear();
        inventory.clear();
        repairButton.setVisible(false);

        int itemSlotsUsed = 0;
        ArrayList<ItemData> items = new ArrayList<>();
        for (int i = 0; i < Current.player().getItems().size(); i++) {
            ItemData item = Current.player().getItems().get(i);
            if (item == null) {
                continue;
            }
            if (item.sprite() == null) {
                System.err.print("Can not find sprite name " + item.iconName + "\n");
                continue;
            }
            if (selectedSlot != null && !selectedSlot.equals(item.equipmentSlot)) {
                continue;
            }
            items.add(item);
        }
        // sort these by slot type and name
        items.sort((o1, o2) -> {
            if (o1.equipmentSlot == null && o2.equipmentSlot == null) {
                return o1.name.compareTo(o2.name);
            } else if (o1.equipmentSlot == null) {
                return 1;
            } else if (o2.equipmentSlot == null) {
                return -1;
            } else {
                int slotCompare = o1.equipmentSlot.compareTo(o2.equipmentSlot);
                if (slotCompare != 0) {
                    return slotCompare;
                }
                return o1.name.compareTo(o2.name);
            }
        });

        for (int i = 0; i < items.size(); i++) {
            if (i % columns == 0)
                inventory.row();
            Button newActor = createInventorySlot();
            inventory.add(newActor).top().left().space(1);
            addToSelectable(new Selectable(newActor) {
                @Override
                public void onSelect(UIScene scene) {
                    setSelected(newActor);
                    super.onSelect(scene);
                }
            });
            inventoryButtons.add(newActor);

            ItemData item = items.get(i);
            Image img = new Image(item.sprite());
            img.setX((newActor.getWidth() - img.getWidth()) / 2);
            img.setY((newActor.getHeight() - img.getHeight()) / 2);
            newActor.addActor(img);
            itemLocation.put(newActor, Pair.of(item.name, item));
            if (item.isEquipped && item.longID != null && Current.player().getEquippedItems().contains(item.longID)) {
                Image overlay = new Image(equipOverlay);
                overlay.setX((newActor.getWidth() - img.getWidth()) / 2);
                overlay.setY((newActor.getHeight() - img.getHeight()) / 2);
                newActor.addActor(overlay);
            } else if (item.isCracked) {
                Image overlay = new Image(unusableOverlay);
                overlay.setX((newActor.getWidth() - img.getWidth()) / 2);
                overlay.setY((newActor.getHeight() - img.getHeight()) / 2);
                newActor.addActor(overlay);
            }
            newActor.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if (((Button) actor).isChecked()) {
                        setSelected((Button) actor);
                    }
                }
            });
            itemSlotsUsed++;
        }

        if (selectedSlot == null) {
            for (int i = 0; i < Current.player().getBoostersOwned().size; i++) {
                if ((i + itemSlotsUsed) % columns == 0)
                    inventory.row();
                Button newActor = createInventorySlot();
                inventory.add(newActor).top().left().space(1);
                addToSelectable(new Selectable(newActor) {
                    @Override
                    public void onSelect(UIScene scene) {
                        setSelected(newActor);
                        super.onSelect(scene);
                    }
                });
                inventoryButtons.add(newActor);
                Deck deck = Current.player().getBoostersOwned().get(i);
                if (deck == null | deck.isEmpty()) {
                    System.err.print("Can not add null / empty booster " + Current.player().getBoostersOwned().get(i) + "\n");
                    continue;
                }
                Sprite deckSprite = Config.instance().getItemSprite("Deck");

                Image img = new Image(deckSprite);
                img.setX((newActor.getWidth() - img.getWidth()) / 2);
                img.setY((newActor.getHeight() - img.getHeight()) / 2);
                newActor.addActor(img);
                deckLocation.put(newActor, Current.player().getBoostersOwned().get(i));
                newActor.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        if (((Button) actor).isChecked()) {
                            setSelected((Button) actor);
                        }
                    }
                });
            }
        }

        for (Map.Entry<String, Button> slot : equipmentSlots.entrySet()) {
            Button slotButton = slot.getValue();
            // Remove the previous item image and border by name (order-independent)
            Actor oldItem = slotButton.findActor(SLOT_ITEM_NAME);
            if (oldItem != null) oldItem.remove();
            removeSlotBorder(slotButton);

            Long id = Current.player().itemInSlot(slot.getKey());
            if (id != null) {
                ItemData item = Current.player().getEquippedItem(id);
                if (item != null) {
                    Image img = new Image(item.sprite());
                    img.setName(SLOT_ITEM_NAME);
                    img.setX((slotButton.getWidth() - img.getWidth()) / 2);
                    img.setY((slotButton.getHeight() - img.getHeight()) / 2);
                    slotButton.addActor(img);
                }
            }
            // Re-add border on top if this slot is currently selected
            if (slot.getKey().equals(selectedSlot)) {
                addSlotBorder(slotButton);
            }
        }
        // make sure repair is clickable
        repairButton.setZIndex(ui.getChildren().size);
    }

    @Override
    public void enter() {
        selectedSlot = null;
        for (Button slot : equipmentSlots.values()) {
            removeSlotBorder(slot);
            slot.setChecked(false);
        }
        clearItemDescription();
        updateInventory();
        //inventory.add().expand();
        super.enter();
    }

    public Button createInventorySlot() {
        return new ImageButton(Controls.getSkin(), "item_frame");
    }
}
