package forge.ai.ability;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.List;

import forge.game.*;
import forge.game.card.CounterEnumType;
import forge.game.spellability.AbilitySub;
import forge.game.zone.ZoneType;
import org.testng.annotations.Test;

import com.google.common.collect.Lists;

import forge.ai.AITest;
import forge.ai.LobbyPlayerAi;
import forge.deck.Deck;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.game.spellability.SpellAbility;

public class StaticAbilitySearchLibraryTest extends AITest {

    //Overrides 2 player game with 3 player game
    @Override
    public Game resetGame() {
        List<RegisteredPlayer> players = Lists.newArrayList();
        Deck d1 = new Deck();
        players.add(new RegisteredPlayer(d1).setPlayer(new LobbyPlayerAi("p1", null)));
        players.add(new RegisteredPlayer(d1).setPlayer(new LobbyPlayerAi("p2", null)));
        players.add(new RegisteredPlayer(d1).setPlayer(new LobbyPlayerAi("p3", null)));
        GameRules rules = new GameRules(GameType.Constructed);
        Match match = new Match(rules, players, "Test 3 player game");
        Game game = new Game(players, rules, match);
        game.setAge(GameStage.Play);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, game.getPlayers().get(0));
        game.getPhaseHandler().onStackResolved();
        return game;
    }

    @Test
    public void testMindlockOrb_canSearchWhenNotInPlay() {
        Game game = initAndCreateGame();
        Player p1 = game.getPlayers().get(1);

        Card aridMesa = addCard("Arid Mesa", p1);

        game.getAction().checkStateEffects(true);

        assertTrue(p1.canSearchLibraryWith(findSearchLibraryAbility(aridMesa), p1));
    }

    @Test
    public void testMindlockOrb_canNotSearchIfPlayerOwnsIt() {
        Game game = initAndCreateGame();
        Player p1 = game.getPlayers().get(1);

        addCard("Mindlock Orb", p1);
        Card aridMesa = addCard("Arid Mesa", p1);

        game.getAction().checkStateEffects(true);

        assertFalse(p1.canSearchLibraryWith(findSearchLibraryAbility(aridMesa), p1));
    }

    @Test
    public void testMindlockOrb_canNotSearchIfOpponentOwnsIt() {
        Game game = initAndCreateGame();

        Player p1 = game.getPlayers().get(1);
        Card aridMesa = addCard("Arid Mesa", p1);

        Player p2 = game.getPlayers().get(0);
        addCard("Mindlock Orb", p2);

        game.getAction().checkStateEffects(true);

        assertFalse(p1.canSearchLibraryWith(findSearchLibraryAbility(aridMesa), p1));
    }

    @Test
    public void testAshiok_ownerCanSearchOwnLibrary() {
        Game game = initAndCreateGame();
        Player p1 = game.getPlayers().get(0);

        addCard("Ashiok, Dream Render", p1);
        Card aridMesa = addCard("Arid Mesa", p1);

        game.getAction().checkStateEffects(true);

        assertTrue(p1.canSearchLibraryWith(findSearchLibraryAbility(aridMesa), p1));
    }

    @Test
    public void testAshiok_opponentCannotSearchOwnLibrary() {
        Game game = initAndCreateGame();
        Player p1 = game.getPlayers().get(0);
        Player p2 = game.getPlayers().get(1);

        Card ashiok = addCard("Ashiok, Dream Render", p2);
        ashiok.setCounters(CounterEnumType.LOYALTY, 5);
        Card aridMesa = addCard("Arid Mesa", p1);

        game.getAction().checkStateEffects(true);

        assertFalse(p1.canSearchLibraryWith(findSearchLibraryAbility(aridMesa), p1));
    }

    @Test
    public void testAshiok_opponentCanCauseOtherOpponentToSearchTheirLibrary() {
        Game game = initAndCreateGame();
        Player p1 = game.getPlayers().get(0);
        Player p2 = game.getPlayers().get(1);
        Player p3 = game.getPlayers().get(2);

        Card ashiok = addCard("Ashiok, Dream Render", p1);
        ashiok.setCounters(CounterEnumType.LOYALTY, 5);

        Card grizzlyBears = addCard("Grizzly Bears", p2);

        Card pathToExile = addCardToZone("Path to Exile", p3, ZoneType.Hand);

        SpellAbility pathToExileSA = pathToExile.getSpellAbilities().get(0);
        pathToExileSA.getTargets().add(grizzlyBears);
        pathToExileSA.setActivatingPlayer(p3);

        game.getAction().checkStateEffects(true);

        AbilitySub sub = pathToExileSA.getSubAbility();

        assertTrue(p2.canSearchLibraryWith(sub, p2));
    }

    @Test
    public void testAshiok_opponentCanNotCauseToSearchTheirOwnLibrary() {
        Game game = initAndCreateGame();
        Player p1 = game.getPlayers().get(0);
        Player p3 = game.getPlayers().get(2);

        Card ashiok = addCard("Ashiok, Dream Render", p1);
        ashiok.setCounters(CounterEnumType.LOYALTY, 5);

        Card grizzlyBears = addCard("Grizzly Bears", p3);

        Card pathToExile = addCardToZone("Path to Exile", p3, ZoneType.Hand);

        SpellAbility pathToExileSA = pathToExile.getSpellAbilities().get(0);
        pathToExileSA.getTargets().add(grizzlyBears);
        pathToExileSA.setActivatingPlayer(p3);

        game.getAction().checkStateEffects(true);

        AbilitySub sub = pathToExileSA.getSubAbility();
        assertFalse(p3.canSearchLibraryWith(sub, p3), "P3 can not search their own library by sending his own creature to exile");
    }

    private SpellAbility findSearchLibraryAbility(Card card){
        return card.getSpellAbilities()
                .stream()
                .filter( sa -> sa.getDescription().toLowerCase().contains("search your library"))
                .findFirst()
                .map( sa -> {
                    //TODO avoid NPE in "forge.game.player.Player.isOpponentOf(forge.game.player.Player)" because the return value of "forge.game.spellability.SpellAbility.getActivatingPlayer()" is null
                    sa.setActivatingPlayer(card.getOwner());
                    return sa;
                })
                .orElse(null);
    }
}
