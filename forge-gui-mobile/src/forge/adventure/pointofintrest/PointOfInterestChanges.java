package forge.adventure.pointofintrest;

import forge.adventure.util.Current;
import forge.adventure.util.SaveFileContent;
import forge.adventure.util.SaveFileData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Class to save point of interest changes, like sold cards and dead enemies
 */
public class PointOfInterestChanges implements SaveFileContent  {
    private final HashSet<Integer> deletedObjects=new HashSet<>();
    private final HashMap<Integer, HashSet<Integer>> cardsBought = new HashMap<>();
    private final java.util.Map<String, Byte> mapFlags = new HashMap<>();
    private final java.util.Map<Integer, Long> shopSeeds = new HashMap<>();
    private final java.util.Map<Integer, Float> shopModifiers = new HashMap<>();
    private final java.util.Map<Integer, Integer> reputation = new HashMap<>();
    private Boolean isBookmarked;

    public static class Map extends HashMap<String,PointOfInterestChanges> implements SaveFileContent {
        @Override
        public void load(SaveFileData data) {
            this.clear();
            if(data==null || !data.containsKey("keys")) return;

            String[] keys= (String[]) data.readObject("keys");
            for(int i=0;i<keys.length;i++) {
                SaveFileData elementData = data.readSubData("value_"+i);
                PointOfInterestChanges newChanges=new PointOfInterestChanges();
                newChanges.load(elementData);
                this.put(keys[i],newChanges);
            }
        }

        @Override
        public SaveFileData save() {
            SaveFileData data=new SaveFileData();
            ArrayList<String> keys=new ArrayList<>();
            ArrayList<PointOfInterestChanges> items=new ArrayList<>();
            for (Map.Entry<String,PointOfInterestChanges>  entry : this.entrySet()) {
                keys.add(entry.getKey());
                items.add(entry.getValue());
            }
            data.storeObject("keys",keys.toArray(new String[0]));
            for(int i=0;i<items.size();i++)
                data.store("value_"+i,items.get(i).save());
            return data;
        }
    }

    @Override
    public void load(SaveFileData data) {
        deletedObjects.clear();
        deletedObjects.addAll((HashSet<Integer>) data.readObject("deletedObjects"));
        cardsBought.clear();
        cardsBought.putAll((HashMap<Integer, HashSet<Integer>>) data.readObject("cardsBought"));
        shopSeeds.clear();
        shopSeeds.putAll((java.util.Map<Integer, Long>) data.readObject("shopSeeds"));
        mapFlags.clear();
        mapFlags.putAll((java.util.Map<String, Byte>) data.readObject("mapFlags"));
        shopModifiers.clear();
        shopModifiers.putAll((java.util.Map<Integer, Float>) data.readObject("shopModifiers"));
        isBookmarked = (Boolean) data.readObject("isBookmarked");
    }

    @Override
    public SaveFileData save() {
        SaveFileData data=new SaveFileData();
        data.storeObject("deletedObjects",deletedObjects);
        data.storeObject("cardsBought",cardsBought);
        data.storeObject("mapFlags", mapFlags);
        data.storeObject("shopSeeds", shopSeeds);
        data.storeObject("shopModifiers", shopModifiers);
        data.storeObject("isBookmarked", isBookmarked);
        return data;
    }

    public boolean isObjectDeleted(int objectID) { return deletedObjects.contains(objectID); }
    public boolean deleteObject(int objectID)    { return deletedObjects.add(objectID); }

    public java.util.Map<String, Byte> getMapFlags() {
        return mapFlags;
    }

    public void buyCard(int objectID, int cardIndex) {
        if( !cardsBought.containsKey(objectID)) {
            cardsBought.put(objectID,new HashSet<>());
        }
        cardsBought.get(objectID).add(cardIndex);
    }
    public boolean wasCardBought(int objectID, int cardIndex) {
        if( !cardsBought.containsKey(objectID)) {
            return false;
        }
        return cardsBought.get(objectID).contains(cardIndex);
    }

    public long getShopSeed(int objectID){
        if (!shopSeeds.containsKey(objectID))
        {
            generateNewShopSeed(objectID);
        }
        return shopSeeds.get(objectID);
    }

    public void generateNewShopSeed(int objectID){

        shopSeeds.put(objectID, Current.world().getRandom().nextLong());
        cardsBought.put(objectID, new HashSet<>()); //Allows cards to appear in slots of previous purchases
    }

    public void setRotatingShopSeed(int objectID, long seed){
        if (shopSeeds.containsKey(objectID) && shopSeeds.get(objectID) != seed) {
            cardsBought.put(objectID, new HashSet<>()); //Allows cards to appear in slots of previous purchases
        }
        shopSeeds.put(objectID, seed);
    }

    public float getShopPriceModifier(int objectID){
        int shopRep = reputation.getOrDefault(objectID, 0);

        shopRep = Integer.min(maxRepToApply, (Integer.max(-maxRepToApply, shopRep)));

        return 1.0f + (shopRep * priceModifierPerRep);
    }

    int maxRepToApply = 20;
    float priceModifierPerRep = 0.005f;

    public float getTownPriceModifier(){
        int townRep = reputation.getOrDefault(0, 0);

        townRep = Integer.min(maxRepToApply, (Integer.max(-maxRepToApply, townRep)));

        return 1.0f - Math.round((priceModifierPerRep * townRep) * 1000)/1000f;
    }

    public void addMapReputation(int delta)
    {
        addObjectReputation(0, delta);
    }

    public void addObjectReputation(int id, int delta)
    {
        reputation.put(id, (reputation.containsKey(id)?reputation.get(id):0) + delta);
    }

    public int getMapReputation(){
        return getObjectReputation(0);
    }

    public int getObjectReputation(int id){
        if (!reputation.containsKey(id))
        {
            reputation.put(id, 0);
        }
        return reputation.get(id);
    }
    public boolean hasDeletedObjects() {
        return deletedObjects != null && !deletedObjects.isEmpty();
    }
    public boolean isBookmarked() {
        if (isBookmarked == null)
            return false;
        return isBookmarked;
    }
    public void setIsBookmarked(boolean val) {
        isBookmarked = val;
    }

    public void clearDeletedObjects() {
        // reset map when assigning as a quest target that needs enemies
        deletedObjects.clear();
    }
}
