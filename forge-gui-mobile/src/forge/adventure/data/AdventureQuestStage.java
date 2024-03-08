package forge.adventure.data;

import forge.adventure.character.EnemySprite;
import forge.adventure.pointofintrest.PointOfInterest;
import forge.adventure.scene.TileMapScene;
import forge.adventure.stage.MapStage;
import forge.adventure.util.AdventureQuestController;
import forge.adventure.util.AdventureQuestEvent;
import forge.adventure.util.AdventureQuestEventType;
import forge.adventure.util.Current;
import forge.util.Aggregates;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import static forge.adventure.util.AdventureQuestController.ObjectiveTypes.*;
import static forge.adventure.util.AdventureQuestController.QuestStatus.*;

public class AdventureQuestStage implements Serializable {

    private static final long serialVersionUID = 12042023L;

    public int id;
    private AdventureQuestController.QuestStatus status = INACTIVE;
    public String name = "";
    public String description = "";
    public boolean anyPOI = false; //false: Pick one PoI. True: Any PoI matching tags is usable
    public String mapFlag; //Map (or quest) flag to check
    public int mapFlagValue; //Minimum value for the flag
    public int count1; //use defined by objective type, this can be enemies to defeat, minimum PoI distance, etc
    public int count2; //use defined by objective type, this can be enemies to defeat, minimum PoI distance, etc
    public int count3; //use defined by objective type, this can be enemies to defeat, minimum PoI distance, etc
    public int count4; //use defined by objective type, this can be enemies to defeat, minimum PoI distance, etc
    private int progress1; //Progress toward count1
    private int progress2; //Progress toward count2
    private int progress3; //Progress toward count3
    private int progress4; //Progress toward count3
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
    private transient List<PointOfInterest> validPOIs;
    public boolean allowInactivePOI = false;

    public UUID stageID;

    public void initialize() {
        if (stageID == null) {
            stageID = UUID.randomUUID();
        }
        validPOIs = Current.world().getAllPointOfInterest();
    }

    public void checkPrerequisites(List<Integer> completedStages) {
        if (status != INACTIVE)
            return;
        for (Integer prereqID : prerequisiteIDs) {
            if (!completedStages.contains(prereqID)) {
                return;
            }
        }
        status = ACTIVE;
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

    public boolean checkIfTargetEnemy(EnemySprite enemy) {
        if (targetEnemyData != null) {
            return (enemy.getData().match(targetEnemyData));
        }
        else if (targetSprite == null) {
            ArrayList<String> candidateTags = new ArrayList<>(Arrays.asList(enemy.getData().questTags));
            int tagCount = candidateTags.size();

            candidateTags.removeAll(enemyExcludeTags);
            if (candidateTags.size() != tagCount) {
                return false;
            }

            candidateTags.removeAll(enemyTags);
            return candidateTags.size() == tagCount - enemyTags.size();
        } else  {
            return targetSprite.equals(enemy);
        }
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
        this.count4 = other.count4;
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

    public AdventureQuestController.QuestStatus handleEvent(AdventureQuestEvent event) {
        if (!checkIfTargetLocation(event.poi))
            return status;

        if (event.enemy != null && !checkIfTargetEnemy(event.enemy))
            return status;

        switch (objective) {
            case CharacterFlag:
                if (event.type == AdventureQuestEventType.CHARACTERFLAG)
                    status = event.flagName != null && event.flagName.equals(this.mapFlag) && event.flagValue >= this.mapFlagValue ? COMPLETE : status;
                break;
            case CompleteQuest:
                status = event.type == AdventureQuestEventType.QUESTCOMPLETE
                        && (anyPOI || event.otherQuest != null && event.otherQuest.sourceID.equals(targetPOI.getID()))
                        && ++progress3 >= count3 ? COMPLETE : status;
                break;
            case Clear:
                if (event.clear && event.winner) {
                    status = COMPLETE;
                }
                break;
            case Defeat:
                if (event.type != AdventureQuestEventType.MATCHCOMPLETE)
                    break;
                if (event.winner) {
                    status = ++progress3 >= count3 ? COMPLETE : status;
                } else {
                    status = ++progress4 >= count4 && count4 > 0 ? FAILED : status;
                }
                break;
            case Arena:
                status = event.type == AdventureQuestEventType.ARENACOMPLETE
                        && event.winner //if event won & not conceded
                        && ++progress3 >= count3 ? COMPLETE : status;
                break;
            case EventFinish:
                if (event.type != AdventureQuestEventType.EVENTCOMPLETE)
                    break;
                status = ++progress3 >= count3 ? COMPLETE : status;
                break;
            case EventWin:
                if (event.type != AdventureQuestEventType.EVENTCOMPLETE)
                    break;
                if (event.winner) {
                    status = ++progress3 >= count3 ? COMPLETE : status;
                } else {
                    status = ++progress4 >= count4 && count4 > 0 ? FAILED : status;
                }
                break;
            case EventWinMatches:
                if (event.type != AdventureQuestEventType.EVENTMATCHCOMPLETE)
                    break;
                if (event.winner) {
                    status = ++progress3 >= count3 ? COMPLETE : status;
                } else {
                    status = ++progress4 >= count4 && count4 > 0 ? FAILED : status;
                }
                break;
            case Fetch:
                status = event.type == AdventureQuestEventType.RECEIVEITEM
                        && (itemNames.isEmpty()) || (event.item != null && itemNames.contains(event.item.name))
                        && ++progress1 >= count1 ? COMPLETE : status;
                break;
            case Hunt:
                if (event.type == AdventureQuestEventType.DESPAWN) {
                    status = event.enemy.equals(targetSprite) ? FAILED : status;
                } else if (event.type == AdventureQuestEventType.MATCHCOMPLETE) {
                    if (event.winner) {
                        status = event.enemy.equals(targetSprite) ? COMPLETE : status;
                    } else {
                        status = ++progress4 >= count4 && count4 > 0 ? FAILED : status;
                    }
                }
                break;
            case Leave:
                if (event.type == AdventureQuestEventType.LEAVEPOI)
                    status = ++progress3 >= count3 ? COMPLETE : status;
                break;
            case MapFlag:
                if (event.type == AdventureQuestEventType.MAPFLAG)
                    status = event.flagName != null &&  event.flagName.equals(this.mapFlag) && event.flagValue >= this.mapFlagValue ? COMPLETE : status;
                break;
            case QuestFlag:
                if (event.type == AdventureQuestEventType.QUESTFLAG)
                    status = event.flagName != null &&  event.flagName.equals(this.mapFlag) && event.flagValue >= this.mapFlagValue ? COMPLETE : status;
                break;
            case HaveReputation:
                //presumed that WorldMapOK will be set on this type, as reputation will occasionally be updated remotely by quests
                if (event.type == AdventureQuestEventType.REPUTATION)
                    status = checkIfTargetLocation(event.poi) && event.count3 >= count3 ? COMPLETE : status;
                break;
            case HaveReputationInCurrentLocation:
                if (event.type == AdventureQuestEventType.ENTERPOI || event.type == AdventureQuestEventType.REPUTATION)
                    status = event.count3 >= count3 ? COMPLETE : status;
                break;
            case Delivery:
                //will eventually differentiate from Travel
            case Travel:
                status = ++progress3 >= count3 ? COMPLETE : status;
                break;
            case Use:
                status = event.type == AdventureQuestEventType.USEITEM
                        && (itemNames.isEmpty()) || itemNames.contains(event.item.name)
                        && ++progress3 >= count3 ? COMPLETE : status;
                break;
        }
        return status;
    }
}
