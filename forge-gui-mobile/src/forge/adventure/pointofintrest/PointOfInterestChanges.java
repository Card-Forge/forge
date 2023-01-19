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
    }

    @Override
    public SaveFileData save() {
        SaveFileData data=new SaveFileData();
        data.storeObject("deletedObjects",deletedObjects);
        data.storeObject("cardsBought",cardsBought);
        data.storeObject("mapFlags", mapFlags);
        data.storeObject("shopSeeds", shopSeeds);
        data.storeObject("shopModifiers", shopModifiers);
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

    public float getShopPriceModifier(int objectID){
        if (!shopModifiers.containsKey(objectID))
        {
            return -1.0f;
        }
        return shopModifiers.get(objectID);
    }

    public void setShopModifier(int objectID, float mod){
        if (objectID!= 0) shopModifiers.put(objectID, mod);
    }

    public float getTownPriceModifier(){
        if (!shopModifiers.containsKey(0))
        {
            return -1.0f;
        }
        return shopModifiers.get(0);
    }

    public void setTownModifier(float mod){
        shopModifiers.put(0, mod);
    }

}
