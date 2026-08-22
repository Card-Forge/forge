package forge.gamemodes.match.input;

import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MassSelectModeShould {

    @Test
    public void cycleThroughAllModesInOrder() {
        assertThat(MassSelectMode.MINE.next()).isEqualTo(MassSelectMode.ALL);
        assertThat(MassSelectMode.ALL.next()).isEqualTo(MassSelectMode.NONE);
    }

    @Test
    public void wrapAroundToMineAfterPassingNone() {
        assertThat(MassSelectMode.NONE.next()).isEqualTo(MassSelectMode.MINE);
    }

    @Test
    public void returnToTheStartingModeAfterAFullCycle() {
        // Given any starting mode
        MassSelectMode start = MassSelectMode.MINE;
        MassSelectMode current = start;
        // When next() is called once per mode
        for (int i = 0; i < MassSelectMode.values().length; i++) {
            current = current.next();
        }
        // Then we are back where we started
        assertThat(current).isEqualTo(start);
    }
}
