package forge.quest.data.bazaar;

/**
 * This interface defines a thing that can be sold at the Bazaar
 *
 * @author Forge
 * @version $Id: $
 */
public interface QuestStallPurchasable extends Comparable<Object> {
    /**
     * <p>getPurchaseName.</p>
     *
     * @return The Name of the item
     */
    public String getPurchaseName();

    /**
     * <p>getPurchaseDescription.</p>
     *
     * @return an HTML formatted item description
     */
    public String getPurchaseDescription();

    /**
     * <p>getImageName.</p>
     *
     * @return the name of the image that is displayed in the bazaar
     */
    public String getImageName();

    /**
     * <p>getPrice.</p>
     *
     * @return the cost of the item in credits
     */
    public int getPrice();

    /**
     * Returns if the item is available for purchase;
     *
     * @return <code>true</code> if the item can be displayed in a store
     *         <code>false</code> if the item should not be displayed in store since, for example, prerequisites are not met
     */
    public boolean isAvailableForPurchase();

    /**
     * Executed when the item is bought
     */
    public void onPurchase();

    /**
     * <p>getStallName.</p>
     *
     * @return the name of the stall form which this item can be purchased
     */
    public String getStallName();
}
