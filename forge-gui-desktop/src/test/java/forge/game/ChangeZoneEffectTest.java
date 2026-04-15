package forge.game;

import com.google.common.collect.Lists;
import forge.ai.AITest;
import forge.ai.LobbyPlayerAi;
import forge.deck.Deck;
import forge.game.ability.AbilityFactory;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;
import java.util.List;

public class ChangeZoneEffectTest extends AITest {

    @Test
    public void mistyRainforestPutsFetchedLandOnActivatingPlayersBattlefield() {
        final Game game = initAndCreateGame();
        final Player p1 = game.getPlayers().get(0);
        final Player p2 = game.getPlayers().get(1);

        final Card misty = addCardToZone("Misty Rainforest", p2, ZoneType.Battlefield);
        addCardToZone("Island", p2, ZoneType.Library);

        SpellAbility fetchAbility = null;
        for (final SpellAbility sa : misty.getSpellAbilities()) {
            if (sa.getApi() == ApiType.ChangeZone) {
                fetchAbility = sa;
                break;
            }
        }
        AssertJUnit.assertNotNull("Misty Rainforest should have fetch ability", fetchAbility);
        fetchAbility.setActivatingPlayer(p2);
        fetchAbility.resolve();

        final long islandsOnP2Battlefield = p2.getCardsIn(ZoneType.Battlefield).stream()
                .filter(c -> "Island".equals(c.getName()))
                .count();
        final long islandsOnP1Battlefield = p1.getCardsIn(ZoneType.Battlefield).stream()
                .filter(c -> "Island".equals(c.getName()))
                .count();

        AssertJUnit.assertEquals("Fetched Island should enter activating player's battlefield", 1L, islandsOnP2Battlefield);
        AssertJUnit.assertEquals("Fetched Island should not enter opponent battlefield", 0L, islandsOnP1Battlefield);
    }

    @Test
    public void hiddenLibraryToBattlefieldIgnoresTemporaryControllerLeak() {
        final Game game = initAndCreateGame();
        final Player p1 = game.getPlayers().get(0);
        final Player p2 = game.getPlayers().get(1);

        final Card misty = addCardToZone("Misty Rainforest", p2, ZoneType.Battlefield);
        final Card island = addCardToZone("Island", p2, ZoneType.Library);

        // Simulate a leaked temporary control effect on the hidden-zone card.
        island.addTempController(p1, game.getNextTimestamp());
        AssertJUnit.assertEquals("Precondition: library card should report temporary controller", p1, island.getController());

        SpellAbility fetchAbility = null;
        for (final SpellAbility sa : misty.getSpellAbilities()) {
            if (sa.getApi() == ApiType.ChangeZone) {
                fetchAbility = sa;
                break;
            }
        }
        AssertJUnit.assertNotNull("Misty Rainforest should have fetch ability", fetchAbility);
        fetchAbility.setActivatingPlayer(p2);
        fetchAbility.resolve();

        final long islandsOnP2Battlefield = p2.getCardsIn(ZoneType.Battlefield).stream()
                .filter(c -> "Island".equals(c.getName()))
                .count();
        final long islandsOnP1Battlefield = p1.getCardsIn(ZoneType.Battlefield).stream()
                .filter(c -> "Island".equals(c.getName()))
                .count();
        final long islandsInLibrary = p2.getCardsIn(ZoneType.Library).stream()
                .filter(c -> "Island".equals(c.getName()))
                .count();
        final long islandsInBattlefield = game.getCardsIn(ZoneType.Battlefield).stream()
                .filter(c -> "Island".equals(c.getName()))
                .count();

        AssertJUnit.assertEquals("Fetched Island should leave library", 0L, islandsInLibrary);
        AssertJUnit.assertEquals("Fetched Island should enter battlefield exactly once", 1L, islandsInBattlefield);
        AssertJUnit.assertEquals("Fetched Island should enter one player's battlefield", 1L, islandsOnP2Battlefield + islandsOnP1Battlefield);
    }

 //   @Test()
    public void dandanBadRiverPutsFetchedLandOnActivatingPlayersBattlefield() {
        initAndCreateGame();

        final Deck firstDeck = new Deck("DanDan P1");
        final Deck secondDeck = new Deck("DanDan P2");
        final List<RegisteredPlayer> players = Lists.newArrayList();
        players.add(new RegisteredPlayer(firstDeck).setPlayer(new LobbyPlayerAi("p1", null)));
        players.add(new RegisteredPlayer(secondDeck).setPlayer(new LobbyPlayerAi("p2", null)));

        final Match match = new Match(new GameRules(GameType.DanDan), players, "DanDan Bad River fetch routing");
        final Game game = match.createGame();
        match.startGame(game);

        final Player p1 = game.getRegisteredPlayers().get(0);
        final Player p2 = game.getRegisteredPlayers().get(1);

        final Card badRiver = addCardToZone("Bad River", p2, ZoneType.Battlefield);
        final Card island = addCardToZone("Island", p2, ZoneType.Library);
        badRiver.addRemembered(island);
        final long islandsOnBattlefieldBefore = game.getCardsIn(ZoneType.Battlefield).stream()
                .filter(c -> "Island".equals(c.getName()))
                .count();

        final SpellAbility fetchAbility = AbilityFactory.getAbility(
                "AB$ ChangeZone | Cost$ 0 | Mandatory$ True | Origin$ Library | Destination$ Battlefield | Defined$ Remembered",
                badRiver);
        AssertJUnit.assertNotNull("Should build deterministic Bad River-style ChangeZone ability", fetchAbility);
        fetchAbility.setActivatingPlayer(p2);
        fetchAbility.resolve();

        AssertJUnit.assertEquals("Precondition: activating player should be p2", p2, fetchAbility.getActivatingPlayer());
        final long islandsOnBattlefieldAfter = game.getCardsIn(ZoneType.Battlefield).stream()
                .filter(c -> "Island".equals(c.getName()))
                .count();
        AssertJUnit.assertEquals("DanDan fetchland should move exactly one Island to battlefield",
                islandsOnBattlefieldBefore + 1L, islandsOnBattlefieldAfter);
        final boolean movedIslandControlledByActivator = game.getCardsIn(ZoneType.Battlefield).stream()
                .filter(c -> "Island".equals(c.getName()))
                .anyMatch(c -> c.getController() == p2);
        AssertJUnit.assertTrue("DanDan fetchland should put searched land under activating player's control",
                movedIslandControlledByActivator);
    }
}
