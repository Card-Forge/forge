package forge.adventure.data;

import forge.adventure.character.EnemySprite;
import forge.adventure.pointofintrest.PointOfInterest;
import forge.adventure.pointofintrest.PointOfInterestChanges;
import forge.adventure.util.AdventureQuestController;
import forge.adventure.util.Current;
import forge.adventure.world.WorldSave;
import forge.util.Aggregates;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

public class AdventureQuestStage implements Serializable {

    private static final long serialVersionUID = 12042023L;

    public int id;
    private AdventureQuestController.QuestStatus status = AdventureQuestController.QuestStatus.Inactive;
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
    public AdventureQuestController.ObjectiveTypes objective;
    public List<String> prerequisiteNames = new ArrayList<>();
    public List<String> enemyTags = new ArrayList<>(); //Tags defining potential targets
    public List<String> itemNames = new ArrayList<>(); //Tags defining items to use
    public List<String> equipNames = new ArrayList<>(); //Tags defining equipment to use
    public boolean prologueDisplayed = false;
    public boolean epilogueDisplayed = false;
    public DialogData prologue;
    public DialogData epilogue;
    public DialogData failureDialog;
    public boolean prequisitesComplete = false;
    public String deliveryItem = ""; //Imaginary item to get/fetch/deliver. Could be a general purpose field.
    public String POIToken; //If defined, ignore tags input and use the target POI from a different stage's objective instead.
    private transient boolean inTargetLocation = false;

    public UUID stageID;

    public void initialize() {
        if (stageID == null) {
            stageID = UUID.randomUUID();
        }
    }

    public void checkPrerequisites() {
        //Todo - implement
    }

    public AdventureQuestController.QuestStatus getStatus() {
        return status;
    }

    public void setStatus(AdventureQuestController.QuestStatus newStatus) {
        if (!status.equals(newStatus) && newStatus.equals(AdventureQuestController.QuestStatus.Active)) {
            AdventureQuestController.instance().addQuestSprites(this);
        }
        status = newStatus;
    }

    public PointOfInterest getTargetPOI() {
        return targetPOI;
    }

    public void setTargetPOI(PointOfInterest target) {
        if (!anyPOI)
            targetPOI = target;
    }

    public void setTargetPOI(Dictionary<String, PointOfInterest> poiTokens, String questName) {
        if (POIToken != null && POIToken.length() > 0) {
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
        if (!anyPOI) {
            List<PointOfInterest> candidates = Current.world().getAllPointOfInterest();
            for (String tag : POITags) {
                candidates.removeIf(q -> Arrays.stream(q.getData().questTags).noneMatch(tag::equals));
            }
            if (candidates.size() < 1) {
                //no POI matched, fall back to anyPOI valid for the objective that doesn't match all tags
                candidates = Current.world().getAllPointOfInterest();
                if (objective == AdventureQuestController.ObjectiveTypes.Clear)
                    candidates.removeIf(q -> !Arrays.asList(q.getData().questTags).contains("Hostile"));
                else
                    candidates.removeIf(q -> Arrays.asList(q.getData().questTags).contains("Hostile"));
                return;
            }
            count1 = (count1 * candidates.size() / 100);
            count2 = (count2 * candidates.size()) / 100;
            int targetIndex = Math.max(0, (int) (count1 - count2 + (new Random().nextFloat() * count2 * 2)));

            if (targetIndex < candidates.size() && targetIndex >= 0) {
                candidates.sort(new AdventureQuestController.DistanceSort());
                setTargetPOI(candidates.get(targetIndex));
            } else {
                if (count1 != 0 || count2 != 0) {
                    System.out.println("Quest '" + questName + "' -  Stage '" + this.name + "' has invalid count1 ('" + count1 + "') and/or count2 ('" + count2 + "') value");
                }
                setTargetPOI(Aggregates.random(candidates));
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
        if (targetEnemyData == null & targetSprite != null)
            return targetSprite.getData();
        return targetEnemyData;
    }

    public void setTargetSprite(EnemySprite target) {
        targetSprite = target;
    }

    public AdventureQuestController.QuestStatus updateEnterPOI(PointOfInterest entered) {
        if (getStatus() == AdventureQuestController.QuestStatus.Complete) {
            return status;
        } else if (getStatus() == AdventureQuestController.QuestStatus.Failed) {
            return status;
        } else {
            checkIfInTargetLocation(entered);
            if (inTargetLocation){
                if (this.objective == AdventureQuestController.ObjectiveTypes.Delivery
                        || this.objective == AdventureQuestController.ObjectiveTypes.Travel) {
                status = AdventureQuestController.QuestStatus.Complete;
                }
                if (this.objective == AdventureQuestController.ObjectiveTypes.HaveReputation) {
                    PointOfInterestChanges changes = WorldSave.getCurrentSave().getPointOfInterestChanges(entered.getID() + entered.getData().map);
                    if (changes.getMapReputation() >= count1) {
                        status = AdventureQuestController.QuestStatus.Complete;
                    }
                }
            }
        }
        return status;
    }

    public AdventureQuestController.QuestStatus updateCharacterFlag(String flagName, int flagValue) {
        if (getStatus() == AdventureQuestController.QuestStatus.Complete) {
            return status;
        } else if (getStatus() == AdventureQuestController.QuestStatus.Failed) {
            return status;
        } else {
            //Not yet implemented as objective type
            //Could theoretically be used as a part of quests for character lifetime achievements
//            if (this.objective == AdventureQuestController.ObjectiveTypes.CharacterFlag) {
//                if (flagName.equals(this.mapFlag) && flagValue >= this.mapFlagValue)
//                    status = AdventureQuestController.QuestStatus.Complete;
//            }
            return status;
        }
    }

    public AdventureQuestController.QuestStatus updateQuestFlag(String flagName, int flagValue) {
        if (getStatus() == AdventureQuestController.QuestStatus.Complete) {
            return status;
        } else if (getStatus() == AdventureQuestController.QuestStatus.Failed) {
            return status;
        } else {
            if (this.objective == AdventureQuestController.ObjectiveTypes.QuestFlag) {
                if (flagName.equals(this.mapFlag) && flagValue >= this.mapFlagValue)
                    status = AdventureQuestController.QuestStatus.Complete;
            }
            return status;
        }
    }

    public AdventureQuestController.QuestStatus updateMapFlag(String mapFlag, int mapFlagValue) {
        if (getStatus() == AdventureQuestController.QuestStatus.Complete) {
            return status;
        } else if (getStatus() == AdventureQuestController.QuestStatus.Failed) {
            return status;
        } else {
            if (this.objective == AdventureQuestController.ObjectiveTypes.MapFlag) {
                if (mapFlag.equals(this.mapFlag) && mapFlagValue >= this.mapFlagValue)
                    status = AdventureQuestController.QuestStatus.Complete;
            }
            return status;
        }
    }

    public AdventureQuestController.QuestStatus updateLeave() {
        if (status == AdventureQuestController.QuestStatus.Complete) {
            return status;
        }
        if (status == AdventureQuestController.QuestStatus.Failed) {
            return status;
        }
        inTargetLocation = false; //todo: handle case when called between multi-map PoIs (if necessary)
        if (this.objective == AdventureQuestController.ObjectiveTypes.Leave) {
            status = AdventureQuestController.QuestStatus.Complete;
        }
        return status;
    }

    public AdventureQuestController.QuestStatus updateWin(EnemySprite defeated, boolean mapCleared) {
        //todo - Does this need to also be called for alternate mob removal types?
        if (status == AdventureQuestController.QuestStatus.Complete) {
            return status;
        }
        if (status == AdventureQuestController.QuestStatus.Failed) {
            return status;
        }
        if (this.objective == AdventureQuestController.ObjectiveTypes.Clear) {
            if (mapCleared && inTargetLocation) {
                status = AdventureQuestController.QuestStatus.Complete;
            }
        } else if (this.objective == AdventureQuestController.ObjectiveTypes.Defeat) {
            {
                if (mixedEnemies) {
                    List<String> defeatedTags = Arrays.stream(defeated.getData().questTags).collect(Collectors.toList());
                    for (String targetTag : enemyTags) {
                        if (!defeatedTags.contains(targetTag)) {
                            //Does not count toward objective
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
                    status = AdventureQuestController.QuestStatus.Complete;
                }
            }
        } else if (this.objective == AdventureQuestController.ObjectiveTypes.Hunt) {
            if (defeated.equals(targetSprite)) {
                status = AdventureQuestController.QuestStatus.Complete;
            }
        }

        return status;
    }

    public void checkIfInTargetLocation(PointOfInterest entered) {
        if (targetPOI == null) {
            List<String> enteredTags = Arrays.stream(entered.getData().questTags).collect(Collectors.toList());
            for (String tag : POITags) {
                if (!enteredTags.contains(tag)) {
                    inTargetLocation = false;
                    return;
                }
            }
        } else if (!targetPOI.getPosition().equals(entered.getPosition())) {
            inTargetLocation = false;
            return;
        }
        inTargetLocation = true;
    }

    public AdventureQuestController.QuestStatus updateLose(EnemySprite defeatedBy) {
        if (status != AdventureQuestController.QuestStatus.Failed && this.objective == AdventureQuestController.ObjectiveTypes.Defeat) {
            {
                if (mixedEnemies) {
                    List<String> defeatedByTags = Arrays.stream(defeatedBy.getData().questTags).collect(Collectors.toList());
                    for (String targetTag : enemyTags) {
                        if (!defeatedByTags.contains(targetTag)) {
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
                if (status == AdventureQuestController.QuestStatus.Active && ++progress2 >= count2 && count2 > 0) {
                    status = AdventureQuestController.QuestStatus.Failed;
                }

            }
        } else if (status == AdventureQuestController.QuestStatus.Active && this.objective == AdventureQuestController.ObjectiveTypes.Hunt) {
            if (defeatedBy.equals(targetSprite)) {
                status = AdventureQuestController.QuestStatus.Failed;
            }
        }
        return status;
    }

    public AdventureQuestController.QuestStatus updateDespawn(EnemySprite despawned) {
        if (status == AdventureQuestController.QuestStatus.Active && this.objective == AdventureQuestController.ObjectiveTypes.Hunt) {
            if (despawned.equals(targetSprite)) {
                status = AdventureQuestController.QuestStatus.Failed;
            }
        }
        return status;
    }

    public void updateArenaComplete(boolean winner) {
        if (this.objective == AdventureQuestController.ObjectiveTypes.Arena) {
            if (inTargetLocation) {
                if (winner) {
                    status = AdventureQuestController.QuestStatus.Complete;
                } else {
                    status = AdventureQuestController.QuestStatus.Failed;
                }
            }
        }
    }

    public void updateEventComplete(AdventureEventData completedEvent) {
        if (this.objective == AdventureQuestController.ObjectiveTypes.EventFinish) {
            if (inTargetLocation) {
                if (++progress1 >= count1) {
                    status = AdventureQuestController.QuestStatus.Complete;
                }
            }
        }
        if (this.objective == AdventureQuestController.ObjectiveTypes.EventWinMatches) {
            if (inTargetLocation) {
                progress1 += completedEvent.matchesWon;
                progress2 += completedEvent.matchesLost;


                if (status == AdventureQuestController.QuestStatus.Active && ++progress2 >= count2 && count2 > 0) {
                    status = AdventureQuestController.QuestStatus.Failed;
                }
                else if (++progress1 >= count1) {
                    status = AdventureQuestController.QuestStatus.Complete;
                }
            }
        }
        if (this.objective == AdventureQuestController.ObjectiveTypes.EventWin) {
            if (inTargetLocation) {

                if (completedEvent.playerWon){
                    progress1++;
                }
                else{
                    progress2++;
                }


                if (status == AdventureQuestController.QuestStatus.Active && ++progress2 >= count2 && count2 > 0) {
                    status = AdventureQuestController.QuestStatus.Failed;
                }
                else if (++progress1 >= count1) {
                    status = AdventureQuestController.QuestStatus.Complete;
                }
            }
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
        this.enemyTags = other.enemyTags;
        this.anyPOI = other.anyPOI;
        this.here = other.here;
        this.targetPOI = other.targetPOI;
        this.objective = other.objective;
        this.mapFlagValue = other.mapFlagValue;
        this.mapFlag = other.mapFlag;
        this.equipNames = other.equipNames;
        this.mixedEnemies = other.mixedEnemies;
        this.itemNames = other.itemNames;
        this.prequisitesComplete = other.prequisitesComplete;
        this.prerequisiteNames = other.prerequisiteNames;
        this.POIToken = other.POIToken;
        this.id = other.id;
        this.POITags = other.POITags;
        this.targetEnemyData = other.targetEnemyData;
        this.deliveryItem = other.deliveryItem;
//        if (this.stageID == null)
//            this.stageID = other.stageID;
    }


}
