package forge.ai;

import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import forge.deck.Deck;
import forge.game.Game;
import forge.game.GameRules;
import forge.game.GameStage;
import forge.game.GameType;
import forge.game.Match;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class OreskosExplorerAiTest extends AITest {
    private Game game;
    private Player ai;
    private Card explorer;

    private void setup(final int[] opposingLands, final int plains, final String plainsName) {
        List<RegisteredPlayer> players = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            LobbyPlayerAi lobby = new LobbyPlayerAi("Explorer-" + i, null);
            lobby.setAiProfile("Default");
            players.add(new RegisteredPlayer(new Deck()).setPlayer(lobby));
        }
        GameRules rules = new GameRules(GameType.Constructed);
        game = new Game(players, rules, new Match(rules, players, "Oreskos Explorer"));
        game.setAge(GameStage.Play);
        ai = game.getPlayers().get(0);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        addCards("Plains", 2, ai);
        for (int i = 0; i < opposingLands.length; i++) {
            addCards("Forest", opposingLands[i], game.getPlayers().get(i + 1));
        }
        explorer = addCardToZone("Oreskos Explorer", ai, ZoneType.Hand);
        for (int i = 0; i < plains; i++) {
            addCardToZone(plainsName, ai, ZoneType.Library);
        }
        fillLibrary(ai, 5);
        game.getAction().checkStaticAbilities();
        game.getTriggerHandler().resetActiveTriggers();
        game.copyLastState();
    }

    private void castAndSearch(final int expected) {
        List<SpellAbility> choices = ((PlayerControllerAi) ai.getController()).getAi().chooseSpellAbilityToPlay();
        Assert.assertNotNull(choices, "The AI should cast the creature even if its search finds nothing");
        Assert.assertTrue(choices.stream().anyMatch(sa -> sa.getHostCard() == explorer));
        SpellAbility spell = explorer.getFirstSpellAbility();
        spell.setActivatingPlayer(ai);
        Assert.assertTrue(ComputerUtil.playStack(spell, ai, game));
        Assert.assertEquals(ai.getLandsInPlay().stream().filter(Card::isTapped).count(), 2L);
        game.getStack().resolveStack();
        game.getTriggerHandler().runWaitingTriggers();
        game.getStack().addAllTriggeredAbilitiesToStack();
        for (int i = 0; i < 8 && !game.getStack().isEmpty(); i++) {
            game.getStack().resolveStack();
        }
        Assert.assertTrue(game.getStack().isEmpty());
        Assert.assertEquals(countCardsWithName(game, "Oreskos Explorer"), 1);
        Assert.assertEquals(ai.getCardsIn(ZoneType.Hand).size(), expected);
        Assert.assertTrue(ai.getCardsIn(ZoneType.Hand).stream().allMatch(c -> c.getType().hasSubtype("Plains")));
        Assert.assertEquals(ai.getLandsInPlay().size(), 2, "Searched lands belong in hand");
    }

    @Test
    public void emptySearchDoesNotPreventCasting() {
        setup(new int[] {3, 3, 3}, 0, "Plains");
        SpellAbility search = explorer.getTriggers().get(0).ensureAbility().copy(ai);
        search.setTrigger(explorer.getTriggers().get(0));
        Assert.assertTrue(SpellApiToAi.Converter.get(search).doTrigger(ai, search, false),
                "An empty up-to search is not a bad ETB effect");
        castAndSearch(0);
    }

    @DataProvider(name = "searches")
    public Object[][] searches() {
        return new Object[][] {
                {new int[] {1, 2, 2}, 5, "Plains", 0},
                {new int[] {1, 2, 3}, 5, "Plains", 1},
                {new int[] {2, 3, 3}, 5, "Plains", 2},
                {new int[] {3, 4, 5}, 5, "Plains", 3},
                {new int[] {3, 4, 5}, 1, "Plains", 1},
                {new int[] {3, 4, 5}, 3, "Hallowed Fountain", 3}
        };
    }

    @Test(dataProvider = "searches")
    public void searchesForPlains(final int[] opposingLands, final int plains,
                                 final String plainsName, final int expected) {
        setup(opposingLands, plains, plainsName);
        castAndSearch(expected);
    }
}
