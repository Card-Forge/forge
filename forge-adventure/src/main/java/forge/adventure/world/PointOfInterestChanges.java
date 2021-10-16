package forge.adventure.world;

import java.util.HashMap;
import java.util.HashSet;

/**
 * Class to save point of interest changes, like sold cards and dead enemies
 */
public class PointOfInterestChanges {



    private final HashSet<Integer> deletedObjects=new HashSet<>();
    private final HashMap<Integer, HashSet<Integer>> cardsBought=new HashMap<>();

    public boolean isObjectDeleted(int objectID)
    {
        return deletedObjects.contains(objectID);
    }
    public boolean deleteObject(int objectID)
    {
        return deletedObjects.add(objectID);
    }

    public void buyCard(int objectID,int cardIndex)
    {
        if( !cardsBought.containsKey(objectID))
        {
            cardsBought.put(objectID,new HashSet<>());
        }
        cardsBought.get(objectID).add(cardIndex);
    }
    public boolean wasCardBought(int objectID, int cardIndex)
    {
        if( !cardsBought.containsKey(objectID))
        {
            return false;
        }
        return cardsBought.get(objectID).contains(cardIndex);
    }
}
