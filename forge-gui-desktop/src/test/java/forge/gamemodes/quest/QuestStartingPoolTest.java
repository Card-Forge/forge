package forge.gamemodes.quest;

import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

import forge.ai.AITest;
import forge.card.MagicColor;
import forge.gamemodes.quest.StartingPoolPreferences.PoolType;
import forge.item.PaperCard;

/**
 * Pins quest starting-pool generation against the all-colors hole: the
 * balanced filter builder used the NON-selected colors as its repetition
 * multiplier, so selecting every color built zero filters and a new quest
 * started with no cards at all - silently (field report 2026-07-30, a
 * "Random Commander"-world Commander quest whose whole pool was the
 * snow-basics grant). Rides AITest's one-time card DB load.
 */
public class QuestStartingPoolTest extends AITest {

    private static List<Byte> colors(Byte... colors) {
        return new ArrayList<>(List.of(colors));
    }

    @Test
    public void allColorsBalancedPoolIsNotEmpty() {
        final StartingPoolPreferences prefs = new StartingPoolPreferences(PoolType.BALANCED,
                colors(MagicColor.BLACK, MagicColor.BLUE, MagicColor.GREEN, MagicColor.RED,
                        MagicColor.WHITE, MagicColor.COLORLESS),
                true, false, false, 0);
        final List<PaperCard> cards = BoosterUtils.getQuestStarterDeck(null, 20, 10, 5, prefs);
        Assert.assertFalse(cards.isEmpty(), "selecting ALL colors must not generate an empty starting pool");
        Assert.assertTrue(cards.size() >= 20, "expected roughly the requested pool size, got " + cards.size());
    }

    @Test
    public void colorSubsetBalancedPoolStillWorks() {
        final StartingPoolPreferences prefs = new StartingPoolPreferences(PoolType.BALANCED,
                colors(MagicColor.RED, MagicColor.GREEN), false, false, false, 0);
        final List<PaperCard> cards = BoosterUtils.getQuestStarterDeck(null, 20, 10, 5, prefs);
        Assert.assertTrue(cards.size() >= 20, "two-color balanced pool regressed, got " + cards.size());
    }

    @Test
    public void noColorsBalancedPoolStillWorks() {
        final StartingPoolPreferences prefs = new StartingPoolPreferences(PoolType.BALANCED,
                colors(), false, false, false, 0);
        final List<PaperCard> cards = BoosterUtils.getQuestStarterDeck(null, 20, 10, 5, prefs);
        Assert.assertTrue(cards.size() >= 20, "no-preference balanced pool regressed, got " + cards.size());
    }
}
