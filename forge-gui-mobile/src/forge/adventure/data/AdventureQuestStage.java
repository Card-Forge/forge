package forge.adventure.data;

import forge.adventure.character.EnemySprite;
import forge.adventure.pointofintrest.PointOfInterest;
import forge.adventure.pointofintrest.PointOfInterestChanges;
import forge.adventure.scene.TileMapScene;
import forge.adventure.stage.MapStage;
import forge.adventure.util.AdventureQuestController;
import forge.adventure.util.Current;
import forge.adventure.world.WorldSave;
import forge.util.Aggregates;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import static forge.adventure.util.AdventureQuestController.ObjectiveTypes.*;
import static forge.adventure.util.AdventureQuestController.QuestStatus.*;

public class AdventureQuestStage implements Serializable {

    private static final long serialVersionUID = 12042023L;

    public int id;
    private AdventureQuestController.QuestStatus status = Inactive;
    public String name = "";
    public String description = "";
    public boolean anyPOI = false; //false: Pick one PoI. True: Any PoI matching tags is usable
    public String mapFlag; //Map (or quest) flag to check
    public int mapFlagValue; //Minimum value for the flag
    public int count1; //use defined by objective type, this can be enemies to defeat, minimum PoI distance, etc
    public int count2; //use defined by objective type, this can be enemies to defeat, minimum PoI distance, etc
    public int count3; //use defined by objective type, this can be enemies to defeat, minimum PoI distance, etc
    private int progress1; //Progress toward count1
    private int progress2; //Progress toward count2
    private int progress3; //Progress toward count3
    public boolean mixedEnemies; //false: Pick one enemy type. True: Combine all potential types
    public boolean here; //Default PoI selection to current location
    private PointOfInterest targetPOI; //Destination. Expand to array to cover "anyPOI?"
    private transient EnemySprite targetSprite; //EnemySprite targeted by this quest stage.
    private EnemyData targetEnemyData; //Valid enemy type for this quest stage when mixedEnemies is false.
    public List<String> POITags = new ArrayList<>(); //Tags defining potential targets
    public boolean worldMapOK = false; //Accept progress toward this objective outside any POI
    public AdventureQuestController.ObjectiveTypes objective;
    public List<Integer> prerequisiteIDs = new ArrayList<>();
    public List<String> enemyTags = new ArrayList<>(); //Tags defining potential targets
    public List<String> enemyExcludeTags = new ArrayList<>(); //Tags denoting invalid targets
    public List<String> itemNames = new ArrayList<>(); //Tags defining items to use
    public List<String> equipNames = new ArrayList<>(); //Tags defining equipment to use
    public boolean prologueDisplayed = false;
    public boolean epilogueDisplayed = false;
    public DialogData prologue;
    public DialogData epilogue;
    public DialogData failureDialog;
    public String deliveryItem = ""; //Imaginary item to get/fetch/deliver. Could be a general purpose field.
    public String POIToken; //If defined, ignore tags input and use the target POI from a different stage's objective instead.
    private transient List<Integer> _parsedPrerequisiteNames;
    private transient List<PointOfInterest> validPOIs = Current.world().getAllPointOfInterest();
    public boolean allowInactivePOI = false;

    public UUID stageID;

    public void initialize() {
        if (stageID == null) {
            stageID = UUID.randomUUID();
        }
    }

    public void checkPrerequisites(List<Integer> completedStages) {
        if (status != Inactive)
            return;
        for (Integer prereqID : prerequisiteIDs) {
            if (!completedStages.contains(prereqID)) {
                return;
            }
        }
        status = Active;
    }

    public AdventureQuestController.QuestStatus getStatus() {
        return status;
    }

    public PointOfInterest getTargetPOI() {
        return targetPOI;
    }

    public void setTargetPOI(PointOfInterest target) {
        if (!anyPOI)
            targetPOI = target;
    }

    public void setTargetPOI(Dictionary<String, PointOfInterest> poiTokens, String questName) {
        if (worldMapOK)
            return;
        if (POIToken != null && !POIToken.isEmpty()) {
            PointOfInterest tokenTarget = poiTokens.get(POIToken);
            if (tokenTarget != null) {
                setTargetPOI(tokenTarget);
                return;
            } else {
                System.out.println("Quest '" + questName + "' -  Stage '" + this.name + "' failed to generate POI from token reference: '" + POIToken + "'");
            }
        }
        if (here) {
            setTargetPOI(AdventureQuestController.instance().mostRecentPOI);
            return;
        }
        if (!allowInactivePOI) {
            validPOIs.removeIf(q -> !q.getActive()); //inactive POIs do not appear on map until conditions are met to activate them
        }
        for (String tag : POITags) {
            validPOIs.removeIf(q -> Arrays.stream(q.getData().questTags).noneMatch(tag::equals));
        }
        if (!anyPOI) {
            if (validPOIs.isEmpty()) {
                //no POI matched, fall back to anyPOI valid for the objective that doesn't match all tags
                validPOIs = Current.world().getAllPointOfInterest();
                return;
            }
            int targetIndex = (count1 * validPOIs.size() / 100);
            int variance = (count2 * validPOIs.size()) / 100;
            targetIndex = Math.max(0, (int) (targetIndex - variance + (new Random().nextFloat() * variance * 2)));

            if (targetIndex < validPOIs.size() && targetIndex >= 0) {
                validPOIs.sort(new AdventureQuestController.DistanceSort());
                setTargetPOI(validPOIs.get(targetIndex));
            } else {
                if (count1 != 0 || count2 != 0) {
                    System.out.println("Quest '" + questName + "' -  Stage '" + this.name + "' has invalid count1 ('" + count1 + "') and/or count2 ('" + count2 + "') value");
                }
                setTargetPOI(Aggregates.random(validPOIs));
            }
        }
        //"else" any POI matching all the POITags is valid, evaluate as needed
    }

    public EnemySprite getTargetSprite() {
        return targetSprite;
    }

    public void setTargetEnemyData(EnemyData target) {
        targetEnemyData = target;
    }

    public EnemyData getTargetEnemyData() {
        if (targetEnemyData == null && targetSprite != null)
            return targetSprite.getData();
        return targetEnemyData;
    }

    public void setTargetSprite(EnemySprite target) {
        targetSprite = target;
    }

    public AdventureQuestController.QuestStatus updateEnterPOI(PointOfInterest entered) {
        if (status != Active || !checkIfTargetLocation()) {
            return status;
        }
        switch (objective) {
            case Delivery:
                status = Complete;
                break;
            case Travel:
                status = ++progress1 >= count3 ? Complete : status;
                break;
           case HaveReputationInCurrentLocation:
               PointOfInterestChanges changes = WorldSave.getCurrentSave().getPointOfInterestChanges(entered.getID());
               status = changes.getMapReputation() >= count1 ? Complete : status;
               break;
        }

        return status;
    }

    public AdventureQuestController.QuestStatus updateReputationChanged(PointOfInterest location, int newReputation) {
        if (status != Active)
            return status;

        switch (objective) {
            case HaveReputation:
                status = checkIfTargetLocation(location) && newReputation >= count1 ? Complete : status;
                break;
            case HaveReputationInCurrentLocation:
                status = checkIfTargetLocation() && newReputation >= count1 ? Complete : status;
                break;
        }

        return status;
    }

    public AdventureQuestController.QuestStatus updateCharacterFlag(String flagName, int flagValue) {
        //Not yet implemented as objective type
        //Could theoretically be used as a part of quests for character lifetime achievements
//        if (status != Active) {
//            return status;
//        }
//            if (objective == CharacterFlag) {
//                if (flagName.equals(this.mapFlag) && flagValue >= this.mapFlagValue)
//                    status = Complete;
//            }
        return status;
    }

    public AdventureQuestController.QuestStatus updateQuestFlag(String flagName, int flagValue) {
        if (status != Active) {
            return status;
        }
        switch (objective) {
            case QuestFlag:
                status = flagName.equals(this.mapFlag) && flagValue >= this.mapFlagValue ? Complete : status;
                break;
        }
        return status;
    }

    public AdventureQuestController.QuestStatus updateMapFlag(String mapFlag, int mapFlagValue) {
        if (status != Active) {
            return status;
        }
        switch (objective) {
            case MapFlag:
                status = checkIfTargetLocation() && mapFlag.equals(mapFlag) && mapFlagValue >= this.mapFlagValue ? Complete : status;
                break;
        }
        return status;
    }

    public AdventureQuestController.QuestStatus updateLeave() {
        if (status != Active) {
            return status;
        }
        switch (objective) {
            case Leave:
                status = checkIfTargetLocation() && ++progress1 >= count1 ? Complete : status;
                break;
        }
        return status;
    }

    public AdventureQuestController.QuestStatus updateWin(EnemySprite defeated, boolean mapCleared) {
        //todo - Does this need to also be called for alternate mob removal types?
        if (status != Active) {
            return status;
        }

        switch (objective) {
            case Clear:
                status = mapCleared && checkIfTargetLocation()? Complete : status;
                break;
            case Defeat:
                if (!checkIfTargetLocation())
                    return status;
                if (mixedEnemies) {
                    List<String> defeatedTags = Arrays.asList(defeated.getData().questTags);
                    for (String targetTag : enemyTags) {
                        if (!defeatedTags.contains(targetTag)) {
                            //Does not count toward objective
                            return status;
                        }
                    }
                    for (String targetTag : enemyExcludeTags) {
                        if (defeatedTags.contains(targetTag)) {
                            //Does not count
                            return status;
                        }
                    }
                } else {
                    if (!defeated.getData().getName().equals(targetEnemyData.getName()))
                        //Does not count
                        return status;
                }
                //All tags matched, kill confirmed
                if (++progress1 >= count1) {
                    status = Complete;
                }
                break;
            case Hunt:
                status = defeated.equals(targetSprite)? Complete : status;
                break;
        }
        return status;
    }

    public boolean checkIfTargetLocation() {
        return checkIfTargetLocation(TileMapScene.instance().rootPoint);
    }

    public boolean checkIfTargetLocation(PointOfInterest locationToCheck) {
        if (!MapStage.getInstance().isInMap())
        {
            return worldMapOK;
        }
        if (targetPOI == null) {
            List<String> enteredTags = Arrays.stream(locationToCheck.getData().questTags).collect(Collectors.toList());
            for (String tag : POITags) {
                if (!enteredTags.contains(tag)) {
                    return false;
                }
            }
        }
        if (targetPOI != null) {
            return targetPOI.getPosition().equals(locationToCheck.getPosition());
        }
        return anyPOI;
    }

    public AdventureQuestController.QuestStatus updateLose(EnemySprite defeatedBy) {
        if (status != Active) {
            return status;
        }
        switch (objective) {
            case Defeat:
            {
                if (mixedEnemies) {
                    List<String> defeatedByTags = Arrays.asList(defeatedBy.getData().questTags);
                    for (String targetTag : enemyTags) {
                        if (!defeatedByTags.contains(targetTag)) {
                            //Does not count
                            return status;
                        }
                    }
                    for (String targetTag : enemyExcludeTags) {
                        if (defeatedByTags.contains(targetTag)) {
                            //Does not count
                            return status;
                        }
                    }
                } else {
                    if (defeatedBy.getData() != targetEnemyData)
                        //Does not count
                        return status;
                }
                //All tags matched
                //progress2: number of times defeated by a matching enemy
                //count2: if > 0, fail once defeated this many times
                if (status == Active && ++progress2 >= count2 && count2 > 0) {
                    status = Failed;
                }
                break;
            }
            case Hunt:
                if (defeatedBy.equals(targetSprite)) {
                    status = Failed;
                }
                break;
        }
        return status;
    }

    public AdventureQuestController.QuestStatus updateDespawn(EnemySprite despawned) {
        if (status != Active) {
            return status;
        }
        switch (objective) {
            case Hunt:
                status = (despawned.equals(targetSprite))? Failed : status;
                break;
        }

        return status;
    }

    public AdventureQuestController.QuestStatus updateArenaComplete(boolean winner) {
        if (status != Active || !checkIfTargetLocation()) {
            return status;
        }
            if (objective == Arena) {
                if (winner) {
                    status = ++progress1 >= count1 ? Complete : status;
                } else {
                    status = ++progress2 >= count2 ? Failed : status;
                }
            }
        return status;
    }

    public AdventureQuestController.QuestStatus updateEventComplete(AdventureEventData completedEvent) {
        if (status != Active || !checkIfTargetLocation()) {
            return status;
        }
        switch (objective) {
            case EventFinish:
                if (++progress1 >= count1) {
                    status = Complete;
                }
            break;
            case EventWinMatches:
                progress1 += completedEvent.matchesWon;
                progress2 += completedEvent.matchesLost;

                if (++progress2 >= count2 && count2 > 0) {
                    status = Failed;
                } else if (++progress1 >= count1) {
                    status = Complete;
                }

                break;
            case EventWin:
                if (completedEvent.playerWon) {
                    status = ++progress1 >=count1 ? Complete : status;
                } else {
                    status = ++progress2 >= count2 && count2 > 0 ? Failed : status;
                }
                break;
        }
        return status;
    }

    public AdventureQuestController.QuestStatus updateQuestComplete(AdventureQuestData completedQuest) {
        if (status != Active) {
            return status;
        }
        switch (objective) {
            case CompleteQuest:
                if (this.anyPOI) {
                    //todo - filter based on POI tags, below implementation is wrong but no quests use it yet
//                    List<String> completedQuestPOITags = Arrays.stream(completedQuest.questPOITags).collect(Collectors.toList());
//                    for (String targetTag : POITags) {
//                        if (!completedQuestPOITags.contains(targetTag)) {
//                            return status;
//                        }
//                    }
                    //All tags matched, completed quest came from valid POI.
                } else {
                    if (!completedQuest.sourceID.equals(this.targetPOI.getID()))
                        return status;
                }
                status = ++progress1 >= count1 ? Complete : status;
                break;
        }
        return status;
    }

    public AdventureQuestController.QuestStatus updateItemUsed(ItemData data) {
        if (status != Active) {
            return status;
        }
        if (objective == Use) {
            status = (itemNames.isEmpty()) || itemNames.contains(data.name) && ++progress1 >= count1 ? Complete : status;
        }
        return status;
    }

    public AdventureQuestController.QuestStatus updateItemReceived(ItemData data) {
        if (status != Active) {
            return status;
        }
        if (objective == Fetch) {
            status = (itemNames.isEmpty()) || itemNames.contains(data.name) && ++progress1 >= count1 ? Complete : status;
        }
        return status;
    }

    public AdventureQuestStage() {

    }

    public AdventureQuestStage(AdventureQuestStage other) {
        this.status = other.status;
        this.prologueDisplayed = other.prologueDisplayed;
        this.prologue = new DialogData(other.prologue);
        this.epilogueDisplayed = other.epilogueDisplayed;
        this.epilogue = new DialogData(other.epilogue);
        this.failureDialog = new DialogData(other.failureDialog);
        this.name = other.name;
        this.description = other.description;
        this.progress1 = other.progress1;
        this.progress2 = other.progress2;
        this.progress3 = other.progress3;
        this.count1 = other.count1;
        this.count2 = other.count2;
        this.count3 = other.count3;
        this.enemyTags = other.enemyTags;
        this.enemyExcludeTags = other.enemyExcludeTags;
        this.anyPOI = other.anyPOI;
        this.here = other.here;
        this.targetPOI = other.targetPOI;
        this.objective = other.objective;
        this.mapFlagValue = other.mapFlagValue;
        this.mapFlag = other.mapFlag;
        this.equipNames = other.equipNames;
        this.mixedEnemies = other.mixedEnemies;
        this.itemNames = other.itemNames;
        this.prerequisiteIDs = other.prerequisiteIDs;
        this.POIToken = other.POIToken;
        this.id = other.id;
        this.POITags = other.POITags;
        this.targetEnemyData = other.targetEnemyData;
        this.deliveryItem = other.deliveryItem;
        this.worldMapOK = other.worldMapOK;
        this.allowInactivePOI = other.allowInactivePOI;
    }


    public List<PointOfInterest> getValidPOIs() {
        if (worldMapOK)
            return new ArrayList<>();
        if (objective == Hunt)
            return new ArrayList<>();
        if (validPOIs == null)
            validPOIs = new ArrayList<>();
        if (validPOIs.size() != 1 && targetPOI != null) {
            validPOIs.clear();
            validPOIs.add(targetPOI);
        }
        if (validPOIs.isEmpty() && targetPOI == null && !POITags.isEmpty())
        {
            validPOIs = Current.world().getAllPointOfInterest();
            if (!allowInactivePOI) {
                validPOIs.removeIf(q -> !q.getActive()); //inactive POIs do not appear on map until conditions are met to activate them
            }
            for (String tag : POITags) {
                validPOIs.removeIf(q -> Arrays.stream(q.getData().questTags).noneMatch(tag::equals));
            }
        }
        return validPOIs;
    }
}
