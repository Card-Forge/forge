package forge.quest.data.bazaar;

/**
 * This interface defines a thing that can be sold at the Bazaar.
 *
 * @author Forge
 * @version $Id$
 */
public interface QuestStallPurchasable extends Comparable<Object> {
    /**
     * <p>
     * getPurchaseName.
     * </p>
     * 
     * @return The Name of the item
     */
    String getPurchaseName();

    /**
     * <p>
     * getPurchaseDescription.
     * </p>
     * 
     * @return an HTML formatted item description
     */
    String getPurchaseDescription();

    /**
     * <p>
     * getImageName.
     * </p>
     * 
     * @return the name of the image that is displayed in the bazaar
     */
    String getImageName();

    /**
     * <p>
     * getPrice.
     * </p>
     * 
     * @return the cost of the item in credits
     */
    int getPrice();

    /**
     * Returns if the item is available for purchase;.
     *
     * @return <code>true</code> if the item can be displayed in a store
     * <code>false</code> if the item should not be displayed in store
     * since, for example, prerequisites are not met
     */
    boolean isAvailableForPurchase();

    /**
     * Executed when the item is bought.
     */
    void onPurchase();

    /**
     * <p>
     * getStallName.
     * </p>
     * 
     * @return the name of the stall form which this item can be purchased
     */
    String getStallName();
}
