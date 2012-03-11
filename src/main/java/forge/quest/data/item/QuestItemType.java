package forge.quest.data.item;

import forge.quest.data.QuestItemCondition;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public enum QuestItemType {
    
    SLEIGHT("Sleight", QuestItemPassive.class, QuestItemCondition.class),
    ESTATES("Estates", QuestItemEstates.class, QuestItemCondition.class),
    LUCKY_COIN("Lucky Coin", QuestItemPassive.class, QuestItemCondition.class),
    MAP("Map", QuestItemPassive.class, QuestItemCondition.class),
    ZEPPELIN("Zeppelin", QuestItemZeppelin.class, QuestItemCondition.class), 
    ELIXIR_OF_LIFE("Elixir of Life", QuestItemElixir.class, QuestItemCondition.class),
    POUND_FLESH("Pound of Flesh", QuestItemPoundFlesh.class, QuestItemCondition.class);
    
    
    private final String saveFileKey; 
    private final Class<? extends QuestItemPassive> bazaarControllerClass;
    private final Class<? extends QuestItemCondition> modelClass;
    
    private QuestItemType(String key, Class<? extends QuestItemPassive> controllerClass0, Class<? extends QuestItemCondition> modelClass0) { 
        saveFileKey = key; 
        bazaarControllerClass = controllerClass0;
        modelClass = modelClass0;
    }
    /**
     * TODO: Write javadoc for this method.
     * @return
     */
    public String getKey() {
        return saveFileKey;
    }
    public Class<? extends QuestItemPassive> getBazaarControllerClass() {
        return bazaarControllerClass;
    }
    public Class<? extends QuestItemCondition> getModelClass() {
        return modelClass;
    }
    
    public static QuestItemType smartValueOf(final String value) {
        if (value == null) {
            return null;
        }
        if ("All".equals(value)) {
            return null;
        }
        final String valToCompate = value.trim();
        for (final QuestItemType v : QuestItemType.values()) {
            if (v.name().compareToIgnoreCase(valToCompate) == 0) {
                return v;
            }
        }
        throw new IllegalArgumentException("No element named " + value + " in enum QuestItemType");
    }


    public static QuestItemType valueFromSaveKey(String name) {
        if (name == null) {
            return null;
        }

        final String valToCompate = name.trim();
        for (final QuestItemType v : QuestItemType.values()) {
            if (v.getKey().compareToIgnoreCase(valToCompate) == 0) {
                return v;
            }
        }
        throw new IllegalArgumentException("No element keyed " + name + " in enum QuestItemType");
    }    
    
}

