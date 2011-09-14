package forge.item;

/** 
 * Interface to define a player's inventory may hold.
 * Should include CardPrinted, Booster, Pets, Plants... etc
 */
public interface InventoryItem /* extends Comparable */ {
    /** An inventory item has to provide a name. */
    String getName();
    /** An inventory item has to provide a picture. */
    String getImageFilename();
    /** Return type as a string */ 
    String getType();
}
