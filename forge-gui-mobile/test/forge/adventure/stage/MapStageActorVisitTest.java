package forge.adventure.stage;

import static org.testng.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.Test;

import com.badlogic.gdx.utils.SnapshotArray;

/**
 * Issue #12008: the per-frame collision pass read past the end of the actor array when a
 * collision's map dialog deleted map objects mid-pass.
 */
public class MapStageActorVisitTest {

    private static SnapshotArray<String> actors(String... names) {
        SnapshotArray<String> items = new SnapshotArray<>(true, 16, String.class);
        items.addAll(names);
        return items;
    }

    @Test
    public void visitsNewestFirst() {
        List<String> visited = new ArrayList<>();
        MapStage.visitNewestFirst(actors("a", "b", "c"), s -> visited.add(s) && false);
        assertEquals(visited, List.of("c", "b", "a"));
    }

    /** The #12008 shape: a trigger zone deletes itself, with the player standing in it. */
    @Test
    public void anActorDeletingItselfDoesNotSkipOrOverrun() {
        SnapshotArray<String> items = actors("chest", "zone");
        List<String> visited = new ArrayList<>();
        MapStage.visitNewestFirst(items, s -> {
            visited.add(s);
            if (s.equals("zone")) items.removeValue("zone", true);
            return false;
        });
        assertEquals(visited, List.of("zone", "chest"));
        assertEquals(items.size, 1);
    }

    /** A dialog deleting several objects at once (itself plus a gate) used to still overrun. */
    @Test
    public void deletingSeveralActorsSkipsTheDeletedOnes() {
        SnapshotArray<String> items = actors("a", "gate", "b", "zone");
        List<String> visited = new ArrayList<>();
        MapStage.visitNewestFirst(items, s -> {
            visited.add(s);
            if (s.equals("zone")) {
                items.removeValue("zone", true);
                items.removeValue("gate", true);
                items.removeValue("b", true);
            }
            return false;
        });
        assertEquals(visited, List.of("zone", "a"));
        assertEquals(items.size, 1);
    }

    @Test
    public void actorsAddedMidPassWaitForTheNextFrame() {
        SnapshotArray<String> items = actors("a", "b");
        List<String> visited = new ArrayList<>();
        MapStage.visitNewestFirst(items, s -> {
            visited.add(s);
            if (s.equals("b")) items.add("spawned");
            return false;
        });
        assertEquals(visited, List.of("b", "a"));
        assertEquals(items.size, 3);
    }

    @Test
    public void stopsWhenAVisitEndsThePass() {
        List<String> visited = new ArrayList<>();
        MapStage.visitNewestFirst(actors("a", "enemy", "b"), s -> visited.add(s) && s.equals("enemy"));
        assertEquals(visited, List.of("b", "enemy"));
    }
}
