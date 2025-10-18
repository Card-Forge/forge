package forge.adventure.util;

import forge.Forge;
import forge.adventure.data.ItemData;
import forge.adventure.player.AdventurePlayer;
import forge.adventure.scene.InventoryScene;
import forge.adventure.world.World;
import forge.adventure.world.WorldSave;
import forge.deck.Deck;
/**
 * Shortcut class to handle global access, may need some redesign
 */
public class Current {
    public static AdventurePlayer player()
    {
        return WorldSave.getCurrentSave().getPlayer();
    }
    public static World world()
    {
        return WorldSave.getCurrentSave().getWorld();
    }

    static Deck deck;
    public static Deck latestDeck() {
        return deck;
    }
    public static void setLatestDeck(Deck generateDeck) {
        deck=generateDeck;
    }
    public static String generateDefeatMessage(boolean hasDied) {
        String key = hasDied ? "lblYouDied" : "lblYouLostTheLastGame";
        String message = Forge.getLocalizer().getMessage(key, player().getName());
        ItemData itemData = player().getRandomEquippedItem();
        if (itemData != null) {
            itemData.isCracked = true;
            player().equip(itemData); //un-equip
            InventoryScene.instance().clearItemDescription();
            message += "\n{GRADIENT=RED;GRAY;1;1}" + itemData.name + " {ENDGRADIENT}" + Forge.getLocalizer().getMessage("lblCracked");
        }
        return message;
    }

}
