package forge.game.ability;

import java.util.List;
import java.util.Map;

import org.testng.annotations.Test;

import com.google.common.collect.Lists;

import forge.ai.AITest;
import forge.ai.LobbyPlayerAi;
import forge.ai.PlayerControllerAi;
import forge.deck.Deck;
import forge.game.Game;
import forge.game.GameRules;
import forge.game.GameStage;
import forge.game.GameType;
import forge.game.Match;
import forge.game.GameEntity;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.player.DelayedReveal;
import forge.game.player.Player;
import forge.game.phase.PhaseType;
import forge.game.player.PlayerController;
import forge.game.player.RegisteredPlayer;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.collect.FCollectionView;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertSame;

/** QuasiLibrarySearch$ is a ChooseCard that stands in for "search your library for a card". */
public class QuasiLibrarySearchTest extends AITest {

    private static final String SEARCH = "DB$ ChooseCard | Defined$ You | ChoiceZone$ Library"
            + " | QuasiLibrarySearch$ You | Choices$ Card.YouOwn | ChoiceTitle$ Search";

    /** Declines every choice and remembers how many cards it was offered. */
    private static class DecliningController extends PlayerControllerAi {
        int offered = -1;

        DecliningController(Game game, Player p) {
            super(game, p, p.getLobbyPlayer());
        }

        @Override
        public <T extends GameEntity> T chooseSingleEntityForEffect(FCollectionView<T> optionList,
                DelayedReveal delayedReveal, SpellAbility sa, String title, boolean isOptional,
                Player targetedPlayer, Map<String, Object> params) {
            offered = optionList.size();
            return null;
        }
    }

    /** Declines every zone-change choice and remembers how many cards it was offered. */
    private static class DecliningFetcher extends PlayerControllerAi {
        int offered = -1;

        DecliningFetcher(Game game, Player p) {
            super(game, p, p.getLobbyPlayer());
        }

        @Override
        public Card chooseSingleCardForZoneChange(ZoneType destination, List<ZoneType> origin, SpellAbility sa,
                CardCollection fetchList, DelayedReveal delayedReveal, String selectPrompt, boolean isOptional,
                Player decider) {
            offered = fetchList.size();
            return null;
        }
    }

    private static SpellAbility search(Card host, Player p) {
        SpellAbility sa = AbilityFactory.getAbility(SEARCH, host);
        sa.setActivatingPlayer(p);
        return sa;
    }

    /** Hands every player it is asked to control a controller that declines every choice. */
    private static class MindSlaveMaker extends LobbyPlayerAi {
        DecliningController slave;

        MindSlaveMaker(String name) {
            super(name, null);
        }

        @Override
        public PlayerController createMindSlaveController(Player master, Player slave) {
            this.slave = new DecliningController(slave.getGame(), slave);
            return this.slave;
        }
    }

    /** Opposition Agent's control of the searcher ends with the search, also when nothing is chosen. */
    @Test
    public void aDeclinedSearchEndsTheControlOfTheSearcher() {
        MindSlaveMaker lobby = new MindSlaveMaker("owner");
        List<RegisteredPlayer> players = Lists.newArrayList();
        Deck deck = new Deck();
        players.add(new RegisteredPlayer(deck).setPlayer(lobby));
        players.add(new RegisteredPlayer(deck).setPlayer(new LobbyPlayerAi("searcher", null)));
        GameRules rules = new GameRules(GameType.Constructed);
        Game game = new Game(players, rules, new Match(rules, players, "Test"));
        Player searcher = game.getPlayers().get(1);
        game.setAge(GameStage.Play);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, searcher);
        game.getPhaseHandler().onStackResolved();

        addCardToZone("Grizzly Bears", searcher, ZoneType.Library);
        Card host = addCard("Grizzly Bears", searcher);
        addCard("Opposition Agent", game.getPlayers().get(0));
        game.getAction().checkStateEffects(true);
        PlayerController own = searcher.getController();

        AbilityUtils.resolve(search(host, searcher));

        assertSame("the searcher is back under their own control", own, searcher.getController());
    }

    /** Aven Mindcensor: the searcher searches the top four cards, so only those can be chosen. */
    @Test
    public void onlyTheTopFourCardsCanBeChosenUnderAvenMindcensor() {
        Game game = initAndCreateGame();
        Player searcher = game.getPlayers().get(1);
        for (int i = 0; i < 6; i++) {
            addCardToZone("Grizzly Bears", searcher, ZoneType.Library);
        }
        Card host = addCard("Grizzly Bears", searcher);
        searcher.addChangedKeywords(Lists.newArrayList("LimitSearchLibrary"), Lists.newArrayList(),
                game.getNextTimestamp(), 0);
        DecliningController controller = new DecliningController(game, searcher);
        searcher.dangerouslySetController(controller);

        AbilityUtils.resolve(search(host, searcher));

        assertEquals("four cards of the six are searched", 4, controller.offered);
    }

    /** Leonin Arbiter: players can't search libraries, so there is nothing to choose from. */
    @Test
    public void nothingCanBeChosenWhenLibrariesCannotBeSearched() {
        Game game = initAndCreateGame();
        Player searcher = game.getPlayers().get(1);
        for (int i = 0; i < 3; i++) {
            addCardToZone("Grizzly Bears", searcher, ZoneType.Library);
        }
        Card host = addCard("Grizzly Bears", searcher);
        searcher.addChangedKeywords(Lists.newArrayList("CantSearchLibrary"), Lists.newArrayList(),
                game.getNextTimestamp(), 0);
        DecliningController controller = new DecliningController(game, searcher);
        searcher.dangerouslySetController(controller);

        AbilityUtils.resolve(search(host, searcher));

        assertEquals("no card may be offered", 0, Math.max(controller.offered, 0));
    }

    private static final String FETCH = "DB$ ChangeZone | Origin$ Library | Destination$ Hand | ChangeType$ Card"
            + " | ChangeNum$ 1";

    /** The ordinary search shares the restriction rules: Aven Mindcensor offers the top four of six. */
    @Test
    public void anOrdinarySearchOffersOnlyTheTopFourCardsUnderAvenMindcensor() {
        Game game = initAndCreateGame();
        Player searcher = game.getPlayers().get(1);
        for (int i = 0; i < 6; i++) {
            addCardToZone("Grizzly Bears", searcher, ZoneType.Library);
        }
        Card host = addCard("Grizzly Bears", searcher);
        searcher.addChangedKeywords(Lists.newArrayList("LimitSearchLibrary"), Lists.newArrayList(),
                game.getNextTimestamp(), 0);
        DecliningFetcher controller = new DecliningFetcher(game, searcher);
        searcher.dangerouslySetController(controller);
        SpellAbility sa = AbilityFactory.getAbility(FETCH, host);
        sa.setActivatingPlayer(searcher);

        AbilityUtils.resolve(sa);

        assertEquals("four cards of the six are searched", 4, controller.offered);
    }

    /** The ordinary search shares the restriction rules: Leonin Arbiter offers nothing. */
    @Test
    public void anOrdinarySearchOffersNothingWhenLibrariesCannotBeSearched() {
        Game game = initAndCreateGame();
        Player searcher = game.getPlayers().get(1);
        for (int i = 0; i < 3; i++) {
            addCardToZone("Grizzly Bears", searcher, ZoneType.Library);
        }
        Card host = addCard("Grizzly Bears", searcher);
        searcher.addChangedKeywords(Lists.newArrayList("CantSearchLibrary"), Lists.newArrayList(),
                game.getNextTimestamp(), 0);
        DecliningFetcher controller = new DecliningFetcher(game, searcher);
        searcher.dangerouslySetController(controller);
        SpellAbility sa = AbilityFactory.getAbility(FETCH, host);
        sa.setActivatingPlayer(searcher);

        AbilityUtils.resolve(sa);

        assertEquals("no card may be offered", 0, Math.max(controller.offered, 0));
    }
}
