package forge.adventure.pointofintrest;

import forge.adventure.util.SaveFileData;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;

public class PointOfInterestChangesClearedStateTest {

    @Test
    public void clearedStateRoundTripsThroughSaveData() {
        PointOfInterestChanges changes = new PointOfInterestChanges();
        changes.setCleared(true);
        changes.markEnemiesSeen();
        changes.setBossAlive(true);
        changes.addSubMap("maps/map/dungeon_floor2.tmx");
        SaveFileData data = changes.save();

        PointOfInterestChanges loaded = new PointOfInterestChanges();
        loaded.load(data);
        assertTrue(loaded.isCleared());
        assertTrue(loaded.hasSeenEnemies());
        assertTrue(loaded.isBossAlive());
        assertEquals(loaded.getSubMaps().size(), 1);
        assertTrue(loaded.getSubMaps().contains("maps/map/dungeon_floor2.tmx"));
    }

    @Test
    public void loadingOldSaveDataDefaultsToNotCleared() {
        SaveFileData data = new PointOfInterestChanges().save();
        data.remove("isCleared");
        data.remove("enemiesSeen");
        data.remove("bossAlive");
        data.remove("subMaps");

        PointOfInterestChanges loaded = new PointOfInterestChanges();
        loaded.setCleared(true);
        loaded.markEnemiesSeen();
        loaded.setBossAlive(true);
        loaded.addSubMap("stale");
        loaded.load(data);
        assertFalse(loaded.isCleared());
        assertFalse(loaded.hasSeenEnemies());
        assertFalse(loaded.isBossAlive());
        assertTrue(loaded.getSubMaps().isEmpty());
    }

    @Test
    public void clearingDeletedObjectsResetsClearedFlag() {
        PointOfInterestChanges changes = new PointOfInterestChanges();
        changes.deleteObject(7);
        changes.setCleared(true);
        changes.clearDeletedObjects();
        assertFalse(changes.isCleared());
        assertFalse(changes.hasDeletedObjects());
    }

    @Test
    public void clearedStateVersionChangesWhenFlagChanges() {
        PointOfInterestChanges changes = new PointOfInterestChanges();
        int before = PointOfInterestChanges.getClearedStateVersion();
        changes.setCleared(true);
        assertNotEquals(PointOfInterestChanges.getClearedStateVersion(), before);
        int after = PointOfInterestChanges.getClearedStateVersion();
        changes.setCleared(true);
        assertEquals(PointOfInterestChanges.getClearedStateVersion(), after);

        changes.markEnemiesSeen();
        assertNotEquals(PointOfInterestChanges.getClearedStateVersion(), after);
        after = PointOfInterestChanges.getClearedStateVersion();
        changes.markEnemiesSeen();
        assertEquals(PointOfInterestChanges.getClearedStateVersion(), after);

        changes.addSubMap("floor2");
        assertNotEquals(PointOfInterestChanges.getClearedStateVersion(), after);
        after = PointOfInterestChanges.getClearedStateVersion();
        changes.addSubMap("floor2");
        assertEquals(PointOfInterestChanges.getClearedStateVersion(), after);

        changes.setBossAlive(true);
        assertNotEquals(PointOfInterestChanges.getClearedStateVersion(), after);
        after = PointOfInterestChanges.getClearedStateVersion();
        changes.setBossAlive(true);
        assertEquals(PointOfInterestChanges.getClearedStateVersion(), after);
    }
}
