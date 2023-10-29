package forge.adventure.util;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import forge.adventure.character.EnemySprite;
import forge.adventure.data.*;
import forge.adventure.pointofintrest.PointOfInterest;
import forge.adventure.pointofintrest.PointOfInterestChanges;
import forge.adventure.stage.GameStage;
import forge.adventure.stage.MapStage;
import forge.util.Aggregates;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class AdventureQuestController implements Serializable {

    public Map<String, Float> getBoostedSpawns(List<EnemyData> localSpawns) {
        Map<String,Float> boostedSpawns = new HashMap<>();
        for (AdventureQuestData q : Current.player().getQuests()){
            for (AdventureQuestStage c : q.stages){
                if (c.getStatus().equals(QuestStatus.Active) && c.objective.equals(ObjectiveTypes.Defeat))
                {
                    List<String> toBoost = new ArrayList<>();
                    if (c.mixedEnemies){
                        for (EnemyData enemy : localSpawns){
                            List<String> candidateTags = Arrays.stream(enemy.questTags).collect(Collectors.toList());
                            for (String targetTag : c.enemyTags) {
                                if (!candidateTags.contains(targetTag)) {
                                    continue;
                                }
                                toBoost.add(enemy.getName());
                            }
                        }
                    }
                    else{
                        toBoost.add(c.getTargetEnemyData().getName());
                    }
                    if (!toBoost.isEmpty()) {
                        float value = 2.0f / toBoost.size();
                        for (String key : toBoost) {
                            float existingValue = boostedSpawns.getOrDefault(key, 0.0f);
                                boostedSpawns.put(key, value + existingValue);
                        }
                    }
                }
            }
        }
        return boostedSpawns;
    }

    public enum ObjectiveTypes{
        None,
        Arena,
        Clear,
        Defeat,
        Delivery,
        Escort,
        EventFinish,
        EventWin,
        EventWinMatches,
        Fetch,
        Find,
        Gather,
        Give,
        HaveReputation,
        Hunt,
        MapFlag,
        Leave,
        Patrol,
        QuestFlag,
        Rescue,
        Siege,
        Travel,
        Use
    }

    public enum QuestStatus{
        None,
        Inactive,
        Active,
        Complete,
        Failed
    }
    private Map<String, Long> nextQuestDate = new HashMap<>();
    private int maximumSideQuests = 5; //todo: move to configuration file
    private transient boolean inDialog = false;
    private transient Array<AdventureQuestData> allQuests = new Array<>();
    private transient Array<AdventureQuestData> allSideQuests = new Array<>();
    private Queue<DialogData> dialogQueue = new LinkedList<>();
    private Map<String,Date> questAvailability = new HashMap<>();
    public PointOfInterest mostRecentPOI;
    private List<EnemySprite> enemySpriteList= new ArrayList<>();
    private int nextQuestID = 0;
    public void showQuestDialogs(GameStage stage) {
        List<AdventureQuestData> finishedQuests = new ArrayList<>();

        if (stage instanceof MapStage){
            for (AdventureQuestData quest : Current.player().getQuests()) {
                DialogData prologue = quest.getPrologue();
                if (prologue != null && (!prologue.text.isEmpty()) ){
                    dialogQueue.add(prologue);
                }
                for (AdventureQuestStage questStage : quest.stages)
                {
                    if (questStage.prologue != null && (!questStage.prologue.text.isEmpty()) && !questStage.prologueDisplayed){
                        questStage.prologueDisplayed = true;
                        dialogQueue.add(questStage.prologue);
                    }

                    if (questStage.getStatus() == QuestStatus.Failed && questStage.failureDialog != null && !questStage.failureDialog.text.isEmpty()){
                        dialogQueue.add(questStage.failureDialog);
                        continue;
                    }

                    if (questStage.getStatus() == QuestStatus.Complete && questStage.epilogue != null && (!questStage.epilogue.text.isEmpty()) && !questStage.epilogueDisplayed){
                        questStage.epilogueDisplayed = true;
                        dialogQueue.add(questStage.epilogue);
                    }
                    if (questStage.getStatus() != QuestStatus.Complete){
                        break;
                    }
                }

                if (quest.failed){
                    finishedQuests.add(quest);
                    if (quest.failureDialog != null && !quest.failureDialog.text.isEmpty()){
                        dialogQueue.add(quest.failureDialog);
                    }
                }

                if (!quest.completed)
                    continue;
                DialogData epilogue = quest.getEpilogue();
                if (epilogue != null && (!epilogue.text.isEmpty())){
                    dialogQueue.add(epilogue);

                }
                finishedQuests.add(quest);
            }
            if (!inDialog){
                inDialog = true;
                displayNextDialog((MapStage) stage);
            }
        }
        for (AdventureQuestData toRemove : finishedQuests) {

            if (!toRemove.failed && locationHasMoreQuests()){
                nextQuestDate.remove(toRemove.sourceID);
            }

            Current.player().removeQuest(toRemove);
            //Todo: Add quest to a separate "completed / failed" log?
        }
    }

    public boolean locationHasMoreQuests(){
        //intent: eventually stop providing quests for the day in a given town to encourage exploration
        //todo: make values configurable
        return new Random().nextFloat() <= 0.85f;
    }
    public void displayNextDialog(MapStage stage){
        if (dialogQueue.peek() == null)
        {
            inDialog = false;
            return;
        }

        MapDialog dialog = new MapDialog(dialogQueue.remove(), stage, -1);
        stage.showDialog();
        dialog.activate();
        ChangeListener listen = new ChangeListener() {
            @Override
            public void changed(ChangeEvent changeEvent, Actor actor) {
                displayNextDialog(stage);
            }
        };
        dialog.addDialogCompleteListener(listen);

    }
    public static class DistanceSort implements Comparator<PointOfInterest>
    {
        //ToDo: Make this more generic, compare PoI, mobs, random points, and player position
        // In process, perhaps adjust nav indicator based on distance to target
        //Sorts POI by distance from the player
        public int compare(PointOfInterest a, PointOfInterest b)
        {
            float distToA = new Vector2(a.getPosition()).sub(Current.player().getWorldPosX(), Current.player().getWorldPosY()).len();
            float distToB = new Vector2(b.getPosition()).sub(Current.player().getWorldPosX(), Current.player().getWorldPosY()).len();
            if (distToA - distToB < 0.0f)
                return -1;
            else if (distToA - distToB > 0.0f)
                return 1;
            return 0;
        }
    }
    private static AdventureQuestController object;

    public static AdventureQuestController instance() {
        if (object == null) {
            object = new AdventureQuestController();
            object.loadData();
        }
        return object;
    }

    public static void clear(){
        object = null;
    }

    private AdventureQuestController(){

    }

    public AdventureQuestController(AdventureQuestController other){
        if (object == null) {
            maximumSideQuests = other.maximumSideQuests;
            mostRecentPOI = other.mostRecentPOI;
            dialogQueue = other.dialogQueue;
            questAvailability = other.questAvailability;

            object = this;
            loadData();
        }
        else{
            System.out.println("Could not initialize AdventureQuestController. An instance already exists and cannot be merged.");
        }
    }

    private void loadData(){
        Json json = new Json();
        FileHandle handle = Config.instance().getFile(Paths.QUESTS);
        if (handle.exists())
        {
            allQuests =json.fromJson(Array.class, AdventureQuestData.class, handle);
        }

        for (AdventureQuestData q : allQuests){
            if (q.storyQuest) continue;
            allSideQuests.add(q);
        }
    }

    public int getNextQuestID(){
        if (nextQuestID == 0 && allQuests.size > 0) {
            for (int i = 0; i < allQuests.size; i++) {
                if (allQuests.get(i).getID() >= nextQuestID){
                    nextQuestID = allQuests.get(i).getID() + 1;
                }
            }
        }
        return nextQuestID++;
    }

    public void updateEnteredPOI(PointOfInterest arrivedAt)
    {
        for(AdventureQuestData currentQuest : Current.player().getQuests()) {
            currentQuest.updateEnteredPOI(arrivedAt);
        }
    }

    public void updateQuestsMapFlag(String updatedMapFlag, int updatedFlagValue)
    {
        for(AdventureQuestData currentQuest : Current.player().getQuests()) {
            currentQuest.updateMapFlag(updatedMapFlag, updatedFlagValue);
        }
    }

    public void updateQuestsCharacterFlag(String updatedCharacterFlag, int updatedCharacterFlagValue)
    {
        for(AdventureQuestData currentQuest : Current.player().getQuests()) {
            currentQuest.updateCharacterFlag(updatedCharacterFlag, updatedCharacterFlagValue);
        }
    }

    public void updateQuestsQuestFlag(String updatedQuestFlag, int updatedQuestFlagValue)
    {
        for(AdventureQuestData currentQuest : Current.player().getQuests()) {
            currentQuest.updateQuestFlag(updatedQuestFlag, updatedQuestFlagValue);
        }
    }

    public void updateQuestsLeave(){
        for(AdventureQuestData currentQuest : Current.player().getQuests()) {
            currentQuest.updateLeave();
        }
    }

    public void updateQuestsWin(EnemySprite defeated, ArrayList<EnemySprite> enemies){
        enemySpriteList.remove(defeated);
        boolean allEnemiesCleared = true;
        if (enemies != null) {
            //battle was won in a dungeon, check for "clear" objectives
            for (EnemySprite enemy : enemies) {
                if (enemy.getStage() != null && !enemy.equals(defeated)) {
                    //actor is an enemy that is present on the map. Check to see if there's a valid reason.
                    if (enemy.defeatDialog != null) {
                        //This enemy cannot be removed from the map by defeating it, ignore it for "cleared" purposes
                        continue;
                    }
                    allEnemiesCleared = false;
                    break;
                }
            }
        }
        for(AdventureQuestData currentQuest : Current.player().getQuests()) {
            currentQuest.updateWin(defeated, allEnemiesCleared);
        }
    }
    public void updateQuestsWin(EnemySprite defeated){
        updateQuestsWin(defeated, null);
    }

    public void updateQuestsLose(EnemySprite defeatedBy){
        enemySpriteList.remove(defeatedBy);
        for(AdventureQuestData currentQuest : Current.player().getQuests()) {
            currentQuest.updateLose(defeatedBy);
        }
    }

    public void updateDespawn(EnemySprite despawned){
        enemySpriteList.remove(despawned);
        for(AdventureQuestData currentQuest: Current.player().getQuests()) {
            currentQuest.updateDespawn(despawned);
        }
    }

    public void updateArenaComplete(boolean winner){
        for(AdventureQuestData currentQuest: Current.player().getQuests()) {
            currentQuest.updateArenaComplete(winner);
        }
    }

    public void updateEventComplete(AdventureEventData completedEvent) {
        for(AdventureQuestData currentQuest: Current.player().getQuests()) {
            currentQuest.updateEventComplete(completedEvent);
        }
    }

    public AdventureQuestData generateQuest(int id){
        AdventureQuestData generated = null;
        for (AdventureQuestData template: allQuests) {
            if (template.isTemplate && template.getID() == id){
                generated = new AdventureQuestData(template);
                generated.initialize();
                break;
            }
        }
        return generated;
    }

    public void addQuestSprites(AdventureQuestStage stage){
        if (stage.getTargetSprite() != null){
            enemySpriteList.add(stage.getTargetSprite());
        }
    }
    public List<EnemySprite> getQuestSprites(){
        return enemySpriteList;
    }

    public void rematchQuestSprite(EnemySprite sprite){
        for (AdventureQuestData q : Current.player().getQuests()){
            for (AdventureQuestStage s : q.stages){
                if (sprite.questStageID != null && s.stageID != null && sprite.questStageID.equals(s.stageID.toString())) {
                    s.setTargetSprite(sprite);
                }
            }
        }
    }

    String randomItemName()
    {  //todo: expand and include in fetch/delivery quests
        String[] options = {"collection of frequently asked questions","case of card sleeves", "well loved playmat", "copy of Richard Garfield's autobiography", "collection of random foreign language cards", "lucky coin", "giant card binder", "unsorted box of commons", "bucket full of pieces of shattered artifacts","depleted mana shard"};

        return Aggregates.random(options);
    }

    public void abandon(AdventureQuestData quest){
        quest.fail();
    }

    public AdventureQuestData getQuestNPCResponse(String pointID, PointOfInterestChanges changes, String questOrigin) {
        AdventureQuestData ret;

        for (AdventureQuestData q : Current.player().getQuests()) {
            if (q.completed || q.storyQuest)
                continue;
            if (q.sourceID.equals(pointID)) {
                //remind player about current active side quest
                DialogData response = new DialogData();
                response.text = "\"You haven't finished the last thing we asked you to do!\" (" + q.name +") ";
                DialogData dismiss = new DialogData();
                dismiss.name = "\"Oh, right, let me go take care of that.\"";
                response.options = new DialogData[]{dismiss};
                ret = new AdventureQuestData();
                ret.offerDialog = response;
                return ret;
            }
        }
        if (nextQuestDate.containsKey(pointID) && nextQuestDate.get(pointID) >= LocalDate.now().toEpochDay()){
            //No more side quests available here today due to previous activity
            DialogData response = new DialogData();
            response.text = "\"We don't have anything new for you to do right now. Come back tomorrow.\"";
            DialogData dismiss = new DialogData();
            dismiss.name = "\"Okay.\" (Leave)";
            response.options = new DialogData[]{dismiss};
            ret = new AdventureQuestData();
            ret.offerDialog = response;
            return ret;
        }

        if (tooManyQuests(Current.player().getQuests())) {
            //No more side quests available here today, too many active
            DialogData response = new DialogData();
            response.text = "\"Adventurer, we need your assistance!\"";
            DialogData dismiss = new DialogData();
            dismiss.name = "\"I can't, I have far too many things to do right now\" (Your quest log is too full already) (Leave)";
            response.options = new DialogData[]{dismiss};
            ret = new AdventureQuestData();
            ret.offerDialog = response;
            return ret;
        }
        //todo - Should quest availability be weighted instead of uniform?
        nextQuestDate.put(pointID, LocalDate.now().toEpochDay());

        Array<AdventureQuestData> validSideQuests = new Array<>();
        for (AdventureQuestData option : allSideQuests){
            if (option.questSourceTags.length == 0)
                validSideQuests.add(option);
            for (int i = 0; i < option.questSourceTags.length; i++){
                if (option.questSourceTags[i] != null && option.questSourceTags[i].equals(questOrigin)){
                    validSideQuests.add(option);
                    break;
                }
            }
        }
        if (validSideQuests.size > 0)
            ret = new AdventureQuestData(Aggregates.random(validSideQuests));
        else
            ret = new AdventureQuestData(Aggregates.random(allSideQuests));
        ret.sourceID = pointID;
        ret.initialize();
        return ret;
    }

    private boolean tooManyQuests(List<AdventureQuestData> existing){
        int sideQuests = 0;

        for (AdventureQuestData quest : existing){
            if (quest.storyQuest || quest.completed || quest.failed)
                continue;
            sideQuests++;
        }
        return (sideQuests >= maximumSideQuests);
    }
}
