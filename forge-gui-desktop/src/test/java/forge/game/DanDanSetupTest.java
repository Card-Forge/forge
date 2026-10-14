package forge.game;

import forge.ai.AITest;
import forge.ai.LobbyPlayerAi;
import forge.deck.CardPool;
import forge.deck.Deck;
import forge.deck.DeckProxy;
import forge.deck.DeckSection;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.util.MyRandom;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Random;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

public class DanDanSetupTest extends AITest {
    private static final Map<String, Integer> OFFICIAL_MAIN = Map.ofEntries(
            Map.entry("accumulated knowledge", 4),
            Map.entry("brainstorm", 2),
            Map.entry("capture of jingzhou", 2),
            Map.entry("chart a course", 2),
            Map.entry("control magic", 2),
            Map.entry("crystal spray", 2),
            Map.entry("dandân", 10),
            Map.entry("day's undoing", 2),
            Map.entry("halimar depths", 2),
            Map.entry("haunted fengraf", 2),
            Map.entry("island", 20),
            Map.entry("lonely sandbar", 2),
            Map.entry("magical hack", 2),
            Map.entry("memory lapse", 8),
            Map.entry("mental note", 2),
            Map.entry("metamorphose", 2),
            Map.entry("mystic sanctuary", 2),
            Map.entry("predict", 2),
            Map.entry("remote isle", 2),
            Map.entry("svyelunite temple", 2),
            Map.entry("telling time", 2),
            Map.entry("the surgical bay", 2),
            Map.entry("unsubstantiate", 2));

    private static final Set<String> LANDS = Set.of(
            "halimar depths", "haunted fengraf", "island", "lonely sandbar",
            "mystic sanctuary", "remote isle", "svyelunite temple", "the surgical bay");

    @Test
    public void officialPresetHasTheWizardsMainDeckAndNoVisionCharmSideboardInIt() {
        Deck deck = getOfficialDeckFromChooserSource();
        assertNotNull(deck, "the official DanDan preset must be shipped and parseable");
        CardPool main = deck.get(DeckSection.Main);
        Map<String, Integer> actual = new HashMap<>();
        int lands = 0;
        for (Map.Entry<PaperCard, Integer> entry : main) {
            String name = normalizedName(entry.getKey().getName());
            actual.merge(name, entry.getValue(), Integer::sum);
            if (LANDS.contains(name)) {
                lands += entry.getValue();
            }
        }

        assertEquals(actual, OFFICIAL_MAIN, "the shipped default must match the official 23-name multiset");
        assertEquals(main.countAll(), 80);
        assertEquals(lands, 34);
        assertEquals(main.countAll() - lands, 46);
        assertFalse(deck.has(DeckSection.Sideboard), "the official preset must not carry the historical sideboard");

        assertEquals(deck.getName(), "DanDan_SecretLair");

        RegisteredPlayer danDanPlayer = RegisteredPlayer.forVariants(2, EnumSet.of(GameType.DanDan),
                deck, null, false, null, null);
        assertEquals(danDanPlayer.getStartingLife(), 20);
        assertEquals(danDanPlayer.getStartingHand(), 7);
    }

    @Test
    public void matchSetupCreatesOneLibraryAndGraveyardForBothPlayers() {
        final Deck deck = getOfficialDeckFromChooserSource();
        final Set<GameType> variants = EnumSet.of(GameType.DanDan);
        final ArrayList<RegisteredPlayer> registeredPlayers = new ArrayList<>();
        registeredPlayers.add(RegisteredPlayer.forVariants(2, variants, deck, null, false, null, null)
                .setPlayer(new LobbyPlayerAi("Player one", null)));
        registeredPlayers.add(RegisteredPlayer.forVariants(2, variants, deck, null, false, null, null)
                .setPlayer(new LobbyPlayerAi("Player two", null)));

        final GameRules rules = new GameRules(GameType.Constructed);
        rules.setAppliedVariants(variants);
        final Match match = new Match(rules, registeredPlayers, "DanDan setup test");
        final Game game = match.createGame();
        final boolean[] setupHookReached = {false};
        final Random previousRandom = MyRandom.getRandom();
        MyRandom.setRandom(new Random(7));
        try {
            match.startGame(game, () -> {
                final Player first = game.getPlayers().get(0);
                final Player second = game.getPlayers().get(1);
                assertSame(first.getZone(ZoneType.Library), second.getZone(ZoneType.Library));
                assertSame(first.getZone(ZoneType.Graveyard), second.getZone(ZoneType.Graveyard));
                assertEquals(first.getStartingLife(), 20);
                assertEquals(second.getStartingLife(), 20);
                assertEquals(first.getStartingHandSize(), 7);
                assertEquals(second.getStartingHandSize(), 7);
                assertEquals(first.getMaxHandSize(), 7);
                assertEquals(second.getMaxHandSize(), 7);
                final Map<String, Integer> physicalCards = new HashMap<>();
                final Set<Integer> physicalIds = new HashSet<>();
                appendPhysicalCards(first.getZone(ZoneType.Library).getCards(false), physicalCards, physicalIds);
                appendPhysicalCards(first.getZone(ZoneType.Hand).getCards(false), physicalCards, physicalIds);
                appendPhysicalCards(second.getZone(ZoneType.Hand).getCards(false), physicalCards, physicalIds);
                assertTrue(first.getZone(ZoneType.Hand).size() <= first.getStartingHandSize());
                assertTrue(second.getZone(ZoneType.Hand).size() <= second.getStartingHandSize());
                assertEquals(first.getZone(ZoneType.Library).size()
                        + first.getZone(ZoneType.Hand).size() + second.getZone(ZoneType.Hand).size(), 80,
                        "mulligans may reduce either opening hand, but every card must remain accounted for");
                assertEquals(physicalIds.size(), 80, "all 80 physical cards must be unique game objects");
                assertEquals(physicalCards, OFFICIAL_MAIN,
                        "opening hands plus the remaining library must equal the official main pile");
                setupHookReached[0] = true;
                game.setGameOver(GameEndReason.Draw);
            });
            assertTrue(setupHookReached[0]);
        } finally {
            MyRandom.setRandom(previousRandom);
        }
    }

    private static Deck getOfficialDeckFromChooserSource() {
        for (DeckProxy proxy : DeckProxy.getAllDanDanDecks()) {
            if (proxy.getName().equals("DanDan_SecretLair")) return proxy.getDeck();
        }
        return null;
    }

    private static String normalizedName(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    private static void appendPhysicalCards(final Iterable<Card> cards, final Map<String, Integer> counts,
                                            final Set<Integer> ids) {
        for (final Card card : cards) {
            counts.merge(normalizedName(card.getName()), 1, Integer::sum);
            assertTrue(ids.add(card.getId()), "each physical card needs a unique game id");
        }
    }
}
