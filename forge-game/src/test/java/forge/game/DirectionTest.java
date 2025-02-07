package forge.game;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import forge.game.Direction;

import org.junit.jupiter.api.Test;

class DirectionTest {

	@Test
    void testDefaultDirection() {
        assertEquals(Direction.Left, Direction.getDefaultDirection(), "Default direction should be Left.");
    }

    @Test
    void testListOfDirections() {
        List<Direction> directions = Direction.getListOfDirections();
        assertEquals(2, directions.size(), "List should contain exactly two directions.");
        assertTrue(directions.contains(Direction.Left), "List should contain Left.");
        assertTrue(directions.contains(Direction.Right), "List should contain Right.");
    }

    @Test
    void testIsDefaultDirection() {
        assertTrue(Direction.Left.isDefaultDirection(), "Left should be the default direction.");
        assertFalse(Direction.Right.isDefaultDirection(), "Right should not be the default direction.");
    }

    @Test
    void testGetShift() {
        assertEquals(1, Direction.Left.getShift(), "Left should return a shift of 1.");
        assertEquals(-1, Direction.Right.getShift(), "Right should return a shift of -1.");
    }

    @Test
    void testGetOtherDirection() {
        assertEquals(Direction.Right, Direction.Left.getOtherDirection(), "Left should return Right as the other direction.");
        assertEquals(Direction.Left, Direction.Right.getOtherDirection(), "Right should return Left as the other direction.");
    }

    @Test
    void testToString() {
        assertEquals("Left", Direction.Left.toString(), "Left should be represented as 'Left'.");
        assertEquals("Right", Direction.Right.toString(), "Right should be represented as 'Right'.");
    }

}
