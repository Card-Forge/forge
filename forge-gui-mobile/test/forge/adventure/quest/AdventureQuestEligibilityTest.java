package forge.adventure.quest;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.XmlReader;
import forge.adventure.data.AdventureQuestData;
import forge.adventure.data.PointOfInterestData;
import forge.adventure.player.PlayerStatistic;
import forge.adventure.pointofintrest.PointOfInterest;
import forge.adventure.pointofintrest.PointOfInterestChanges;
import forge.adventure.util.AdventureQuestEligibility;
import forge.adventure.util.AdventureQuestHistory;
import forge.adventure.util.SaveFileData;
import forge.util.Localizer;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiPredicate;

public class AdventureQuestEligibilityTest {
    private static final String RULES = """
            [{"questID": 1, "oneTime": true, "requiredEnemy": "Boss", "requiredPOI": "Boss Hideout"}]
            """;
    private static final String TEMPLATES = """
            [
              {"id": 1, "isTemplate": true, "name": "Boss quest", "questSourceTags": ["town"],
               "stages": [{"id": 1, "objective": "Clear"},
                          {"id": 2, "objective": "Travel", "prerequisiteIDs": [1]}]},
              {"id": 2, "isTemplate": true, "name": "Repeatable errand",
               "questSourceTags": ["other"]}
            ]
            """;

    private Path resources;

    private Array<AdventureQuestData> readQuests(String source) {
        return new Json().fromJson(Array.class, AdventureQuestData.class, source);
    }

    private PointOfInterest location(PointOfInterestData data, String id) {
        return new PointOfInterest() {
            @Override
            public PointOfInterestData getData() {
                return data;
            }

            @Override
            public String getID() {
                return id;
            }
        };
    }

    private String enemyAt(FileHandle map, int objectID) {
        for (XmlReader.Element object : new XmlReader().parse(map).getChildrenByNameRecursively("object")) {
            if (object.getIntAttribute("id") == objectID) {
                for (XmlReader.Element property : object.getChildByName("properties").getChildrenByName("property")) {
                    if ("enemy".equals(property.getAttribute("name"))) {
                        return property.getAttribute("value");
                    }
                }
            }
        }
        return null;
    }

    @BeforeClass
    public void initializeLocalization() {
        resources = Paths.get(System.getProperty("user.dir"), "forge-gui", "res");
        if (!Files.isDirectory(resources)) {
            resources = Paths.get(System.getProperty("user.dir"), "..", "forge-gui", "res");
        }
        Localizer.getInstance().initialize("en-US", resources.resolve("languages").normalize().toString());
    }

    @Test
    public void duplicateAcceptanceAcrossTownsAndCallbacksKeepsOneQuest() {
        AdventureQuestEligibility eligibility = new AdventureQuestEligibility();
        eligibility.readRules(RULES);
        Array<AdventureQuestData> templates = readQuests(TEMPLATES);
        AdventureQuestHistory history = new AdventureQuestHistory();
        BiPredicate<String, String> targetDefeated = (poi, enemy) -> false;
        List<AdventureQuestData> active = new ArrayList<>();
        AdventureQuestData first = new AdventureQuestData(templates.first());
        first.sourceID = "town-A";
        AdventureQuestData second = new AdventureQuestData(templates.first());
        second.sourceID = "town-B";

        Assert.assertTrue(eligibility.accept(first, active, history, targetDefeated));
        Assert.assertTrue(eligibility.offers(templates, "town", active, history, targetDefeated).isEmpty());
        Assert.assertFalse(eligibility.accept(second, active, history, targetDefeated));
        Assert.assertFalse(eligibility.accept(first, active, history, targetDefeated));
        Assert.assertEquals(active.size(), 1);
        Assert.assertSame(active.get(0), first);

        // Old saves can already contain duplicates. Keep the furthest stage and its original town.
        AdventureQuestData progressed = readQuests(
                TEMPLATES.replace("\"objective\": \"Clear\"", "\"objective\": \"Clear\", \"status\": \"COMPLETE\""))
                .first();
        progressed.sourceID = "town-B";
        first.isTracked = true;
        active.add(progressed);
        active.add(progressed);
        SaveFileData oldPlayerData = new SaveFileData();
        oldPlayerData.storeObject("quests", active.toArray());
        active = new ArrayList<>();
        for (Object quest : (Object[]) oldPlayerData.readObject("quests")) {
            active.add((AdventureQuestData) quest);
        }
        progressed = active.get(1);
        eligibility.reconcile(active);
        Assert.assertEquals(active.size(), 1);
        Assert.assertSame(active.get(0), progressed);
        Assert.assertEquals(progressed.sourceID, "town-B");
        Assert.assertTrue(progressed.isTracked);
        Assert.assertFalse(progressed.failed);
        Assert.assertFalse(progressed.completed);
        Assert.assertFalse(history.contains(1));
    }

    @Test
    public void completionHistorySurvivesSaveLoadWithoutDisablingRepeatableQuests() {
        AdventureQuestEligibility eligibility = new AdventureQuestEligibility();
        eligibility.readRules(RULES);
        Array<AdventureQuestData> templates = readQuests(TEMPLATES);
        AdventureQuestHistory original = new AdventureQuestHistory();
        original.record(1);
        original.record(2);
        SaveFileData playerData = new SaveFileData();
        playerData.store("questHistory", original.save());
        AdventureQuestHistory restored = new AdventureQuestHistory();
        restored.load(playerData.readSubData("questHistory"));
        Assert.assertTrue(restored.contains(1));
        Assert.assertTrue(restored.contains(2));

        List<AdventureQuestData> active = new ArrayList<>();
        BiPredicate<String, String> targetDefeated = (poi, enemy) -> false;
        Assert.assertTrue(eligibility.offers(templates, "town", active, restored, targetDefeated).isEmpty());
        Assert.assertFalse(eligibility.accept(new AdventureQuestData(templates.first()), active, restored, targetDefeated));
        Assert.assertTrue(eligibility.accept(new AdventureQuestData(templates.get(1)), active, restored, targetDefeated));
        Assert.assertTrue(eligibility.accept(new AdventureQuestData(templates.get(1)), active, restored, targetDefeated));
        Assert.assertEquals(active.size(), 2);

        restored.clear();
        Assert.assertTrue(eligibility.isEligible(templates.first(), active, restored, targetDefeated));
    }

    @Test
    public void olderSavesUseTargetLocationInsteadOfGlobalBossVictories() throws Exception {
        AdventureQuestHistory history = new AdventureQuestHistory();
        history.record(36);
        history.load(new SaveFileData().readSubData("questHistory"));
        Assert.assertFalse(history.contains(36));
        PlayerStatistic original = new PlayerStatistic();
        original.setResult("Teferi", true);
        PlayerStatistic restoredStatistics = new PlayerStatistic();
        restoredStatistics.load(original.save());
        Assert.assertEquals(restoredStatistics.getWinLossRecord().get("Teferi").getLeft().intValue(), 1);
        List<AdventureQuestData> active = new ArrayList<>();
        Path adventure = resources.resolve("adventure");
        Path common = adventure.resolve("common");
        FileHandle mirrorMap = new FileHandle(adventure.resolve(
                "Realm of Legends/maps/map/magetower/Mirror_Gallery_U.tmx").toFile());
        Assert.assertEquals(enemyAt(mirrorMap, 103), "Teferi");

        for (String variant : Arrays.asList("Shandalar", "Shandalar Old Border", "Realm of Legends",
                "Innistrad", "Crystal_Kingdoms")) {
            AdventureQuestEligibility eligibility = new AdventureQuestEligibility();
            Path world = adventure.resolve(variant).resolve("world");
            String rules = Files.readString(world.resolve("quest_rules.json"));
            eligibility.readRules(rules);
            String requiredPOI = new JsonReader().parse(rules).child.getString("requiredPOI");
            Array<AdventureQuestData> quests = readQuests(Files.readString(world.resolve("quests.json")));
            AdventureQuestData bossQuest = null;
            for (AdventureQuestData quest : quests) {
                if (quest.getID() == 36) {
                    bossQuest = quest;
                    break;
                }
            }
            Assert.assertNotNull(bossQuest, variant);
            AdventureQuestHistory completed = new AdventureQuestHistory();
            completed.record(36);
            Assert.assertFalse(eligibility.isEligible(bossQuest, active, completed, (poi, enemy) -> false), variant);

            Path poiFile = world.resolve("points_of_interest.json");
            if (!Files.exists(poiFile)) {
                poiFile = common.resolve("world/points_of_interest.json");
            }
            Array<PointOfInterestData> points = new Json().fromJson(Array.class, PointOfInterestData.class,
                    Files.readString(poiFile));
            PointOfInterestData hideoutData = null;
            for (PointOfInterestData point : points) {
                if (point.name.equals(requiredPOI)) {
                    hideoutData = point;
                    break;
                }
            }
            // Innistrad currently has no Teferi hideout; it must not infer its state from global victories.
            if (hideoutData == null) {
                Assert.assertFalse(eligibility.isTargetEnemyDefeated(requiredPOI, "Teferi", new ArrayList<>(),
                        map -> new FileHandle(common.resolve(map).normalize().toFile()), id -> null), variant);
                continue;
            }
            int bossID = variant.equals("Realm of Legends") ? 142 : 67;
            FileHandle hideoutMap = new FileHandle(common.resolve(hideoutData.map).normalize().toFile());
            Assert.assertEquals(enemyAt(hideoutMap, bossID), "Teferi", variant);
            PointOfInterestData mirrorData = new PointOfInterestData();
            mirrorData.name = "Mirror Gallery";
            mirrorData.map = "../Realm of Legends/maps/map/magetower/Mirror_Gallery_U.tmx";
            List<PointOfInterest> locations = Arrays.asList(location(hideoutData, "hideout"),
                    location(mirrorData, "mirror"));
            PointOfInterestChanges.Map originalChanges = new PointOfInterestChanges.Map();
            PointOfInterestChanges hideout = new PointOfInterestChanges();
            hideout.visit();
            hideout.deleteObject(999999); // Visiting and deleting unrelated objects do not defeat the boss.
            originalChanges.put("hideout", hideout);
            PointOfInterestChanges mirror = new PointOfInterestChanges();
            mirror.deleteObject(103);
            mirror.deleteObject(bossID); // Even equal object IDs in another POI must not affect the target.
            originalChanges.put("mirror", mirror);
            PointOfInterestChanges.Map restored = new PointOfInterestChanges.Map();
            restored.load(originalChanges.save());
            BiPredicate<String, String> targetDefeated = (poi, enemy) -> eligibility.isTargetEnemyDefeated(
                    poi, enemy, locations, map -> new FileHandle(common.resolve(map).normalize().toFile()), restored::get);
            String origin = bossQuest.questSourceTags[0];
            List<AdventureQuestData> candidates = Arrays.asList(bossQuest);
            Assert.assertTrue(eligibility.isEligible(bossQuest, active, history, targetDefeated), variant);
            Assert.assertEquals(eligibility.offers(candidates, origin, active, history, targetDefeated).size, 1, variant);

            // An older save with the actual hideout boss removed must still suppress the quest.
            restored.get("hideout").deleteObject(bossID);
            PointOfInterestChanges.Map reloaded = new PointOfInterestChanges.Map();
            reloaded.load(restored.save());
            BiPredicate<String, String> defeatedAfterLoad = (poi, enemy) -> eligibility.isTargetEnemyDefeated(
                    poi, enemy, locations, map -> new FileHandle(common.resolve(map).normalize().toFile()), reloaded::get);
            Assert.assertFalse(eligibility.isEligible(bossQuest, active, history, defeatedAfterLoad), variant);
            Assert.assertTrue(eligibility.offers(candidates, origin, active, history, defeatedAfterLoad).isEmpty(), variant);
            Assert.assertFalse(eligibility.accept(new AdventureQuestData(bossQuest), active, history, defeatedAfterLoad), variant);
            Assert.assertTrue(active.isEmpty());
        }
    }
}
