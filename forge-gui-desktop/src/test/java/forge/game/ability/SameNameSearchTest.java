package forge.game.ability;

import java.util.List;

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
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.phase.PhaseType;
import forge.game.player.DelayedReveal;
import forge.game.player.Player;
import forge.game.player.PlayerController;
import forge.game.player.RegisteredPlayer;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertSame;

/** SameNameAsEach$: one library search that finds a card with the same name as each of several cards. */
public class SameNameSearchTest extends AITest {

    /** "For each creature you control, search your library for a card with the same name." */
    private static final String SEARCH = "DB$ ChangeZone | Origin$ Library | Destination$ Hand | ChangeType$ Card"
            + " | SameNameAsEach$ Valid Creature.YouCtrl";

    /** Declines every zone-change choice and remembers how many cards it was offered. */
    private static class DecliningController extends PlayerControllerAi {
        int offered = -1;

        DecliningController(Game game, Player p) {
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

    /** Takes the first card it is offered each time and counts the choices it was asked for. */
    private static class TakingController extends PlayerControllerAi {
        int choices;

        TakingController(Game game, Player p) {
            super(game, p, p.getLobbyPlayer());
        }

        @Override
        public Card chooseSingleCardForZoneChange(ZoneType destination, List<ZoneType> origin, SpellAbility sa,
                CardCollection fetchList, DelayedReveal delayedReveal, String selectPrompt, boolean isOptional,
                Player decider) {
            choices++;
            return fetchList.isEmpty() ? null : fetchList.get(0);
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

    private static int count(Player p, ZoneType zone, String name) {
        int n = 0;
        for (Card c : p.getCardsIn(zone)) {
            if (c.getName().equals(name)) {
                n++;
            }
        }
        return n;
    }

    /** Two Grizzly Bears and an Elvish Mystic: three choices in one search, one card each, no card found twice. */
    @Test
    public void oneSearchFindsACardForEachNamedCard() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        p.getZone(ZoneType.Library).removeAllCards(true);
        for (int i = 0; i < 3; i++) {
            addCardToZone("Grizzly Bears", p, ZoneType.Library);
        }
        addCardToZone("Elvish Mystic", p, ZoneType.Library);
        addCardToZone("Forest", p, ZoneType.Library);
        addCard("Grizzly Bears", p);
        addCard("Grizzly Bears", p);
        Card host = addCard("Elvish Mystic", p);
        TakingController controller = new TakingController(game, p);
        p.dangerouslySetController(controller);

        AbilityUtils.resolve(search(host, p));

        assertEquals("one choice per named card", 3, controller.choices);
        assertEquals("two Grizzly Bears found", 2, count(p, ZoneType.Hand, "Grizzly Bears"));
        assertEquals("one Elvish Mystic found", 1, count(p, ZoneType.Hand, "Elvish Mystic"));
        assertEquals("the third Bears stays", 1, count(p, ZoneType.Library, "Grizzly Bears"));
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

    /** Doubling Chant: a creature card with the same name as each creature, onto the battlefield. */
    @Test
    public void doublingChantPutsOneCopyOfEachCreatureOntoTheBattlefield() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        p.getZone(ZoneType.Library).removeAllCards(true);
        addCardToZone("Grizzly Bears", p, ZoneType.Library);
        addCardToZone("Elvish Mystic", p, ZoneType.Library);
        addCard("Grizzly Bears", p);
        addCard("Elvish Mystic", p);
        p.dangerouslySetController(new TakingController(game, p));
        Card chant = addCardToZone("Doubling Chant", p, ZoneType.Hand);
        SpellAbility sa = chant.getSpells().get(0);
        sa.setActivatingPlayer(p);

        AbilityUtils.resolve(sa);

        assertEquals("two Grizzly Bears", 2, count(p, ZoneType.Battlefield, "Grizzly Bears"));
        assertEquals("two Elvish Mystics", 2, count(p, ZoneType.Battlefield, "Elvish Mystic"));
        assertEquals("the library is empty", 0, p.getCardsIn(ZoneType.Library).size());
    }

    /** Dichotomancy: the opponent's library is searched, the cards come under the caster's control. */
    @Test
    public void dichotomancyTakesTheOpponentsCopiesOfItsTappedPermanents() {
        Game game = initAndCreateGame();
        Player me = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);
        opp.getZone(ZoneType.Library).removeAllCards(true);
        addCardToZone("Grizzly Bears", opp, ZoneType.Library);
        Card tapped = addCard("Grizzly Bears", opp);
        tapped.tap(true, null, null);
        me.dangerouslySetController(new TakingController(game, me));
        Card spell = addCardToZone("Dichotomancy", me, ZoneType.Hand);
        SpellAbility sa = spell.getSpells().get(0);
        sa.setActivatingPlayer(me);
        sa.getTargets().add(opp);

        AbilityUtils.resolve(sa);

        assertEquals("the copy is on the caster's side", 1, count(me, ZoneType.Battlefield, "Grizzly Bears"));
        assertEquals("the opponent's library lost it", 0, opp.getCardsIn(ZoneType.Library).size());
    }

    /** Clarion Ultimatum: the chosen permanents' names are searched, the cards enter tapped. */
    @Test
    public void clarionUltimatumPutsCopiesOfTheChosenPermanentsOntoTheBattlefieldTapped() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        p.getZone(ZoneType.Library).removeAllCards(true);
        addCardToZone("Grizzly Bears", p, ZoneType.Library);
        Card bears = addCard("Grizzly Bears", p);
        p.dangerouslySetController(new TakingController(game, p));
        Card spell = addCardToZone("Clarion Ultimatum", p, ZoneType.Hand);
        SpellAbility sa = spell.getSpells().get(0);
        sa.setActivatingPlayer(p);
        spell.setChosenCards(new CardCollection(bears)); // what the ChooseCard step leaves

        AbilityUtils.resolve(sa.getSubAbility());

        assertEquals("two Grizzly Bears", 2, count(p, ZoneType.Battlefield, "Grizzly Bears"));
        int tappedBears = 0;
        for (Card c : p.getCardsIn(ZoneType.Battlefield)) {
            if (c.getName().equals("Grizzly Bears") && c.isTapped()) {
                tappedBears++;
            }
        }
        assertEquals("the found copy is tapped", 1, tappedBears);
    }
}
