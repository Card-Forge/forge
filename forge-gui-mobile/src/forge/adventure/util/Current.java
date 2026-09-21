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

    private static final StringBuilder stringBuilder = new StringBuilder(512);

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
        deck = generateDeck;
    }

    public static String generateDefeatMessage(boolean hasDied) {
        final String key = hasDied ? "lblYouDied" : "lblYouLostTheLastGame";
        final String baseMessage = Forge.getLocalizer().getMessage(key, player().getName());

        final ItemData itemData = player().getRandomEquippedItem();
        if (itemData != null && !(Config.instance().getSettingData().disableCrackedItems)) {
            itemData.isCracked = true;
            player().equip(itemData); // un-equip
            InventoryScene.instance().clearItemDescription();

            stringBuilder.setLength(0);
            return stringBuilder.append(baseMessage)
                .append("\n{GRADIENT=RED;GRAY;1;1}").append(itemData.name).append(" {ENDGRADIENT}")
                .append(Forge.getLocalizer().getMessage("lblCracked")).toString();
        }

        return baseMessage;
    }
}
