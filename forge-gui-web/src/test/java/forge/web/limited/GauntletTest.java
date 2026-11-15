package forge.web.limited;

import forge.deck.Deck;
import forge.deck.DeckGroup;
import forge.game.GameRules;
import forge.game.GameType;
import forge.game.player.RegisteredPlayer;
import forge.gamemodes.limited.LimitedMatch;
import forge.gamemodes.limited.SealedCardPoolGenerator;
import forge.gui.GuiBase;
import forge.model.FModel;
import forge.util.storage.IStorage;
import forge.web.PlayerSettings;
import forge.web.WebGuiBase;
import forge.web.WebTestSupport;
import forge.web.match.WebGuiGame;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;

/** Tests that core's limited gauntlet plays each round at the browser's table when the web gives it one. */
public class GauntletTest {
    private record Started(GameRules rules, List<RegisteredPlayer> players, RegisteredPlayer human) { }

    private String pool;

    @BeforeClass
    public void setUp() {
        WebTestSupport.initModel();
    }

    @AfterMethod(alwaysRun = true)
    public void cleanUp() {
        ui().playSolo(null);
        final IStorage<DeckGroup> kept = FModel.getDecks().getSealed();
        if (pool != null && kept.contains(pool)) {
            kept.delete(pool);
        }
    }

    // Fails if a round is not started at the browser's table or meets the pool's opponents out of order
    @Test
    public void eachRoundIsPlayedAtTheBrowsersTable() {
        pool = "Gauntlet test " + UUID.randomUUID().toString().substring(0, 8);
        final SealedCardPoolGenerator gen = SealedCardPoolGenerator.createFull(6);
        final DeckGroup group = gen.buildDeckGroup(pool, gen.getCardPool(false), false);
        FModel.getDecks().getSealed().add(group);

        final List<Started> started = new ArrayList<>();
        ui().playSolo(new WebGuiBase.SoloTable(() -> new WebGuiGame(PlayerSettings.saved()),
                (match, rules, players, human, begin) -> started.add(new Started(rules, players, human))));
        final LimitedMatch gauntlet = FModel.getLimitedMatch();
        gauntlet.launchGauntlet(GameType.Sealed, group.getHumanDeck(), 2);
        gauntlet.nextRound();

        assertEquals(started.size(), 2);
        final List<Deck> ai = FModel.getDecks().getSealed().get(pool).getAiDecks();
        for (int round = 0; round < 2; round++) {
            final Started match = started.get(round);
            assertEquals(match.rules().getGameType(), GameType.Sealed);
            assertSame(match.players().get(0), match.human());
            assertEquals(cards(match.players().get(1).getDeck()), cards(ai.get(round)));
        }
        assertEquals(gauntlet.getCurrentRound(), 2);
    }

    private static WebGuiBase ui() {
        return (WebGuiBase) GuiBase.getInterface();
    }

    private static List<String> cards(final Deck deck) {
        return deck.getMain().toFlatList().stream().map(c -> c.getName()).sorted().toList();
    }
}
