package forge.adventure.util;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.XmlReader;
import forge.adventure.data.AdventureQuestData;
import forge.adventure.pointofintrest.PointOfInterest;
import forge.adventure.pointofintrest.PointOfInterestChanges;
import forge.adventure.world.WorldSave;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;

/**
 * Optional world/quest_rules.json entries: oneTime prevents repeat offers and requiredEnemy must not
 * have been defeated in requiredPOI. Rules are never serialized into active quests.
 */
public class AdventureQuestEligibility {
    private static AdventureQuestEligibility current = new AdventureQuestEligibility();
    private final Map<Integer, Rule> rules = new HashMap<>();
    private final Map<String, List<Integer>> enemyObjects = new HashMap<>();

    private static class Rule {
        private final boolean oneTime;
        private final String requiredEnemy;
        private final String requiredPOI;

        private Rule(boolean oneTime, String requiredEnemy, String requiredPOI) {
            this.oneTime = oneTime;
            this.requiredEnemy = requiredEnemy;
            this.requiredPOI = requiredPOI;
        }
    }

    public static AdventureQuestEligibility current() {
        AdventureQuestController.instance();
        return current;
    }

    public static void clear() {
        current = new AdventureQuestEligibility();
    }

    public static void loadRules(String source) {
        AdventureQuestEligibility loaded = new AdventureQuestEligibility();
        loaded.readRules(source);
        current = loaded;
    }

    public void readRules(String source) {
        rules.clear();
        enemyObjects.clear();
        // TODO: Audit and opt in other fixed boss quests (Kiora, Xira, Slobad) before applying one-time rules to them.
        for (JsonValue value : new JsonReader().parse(source)) {
            int questID = value.getInt("questID");
            if (questID <= 0) {
                throw new IllegalArgumentException("Quest eligibility rules require a stable positive questID");
            }
            boolean oneTime = value.getBoolean("oneTime", false);
            String requiredEnemy = value.getString("requiredEnemy", "");
            String requiredPOI = value.getString("requiredPOI", "");
            if (!requiredEnemy.isEmpty() && requiredPOI.isEmpty()) {
                throw new IllegalArgumentException("requiredEnemy needs a requiredPOI to identify its target location");
            }
            rules.put(questID, new Rule(oneTime, requiredEnemy, requiredPOI));
        }
    }

    public boolean isEligible(AdventureQuestData quest, List<AdventureQuestData> active,
                              AdventureQuestHistory history, BiPredicate<String, String> targetEnemyDefeated) {
        Rule rule = rules.get(quest.getID());
        for (AdventureQuestData existing : active) {
            if (existing == quest) {
                return false; // Replaying the same acceptance must not add the same object twice.
            }
            if (rule != null && rule.oneTime && existing.getID() == quest.getID() && !existing.failed) {
                return false; // Includes completed instances awaiting their completion dialog.
            }
        }
        if (rule == null) {
            return true;
        }
        if (rule.oneTime && history.contains(quest.getID())) {
            return false;
        }
        // Older saves already retain deleted enemy objects separately for each location.
        return rule.requiredEnemy.isEmpty()
                || !targetEnemyDefeated.test(rule.requiredPOI, rule.requiredEnemy);
    }

    public boolean isTargetEnemyDefeated(String poiName, String enemyName) {
        return isTargetEnemyDefeated(poiName, enemyName, Current.world().getAllPointOfInterest(),
                map -> new FileHandle(Config.instance().getCommonFilePath(map)),
                id -> WorldSave.getCurrentSave().getPointOfInterestChanges(id));
    }

    public boolean isTargetEnemyDefeated(String poiName, String enemyName, Iterable<PointOfInterest> locations,
                                       Function<String, FileHandle> mapFiles,
                                       Function<String, PointOfInterestChanges> savedChanges) {
        for (PointOfInterest poi : locations) {
            if (!poiName.equals(poi.getData().name)) {
                continue;
            }
            PointOfInterestChanges changes = savedChanges.apply(poi.getID());
            if (changes == null) {
                continue;
            }
            String map = poi.getData().map;
            List<Integer> ids = enemyObjects.computeIfAbsent(map + "|" + enemyName,
                    key -> readEnemyObjects(mapFiles.apply(map), enemyName));
            for (int id : ids) {
                if (changes.isObjectDeleted(id)) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<Integer> readEnemyObjects(FileHandle map, String enemyName) {
        List<Integer> ids = new ArrayList<>();
        // TODO: Support bosses in connected submaps and enemy properties inherited from object templates.
        // The configured Teferi hideouts declare their enemy objects explicitly in the root map.
        for (XmlReader.Element object : new XmlReader().parse(map).getChildrenByNameRecursively("object")) {
            XmlReader.Element properties = object.getChildByName("properties");
            if (properties == null) {
                continue;
            }
            for (XmlReader.Element property : properties.getChildrenByName("property")) {
                if ("enemy".equals(property.getAttribute("name"))
                        && enemyName.equals(property.getAttribute("value", property.getText()))) {
                    ids.add(object.getIntAttribute("id"));
                    break;
                }
            }
        }
        return ids;
    }

    public boolean accept(AdventureQuestData quest, List<AdventureQuestData> active,
                          AdventureQuestHistory history, BiPredicate<String, String> targetEnemyDefeated) {
        if (!isEligible(quest, active, history, targetEnemyDefeated)) {
            return false;
        }
        active.add(quest);
        return true;
    }

    public Array<AdventureQuestData> offers(Iterable<AdventureQuestData> templates, String origin,
                                          List<AdventureQuestData> active, AdventureQuestHistory history,
                                          BiPredicate<String, String> targetEnemyDefeated) {
        Array<AdventureQuestData> offers = new Array<>();
        for (AdventureQuestData quest : templates) {
            if (!isEligible(quest, active, history, targetEnemyDefeated)) {
                continue;
            }
            if (quest.questSourceTags.length == 0) {
                offers.add(quest);
                continue;
            }
            for (String tag : quest.questSourceTags) {
                if (tag != null && tag.equals(origin)) {
                    offers.add(quest);
                    break;
                }
            }
        }
        return offers;
    }

    public void reconcile(List<AdventureQuestData> quests) {
        Map<Integer, AdventureQuestData> retained = new HashMap<>();
        for (AdventureQuestData quest : quests) {
            Rule rule = rules.get(quest.getID());
            if (rule == null || !rule.oneTime) {
                continue;
            }
            AdventureQuestData previous = retained.get(quest.getID());
            if (previous == null) {
                retained.put(quest.getID(), quest);
            } else {
                // TODO: Compare partial objective progress if one-time rules expand beyond Clear/Travel quests.
                boolean preferQuest = (previous.failed && !quest.failed)
                        || (previous.failed == quest.failed
                        && quest.getCompletedStageIDs().size() > previous.getCompletedStageIDs().size());
                AdventureQuestData survivor = preferQuest ? quest : previous;
                survivor.isTracked = previous.isTracked || quest.isTracked;
                retained.put(quest.getID(), survivor);
            }
        }
        Set<Integer> seen = new HashSet<>();
        // Remove duplicates directly: abandonment would apply penalties and completion would grant rewards.
        quests.removeIf(quest -> retained.containsKey(quest.getID())
                && (retained.get(quest.getID()) != quest || !seen.add(quest.getID())));
    }
}
