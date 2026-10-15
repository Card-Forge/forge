package forge.game;

import com.google.common.eventbus.Subscribe;
import forge.ai.AITest;
import forge.ai.LobbyPlayerAi;
import forge.ai.PlayerControllerAi;
import forge.deck.Deck;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.ability.effects.SubgameEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.event.EventValueChangeType;
import forge.game.event.GameEventSubgameStart;
import forge.game.event.GameEventZone;
import forge.game.phase.PhaseType;
import forge.game.player.DelayedReveal;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.testng.Assert.*;

public class DanDanSubgameTest extends AITest {
    @Test
    public void eitherJointOwnerCanWishForTheSingleSubgameCounterpart() {
        for (int wishingPlayer = 0; wishingPlayer < 2; wishingPlayer++) {
            Game main = newGame(true);
            Player first = main.getPlayers().get(0);
            Card library = addCardToZone("Island", first, ZoneType.Library);
            library.setOwners(main.getPlayers());
            Card parent = addCardToZone("Predict", first, ZoneType.Hand);
            parent = main.getAction().moveToGraveyard(parent, null);
            parent.setOwners(main.getPlayers());
            final Card outsideParent = parent;
            int recipientIndex = wishingPlayer;
            SubgameObserver observer = new SubgameObserver(subgame -> {
                Player recipient = subgame.getPlayers().get(recipientIndex);
                Player other = subgame.getPlayers().get(1 - recipientIndex);
                assertEquals(subgame.getCardsIn(ZoneType.Library).size(), 1,
                        "the shared parent library produces one physical subgame deck");
                assertEquals(subgame.getCardsIn(ZoneType.Sideboard).stream()
                        .filter(c -> c.getName().equals("Predict")).count(), 1L,
                        "one parent card must have only one Wish counterpart");
                Card counterpart = subgame.getCardsIn(ZoneType.Sideboard).stream()
                        .filter(c -> c.getName().equals("Predict")).findFirst().orElseThrow();
                resolveWish(subgame, recipient);
                assertTrue(recipient.getCardsIn(ZoneType.Hand).contains(counterpart),
                        "either joint owner can use Cunning Wish to acquire the counterpart");
                assertSame(subgame.getCardState(counterpart).getOwner(), recipient);
                assertTrue(main.getCardsIn(ZoneType.Subgame).contains(outsideParent),
                        "Wish moves the mapped parent physical card into the main game's Subgame zone");
                assertFalse(main.getCardsIn(ZoneType.Graveyard).contains(outsideParent));
                resolveWish(subgame, other);
                assertFalse(other.getCardsIn(ZoneType.Hand).contains(counterpart),
                        "the other owner cannot fetch a stale duplicate after Wish");
                assertEquals(main.getCardsIn(ZoneType.Subgame).size(), 1);
                subgame.setGameOver(GameEndReason.Draw);
            });
            runSubgame(main, observer);
            assertTrue(observer.reached);
            assertEquals(main.getCardsIn(ZoneType.Library).size(), 2);
            assertTrue(main.getCardsIn(ZoneType.Library).contains(library));
            assertEquals(main.getCardsIn(ZoneType.Library).stream()
                    .filter(c -> c.getId() == outsideParent.getId()).count(), 1L,
                    "the single wished parent returns to the shared library once");
            assertTrue(main.getCardsIn(ZoneType.Subgame).isEmpty());
        }
    }

    @Test
    public void ordinarySubgameWishKeepsTheOutsideCardsSoleOwner() {
        Game main = newGame(false);
        Player owner = main.getPlayers().get(0);
        Card parent = addCardToZone("Predict", owner, ZoneType.Graveyard);
        SubgameObserver observer = new SubgameObserver(subgame -> {
            Player subOwner = subgame.getPlayers().get(0);
            Player opponent = subgame.getPlayers().get(1);
            resolveWish(subgame, opponent);
            assertFalse(opponent.getCardsIn(ZoneType.Hand).stream().anyMatch(c -> c.getName().equals("Predict")));
            assertTrue(main.getCardsIn(ZoneType.Graveyard).contains(parent));
            resolveWish(subgame, subOwner);
            assertTrue(subOwner.getCardsIn(ZoneType.Hand).stream().anyMatch(c -> c.getName().equals("Predict")));
            assertTrue(owner.getCardsIn(ZoneType.Subgame).contains(parent));
            subgame.setGameOver(GameEndReason.Draw);
        });
        runSubgame(main, observer);
        assertTrue(observer.reached);
        assertTrue(owner.getCardsIn(ZoneType.Library).contains(parent));
        assertTrue(main.getPlayers().get(1).getCardsIn(ZoneType.Library).isEmpty());
        assertSame(parent.getOwner(), owner);
    }

    @Test
    public void nestedWishMovesEachMappedParentOnce() {
        Game main = newGame(true);
        Card parent = addCardToZone("Predict", main.getPlayers().get(0), ZoneType.Exile);
        parent.setOwners(main.getPlayers());
        SubgameObserver outer = new SubgameObserver(subgame -> {
            Card counterpart = subgame.getCardsIn(ZoneType.Sideboard).stream()
                    .filter(c -> c.getName().equals("Predict")).findFirst().orElseThrow();
            SubgameObserver inner = new SubgameObserver(nested -> {
                assertEquals(nested.getCardsIn(ZoneType.Sideboard).stream()
                        .filter(c -> c.getName().equals("Predict")).count(), 1L);
                resolveWish(nested, nested.getPlayers().get(1));
                assertTrue(subgame.getCardsIn(ZoneType.Subgame).contains(counterpart),
                        "the nested Wish moves its immediate parent counterpart");
                assertTrue(main.getCardsIn(ZoneType.Subgame).contains(parent),
                        "the same Wish follows the mapping through to the original parent");
                nested.setGameOver(GameEndReason.Draw);
            });
            runSubgame(subgame, inner);
            assertTrue(inner.reached);
            assertEquals(subgame.getCardsIn(ZoneType.Library).stream()
                    .filter(c -> c.getId() == counterpart.getId()).count(), 1L);
            subgame.setGameOver(GameEndReason.Draw);
        });
        runSubgame(main, outer);
        assertTrue(outer.reached);
        assertEquals(main.getCardsIn(ZoneType.Library).stream()
                .filter(c -> c.getId() == parent.getId()).count(), 1L);
        assertTrue(main.getCardsIn(ZoneType.Subgame).isEmpty());
    }

    @Test
    public void wishedPrivateCardRejoinsTheParentSharedPool() {
        Game main = newGame(true);
        Player owner = main.getPlayers().get(0);
        Card parent = addCardToZone("Predict", owner, ZoneType.Exile);
        SubgameObserver observer = new SubgameObserver(subgame -> {
            resolveWish(subgame, subgame.getPlayers().get(0));
            assertTrue(main.getCardsIn(ZoneType.Subgame).contains(parent));
            subgame.setGameOver(GameEndReason.Draw);
        });
        runSubgame(main, observer);
        assertTrue(observer.reached);
        Card returned = main.getCardsIn(ZoneType.Library).stream()
                .filter(c -> c.getId() == parent.getId()).findFirst().orElseThrow();
        assertTrue(returned.isOwnedBy(main.getPlayers().get(1)),
                "a private parent card enters the joint pool when the subgame returns it to the shared library");
    }

    @Test
    public void restartMovesEverySharedPhysicalCardToLibraryOnce() {
        Game game = newGame(true);
        Player first = game.getPlayers().get(0);
        Player second = game.getPlayers().get(1);
        Card library = addCardToZone("Island", first, ZoneType.Library);
        Card graveyard = addCardToZone("Predict", first, ZoneType.Graveyard);
        Card hand = addCardToZone("Memory Lapse", second, ZoneType.Hand);
        Card karn = addCard("Karn Liberated", first);
        LibraryEntries entries = new LibraryEntries();
        game.subscribeToEvents(entries);
        SpellAbility restart = karn.getSpellAbilities().stream()
                .filter(sa -> sa.getApi() == ApiType.RestartGame).findFirst().orElseThrow();
        restart.setActivatingPlayer(first);
        AbilityUtils.resolve(restart);
        assertEquals(entries.counts, Map.of(library.getId(), 1, graveyard.getId(), 1, hand.getId(), 1, karn.getId(), 1),
                "restart must process each physical card once even though both players share the library");
        assertEquals(game.getCardsIn(ZoneType.Library).size(), 4);
        assertTrue(game.getCardsIn(ZoneType.Graveyard).isEmpty());
        assertTrue(second.getCardsIn(ZoneType.Hand).isEmpty());
        assertSame(first.getZone(ZoneType.Library), second.getZone(ZoneType.Library));
    }

    private Game newGame(boolean shared) {
        List<RegisteredPlayer> players = new ArrayList<>();
        players.add(new RegisteredPlayer(new Deck()).setPlayer(new LobbyPlayerAi("subgame first", null)));
        players.add(new RegisteredPlayer(new Deck()).setPlayer(new LobbyPlayerAi("subgame second", null)));
        GameRules rules = new GameRules(GameType.Constructed);
        if (shared) {
            rules.setAppliedVariants(EnumSet.of(GameType.DanDan));
        }
        Game game = new Match(rules, players, "subgame ownership test").createGame();
        game.setAge(GameStage.Play);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, game.getPlayers().get(0));
        return game;
    }

    private void resolveWish(Game game, Player player) {
        Card wish = addCardToZone("Cunning Wish", player, ZoneType.Hand);
        SpellAbility ability = wish.getFirstSpellAbility();
        ability.setActivatingPlayer(player);
        AbilityUtils.resolve(ability);
    }

    private void runSubgame(Game main, SubgameObserver observer) {
        observer.parent = main;
        main.getMatch().subscribeToEvents(observer);
        Card shahrazad = addCardToZone("Shahrazad", main.getPlayers().get(0), ZoneType.Hand);
        SpellAbility ability = shahrazad.getFirstSpellAbility();
        ability.setActivatingPlayer(main.getPlayers().get(0));
        new SubgameEffect().resolve(ability);
    }

    public static class LibraryEntries {
        private final Map<Integer, Integer> counts = new HashMap<>();

        @Subscribe
        public void onZone(GameEventZone event) {
            if (event.zoneType() == ZoneType.Library && event.mode() == EventValueChangeType.Added && event.card() != null) {
                counts.merge(event.card().getId(), 1, Integer::sum);
            }
        }
    }

    public static class SubgameObserver {
        private final Consumer<Game> scenario;
        private Game parent;
        private boolean reached;

        SubgameObserver(Consumer<Game> scenario) {
            this.scenario = scenario;
        }

        @Subscribe
        public void onSubgame(GameEventSubgameStart event) {
            Game subgame = event.subgame();
            if (subgame.getMaingame() != parent) {
                return;
            }
            for (Player player : subgame.getPlayers()) {
                player.dangerouslySetController(new PlayerControllerAi(subgame, player,
                        new LobbyPlayerAi(player.getName(), null)) {
                    @Override
                    public Player chooseStartingPlayer(boolean isFirstGame) {
                        reached = true;
                        scenario.accept(subgame);
                        return player;
                    }

                    @Override
                    public Card chooseSingleCardForZoneChange(ZoneType destination, List<ZoneType> origin,
                            SpellAbility sa, CardCollection fetchList, DelayedReveal reveal,
                            String prompt, boolean optional, Player decider) {
                        return fetchList.stream().filter(c -> c.getName().equals("Predict")).findFirst().orElse(null);
                    }
                });
            }
        }
    }
}
