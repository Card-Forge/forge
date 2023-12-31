package forge.adventure.data;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import forge.adventure.character.EnemySprite;
import forge.adventure.pointofintrest.PointOfInterest;
import forge.adventure.scene.GameScene;
import forge.adventure.scene.TileMapScene;
import forge.adventure.stage.GameHUD;
import forge.adventure.stage.MapStage;
import forge.adventure.util.AdventureQuestController;
import forge.adventure.util.AdventureQuestEvent;
import forge.adventure.util.Current;
import forge.adventure.world.WorldSave;
import forge.util.Aggregates;

import java.io.Serializable;
import java.util.*;

import static forge.adventure.util.AdventureQuestController.QuestStatus.*;

public class AdventureQuestData implements Serializable {

    private int id;

    public int getID(){
        if (isTemplate && id < 1) {
            id = AdventureQuestController.instance().getNextQuestID();
        }
        return id;
    }
    public boolean isTemplate = false;
    public String name = "";
    public String description = "";
    public String synopsis =""; //Intended for Dev Mode only at most
    public transient boolean completed = false;
    public transient boolean failed = false;
    private transient boolean prologueDisplayed = false;
    private transient boolean epilogueDisplayed = false;

    public DialogData offerDialog;
    public DialogData prologue;
    public DialogData epilogue;
    public DialogData failureDialog;

    public DialogData declinedDialog;

    public RewardData reward;
    public String rewardDescription = "";

    public AdventureQuestStage[] stages = new AdventureQuestStage[0];
    public String[] questSourceTags = new String[0];
    public String[] questEnemyTags = new String[0];
    public String[] questPOITags = new String[0];
    private transient EnemySprite targetEnemySprite = null;
    private PointOfInterest targetPoI = null;
    Dictionary<String, PointOfInterest> poiTokens = new Hashtable<>();
    Dictionary<String, String> poiBiomeTokens = new Hashtable<>();
    Dictionary<String, EnemyData> enemyTokens = new Hashtable<>();
    Dictionary<String, String> otherTokens = new Hashtable<>();
    public boolean storyQuest = false;
    public boolean isTracked = false;
    public boolean autoTrack = false;
    public String sourceID = "";

    public String getName() {
        return name;
    }
    public String getDescription() {
        return description;
    }
    public RewardData getReward() {
        return reward;
    }

    public AdventureQuestData(AdventureQuestData data){
        id = data.id;
        isTemplate = false; //Anything being copied is by definition not a template
        name = data.name;
        description = data.description;
        synopsis = data.synopsis;
        offerDialog = new DialogData(data.offerDialog);
        prologue = new DialogData(data.prologue);
        epilogue = new DialogData(data.epilogue);
        failureDialog = new DialogData(data.failureDialog);
        declinedDialog = new DialogData(data.declinedDialog);
        reward = new RewardData(data.reward);
        rewardDescription = data.rewardDescription;
        completed = data.completed;
        stages = new AdventureQuestStage[data.stages.length];
        for (int i = 0; i < stages.length; i++){
            stages[i] = new AdventureQuestStage(data.stages[i]);
        }
        questSourceTags = data.questSourceTags.clone();
        questPOITags = data.questPOITags.clone();
        questEnemyTags = data.questEnemyTags.clone();
        targetPoI = data.targetPoI;
        targetEnemySprite = data.targetEnemySprite;
        storyQuest = data.storyQuest;
        sourceID = data.sourceID;
        poiTokens = data.poiTokens;
        enemyTokens = data.enemyTokens;
        otherTokens = data.otherTokens;
        isTracked = data.isTracked;
    }

    public AdventureQuestData()
    {
        declinedDialog = new DialogData();
        declinedDialog.text = "Come back tomorrow and perhaps I'll have something that you'll actually be willing to do.";
        DialogData dismiss = new DialogData();
        dismiss.name = "(Catching the not so subtle hint, you leave.)";
        declinedDialog.options = new DialogData[1];
        declinedDialog.options[0] = dismiss;
    }

    public List<AdventureQuestStage> getActiveStages(){
        List<AdventureQuestStage> toReturn = new ArrayList<>();

        //Temporarily allow only one active stage until parallel stages and prerequisites are implemented
        for (AdventureQuestStage stage : stages) {
            if (stage.getStatus() == ACTIVE) {
                toReturn.add(stage);
            }
        }
        return toReturn;
    }

    public List<AdventureQuestStage> getCompletedStages(){
        List<AdventureQuestStage> toReturn = new ArrayList<>();

        for (AdventureQuestStage stage : stages) {
            if (stage.getStatus() == COMPLETE)
                toReturn.add(stage);
        }
        return toReturn;
    }

    public List<Integer> getCompletedStageIDs(){
        List<Integer> toReturn = new ArrayList<>();

        for (AdventureQuestStage stage : getCompletedStages()) {
            toReturn.add(stage.id);
        }
        return toReturn;
    }

    public PointOfInterest getTargetPOI() {

        for (AdventureQuestStage stage : getActiveStages()) {
            targetPoI = stage.getTargetPOI();
            if (targetPoI != null)
                break;
        }

        return targetPoI;
    }

    public EnemySprite getTargetEnemySprite(){
        if (targetEnemySprite == null){
            for (AdventureQuestStage stage : getActiveStages()) {
                targetEnemySprite = stage.getTargetSprite();
                if (targetEnemySprite != null){
                    break;
                }
            }
        }
        return targetEnemySprite;
    }

    public void initialize(){
        poiTokens = new Hashtable<>();

        for (AdventureQuestStage stage : stages){
            initializeStage(stage);
        }

        replaceTokens();
    }

    public void initializeStage(AdventureQuestStage stage){
        if (stage == null || stage.objective == null) return;

        stage.initialize();

        switch  (stage.objective){
            case Arena:
                stage.setTargetPOI(poiTokens, this.name);
                break;
            case Clear:
                stage.setTargetPOI(poiTokens, this.name);
                break;
            case CompleteQuest:
                stage.setTargetPOI(poiTokens, this.name);
            case Defeat:
                stage.setTargetPOI(poiTokens, this.name);
                if (!stage.mixedEnemies)
                    stage.setTargetEnemyData(generateTargetEnemyData(stage));
                break;
            case Delivery:
                stage.setTargetPOI(poiTokens, this.name);
                //Set delivery item as a miscellaneous token
                break;
            case Escort:
                //add configuration of what is being escorted.
                stage.setTargetPOI(poiTokens, this.name);
                if (!stage.mixedEnemies)
                    stage.setTargetEnemyData(generateTargetEnemyData(stage));
                break;
            case Fetch:
                stage.setTargetPOI(poiTokens, this.name);
            case Hunt:
                stage.setTargetSprite(generateTargetEnemySprite(stage));
                break;
            case Leave:
                stage.setTargetPOI(poiTokens, this.name);
                break;
            case MapFlag:
                stage.setTargetPOI(poiTokens, this.name);
                break;
            case Patrol:
                //Need ability to set a series of target coordinates that can be reached, point nav arrow to them
                // This might get oddly complex.
                break;
            case QuestFlag:
                stage.setTargetPOI(poiTokens, this.name);
                break;
            case Rescue:
                stage.setTargetPOI(poiTokens, this.name);
                break;
            case Travel:
                stage.setTargetPOI(poiTokens, this.name);
        }

        if (stage.getTargetPOI() != null
                && ("cave".equalsIgnoreCase( stage.getTargetPOI().getData().type)
                || "dungeon".equalsIgnoreCase( stage.getTargetPOI().getData().type))){
            //todo: decide how to handle this in "anyPOI" scenarios
            WorldSave.getCurrentSave().getPointOfInterestChanges(stage.getTargetPOI().getID()).clearDeletedObjects();
        }

        PointOfInterest temp = stage.getTargetPOI();
        if (temp != null) {
            poiTokens.put("$(poi_" + stage.id + ")", temp);
            poiBiomeTokens.put("$(biome_" + stage.id + ")", GameScene.instance().getBiomeByPosition(temp.getPosition()));
        }

        EnemyData target = stage.getTargetEnemyData();
        if (target != null)
            enemyTokens.put("$(enemy_" + stage.id +")", target);

        otherTokens.put("$(playername)", Current.player().getName());
        otherTokens.put("$(currentbiome)", GameScene.instance().getAdventurePlayerLocation(false,true));
        otherTokens.put("$(playerrace)", Current.player().raceName());
    }

    public void replaceTokens(){
        replaceTokens(offerDialog);
        replaceTokens(prologue);
        replaceTokens(epilogue);
        replaceTokens(failureDialog);
        replaceTokens(declinedDialog);

        name = replaceTokens(name);
        description = replaceTokens(description);
        rewardDescription = replaceTokens(rewardDescription);

        for (AdventureQuestStage stage: stages)
        {
            replaceTokens(stage);
        }
    }

    private void replaceTokens(AdventureQuestStage stage){
        replaceTokens(stage.prologue);
        replaceTokens(stage.epilogue);
        replaceTokens(stage.failureDialog);
        stage.name = replaceTokens(stage.name);
        stage.description = replaceTokens(stage.description);
    }

    private String replaceTokens(String data){
        for (Enumeration<String> e = poiTokens.keys(); e.hasMoreElements();){
            String key = e.nextElement();
            data = data.replace(key, poiTokens.get(key).getDisplayName());
        }
        for (Enumeration<String> e = poiBiomeTokens.keys(); e.hasMoreElements();){
            String key = e.nextElement();
            data = data.replace(key, poiBiomeTokens.get(key));
        }
        for (Enumeration<String> enemy = enemyTokens.keys(); enemy.hasMoreElements();){
            String enemyKey = enemy.nextElement();
            data = data.replace(enemyKey, enemyTokens.get(enemyKey).getName());
        }
        for (Enumeration<String> other = otherTokens.keys(); other.hasMoreElements();){
            String key = other.nextElement();
            data = data.replace(key, otherTokens.get(key));
        }
        return data;
    }

    private void replaceTokens(DialogData data){
        for (DialogData option : data.options){
            replaceTokens(option);
        }
        for (Enumeration<String> e = poiTokens.keys(); e.hasMoreElements();){
            String key = e.nextElement();
            data.text = data.text.replace(key, poiTokens.get(key).getDisplayName());
            data.name = data.name.replace(key, poiTokens.get(key).getDisplayName());
        }

        for (Enumeration<String> e = poiBiomeTokens.keys(); e.hasMoreElements();){
            String key = e.nextElement();
            data.text = data.text.replace(key, poiBiomeTokens.get(key));
            data.name = data.name.replace(key, poiBiomeTokens.get(key));
        }

        for (Enumeration<String> e = enemyTokens.keys(); e.hasMoreElements();){
            String key = e.nextElement();
            data.text = data.text.replace(key, enemyTokens.get(key).getName());
            data.name = data.name.replace(key, enemyTokens.get(key).getName());
        }

        for (Enumeration<String> other = otherTokens.keys(); other.hasMoreElements();){
            String key = other.nextElement();
            data.text = data.text.replace(key, otherTokens.get(key));
            data.name = data.name.replace(key, otherTokens.get(key));
        }

        for (DialogData.ActionData ad: data.action) {
            if ( ad != null && ad.POIReference != null)
            {
                for (Enumeration<String> e = poiTokens.keys(); e.hasMoreElements(); ) {
                    String key = e.nextElement();
                    ad.POIReference = ad.POIReference.replace(key, poiTokens.get(key).getID());
                }
            }
        }
    }

    private EnemySprite generateTargetEnemySprite(AdventureQuestStage stage){
        if (stage.objective == AdventureQuestController.ObjectiveTypes.Hunt){
            EnemyData toUse = generateTargetEnemyData(stage);
            toUse.lifetime = stage.count1;
            EnemySprite toReturn =  new EnemySprite(toUse);
            toReturn.questStageID = stage.stageID.toString();
            return toReturn;
        }
        return null;
    }

    private EnemyData generateTargetEnemyData(AdventureQuestStage stage)
    {
        ArrayList<EnemyData> matchesTags = new ArrayList<>();
        for(EnemyData data: new Array.ArrayIterator<>(WorldData.getAllEnemies())) {
            ArrayList<String> candidateTags = new ArrayList<>(Arrays.asList(data.questTags));
            int tagCount = candidateTags.size();

            candidateTags.removeAll(stage.enemyExcludeTags);
            if (candidateTags.size() != tagCount) {
                continue;
            }

            candidateTags.removeAll(stage.enemyTags);
            if (candidateTags.size() == tagCount - stage.enemyTags.size()) {
                matchesTags.add(data);
            }
        }
        if (matchesTags.isEmpty()){
            return new EnemyData(Aggregates.random(WorldData.getAllEnemies()));
        }
        else{
            return new EnemyData(Aggregates.random(matchesTags));
        }
    }



    class questUpdate {

    }

    public void updateStages(AdventureQuestEvent event){
        boolean done = true;
        if (event.poi == null && MapStage.getInstance().isInMap())
            event.poi = TileMapScene.instance().rootPoint;
        for (AdventureQuestStage stage: stages) {
            switch (stage.getStatus()) {
                case ACTIVE:
                    done = stage.handleEvent(event) == COMPLETE && done;
                    break;
                case COMPLETE:
                    continue;
                default:
                    done = false;
                    break;
            }
            failed |= stage.getStatus() == FAILED;
        }
        completed = done;
    }

    public DialogData getPrologue() {
        if (!prologueDisplayed) {
            prologueDisplayed = true;
            return prologue;
        }
        return null;
    }

    public DialogData getEpilogue() {
        if (!epilogueDisplayed) {
            epilogueDisplayed = true;
            return epilogue;
        }
        return null;
    }

    public void fail(){
        failed = true;
        isTracked = false;
        //todo: handle any necessary cleanup or reputation loss
    }

    public void activateNextStages() {
        boolean showNotification = false;
        for (AdventureQuestStage s : stages) {
            if (s.getStatus() == INACTIVE){
                s.checkPrerequisites(getCompletedStageIDs());
                if (s.getStatus() == ACTIVE) {
                    AdventureQuestController.instance().addQuestSprites(s);
                    showNotification = true;
                }
            }
        }
        if (showNotification) {
            StringBuilder description = new StringBuilder();
            description.append("[!]").append(name).append("[]");
            for (AdventureQuestStage stage : getActiveStages()) {
                description.append("\n")
                        .append(stage.name).append("\n[/]")
                        //.append(stage.description.length()<=50?stage.description:stage.description.substring(0,49) + "...")
                        .append(stage.description)
                        .append("[]");
            }
            GameHUD.getInstance().addNotification(description.toString());
        }

    }

    public PointOfInterest getClosestValidPOI(Vector2 pos) {
        List<PointOfInterest> validPOIs = new ArrayList<>();
        for (AdventureQuestStage stage : getActiveStages()) {
            validPOIs.addAll(stage.getValidPOIs());
        }
        if (validPOIs.isEmpty())
            return null;
        validPOIs.sort(Comparator.comparingInt(a -> (int) a.getPosition().dst(pos)));
        return validPOIs.get(0);

    }
}

