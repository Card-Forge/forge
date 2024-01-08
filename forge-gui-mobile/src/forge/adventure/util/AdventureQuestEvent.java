package forge.adventure.util;

import forge.adventure.character.EnemySprite;
import forge.adventure.data.AdventureQuestData;
import forge.adventure.data.ItemData;
import forge.adventure.pointofintrest.PointOfInterest;

public class AdventureQuestEvent {
    public AdventureQuestEventType type;
    public PointOfInterest poi;
    public EnemySprite enemy;
    public ItemData item;
    public boolean clear;
    public boolean winner;
    public String flagName;
    public int flagValue;
    public int count1;
    public int count2;
    public AdventureQuestData otherQuest;
}

