package forge.item;

/**
 * Interface to define a player's inventory may hold. Should include
 * CardPrinted, Booster, Pets, Plants... etc
 */
public interface InventoryItemFromSet extends InventoryItem {

    /**
     * An inventory item has to provide a name.
     * 
     * @return the name
     */
    @Override
    String getName();

    /**
     * An inventory item has to provide a picture.
     * 
     * @return the image filename
     */
    @Override
    String getImageFilename();

    /**
     * An item belonging to a set should return its set as well.
     * 
     * @return the sets the
     */
    String getSet();
}
