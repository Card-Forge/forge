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
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;
import java.lang.reflect.Method;
import java.util.List;

public class ChangeZoneEffectTest extends AITest {

    @Test
    public void faceDownInstantInExileIsNotPermanent() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(0);
        final Card bolt = addCardToZone("Lightning Bolt", p, ZoneType.Exile);
        bolt.turnFaceDown(true);
        AssertJUnit.assertTrue("Precondition: instant should be face down in exile", bolt.isFaceDown());
        AssertJUnit.assertFalse(
                "Face-down instant in exile must not count as permanent (ValidTgts$ Permanent uses isPermanent)",
                bolt.isPermanent());
    }

    @Test
    public void faceDownCreatureInExileIsPermanent() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(0);
        final Card bears = addCardToZone("Grizzly Bears", p, ZoneType.Exile);
        bears.turnFaceDown(true);
        AssertJUnit.assertTrue("Precondition: creature should be face down in exile", bears.isFaceDown());
        AssertJUnit.assertTrue(
                "Face-down creature card in exile should still count as permanent",
                bears.isPermanent());
    }

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

    @Test
    public void dandanLibraryToHandSearchPutsCardInActivatingPlayersHand() {
        initAndCreateGame();

        final Deck firstDeck = new Deck("DanDan P1");
        final Deck secondDeck = new Deck("DanDan P2");
        final List<RegisteredPlayer> players = Lists.newArrayList();
        players.add(new RegisteredPlayer(firstDeck).setPlayer(new LobbyPlayerAi("p1", null)));
        players.add(new RegisteredPlayer(secondDeck).setPlayer(new LobbyPlayerAi("p2", null)));

        final Match match = new Match(new GameRules(GameType.DanDan), players, "DanDan Demonic Tutor hand routing");
        final Game game = match.createGame();
        match.startGame(game);

        final Player p1 = game.getRegisteredPlayers().get(0);
        final Player p2 = game.getRegisteredPlayers().get(1);

        final Card island = addCardToZone("Island", p1, ZoneType.Library);
        final Card moved = game.getAction().moveToHand(island, p2, null, null);

        AssertJUnit.assertTrue("Moved card should be in activating player's hand", p2.getZone(ZoneType.Hand).contains(moved));
        AssertJUnit.assertFalse("Moved card should not be in non-activating player's hand", p1.getZone(ZoneType.Hand).contains(moved));
        AssertJUnit.assertEquals("DanDan hand routing should set owner to hand recipient", p2, moved.getOwner());
        AssertJUnit.assertEquals("DanDan hand routing should set controller to hand recipient", p2, moved.getController());
    }

    /**
     * When a non-null cause has a different activator than the hand recipient, owner must still
     * be the hand recipient (regression: old code used activator, breaking TargetedOwner for
     * cards P2 drew and cast, e.g. Lost in Space).
     */
    @Test
    public void dandanLibraryToHandWithMismatchedCauseActivatorSetsOwnerToHandRecipient() {
        initAndCreateGame();

        final Deck firstDeck = new Deck("DanDan P1");
        final Deck secondDeck = new Deck("DanDan P2");
        final List<RegisteredPlayer> players = Lists.newArrayList();
        players.add(new RegisteredPlayer(firstDeck).setPlayer(new LobbyPlayerAi("p1", null)));
        players.add(new RegisteredPlayer(secondDeck).setPlayer(new LobbyPlayerAi("p2", null)));

        final Match match = new Match(new GameRules(GameType.DanDan), players, "DanDan hand owner vs cause activator");
        final Game game = match.createGame();
        match.startGame(game);

        final Player p1 = game.getRegisteredPlayers().get(0);
        final Player p2 = game.getRegisteredPlayers().get(1);

        final Card island = addCardToZone("Island", p1, ZoneType.Library);
        final Card hostForCause = addCardToZone("Grizzly Bears", p1, ZoneType.Hand);
        final SpellAbility causeSa = hostForCause.getFirstSpellAbility();
        AssertJUnit.assertNotNull("Grizzly Bears should have a spell ability for cause", causeSa);
        causeSa.setActivatingPlayer(p1);

        final Card moved = game.getAction().moveToHand(island, p2, causeSa, null);

        AssertJUnit.assertTrue("Moved card should be in hand recipient's hand", p2.getZone(ZoneType.Hand).contains(moved));
        AssertJUnit.assertEquals("Hand recipient should own the card, not the cause's activator", p2, moved.getOwner());
        AssertJUnit.assertEquals("Hand recipient should control the card", p2, moved.getController());
    }

    @Test
    public void dandanGraveyardToBattlefieldReassignsOwnerToActivator() {
        initAndCreateGame();

        final Deck firstDeck = new Deck("DanDan P1");
        final Deck secondDeck = new Deck("DanDan P2");
        final List<RegisteredPlayer> players = Lists.newArrayList();
        players.add(new RegisteredPlayer(firstDeck).setPlayer(new LobbyPlayerAi("p1", null)));
        players.add(new RegisteredPlayer(secondDeck).setPlayer(new LobbyPlayerAi("p2", null)));

        final Match match = new Match(new GameRules(GameType.DanDan), players, "DanDan ETB owner to activator");
        final Game game = match.createGame();
        match.startGame(game);

        final Player p0 = game.getRegisteredPlayers().get(0);
        final Player p1 = game.getRegisteredPlayers().get(1);

        final Card bear = addCardToZone("Grizzly Bears", p1, ZoneType.Graveyard);
        AssertJUnit.assertEquals("Precondition: creature in graveyard should be owned by p1", p1, bear.getOwner());

        final Card causeHost = addCardToZone("Forest", p0, ZoneType.Hand);
        final SpellAbility reanimateCause = AbilityFactory.getAbility(
                "DB$ ChangeZone | ValidTgts$ Creature | Origin$ Graveyard | Destination$ Battlefield | Mandatory$ True",
                causeHost);
        AssertJUnit.assertNotNull(reanimateCause);
        reanimateCause.setActivatingPlayer(p0);

        final Zone from = game.getZoneOf(bear);
        final Card moved = game.getAction().changeZone(from, p0.getZone(ZoneType.Battlefield), bear, 0, reanimateCause);

        AssertJUnit.assertTrue("Grizzly Bears should be on the battlefield", moved.getZone() != null
                && moved.getZone().is(ZoneType.Battlefield));
        AssertJUnit.assertEquals("DanDan ETB should reassign owner to the ability's activator", p0, moved.getOwner());
        AssertJUnit.assertEquals("DanDan ETB should set controller to match", p0, moved.getController());
    }

    @Test
    public void dandanBoomerangStyleBattlefieldToHandUsesOwnerHand() throws Exception {
        initAndCreateGame();

        final Deck firstDeck = new Deck("DanDan P1");
        final Deck secondDeck = new Deck("DanDan P2");
        final List<RegisteredPlayer> players = Lists.newArrayList();
        players.add(new RegisteredPlayer(firstDeck).setPlayer(new LobbyPlayerAi("p1", null)));
        players.add(new RegisteredPlayer(secondDeck).setPlayer(new LobbyPlayerAi("p2", null)));

        final Match match = new Match(new GameRules(GameType.DanDan), players, "DanDan Boomerang owner hand routing");
        final Game game = match.createGame();
        match.startGame(game);

        final Player p1 = game.getRegisteredPlayers().get(0);
        final Player p2 = game.getRegisteredPlayers().get(1);

        final Card target = addCardToZone("Island", p1, ZoneType.Battlefield);
        target.setOwner(p1);
        target.setController(p1, 0);
        final Card boomerang = addCardToZone("Boomerang", p2, ZoneType.Hand);
        SpellAbility boomerangSa = null;
        for (final SpellAbility sa : boomerang.getSpellAbilities()) {
            if (sa.getApi() == ApiType.ChangeZone) {
                boomerangSa = sa;
                break;
            }
        }
        AssertJUnit.assertNotNull("Boomerang should provide ChangeZone spell ability", boomerangSa);
        boomerangSa.setActivatingPlayer(p2);

        final Method resolver = Class.forName("forge.game.ability.effects.ChangeZoneEffect")
                .getDeclaredMethod("resolveHandRecipientForDanDan", SpellAbility.class, Player.class, Card.class);
        resolver.setAccessible(true);
        final Player recipient = (Player) resolver.invoke(null, boomerangSa, p2, target);

        AssertJUnit.assertEquals("DanDan Boomerang should resolve owner-hand recipient, not activating player hand",
                p1, recipient);
    }
}
