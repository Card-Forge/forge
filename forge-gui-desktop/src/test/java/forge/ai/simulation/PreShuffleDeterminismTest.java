package forge.ai.simulation;

import java.lang.reflect.Method;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import forge.deck.CardPool;
import forge.game.Game;
import forge.game.Match;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.model.FModel;
import forge.util.MyRandom;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

/**
 * Zone preparation must be a pure function of (deck, seed): CardPool's
 * backing map does not guarantee iteration order, so without sorted
 * iteration the pre-shuffle library order, random-foil roll sequence, and
 * card id assignment could vary across JVMs for the same seed.
 */
public class PreShuffleDeterminismTest extends SimulationTest {

    private static final String[][] DECK = {
            {"Plains", "12"}, {"Island", "12"}, {"Runeclaw Bear", "4"},
            {"Grizzly Bears", "4"}, {"Lightning Bolt", "4"}, {"Counterspell", "4"},
            {"Sol Ring", "1"}, {"Fireball", "3"}, {"Shock", "4"},
            {"Giant Growth", "4"}, {"Llanowar Elves", "4"}, {"Divination", "4"},
    };

    private CardPool pool(int[] order) {
        CardPool p = new CardPool();
        for (int i : order) {
            PaperCard pc = FModel.getMagicDb().getCommonCards().getCard(DECK[i][0]);
            AssertJUnit.assertNotNull(DECK[i][0], pc);
            p.add(pc, Integer.parseInt(DECK[i][1]));
        }
        return p;
    }

    private List<String> prepare(Player player, CardPool section, long seed) throws Exception {
        Method m = Match.class.getDeclaredMethod("preparePlayerZone",
                Player.class, ZoneType.class, CardPool.class, boolean.class);
        m.setAccessible(true);
        MyRandom.setRandom(new Random(seed));
        try {
            m.invoke(null, player, ZoneType.Library, section, true);
        } finally {
            MyRandom.setRandom(new SecureRandom());
        }
        List<String> names = new ArrayList<>();
        for (Card c : player.getZone(ZoneType.Library)) {
            names.add(c.getName());
        }
        return names;
    }

    @Test
    public void testLibraryPreparationIsInsertionOrderInvariant() throws Exception {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(0);

        int n = DECK.length;
        int[] fwd = new int[n];
        int[] rev = new int[n];
        for (int i = 0; i < n; i++) {
            fwd[i] = i;
            rev[i] = n - 1 - i;
        }

        List<String> a = prepare(p, pool(fwd), 7L);
        List<String> b = prepare(p, pool(rev), 7L);

        AssertJUnit.assertEquals(60, a.size());
        // same deck, same seed, different CardPool insertion order ->
        // byte-identical library
        AssertJUnit.assertEquals(a, b);
        // and the pre-shuffle order is the sorted one (name-major), so it is
        // stable across JVMs, not merely within this one
        List<String> sorted = new ArrayList<>(a);
        java.util.Collections.sort(sorted, String.CASE_INSENSITIVE_ORDER);
        AssertJUnit.assertEquals(sorted, a);
    }
}
