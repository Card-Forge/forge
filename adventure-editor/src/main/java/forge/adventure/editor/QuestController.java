package forge.adventure.editor;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import forge.adventure.data.AdventureQuestData;
import forge.adventure.data.EnemyData;
import forge.adventure.data.PointOfInterestData;
import forge.adventure.util.Config;
import forge.adventure.util.Paths;

import javax.swing.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class QuestController {

    public final DefaultListModel<String> POITags = new DefaultListModel<>();
    public final DefaultListModel<String> enemyTags = new DefaultListModel<>();
    public final DefaultListModel<String> questEnemyTags = new DefaultListModel<>();
    public final DefaultListModel<String> questTags = new DefaultListModel<>();
    public final DefaultListModel<String> questPOITags = new DefaultListModel<>();
    private final DefaultListModel<PointOfInterestData> allPOI = new DefaultListModel<>();
    private final DefaultListModel<EnemyData> allEnemies = new DefaultListModel<>();
    private final DefaultListModel<String> questSourceTags = new DefaultListModel<>();

    private final DefaultListModel<AdventureQuestData> allQuests = new DefaultListModel<>();

    private static QuestController instance;

    public static QuestController getInstance() {
        if (instance == null)
            instance = new QuestController();
        return instance;
    }

    public DefaultListModel<AdventureQuestData> getAllQuests() {
            return allQuests;
    }

    private QuestController(){
        load();
    }

    public DefaultListModel<String> getEnemyTags(){
        DefaultListModel<String> toReturn = new DefaultListModel<>();
        for (int i = 0; i < enemyTags.size(); i++){
            toReturn.removeElement(enemyTags.get(i));
            toReturn.addElement(enemyTags.get(i));
        }

        List<Object> sortedObjects = Arrays.stream(toReturn.toArray()).sorted().collect(Collectors.toList());

        toReturn.clear();

        for (Object sortedObject : sortedObjects) {
            toReturn.addElement((String) sortedObject);
        }

        return toReturn;
    }

    public DefaultListModel<String> getPOITags(){
        DefaultListModel<String> toReturn = new DefaultListModel<>();
        for (int i = 0; i < POITags.size(); i++){
            toReturn.removeElement(POITags.get(i));
            toReturn.addElement(POITags.get(i));
        }

        List<Object> sortedObjects = Arrays.stream(toReturn.toArray()).sorted().collect(Collectors.toList());

        toReturn.clear();

        for (Object sortedObject : sortedObjects) {
            toReturn.addElement((String) sortedObject);
        }

        return toReturn;
    }

    public DefaultListModel<String> getSourceTags(){
        DefaultListModel<String> toReturn = new DefaultListModel<>();

        for (int i = 0; i < questSourceTags.size(); i++)
        {
            toReturn.removeElement(questSourceTags.get(i));
            toReturn.addElement(questSourceTags.get(i));
        }

        List<Object> sortedObjects = Arrays.stream(toReturn.toArray()).sorted().collect(Collectors.toList());

        toReturn.clear();

        for (Object sortedObject : sortedObjects) {
            toReturn.addElement((String) sortedObject);
        }

        return toReturn;
    }

    public void refresh(){
        enemyTags.clear();
        POITags.clear();
        questPOITags.clear();
        questEnemyTags.clear();
        questTags.clear();
        questSourceTags.clear();

        for (int i=0;i<allEnemies.size();i++) {
            for (String tag : allEnemies.get(i).questTags)
            {
                enemyTags.removeElement(tag); //Ensure uniqueness
                if (tag!= null)
                    enemyTags.addElement(tag);
            }
        }

        for (int i=0;i<allPOI.size();i++) {
            for (String tag : allPOI.get(i).questTags)
            {
                POITags.removeElement(tag); //Ensure uniqueness
                if (tag!= null)
                    POITags.addElement(tag);
            }
        }

        for (int i=0;i<allQuests.size();i++) {

            for (String tag : allQuests.get(i).questEnemyTags)
            {
                questEnemyTags.removeElement(tag); //Ensure uniqueness
                if (tag!= null)
                    questEnemyTags.addElement(tag);
            }
            for (String tag : allQuests.get(i).questPOITags)
            {
                questPOITags.removeElement(tag); //Ensure uniqueness
                if (tag!= null)
                    questPOITags.addElement(tag);
            }

            for (String tag : allQuests.get(i).questSourceTags)
            {
                questSourceTags.removeElement(tag); //Ensure uniqueness
                if (tag!= null)
                    questSourceTags.addElement(tag);
            }

        }
    }

    public void load()
    {
        allEnemies.clear();
        Array<EnemyData> enemyJSON=new Array<>();
        Json json = new Json();
        FileHandle handle = Config.instance().getFile(Paths.ENEMIES);
        if (handle.exists())
        {
            enemyJSON = json.fromJson(Array.class, EnemyData.class, handle);
        }
        for (int i=0;i<enemyJSON.size;i++) {
            allEnemies.add(i,enemyJSON.get(i));
            for (String tag : enemyJSON.get(i).questTags)
            {
                enemyTags.removeElement(tag); //Ensure uniqueness
                if (tag!= null)
                    enemyTags.addElement(tag);
            }
        }

        allPOI.clear();
        Array<PointOfInterestData> POIJSON=new Array<>();
        json = new Json();
        handle = Config.instance().getFile(Paths.POINTS_OF_INTEREST);
        if (handle.exists())
        {
            POIJSON = json.fromJson(Array.class, PointOfInterestData.class, handle);
        }
        for (int i=0;i<POIJSON.size;i++) {
            allPOI.add(i,POIJSON.get(i));
            for (String tag : POIJSON.get(i).questTags)
            {
                POITags.removeElement(tag); //Ensure uniqueness
                if (tag!= null)
                    POITags.addElement(tag);
            }
        }

        allQuests.clear();
        Array<AdventureQuestData> questJSON=new Array<>();
        json = new Json();
        handle = Config.instance().getFile(Paths.QUESTS);
        if (handle.exists())
        {
            questJSON = json.fromJson(Array.class, AdventureQuestData.class, handle);
        }
        for (int i=0;i<questJSON.size;i++) {
            AdventureQuestData template = questJSON.get(i);
            template.isTemplate = true;

            allQuests.add(i,template);

            for (String tag : template.questEnemyTags)
            {
                questEnemyTags.removeElement(tag); //Ensure uniqueness
                if (tag!= null)
                    questEnemyTags.addElement(tag);
            }
            for (String tag : template.questPOITags)
            {
                questPOITags.removeElement(tag); //Ensure uniqueness
                if (tag!= null)
                    questPOITags.addElement(tag);
            }
            for (String tag : template.questSourceTags)
            {
                questSourceTags.removeElement(tag); //Ensure uniqueness
                if (tag!= null)
                    questSourceTags.addElement(tag);
            }
        }
    }

    void save()
    {
        Json json = new Json(JsonWriter.OutputType.json);
        FileHandle handle = Config.instance().getFile(Paths.QUESTS);
        AdventureQuestData[] saveData = Arrays.stream(allQuests.toArray()).map(AdventureQuestData.class::cast).toArray(AdventureQuestData[]::new);

        handle.writeString(json.prettyPrint(json.toJson(saveData,Array.class, AdventureQuestData.class)),false);

    }
}
