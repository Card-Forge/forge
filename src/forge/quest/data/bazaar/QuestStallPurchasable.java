package forge.quest.data.bazaar;

/**
 * This interface defines a thing that can be sold at the Bazaar
 */
public interface QuestStallPurchasable extends Comparable<Object>{
    /**
     * @return The Name of the item
     */
    public String getPurchaseName();

    /**
     * @return an HTML formatted item description
     */
    public String getPurchaseDescription();

    /**
     * @return the name of the image that is displayed in the bazaar
     */
    public String getImageName();

    /**
     * @return the cost of the item in credits
     */
    public int getPrice();

    /**
     * Returns if the item is available for purchase;
     * @return <code>true</code> if the item can be displayed in a store
     * <code>false</code> if the item should not be displayed in store since, for example, prerequisites are not met
     */
    public boolean isAvailableForPurchase();

    /**
     * Executed when the item is bought
     */
    public void onPurchase();

    /**
     * @return the name of the stall form which this item can be purchased
     */
    public String getStallName();
}
