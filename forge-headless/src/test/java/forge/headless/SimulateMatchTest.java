package forge.headless;

import static org.junit.Assert.assertNotNull;

import java.nio.file.Path;

import org.junit.Test;

import forge.game.GameType;

public class SimulateMatchTest {
    private static final Path TEST_DECK = Path.of("test_decks", "monored.dck");

    @Test
    public void loadsDeckFromRelativeFilePath() {
        assertNotNull(SimulateMatch.deckFromCommandLineParameter(TEST_DECK.toString(), GameType.Constructed));
    }

    @Test
    public void loadsDeckFromAbsoluteFilePath() {
        assertNotNull(SimulateMatch.deckFromCommandLineParameter(
                TEST_DECK.toAbsolutePath().toString(), GameType.Constructed));
    }
}
