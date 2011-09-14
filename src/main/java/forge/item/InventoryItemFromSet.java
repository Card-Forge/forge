package forge.item;

/** 
 * Interface to define a player's inventory may hold.
 * Should include CardPrinted, Booster, Pets, Plants... etc
 */
public interface InventoryItemFromSet extends InventoryItem {
    /** An inventory item has to provide a name. */
    String getName();
    /** An inventory item has to provide a picture. */
    String getImageFilename();
    /** An item belonging to a set should return its set as well*/
    String getSet();
}
