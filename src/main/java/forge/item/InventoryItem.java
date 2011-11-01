package forge.item;

/**
 * Interface to define a player's inventory may hold. Should include
 * CardPrinted, Booster, Pets, Plants... etc
 */
public interface InventoryItem {

    /**
     * An inventory item has to provide a name.
     * 
     * @return the name
     */
    String getName();

    /**
     * An inventory item has to provide a picture.
     * 
     * @return the image filename
     */
    String getImageFilename();

    /**
     * Return type as a string.
     * 
     * @return the type
     */
    String getType();
}
